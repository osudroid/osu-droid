package ru.nsu.ccfit.zuev.osu.menu;

import com.edlplan.ext.EdExtensionHelper;
import com.edlplan.ui.fragment.FavoriteManagerFragment;
import com.edlplan.ui.fragment.PropsMenuFragment;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;

import java.io.File;

import ru.nsu.ccfit.zuev.osu.BeatmapProperties;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainScene;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.helper.InputManager;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineMapInfo;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

public class PropsMenu implements IUpdateHandler, IPropsMenu {
    private static IPropsMenu instance;
    private Scene scene = null;
    private ChangeableText filterText;
    private String offset = "0";
    private SongMenu menu;
    private boolean favorite = false;
    private ChangeableText favs;
    private ChangeableText delText;
    private ChangeableText mapUpdate;
    private boolean requestDelete = false;
    private boolean mapUpdatable = false;
    private MenuItem item = null;

    private PropsMenu() {
    }

    public static IPropsMenu getInstance() {
        if (instance == null) {
            instance = new PropsMenuFragment();
        }
        return instance;
    }

    public void setUpdatable(OnlineMapInfo p, String mapState) {
        if (mapState.equals("Offline")) return;
        mapUpdatable = mapState.equals("Long press beatmap to update");
        mapUpdate = new ChangeableText(Utils.toRes(10), Utils.toRes(480),
                ResourceManager.getInstance().getFont("CaptionFont"),
                mapUpdatable ? StringTable.get(R.string.menu_map_updatable) : (mapState + " beatmap"), 100) {


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown() && mapUpdatable) {
                    if (p.updateMap()) {
                        File path;
                        ToastLogger.showText(StringTable.get(R.string.message_update_download_success), false);
                        saveChanges();
                        menu.getScene().clearChildScene();
                        scene = null;
                        new AsyncTaskLoader().execute(new OsuAsyncCallback() {
                            final File path = new File(menu.getSelectedTrack().getFilename()).getParentFile();

                            public void run() {
                                GlobalManager.getInstance().getEngine().setScene(new LoadingScreen().getScene());
                                LibraryManager.getInstance().updateMapSet(path, menu.getSelectedTrack().getBeatmap());
                                GlobalManager.getInstance().getSongMenu().reload();
                            }

                            public void onComplete() {
                                GlobalManager.getInstance().getMainScene().musicControl(MainScene.MusicOption.PLAY);
                                GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getSongMenu().getScene());
                                for (MenuItem i : GlobalManager.getInstance().getSongMenu().getMenuItems()) {
                                    if (i.getBeatmap().getPath().equals(path.getPath())) {
                                        i.select(false, true);
                                        break;
                                    }
                                }
                                GlobalManager.getInstance().getSongMenu().toggleScoringSwitcher();
                            }
                        });
                        return false;
                    }
                    setText(StringTable.get(R.string.message_update_download_fail));
                    mapUpdate.setPosition(Config.getRES_WIDTH() / 2 - mapUpdate.getWidth() / 2, mapUpdate.getY());
                }
                return false;
            }
        };
        mapUpdate.setColor(1, 1, 1);
        mapUpdate.setPosition(Config.getRES_WIDTH() / 2 - mapUpdate.getWidth() / 2, mapUpdate.getY());

        scene.attachChild(mapUpdate);
        scene.registerTouchArea(mapUpdate);
    }

    public void reload() {
        favorite = false;
        offset = "0";
        init();
    }

    public void show(SongMenu menu, MenuItem item, OnlineMapInfo p, String mapState) {
        setSongMenu(menu);
        setMenuItem(item);
        setUpdatable(p, mapState);
        menu.scene.setChildScene(getScene(), false, true,
                true);
    }

    public void setSongMenu(final SongMenu menu) {
        this.menu = menu;
    }

    public void setMenuItem(final MenuItem item) {
        this.item = item;
        if (scene == null) {
            init();
        }
        final BeatmapProperties props = PropertiesLibrary.getInstance()
                .getProperties(item.getBeatmap().getPath());
        if (props == null) {
            offset = "0";
            favorite = false;
        } else {
            offset = String.valueOf(props.getOffset());
            favorite = props.isFavorite();
        }

        // InputManager.getInstance().setText(offset);
        filterText.setText(offset);

        if (favorite == false) {
            favs.setText(StringTable.get(R.string.menu_properties_tofavs));
            favs.setColor(0, 1, 0);
        } else {
            favs.setText(StringTable.get(R.string.menu_properties_fromfavs));
            favs.setColor(1, 0, 0);
        }
        favs.setPosition(Config.getRES_WIDTH() / 2 - favs.getWidth() / 2,
                favs.getY());

        delText.setText(StringTable.get(R.string.menu_properties_delete));
        delText.setColor(1, 1, 1);
        delText.setPosition(Config.getRES_WIDTH() / 2 - delText.getWidth() / 2,
                delText.getY());
        requestDelete = false;
    }

    public void saveChanges() {
        if (item == null) {
            return;
        }
        final BeatmapProperties props = new BeatmapProperties();
        props.favorite = favorite;
        int off = 0;
        try {
            off = Integer.parseInt(offset);
            off = (int) (Math.signum(off) * Math.min(250, Math.abs(off)));
        } catch (final NumberFormatException e) {
            off = 0;
        }
        props.setOffset(off);
        PropertiesLibrary.getInstance().setProperties(
                item.getBeatmap().getPath(), props);
        item.setFavorite(favorite);
        PropertiesLibrary.getInstance().saveAsync();
    }

    public void init() {
        scene = new Scene();
        scene.setBackgroundEnabled(false);
        final Rectangle bg = new Rectangle(Config.getRES_WIDTH() / 4, 0,
                Config.getRES_WIDTH() / 2, Config.getRES_HEIGHT());
        bg.setColor(0, 0, 0, 0.7f);
        scene.attachChild(bg);

        final Text caption = new Text(0, Utils.toRes(60), ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_properties_title));
        caption.setPosition(Config.getRES_WIDTH() / 2 - caption.getWidth() / 2,
                caption.getY());
        scene.attachChild(caption);

        final Font font = ResourceManager.getInstance().getFont("font");

        final Text capt1 = new Text(Utils.toRes(355), Utils.toRes(172), font,
                StringTable.get(R.string.menu_properties_offset));
        capt1.setPosition(Config.getRES_WIDTH() / 2 - capt1.getWidth(), capt1.getY());
        scene.attachChild(capt1);

        final Rectangle filterBorder = new Rectangle(capt1.getX()
                + capt1.getWidth() + 10, Utils.toRes(155), Utils.toRes(110),
                capt1.getHeight() + Utils.toRes(30));
        scene.attachChild(filterBorder);
        filterBorder.setColor(1, 150f / 255, 0);
        filterBorder.setVisible(false);
        final Rectangle filterBg = new Rectangle(filterBorder.getX() + 5,
                Utils.toRes(160), Utils.toRes(100),
                capt1.getHeight() + Utils.toRes(20)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    filterBorder.setVisible(true);
                    InputManager.getInstance().startInput(offset, 6);
                    return true;
                }
                return false;
            }
        };
        scene.registerTouchArea(filterBg);
        scene.attachChild(filterBg);

        filterText = new ChangeableText(filterBg.getX(), Utils.toRes(172),
                font, offset, 6);
        filterText.setColor(0, 0, 0);
        scene.attachChild(filterText);

        final Text back = new Text(0, Utils.toRes(580), ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_mod_back)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (menu != null) {
                        saveChanges();
                        menu.getScene().clearChildScene();
                        scene = null;
                        menu.setRank();
                    }
                    return true;
                }
                return false;
            }
        };
        back.setPosition(Config.getRES_WIDTH() / 2 - back.getWidth() / 2,
                back.getY());
        back.setScale(1.5f);
        scene.attachChild(back);
        scene.registerTouchArea(back);

        favs = new ChangeableText(Utils.toRes(10), Utils.toRes(320),
                ResourceManager.getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_properties_fromfavs)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (favorite) {
                        setText(StringTable
                                .get(R.string.menu_properties_tofavs));
                        setPosition(Config.getRES_WIDTH() / 2 - getWidth() / 2,
                                getY());
                        setColor(0, 1, 0);
                    } else {
                        setText(StringTable
                                .get(R.string.menu_properties_fromfavs));
                        setPosition(Config.getRES_WIDTH() / 2 - getWidth() / 2,
                                getY());
                        setColor(1, 0, 0);
                    }
                    favorite = !favorite;
                }
                return false;
            }
        };
        favs.setColor(1, 0, 0);
        if (favorite == false) {
            favs.setText(StringTable.get(R.string.menu_properties_tofavs));
            favs.setColor(0, 1, 0);
        }
        favs.setPosition(Config.getRES_WIDTH() / 2 - favs.getWidth() / 2,
                favs.getY());
        favs.detachSelf();
        scene.attachChild(favs);
        scene.registerTouchArea(favs);

        delText = new ChangeableText(Utils.toRes(10), Utils.toRes(400),
                ResourceManager.getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_properties_delete), 100) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (requestDelete) {
                        setText("");
                        setPosition(-200, -200);
                        if (item != null) {
                            item.delete();
                        }
                    } else {
                        setText(StringTable
                                .get(R.string.menu_properties_delete_confirm));
                        setPosition(Config.getRES_WIDTH() / 2 - getWidth() / 2,
                                getY());
                        setColor(1, 0, 0);
                    }
                    requestDelete = !requestDelete;
                }
                return false;
            }
        };
        delText.setColor(1, 1, 1);
        delText.setText(StringTable.get(R.string.menu_properties_delete));
        delText.setPosition(Config.getRES_WIDTH() / 2 - delText.getWidth() / 2,
                delText.getY());
        scene.attachChild(delText);
        scene.registerTouchArea(delText);

        {
            ChangeableText sbText = new ChangeableText(Utils.toRes(10), Utils.toRes(250),
                    ResourceManager.getInstance().getFont("CaptionFont"),
                    StringTable.get(R.string.favorite_manage), 100) {
                boolean startStoryboard = false;

                @Override
                public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                             final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                    if (pSceneTouchEvent.isActionDown()) {
                        GlobalManager.getInstance().getMainActivity().runOnUiThread(() -> {
                            FavoriteManagerFragment dialog = new FavoriteManagerFragment();
                            dialog.showToAddToFloder(ScoreLibrary.getTrackDir(GlobalManager.getInstance().getSelectedTrack().getFilename()));
                        });
                        return true;
                    }
                    return false;
                }
            };
            sbText.setColor(1, 1, 1);
            sbText.setText(StringTable.get(R.string.favorite_manage));
            sbText.setScale(0.9f);
            sbText.setPosition(Config.getRES_WIDTH() / 2 - sbText.getWidth() / 2,
                    sbText.getY());
            scene.attachChild(sbText);
            scene.registerTouchArea(sbText);
        }

        if (Config.isEnableExtension()) {
            ChangeableText sbText = new ChangeableText(Utils.toRes(10), Utils.toRes(250),
                    ResourceManager.getInstance().getFont("CaptionFont"),
                    StringTable.get(R.string.message_open_beatmap_in_extension), 100) {
                boolean startStoryboard = false;

                @Override
                public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                             final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                    if (pSceneTouchEvent.isActionDown()) {
                        if (GlobalManager.getInstance().getSelectedTrack() != null) {
                            EdExtensionHelper.openBeatmap(GlobalManager.getInstance().getSelectedTrack().getFilename());
                        }
                        return true;
                    }
                    return false;
                }
            };
            sbText.setColor(1, 1, 1);
            sbText.setText(StringTable.get(R.string.message_open_beatmap_in_extension));
            sbText.setScale(0.9f);
            sbText.setPosition(Config.getRES_WIDTH() / 2 - sbText.getWidth() / 2,
                    sbText.getY());
            scene.attachChild(sbText);
            scene.registerTouchArea(sbText);
        }

        scene.registerUpdateHandler(this);
        scene.setTouchAreaBindingEnabled(true);
    }

    public Scene getScene() {
        if (scene == null) {
            init();
        }
        return scene;
    }

    public void release() {
        // scene = null;
    }


    public void onUpdate(final float pSecondsElapsed) {
        if (InputManager.getInstance().isChanged()) {
            offset = InputManager.getInstance().getText();
            if (offset.length() > 0) {
                filterText.setText(offset);
            }
        }
    }


    public void reset() {
        // TODO Auto-generated method stub

    }

}
