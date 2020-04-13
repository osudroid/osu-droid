package ru.nsu.ccfit.zuev.osu.menu;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Process;

import com.edlplan.ext.EdExtensionHelper;
import com.edlplan.favorite.FavoriteLibrary;
import com.edlplan.replay.OdrDatabase;
import com.edlplan.ui.fragment.PropsMenuFragment;
import com.edlplan.ui.fragment.ScoreMenuFragment;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.util.MathUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.SkinJson;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;
import ru.nsu.ccfit.zuev.osu.online.OnlineMapInfo;
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SongMenu implements IUpdateHandler, MenuItemListener,
        IScrollBarListener {
    private final static Boolean musicMutex = new Boolean(true);
    private final Boolean backgroundMutex = new Boolean(true);
    public Scene scene;
    public Entity frontLayer = new Entity();
    SortOrder sortOrder = SortOrder.Title;
    private Engine engine;
    private GameScene game;
    private ScoringScene scorescene;
    private float camY = 0;
    private float velocityY;
    private Activity context;
    private Entity backLayer = new Entity();
    private ArrayList<MenuItem> items = new ArrayList<MenuItem>();
    private MenuItem selectedItem = null;
    private TrackInfo selectedTrack;
    private Sprite bg = null;
    private Boolean bgLoaded = new Boolean(false);
    private String bgName = "";
    private ScoreBoard board;
    private Float touchY = null;
    private String filterText = "";
    private boolean favsOnly = false;
    private Set<String> limitC;
    private float secondsSinceLastSelect = 0;
    private float maxY = 100500;
    private int pointerId = -1;
    private float initalY = -1;
    private float secPassed = 0, tapTime;
    private Sprite backButton = null;
    private ScrollBar scrollbar;
    private ChangeableText trackInfo, mapper, beatmapInfo, beatmapInfo2, dimensionInfo;
    private boolean isSelectComplete = true;
    private OnlineMapInfo ppy = null;
    private HashMap<Integer, String> mapStateHashmap = new HashMap<Integer, String>();
    private int mapState;
    private AnimSprite scoringSwitcher = null;
    private AsyncTask<OsuAsyncCallback, Integer, Boolean> boardTask;

    public SongMenu() {
    }

    public static void stopMusicStatic() {
        synchronized (musicMutex) {
            if (GlobalManager.getInstance().getSongService() != null) {
                GlobalManager.getInstance().getSongService().stop();
            }
        }
    }

    public void setScoringScene(final ScoringScene ss) {
        scorescene = ss;
    }

    public int getMapState() {
        return mapState;
    }

    public ScoreBoard.ScoreBoardItems[] getBoard() {
        return board.getScoreBoardItems();
    }

    public boolean isBoardOnline() {
        return board.isShowOnlineScores();
    }

    public ArrayList<MenuItem> getMenuItems() {
        return items;
    }

    public OnlineMapInfo getOnlineMapInfo() {
        return ppy;
    }

    public void init(final Activity context, final Engine engine,
                     final GameScene pGame) {
        this.engine = engine;
        game = pGame;
        this.context = context;
    }

    public void loadFilter(IFilterMenu filterMenu) {
        setFilter(filterMenu.getFilter(), filterMenu.getOrder(), filterMenu.isFavoritesOnly(),
                filterMenu.getFavoriteFolder() == null ? null : FavoriteLibrary.get().getMaps(filterMenu.getFavoriteFolder()));
    }

    public void reload() {
        frontLayer = new Entity();
        backLayer = new Entity();
        scene.unregisterUpdateHandler(this);
        scene.setTouchAreaBindingEnabled(false);
        load();
        GlobalManager.getInstance().getGameScene().setOldScene(scene);
    }

    public synchronized void load() {
        ppy = new OnlineMapInfo();
        mapStateHashmap.put(0, "Offline");
        mapStateHashmap.put(1, "Loading");
        mapStateHashmap.put(2, "Ranked");
        mapStateHashmap.put(3, "Latest pending");
        mapStateHashmap.put(4, "Loved");
        mapStateHashmap.put(5, "Unsubmitted");
        mapStateHashmap.put(6, "Updatable");
        mapStateHashmap.put(7, "Unknown");
        mapState = 7;
        scene = new Scene();
        camY = 0;
        velocityY = 0;
        selectedItem = null;
        items = new ArrayList<MenuItem>();
        selectedTrack = null;
        bgLoaded = true;
        SongMenuPool.getInstance().init();
        FilterMenu.getInstance().loadConfig(context);
        ModMenu.getInstance().reload();
        bindDataBaseChangedListener();

        scene.attachChild(backLayer);
        scene.attachChild(frontLayer);

        final TextureRegion tex = ResourceManager.getInstance().getTexture("menu-background");
        float height = tex.getHeight();
        height *= Config.getRES_WIDTH() / (float) tex.getWidth();
        final Sprite bg = new Sprite(0, (Config.getRES_HEIGHT() - height) / 2,
                Config.getRES_WIDTH(), height, tex);
        scene.setBackground(new SpriteBackground(bg));

        final Rectangle bgDimRect = new Rectangle(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        bgDimRect.setColor(0, 0, 0, 0.2f);
        backLayer.attachChild(bgDimRect);

        board = new ScoreBoard(scene, backLayer, this, context);

        float oy = 10;
        for (final BeatmapInfo i : LibraryManager.getInstance().getLibrary()) {
            final MenuItem item = new MenuItem(this, i, 400, oy);
            items.add(item);
            item.attachToScene(scene, backLayer);
            oy += item.getHeight();
        }
        sortOrder = SortOrder.Title;
        sort();

        if (items.size() == 0) {
            final Text text = new Text(0, 0, ResourceManager.getInstance()
                    .getFont("CaptionFont"), "There are no songs in library",
                    HorizontalAlign.CENTER);
            text.setPosition(Config.getRES_WIDTH() / 2 - text.getWidth() / 2,
                    Config.getRES_HEIGHT() / 2 - text.getHeight() / 2);
            text.setScale(1.5f);
            text.setColor(0, 0, 0);
            scene.attachChild(text);
            return;
        }

        scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
            public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent evt) {
                if (evt.getX() < Config.getRES_WIDTH() / 5 * 2) {
                    return false;
                }
                switch (evt.getAction()) {
                    case (TouchEvent.ACTION_DOWN):
                        velocityY = 0;
                        touchY = evt.getY();
                        pointerId = evt.getPointerID();
                        tapTime = secPassed;
                        initalY = touchY;
                        break;
                    case (TouchEvent.ACTION_MOVE):
                        if (pointerId != -1 && pointerId != evt.getPointerID()) {
                            break;
                        }
                        if (initalY == -1) {
                            velocityY = 0;
                            touchY = evt.getY();
                            initalY = touchY;
                            tapTime = secPassed;
                            pointerId = evt.getPointerID();
                        }
                        final float dy = evt.getY() - touchY;

                        camY -= dy;
                        touchY = evt.getY();
                        if (camY <= -Config.getRES_HEIGHT() / 2) {
                            camY = -Config.getRES_HEIGHT() / 2;
                            velocityY = 0;
                        } else if (camY >= maxY) {
                            camY = maxY;
                            velocityY = 0;
                        }

                        // velocityY = -3f * dy;
                        break;
                    default: {
                        if (pointerId != -1 && pointerId != evt.getPointerID()) {
                            break;
                        }
                        touchY = null;
                        if (secPassed - tapTime < 0.001f || initalY == -1) {
                            velocityY = 0;
                        } else {
                            velocityY = (initalY - evt.getY())
                                    / (secPassed - tapTime);
                        }
                        pointerId = -1;
                        initalY = -1;

                    }
                    break;
                }
                return true;
            }
        });

        scene.registerUpdateHandler(this);
        scene.setTouchAreaBindingEnabled(true);

        scrollbar = new ScrollBar(scene, this);

		/*final Rectangle bgTopRect = new Rectangle(0, 0, Config.getRES_WIDTH(), Utils.toRes(170));
		bgTopRect.setColor(0, 0, 0, 0.8f);*/
        final TextureRegion songSelectTopTexture = ResourceManager.getInstance().getTexture("songselect-top");
        final Sprite songSelectTop = new Sprite(0, 0, songSelectTopTexture);
        songSelectTop.setSize(songSelectTopTexture.getWidth() * songSelectTopTexture.getHeight() / 184, 184);
        songSelectTop.setPosition(-1640, songSelectTop.getY());
        songSelectTop.setAlpha(0.6f);
        frontLayer.attachChild(songSelectTop);

		/*final Rectangle bgbottomRect = new Rectangle(0, 0, Config.getRES_WIDTH(), Utils.toRes(110));
		bgbottomRect.setPosition(0, Config.getRES_HEIGHT() - bgbottomRect.getHeight());
		bgbottomRect.setColor(0, 0, 0, 0.8f);
		frontLayer.attachChild(bgbottomRect);*/

        trackInfo = new ChangeableText(Utils.toRes(70), Utils.toRes(2),
                ResourceManager.getInstance().getFont("font"), "title", 1024);
        frontLayer.attachChild(trackInfo);

        mapper = new ChangeableText(Utils.toRes(70), trackInfo.getY() + trackInfo.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("middleFont"), "mapper", 1024);
        frontLayer.attachChild(mapper);

        beatmapInfo = new ChangeableText(Utils.toRes(4), mapper.getY() + mapper.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("middleFont"), "beatmapInfo", 1024);
        frontLayer.attachChild(beatmapInfo);

        beatmapInfo2 = new ChangeableText(Utils.toRes(4), beatmapInfo.getY() + beatmapInfo.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("middleFont"), "beatmapInfo2", 1024);
        frontLayer.attachChild(beatmapInfo2);

        dimensionInfo = new ChangeableText(Utils.toRes(4), beatmapInfo2.getY() + beatmapInfo2.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("smallFont"), "dimensionInfo", 1024);
        frontLayer.attachChild(dimensionInfo);

        SkinJson.Layout layoutBackButton = SkinJson.get().getLayout("BackButton");
        SkinJson.Layout layoutMods = SkinJson.get().getLayout("ModsButton");
        SkinJson.Layout layoutOptions = SkinJson.get().getLayout("OptionsButton");
        SkinJson.Layout layoutRandom = SkinJson.get().getLayout("RandomButton");

        if (ResourceManager.getInstance().isTextureLoaded("menu-back-0")) {
            List<String> loadedBackTextures = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                if (ResourceManager.getInstance().isTextureLoaded("menu-back-" + i))
                    loadedBackTextures.add("menu-back-" + i);
            }
            backButton = new AnimSprite(0, 0, loadedBackTextures.size(), loadedBackTextures.toArray(new String[loadedBackTextures.size()])) {
                boolean moved = false;
                float dx = 0, dy = 0;
                boolean scaleWhenHold = true;

                {
                    if (layoutBackButton != null) {
                        scaleWhenHold = layoutBackButton.property.optBoolean("scaleWhenHold", true);
                    }
                }

                @Override
                public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                             final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                    if (pSceneTouchEvent.isActionDown()) {
                        if (scaleWhenHold) backButton.setScale(1.25f);
                        moved = false;
                        dx = pTouchAreaLocalX;
                        dy = pTouchAreaLocalY;
                        BassSoundProvider playSnd = ResourceManager.getInstance().getSound("menuback");
                        if (playSnd != null) {
                            playSnd.play();
                        }
                        return true;
                    }
                    if (pSceneTouchEvent.isActionUp()) {
                        // back
                        if (selectedTrack == null) {
                            return true;
                        }
                        if (moved == false) {
                            backButton.setScale(1f);
                            unbindDataBaseChangedListener();
                            GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getMainScene().getScene());
                            GlobalManager.getInstance().getSongService().setGaming(false);
                            GlobalManager.getInstance().getMainScene().setBeatmap(selectedTrack.getBeatmap());
                        }
                        return true;
                    }
                    if (pSceneTouchEvent.isActionOutside()
                            || pSceneTouchEvent.isActionMove()
                            && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                            pTouchAreaLocalY) > 50)) {
                        backButton.setScale(1f);
                        moved = true;
                    }
                    return false;
                }
            };
        } else {
            backButton = new Sprite(0, 0, ResourceManager.getInstance().getTexture("menu-back")) {
                boolean moved = false;
                float dx = 0, dy = 0;
                boolean scaleWhenHold = true;

                {
                    if (layoutBackButton != null) {
                        scaleWhenHold = layoutBackButton.property.optBoolean("scaleWhenHold", true);
                    }
                }

                @Override
                public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                             final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                    if (pSceneTouchEvent.isActionDown()) {
                        if (scaleWhenHold) backButton.setScale(1.25f);
                        moved = false;
                        dx = pTouchAreaLocalX;
                        dy = pTouchAreaLocalY;
                        BassSoundProvider playSnd = ResourceManager.getInstance().getSound("menuback");
                        if (playSnd != null) {
                            playSnd.play();
                        }
                        return true;
                    }
                    if (pSceneTouchEvent.isActionUp()) {
                        // back
                        if (selectedTrack == null) {
                            return true;
                        }
                        if (moved == false) {
                            backButton.setScale(1f);
                            unbindDataBaseChangedListener();
                            GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getMainScene().getScene());
                            GlobalManager.getInstance().getSongService().setGaming(false);
                            GlobalManager.getInstance().getMainScene().setBeatmap(selectedTrack.getBeatmap());
                        }
                        return true;
                    }
                    if (pSceneTouchEvent.isActionOutside()
                            || pSceneTouchEvent.isActionMove()
                            && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                            pTouchAreaLocalY) > 50)) {
                        backButton.setScale(1f);
                        moved = true;
                    }
                    return false;
                }
            };
        }

        final AnimSprite modSelection = new AnimSprite(0, 0, 0,
                "selection-mods", "selection-mods-over") {
            boolean moved = false;
            private float dx = 0, dy = 0;


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setFrame(1);
                    moved = false;
                    dx = pTouchAreaLocalX;
                    dy = pTouchAreaLocalY;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setFrame(0);
                    if (moved == false) {
                        velocityY = 0;
                        ModMenu.getInstance().show(SongMenu.this, selectedTrack);
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50)) {
                    moved = true;
                    setFrame(0);
                }
                return false;
            }
        };

        final AnimSprite optionSelection = new AnimSprite(0, 0, 0,
                "selection-options", "selection-options-over") {
            boolean moved = false;
            private float dx = 0, dy = 0;


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setFrame(1);
                    moved = false;
                    dx = pTouchAreaLocalX;
                    dy = pTouchAreaLocalY;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {

                    setFrame(0);
                    if (moved == false) {
                        velocityY = 0;

                        FilterMenu.getInstance().showMenu(SongMenu.this);
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50)) {
                    moved = true;
                    setFrame(0);
                }
                return false;
            }
        };

        final AnimSprite randomMap = new AnimSprite(0, 0, 0,
                "selection-random", "selection-random-over") {
            boolean moved = false;
            private float dx = 0, dy = 0;


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setFrame(1);
                    moved = false;
                    dx = pTouchAreaLocalX;
                    dy = pTouchAreaLocalY;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setFrame(0);
                    if (isSelectComplete == false) {
                        return true;
                    }
                    if (moved == false) {
                        velocityY = 0;
                        if (items.size() <= 1) {
                            return true;
                        }
                        int rnd = MathUtils.random(0, items.size() - 1);
                        int index = 0;
                        while (rnd > 0) {
                            rnd--;
                            int oldIndex = index;
                            do {
                                index = (index + 1) % items.size();
                                if (index == oldIndex)
                                    return true;
                            } while (items.get(index).isVisible() == false);
                        }
                        if (items.get(index).isVisible() == false) {
                            return true;
                        }
                        if (selectedItem == items.get(index)) {
                            return true;
                        }
                        ResourceManager.getInstance().getSound("menuclick")
                                .play();
                        items.get(index).select(true, true);
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50)) {
                    moved = true;
                    setFrame(0);
                }
                return false;
            }
        };


        modSelection.setScale(1.5f);
        optionSelection.setScale(1.5f);
        randomMap.setScale(1.5f);
        if (SkinJson.get().isUseNewLayout()) {
            if (layoutBackButton != null) {
                layoutBackButton.baseApply(backButton);
            }
            if (layoutMods != null) {
                layoutMods.baseApply(modSelection);
            }
            if (layoutOptions != null) {
                layoutOptions.baseApply(optionSelection);
            }
            if (layoutRandom != null) {
                layoutRandom.baseApply(randomMap);
            }
            backButton.setPosition(0, Config.getRES_HEIGHT() - backButton.getHeightScaled());
            modSelection.setPosition(backButton.getX() + backButton.getWidth(),
                    Config.getRES_HEIGHT() - modSelection.getHeightScaled());
            optionSelection.setPosition(
                    modSelection.getX() + modSelection.getWidthScaled(),
                    Config.getRES_HEIGHT() - optionSelection.getHeightScaled());
            randomMap.setPosition(
                    optionSelection.getX() + optionSelection.getWidthScaled(),
                    Config.getRES_HEIGHT() - randomMap.getHeightScaled());
        } else {
            backButton.setPosition(0, Config.getRES_HEIGHT() - backButton.getHeight());
            modSelection.setPosition(backButton.getX() + backButton.getWidth(),
                    Config.getRES_HEIGHT() - Utils.toRes(90));
            optionSelection.setPosition(
                    modSelection.getX() + modSelection.getWidthScaled(),
                    Config.getRES_HEIGHT() - Utils.toRes(90));
            randomMap.setPosition(
                    optionSelection.getX() + optionSelection.getWidthScaled(),
                    Config.getRES_HEIGHT() - Utils.toRes(90));
        }


        frontLayer.attachChild(backButton);
        scene.registerTouchArea(backButton);
        frontLayer.attachChild(modSelection);
        scene.registerTouchArea(modSelection);
        frontLayer.attachChild(optionSelection);
        scene.registerTouchArea(optionSelection);
        frontLayer.attachChild(randomMap);
        scene.registerTouchArea(randomMap);

        if (OnlineScoring.getInstance().createSecondPanel() != null) {
            OnlinePanel panel = OnlineScoring.getInstance().getSecondPanel();
            panel.detachSelf();
            panel.setPosition(randomMap.getX() + randomMap.getWidthScaled() - 18, Config.getRES_HEIGHT() - Utils.toRes(110));
            OnlineScoring.getInstance().loadAvatar(false);
            frontLayer.attachChild(panel);


            scoringSwitcher = new AnimSprite(Utils.toRes(5), Utils.toRes(10), 0, "ranking_enabled", "ranking_disabled", "ranking_ranked", "ranking_latest", "ranking_loved", "ranking_unsubmitted", "ranking_download", "ranking_unknown") {
                @Override
                public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
                                             float pTouchAreaLocalX, float pTouchAreaLocalY) {
                    if (pSceneTouchEvent.isActionDown() == false) return false;
                    board.cancleLoadAvatar();
                    toggleScoringSwitcher();
                    return true;
                }
            };
            scoringSwitcher.setFrame(1);
            scoringSwitcher.setPosition(10, 10);
            scene.registerTouchArea(scoringSwitcher);
            frontLayer.attachChild(scoringSwitcher);
        }
    }

    public void toggleScoringSwitcher() {
        if (board.isShowOnlineScores()) {
            board.setShowOnlineScores(false);
            board.init(selectedTrack);
            scoringSwitcher.setFrame(1);
            updateInfo(selectedTrack, 0);
        } else if (OnlineManager.getInstance().isStayOnline()) {
            board.setShowOnlineScores(true);
            board.init(selectedTrack);
            setRank();
        }
    }

    public void setRank() {
        if (!board.isShowOnlineScores()) {
            if (scoringSwitcher != null) {
                scoringSwitcher.setFrame(1);
            }
            updateInfo(selectedTrack, 0);
            return;
        }
        scoringSwitcher.setFrame(7);
        if (selectedTrack == null) return;
        (new Thread() {
            public void run() {
                int state = ppy.getBeatmapsStateFromHash(selectedTrack);
                mapState = state == 0 ? ppy.getBeatmapsStateFromHash(selectedTrack) : state;
                updateInfo(selectedTrack, state);
                scoringSwitcher.setFrame(state);
                ppy.setUpdateNeccessary(state == 6);
            }
        }).start();
    }

    public Scene getScene() {
        return scene;
    }

    public void setFilter(final String filter, final SortOrder order,
                          final boolean favsOnly, Set<String> limit) {
        if (order.equals(sortOrder) == false) {
            sortOrder = order;
            sort();
        }
        if (filter == null || filterText.equals(filter)) {
            if (favsOnly == this.favsOnly && limitC == limit) {
                return;
            }
        }
        limitC = limit;
        filterText = filter;
        camY = 0;
        velocityY = 0;
        final String lowerFilter = filter.toLowerCase();
        for (final MenuItem item : items) {
            item.applyFilter(lowerFilter, favsOnly, limit);
        }
        if (selectedItem != null && selectedItem.isVisible() == false) {
            selectedItem = null;
            selectedTrack = null;
        }
        this.favsOnly = favsOnly;
        System.gc();
    }

    public void sort() {
        if (sortOrder.equals(FilterMenu.getInstance().getOrder()) == false) {
            sortOrder = FilterMenu.getInstance().getOrder();
        }
        Collections.sort(items, new Comparator<MenuItem>() {


            public int compare(final MenuItem i1, final MenuItem i2) {
                String s1;
                String s2;
                switch (sortOrder) {
                    case Artist:
                        s1 = i1.getBeatmap().getArtist();
                        s2 = i2.getBeatmap().getArtist();
                        break;
                    case Creator:
                        s1 = i1.getBeatmap().getCreator();
                        s2 = i2.getBeatmap().getCreator();
                        break;
                    case Date:
                        final Long int1 = i1.getBeatmap().getDate();
                        final Long int2 = i2.getBeatmap().getDate();
                        return int2.compareTo(int1);
                    default:
                        s1 = i1.getBeatmap().getTitle();
                        s2 = i2.getBeatmap().getTitle();
                }

                return s1.compareToIgnoreCase(s2);
            }
        });
    }

    public void onUpdate(final float pSecondsElapsed) {
        secPassed += pSecondsElapsed;
        if (GlobalManager.getInstance().getSongService() != null) {
            synchronized (musicMutex) {
                if (GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING && GlobalManager.getInstance().getSongService().getVolume() < 1) {
                    float vol = Math.min(1, GlobalManager.getInstance().getSongService().getVolume() + 0.01f);
                    GlobalManager.getInstance().getSongService().setVolume(vol);
                }
            }
        }

        if (bg != null) {
            synchronized (backgroundMutex) {
                if (bg != null && bg.getRed() < 1) {
                    final float col = Math
                            .min(1, bg.getRed() + pSecondsElapsed);
                    bg.setColor(col, col, col);
                }
            }
        }

        secondsSinceLastSelect += pSecondsElapsed;
        float oy = -camY;
        for (final MenuItem item : items) {
            final float cy = oy + Config.getRES_HEIGHT() / 2 + item.getHeight()
                    / 2;
            float ox = Config.getRES_WIDTH() / 1.85f + 200 * (float) Math.abs(Math.cos(cy * Math.PI
                    / (Config.getRES_HEIGHT() * 2)));
            ox = Utils.toRes(ox);
            item.setPos(ox, oy);
            oy += item.getHeight();
        }
        oy += camY;
        camY += velocityY * pSecondsElapsed;
        maxY = oy - Config.getRES_HEIGHT() / 2;
        if (camY <= -Config.getRES_HEIGHT() / 2 && velocityY < 0
                || camY >= maxY && velocityY > 0) {
            camY -= velocityY * pSecondsElapsed;
            velocityY = 0;
        }
        if (Math.abs(velocityY) > Utils.toRes(1000) * pSecondsElapsed) {
            velocityY -= Utils.toRes(1000) * pSecondsElapsed
                    * Math.signum(velocityY);
        } else {
            velocityY = 0;
        }

        if (selectedItem != null) {
            if (selectedItem.percentAppeared < 1) {
                selectedItem.percentAppeared += 2 * pSecondsElapsed;
            } else {
                selectedItem.percentAppeared = 1;
            }
            selectedItem.update(pSecondsElapsed);
        }

        board.update(pSecondsElapsed);
        scrollbar.setPosition(camY + Config.getRES_HEIGHT() / 2, oy);
        if (Math.abs(velocityY) > Utils.toRes(500)) {
            scrollbar.setVisible(true);
        } else {
            scrollbar.setVisible(false);
        }
    }

    public void reset() {
    }

    public void select(final MenuItem item) {
        secondsSinceLastSelect = 0;
        if (selectedItem != null) {
            selectedItem.deselect();
        }

        selectedItem = item;
        velocityY = 0;
        selectedTrack = null;
        float height = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == selectedItem) {
                break;
            }
            height += items.get(i).getInitialHeight();
        }
        camY = height - Config.getRES_HEIGHT() / 2;
        camY += item.getTotalHeight() / 2;
    }

    public void changeDimensionInfo(TrackInfo track) {
        if (track == null) {
            return;
        }
        float ar = track.getApproachRate();
        float od = track.getOverallDifficulty();
        float cs = track.getCircleSize();
        float hp = track.getHpDrain();
        float bpm_max = track.getBpmMax();
        float bpm_min = track.getBpmMin();
        long length = track.getMusicLength();
        EnumSet<GameMod> mod = ModMenu.getInstance().getMod();

        dimensionInfo.setColor(1, 1, 1);
        beatmapInfo.setColor(1, 1, 1);

        if (mod.contains(GameMod.MOD_EASY)) {
            ar *= 0.5f;
            od *= 0.5f;
            cs -= 1f;
            hp *= 0.5f;
            dimensionInfo.setColor(46 / 255f, 139 / 255f, 87 / 255f);
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            ar *= 1.4f;
            od *= 1.4f;
            cs += 1f;
            hp *= 1.4f;
            dimensionInfo.setColor(205 / 255f, 85 / 255f, 85 / 255f);
        }
        if (mod.contains(GameMod.MOD_DOUBLETIME)) {
            bpm_max *= 1.5f;
            bpm_min *= 1.5f;
            length *= 2 / 3f;
            beatmapInfo.setColor(205 / 255f, 85 / 255f, 85 / 255f);
            dimensionInfo.setColor(205 / 255f, 85 / 255f, 85 / 255f);
        }
        if (mod.contains(GameMod.MOD_NIGHTCORE)) {
            bpm_max *= 1.5f;
            bpm_min *= 1.5f;
            length *= 2 / 3f;
            beatmapInfo.setColor(205 / 255f, 85 / 255f, 85 / 255f);
            dimensionInfo.setColor(205 / 255f, 85 / 255f, 85 / 255f);
        }
        if (mod.contains(GameMod.MOD_HALFTIME)) {
            bpm_max *= 0.75f;
            bpm_min *= 0.75f;
            length *= 4 / 3f;
            beatmapInfo.setColor(46 / 255f, 139 / 255f, 87 / 255f);
            dimensionInfo.setColor(46 / 255f, 139 / 255f, 87 / 255f);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        String binfoStr = String.format(StringTable.get(R.string.binfoStr1), sdf.format(length),
                (bpm_min == bpm_max ? GameHelper.Round(bpm_min, 1) : GameHelper.Round(bpm_min, 1) + "-" + GameHelper.Round(bpm_max, 1)),
                track.getMaxCombo());
        beatmapInfo.setText(binfoStr);

        final StringBuilder dimensionStringBuilder = new StringBuilder();
        ar = Math.min(10.f, ar);
        od = Math.min(10.f, od);
        cs = Math.min(10.f, cs);
        hp = Math.min(10.f, hp);

        if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)) {
            ar = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(ar) * 2 / 3), 2);
            od = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(od) * 2 / 3), 2);
        } else if (mod.contains(GameMod.MOD_HALFTIME)) {
            ar = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(ar) * 4 / 3), 2);
            od = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(od) * 4 / 3), 2);
        }

        dimensionStringBuilder.append("AR: ").append(GameHelper.Round(ar, 2)).append(" ")
                .append("OD: ").append(GameHelper.Round(od, 2)).append(" ")
                .append("CS: ").append(GameHelper.Round(cs, 2)).append(" ")
                .append("HP: ").append(GameHelper.Round(hp, 2)).append(" ")
                .append("Stars: ").append(GameHelper.Round(track.getDifficulty(), 2));

        dimensionInfo.setText(dimensionStringBuilder.toString());
    }

    public void updateInfo(TrackInfo track, int mapState) {
        this.mapState = mapState;
        if (track == null) {
            return;
        }

        String tinfoStr = (track.getBeatmap().getArtistUnicode() == null || Config.isForceRomanized() ? track.getBeatmap().getArtist() : track.getBeatmap().getArtistUnicode()) + " - " +
                (track.getBeatmap().getTitleUnicode() == null || Config.isForceRomanized() ? track.getBeatmap().getTitle() : track.getBeatmap().getTitleUnicode()) + " [" + track.getMode() + "]";
        String mapperStr = "Beatmap by " + track.getCreator() + (mapState != 0 ? (" (" + this.mapStateHashmap.get(mapState) + ")") : "");
        String binfoStr2 = String.format(StringTable.get(R.string.binfoStr2),
                track.getHitCircleCount(), track.getSliderCount(), track.getSpinerCount(), track.getBeatmapSetID());
        trackInfo.setText(tinfoStr);
        mapper.setText(mapperStr);
        beatmapInfo2.setText(binfoStr2);
        changeDimensionInfo(track);
    }

    public void selectTrack(final TrackInfo track, boolean reloadBG) {
//		BassAudioProvider provider = BassAudioPlayer.getProvider();
//		if (provider != null && provider.getStatus() != Status.STOPPED) {
//			provider.stop();
//		}
        (new Thread() {
            @Override
            public void run() {
                if (board.isShowOnlineScores()) setRank();
            }
        }).start();
        if (selectedTrack == track) {
            synchronized (bgLoaded) {
                if (bgLoaded == false) {
                    return;
                }
            }

            ResourceManager.getInstance().getSound("menuhit").play();
            stopMusic();
            game.startGame(track, null);
            unload();
            return;
        }
        isSelectComplete = false;
        selectedTrack = track;
        mapState = 1;
        EdExtensionHelper.onSelectTrack(selectedTrack);
        GlobalManager.getInstance().setSelectedTrack(track);
        updateInfo(track, board.isShowOnlineScores() ? 1 : 0);
        board.cancleLoadAvatar();
        if (boardTask != null && boardTask.getStatus() != AsyncTask.Status.FINISHED) {
            boardTask.cancel(true);
            board.cancleLoadOnlineScores();
        }
        boardTask = new AsyncTaskLoader().execute(new OsuAsyncCallback() {
            @Override
            public void run() {
                board.init(track);
            }

            @Override
            public void onComplete() {
                // Do something cleanup
            }
        });

        final int quality = Config.getBackgroundQuality();
        synchronized (backgroundMutex) {

            if (!reloadBG && bgName.equals(track.getBackground())) {
                isSelectComplete = true;
                return;
            }
            bgName = track.getBackground();
            bg = null;
            bgLoaded = false;
            scene.setBackground(new ColorBackground(0, 0, 0));
            if (quality == 0) {
                Config.setBackgroundQuality(4);
            }
        }
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {


            public void run() {
                // Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                synchronized (backgroundMutex) {
                    final TextureRegion tex = ResourceManager.getInstance()
                            .loadBackground(bgName);
                    if (tex != null) {
                        float height = tex.getHeight();
                        height *= Config.getRES_WIDTH()
                                / (float) tex.getWidth();
                        bg = new Sprite(0,
                                (Config.getRES_HEIGHT() - height) / 2, Config
                                .getRES_WIDTH(), height, tex);
                        bg.setColor(0, 0, 0);
                    }

                    SyncTaskManager.getInstance().run(new Runnable() {
                        public void run() {
                            synchronized (backgroundMutex) {
                                if (bg != null) {
                                    scene.setBackground(new SpriteBackground(bg));
                                } else {
                                    final TextureRegion tex = ResourceManager
                                            .getInstance().getTexture("menu-background");
                                    float height = tex.getHeight();
                                    height *= Config.getRES_WIDTH()
                                            / (float) tex.getWidth();
                                    bg = new Sprite(
                                            0,
                                            (Config.getRES_HEIGHT() - height) / 2,
                                            Config.getRES_WIDTH(), height, tex);
                                    bgName = "";
                                    scene.setBackground(new SpriteBackground(bg));
                                }
                                Config.setBackgroundQuality(quality);
                                synchronized (bgLoaded) {
                                    bgLoaded = true;
                                }
                            }
                        }// run()
                    });// SyncTask.run

                }
            }


            public void onComplete() {
                isSelectComplete = true;
            }// onComplete
        });
    }

    public void stopScroll(final float y) {
        velocityY = 0;
        touchY = y;
        initalY = -1;
    }

    public void updateScore() {
        board.init(selectedTrack);
        if (selectedItem != null) {
            selectedItem.updateMarks(selectedTrack);
        }
    }

    public void openScore(final int id, boolean showOnline, final String playerName) {
        if (showOnline) {
            engine.setScene(new LoadingScreen().getScene());
            ToastLogger.showTextId(R.string.online_loadrecord, false);
            new AsyncTaskLoader().execute(new OsuAsyncCallback() {


                public void run() {
                    try {
                        String scorePack = OnlineManager.getInstance().getScorePack(id);
                        String[] params = scorePack.split("\\s+");
                        if (params.length < 11) return;

                        StatisticV2 stat = new StatisticV2(params);
                        stat.setPlayerName(playerName);
                        scorescene.load(stat, null, null, OnlineManager.getReplayURL(id), null, selectedTrack);
                        engine.setScene(scorescene.getScene());

                    } catch (OnlineManagerException e) {
                        Debug.e("Cannot load play info: " + e.getMessage(), e);
                        engine.setScene(scene);
                    }

                }


                public void onComplete() {
                    // TODO Auto-generated method stub

                }
            });
            return;
        }


        StatisticV2 stat = ScoreLibrary.getInstance().getScore(id);
        scorescene.load(stat, null, null, stat.getReplayName(), null, selectedTrack);
        engine.setScene(scorescene.getScene());
    }

    public void onScroll(final float where) {
        velocityY = 0;
        camY = where - Config.getRES_HEIGHT() / 2;
    }

    public void unload() {
    }

    public void bindDataBaseChangedListener(){
        OdrDatabase.get().setOnDatabaseChangedListener(() -> {
            this.reloadScoreBroad();
        });
    }

    public void unbindDataBaseChangedListener(){
        OdrDatabase.get().setOnDatabaseChangedListener(null);
    }

    public void setY(final float y) {
        velocityY = 0;
        camY = y;
    }

    public void stopMusic() {
        synchronized (musicMutex) {
            if (GlobalManager.getInstance().getSongService() != null) {
                GlobalManager.getInstance().getSongService().stop();
            }
        }
    }

    public void playMusic(final String filename, final int previewTime) {
        if (Config.isPlayMusicPreview() == false) {
            return;
        }
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {


            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                synchronized (musicMutex) {
                    if (GlobalManager.getInstance().getSongService() != null) {
                        GlobalManager.getInstance().getSongService().stop();
                    }

                    try {
//                        GlobalManager.getInstance().getSongService().preLoadWithLoop(filename);
                        GlobalManager.getInstance().getSongService().preLoad(filename);
                        GlobalManager.getInstance().getSongService().play();
                        GlobalManager.getInstance().getSongService().setVolume(0);
                        if (previewTime >= 0) {
                            GlobalManager.getInstance().getSongService().seekTo(previewTime);
                        } else {
                            GlobalManager.getInstance().getSongService().seekTo(GlobalManager.getInstance().getSongService().getLength() / 2);
                        }
                    } catch (final Exception e) {
                        Debug.e("LoadingMusic: " + e.getMessage(), e);
                    }

                }
            }


            public void onComplete() {
                // TODO Auto-generated method stub

            }
        });
    }

    public boolean isSelectAllowed() {
        if (bgLoaded == false) {
            return false;
        }
        return secondsSinceLastSelect > 0.5f;
    }

    public void showPropertiesMenu(MenuItem item) {
        if (item == null) {
            if (selectedItem == null) {
                return;
            }
            item = selectedItem;
        }
        (new PropsMenuFragment()).show(SongMenu.this, item, ppy, mapStateHashmap.get(mapState));
    }

    public void showDeleteScoreMenu(int scoreId) {
        (new ScoreMenuFragment()).show(scoreId);
        //ScorePropsMenu.getInstance().setSongMenu(SongMenu.this);
        //ScorePropsMenu.getInstance().setScoreId(scoreId);
        //scene.setChildScene(ScorePropsMenu.getInstance().getScene(), false, true, true);
    }

    public void reloadScoreBroad() {
        board.cancleLoadAvatar();
        if (boardTask != null && boardTask.getStatus() != AsyncTask.Status.FINISHED) {
            boardTask.cancel(true);
            board.cancleLoadOnlineScores();
        }
        boardTask = new AsyncTaskLoader().execute(new OsuAsyncCallback() {
            @Override
            public void run() {
                board.init(selectedTrack);
            }

            @Override
            public void onComplete() {
                // Do something cleanup
            }
        });
    }

    public Entity getFrontLayer() {
        return frontLayer;
    }

    public Entity getBackLayer() {
        return backLayer;
    }

    public void select() {
        if (GlobalManager.getInstance().getMainScene().getBeatmapInfo() != null) {
            BeatmapInfo beatmapInfo = GlobalManager.getInstance().getMainScene().getBeatmapInfo();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getBeatmap().getArtist().equals(beatmapInfo.getArtist()) &&
                        items.get(i).getBeatmap().getTitle().equals(beatmapInfo.getTitle()) &&
                        items.get(i).getBeatmap().getCreator().equals(beatmapInfo.getCreator())) {
                    secondsSinceLastSelect = 2;
                    items.get(i).select(false, true);
                    break;
                }
            }
        }
    }

    public TrackInfo getSelectedTrack() {
        return selectedTrack;
    }

    public boolean checkBG(String filePath) {
        if (filePath == null) return false;
        if (filePath.trim().equals("")) return false;

        BitmapFactory.Options bgOption = new BitmapFactory.Options();
        bgOption.inJustDecodeBounds = true;
        int w = BitmapFactory.decodeFile(filePath).getWidth();
        int h = BitmapFactory.decodeFile(filePath).getHeight();
        bgOption.inJustDecodeBounds = false;
        bgOption = null;
        return (w * h) > 0;
    }

    public enum SortOrder {
        Title, Artist, Creator, Date
    }
}
