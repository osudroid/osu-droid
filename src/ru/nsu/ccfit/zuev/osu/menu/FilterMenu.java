package ru.nsu.ccfit.zuev.osu.menu;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.edlplan.ui.fragment.FavoriteManagerFragment;
import com.edlplan.ui.fragment.FilterMenuFragment;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.InputManager;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.helper.TextButton;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu.SortOrder;
import ru.nsu.ccfit.zuev.osuplus.R;

public class FilterMenu implements IUpdateHandler, IFilterMenu {
    private Context configContext = null;
    private static IFilterMenu instance;
    private Scene scene = null;
    private ChangeableText filterText;
    private String filter = "";
    private ChangeableText sortText;
    private SongMenu menu;
    private SortOrder order = SortOrder.Title;
    private boolean favoritesOnly = false;
    private String favoriteFolder = null;

    private FilterMenu() {
    }

    public static IFilterMenu getInstance() {
        if (instance == null) {
            instance = new FilterMenuFragment();
        }
        return instance;
    }

    @Override
    public String getFavoriteFolder() {
        return favoriteFolder;
    }

    public void loadConfig(final Context context) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
    
        final int sortOrder = prefs.getInt("sortorder", 0);
        switch (sortOrder) {
            case 1:
                order = SortOrder.Artist;
                break;
            case 2:
                order = SortOrder.Creator;
                break;
            case 3:
                order = SortOrder.Date;
                break;
            case 4:
                order = SortOrder.Bpm;
                break;
            case 5:
                order = SortOrder.Stars;
                break;
            case 6:
                order = SortOrder.Length;
                break;
            default:
                order = SortOrder.Title;
                break;
        }
        configContext = context;
        setSortText();
    }

    public void saveConfig() {
        if (configContext == null) {
            return;
        }
    
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(configContext);
        final SharedPreferences.Editor editor = prefs.edit();
    
        switch (order) {
            case Artist:
                editor.putInt("sortorder", 1);
                break;
            case Creator:
                editor.putInt("sortorder", 2);
                break;
            case Date:
                editor.putInt("sortorder", 3);
                break;
            case Bpm:
                editor.putInt("sortorder", 4);
                break;
            case Stars:
                editor.putInt("sortorder", 5);
                break;
            case Length:
                editor.putInt("sortorder", 6);
                break;
            default:
                editor.putInt("sortorder", 0);
                break;
        }
    
        editor.apply();
    }

    public void setSongMenu(final SongMenu menu) {
        this.menu = menu;
    }

    public void reload() {
        init();
    }

    public void init() {
        scene = new Scene();
        scene.setBackgroundEnabled(false);
        final Rectangle bg = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT());
        bg.setColor(0, 0, 0, 0.7f);
        scene.attachChild(bg);

        final Text caption = new Text(0, Utils.toRes(60), ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_search_title));
        caption.setPosition(Config.getRES_WIDTH() / 2f - caption.getWidth() / 2,
                caption.getY());
        scene.attachChild(caption);

        final Font font = ResourceManager.getInstance().getFont("font");

        final Text capt1 = new Text(Utils.toRes(100), Utils.toRes(160), font,
                StringTable.get(R.string.menu_search_filter));
        capt1.setPosition(Config.getRES_WIDTH() / 4f - capt1.getWidth(), capt1.getY());
        scene.attachChild(capt1);

        final Rectangle filterBorder = new Rectangle(capt1.getX(),
                Utils.toRes(195), Utils.toRes(330), capt1.getHeight()
                + Utils.toRes(30));
        scene.attachChild(filterBorder);
        filterBorder.setColor(1, 150f / 255, 0);
        filterBorder.setVisible(false);
        final Rectangle filterBg = new Rectangle(filterBorder.getX() + 5,
                Utils.toRes(200), Utils.toRes(320), capt1.getHeight()
                + Utils.toRes(20)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    filterBorder.setVisible(true);
                    InputManager.getInstance().startInput(
                            FilterMenu.getInstance().getFilter(), 20);
                    return true;
                }
                return false;
            }
        };
        scene.registerTouchArea(filterBg);
        scene.attachChild(filterBg);

        filterText = new ChangeableText(capt1.getX(), Utils.toRes(210), font,
                filter, 21);
        filterText.setColor(0, 0, 0);
        scene.attachChild(filterText);

        final Text capt2 = new Text(Utils.toRes(700), Utils.toRes(160), font,
                StringTable.get(R.string.menu_search_sort));
        capt2.setPosition(Config.getRES_WIDTH() * 2f / 3 - capt2.getWidth(), capt2.getY());
        scene.attachChild(capt2);

        final Rectangle sortBg = new Rectangle(capt2.getX(), Utils.toRes(200),
                Utils.toRes(200), capt2.getHeight() + Utils.toRes(20)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    SortOrder newOrder;
                    switch (order) {
                        case Title:
                            newOrder = SortOrder.Artist;
                            break;
                        case Artist:
                            newOrder = SortOrder.Creator;
                            break;
                        case Creator:
                            newOrder = SortOrder.Date;
                            break;
                        case Date:
                            newOrder = SortOrder.Bpm;
                            break;
                        case Bpm:
                            newOrder = SortOrder.Stars;
                            break;
                        case Stars:
                            newOrder = SortOrder.Length;
                            break;
                        default:
                            newOrder = SortOrder.Title;
                            break;
                    }
                    order = newOrder;
                    setSortText();
                    saveConfig();
                    return true;
                }
                return false;
            }
        };
        scene.registerTouchArea(sortBg);
        scene.attachChild(sortBg);
        sortText = new ChangeableText(capt2.getX() + 5,
                Utils.toRes(210), font,
                StringTable.get(R.string.menu_search_sort_title), 10);
        sortText.setColor(0, 0, 0);
        setSortText();
        sortText.detachSelf();
        scene.attachChild(sortText);

        final TextButton back = new TextButton(ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_mod_back)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionUp()) {
                    hideMenu();
                    return true;
                }
                return false;
            }
        };
        back.setWidth(Utils.toRes(400));
        back.setScale(1.2f);
        back.setPosition(Config.getRES_WIDTH() / 2f - back.getWidth() / 2, Config.getRES_HEIGHT() * 3f / 4 - back.getHeight() / 2);
        back.setColor(66 / 255f, 76 / 255f, 80 / 255f);
        scene.attachChild(back);
        scene.registerTouchArea(back);

        final ChangeableText favs = new ChangeableText(capt1.getX(),
                Utils.toRes(300), ResourceManager.getInstance().getFont(
                "CaptionFont"),
                StringTable.get(R.string.menu_search_favsdisabled)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionUp()) {
                    if (favoritesOnly) {
                        setText(StringTable
                                .get(R.string.menu_search_favsdisabled));
                        setColor(1, 1, 1);
                    } else {
                        setText(StringTable
                                .get(R.string.menu_search_favsenabled));
                        setColor(0, 1, 0);
                    }
                    favoritesOnly = !favoritesOnly;
                    return true;
                }
                return false;
            }
        };
        if (favoritesOnly) {
            favs.setText(StringTable.get(R.string.menu_search_favsenabled));
            favs.setColor(0, 1, 0);
        }
        favs.setPosition(capt1.getX(),
                favs.getY());
        scene.attachChild(favs);
        scene.registerTouchArea(favs);


        final ChangeableText folder = new ChangeableText(favs.getX(),
                favs.getY() + favs.getHeight() + Utils.toRes(20), ResourceManager.getInstance().getFont(
                "CaptionFont"),
                StringTable.get(R.string.favorite_folder) + " " + (favoriteFolder == null ? StringTable.get(R.string.favorite_default) : favoriteFolder), 40) {
            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionUp()) {
                    //显示选择收藏夹的dialog
                    GlobalManager.getInstance().getMainActivity().runOnUiThread(() -> {
                        FavoriteManagerFragment dialog = new FavoriteManagerFragment();
                        dialog.showToSelectFloder(folder1 -> {
                            favoriteFolder = folder1;
                            this.setText(StringTable.get(R.string.favorite_folder) + " " + (favoriteFolder == null ? StringTable.get(R.string.favorite_default) : favoriteFolder));
                        });
                    });
                    return true;
                }
                return false;
            }
        };

        folder.setPosition(favs.getX(),
                favs.getY() + favs.getHeight() + Utils.toRes(20));
        scene.attachChild(folder);
        scene.registerTouchArea(folder);

        scene.registerUpdateHandler(this);
        scene.setTouchAreaBindingEnabled(true);
    }

    public void hideMenu() {
        if (menu != null) {
            menu.getScene().clearChildScene();
            menu.loadFilter(this);
            scene = null;
        }
    }

    @Override
    public void showMenu(SongMenu parent) {
        reload();
        setSongMenu(parent);
        parent.scene.setChildScene(
                scene, false,
                true, true);
    }

    private void setSortText() {
        if (sortText == null) {
            return;
        }
        String s;
        switch (order) {
            case Title:
                s = StringTable.get(R.string.menu_search_sort_title);
                break;
            case Artist:
                s = StringTable.get(R.string.menu_search_sort_artist);
                break;
            case Date:
                s = StringTable.get(R.string.menu_search_sort_date);
                break;
            case Bpm:
                s = StringTable.get(R.string.menu_search_sort_bpm);
                break;
            case Stars:
                s = StringTable.get(R.string.menu_search_sort_stars);
                break;
            case Length:
                s = StringTable.get(R.string.menu_search_sort_length);
                break;
            default:
                s = StringTable.get(R.string.menu_search_sort_creator);
                break;
        }
        sortText.setText(s);
    }

    public Scene getScene() {
        if (scene == null) {
            init();
        }
        return scene;
    }

    public void onUpdate(final float pSecondsElapsed) {
        if (InputManager.getInstance().isChanged()) {
            filter = InputManager.getInstance().getText();
            filterText.setText(filter);
        }
    }


    public void reset() {
        // TODO Auto-generated method stub

    }

    public SortOrder getOrder() {
        return order;
    }

    public String getFilter() {
        return filter;
    }

    public boolean isFavoritesOnly() {
        return favoritesOnly;
    }

}
