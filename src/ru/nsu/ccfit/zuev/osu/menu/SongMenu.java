package ru.nsu.ccfit.zuev.osu.menu;

import android.annotation.SuppressLint;
import android.app.Activity;

import com.edlplan.ui.fragment.FilterMenuFragment;
import com.edlplan.ui.fragment.PropsMenuFragment;
import com.edlplan.ui.fragment.ScoreMenuFragment;
import com.reco1l.ibancho.RoomAPI;
import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.data.BeatmapSetInfo;
import com.reco1l.osu.data.DatabaseManager;
import com.reco1l.osu.Execution;
import com.reco1l.osu.multiplayer.Multiplayer;
import com.reco1l.osu.multiplayer.RoomScene;

import com.rian.osu.beatmap.parser.BeatmapParser;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import com.rian.osu.difficulty.calculator.DifficultyCalculationParameters;
import com.rian.osu.ui.DifficultyAlgorithmSwitcher;
import com.rian.osu.utils.LRUCache;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
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
import java.util.*;

import org.jetbrains.annotations.Nullable;
import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.RankedStatus;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.SkinLayout;

import static com.reco1l.osu.data.BeatmapsKt.BeatmapInfo;
import static com.rian.osu.utils.ModConverter.convertLegacyMods;

public class SongMenu implements IUpdateHandler, MenuItemListener,
        IScrollBarListener {
    private final static Boolean musicMutex = true;
    private final static Boolean bgMutex = true;
    private final Boolean backgroundMutex = true;
    public Scene scene;
    public Entity frontLayer = new Entity();
    SortOrder sortOrder = SortOrder.Title;
    private Engine engine;
    public GameScene game;
    private ScoringScene scoreScene;
    private float camY = 0;
    private float velocityY;
    private Activity context;
    private Entity backLayer = new Entity();
    private ArrayList<BeatmapSetItem> items = new ArrayList<>();
    private BeatmapSetItem selectedItem = null;
    private BeatmapInfo selectedBeatmap;
    private Sprite bg = null;
    private Boolean bgLoaded = false;
    private String backgroundPath = "";
    private ScoreBoard board;
    private Float touchY = null;
    private String filterText = "";
    private boolean favsOnly = false;
    private List<String> limitC;
    private float secondsSinceLastSelect = 0;
    private float maxY = 100500;
    private int pointerId = -1;
    private float initalY = -1;
    private float secPassed = 0, tapTime;
    private Sprite backButton = null;
    private ScrollBar scrollbar;

    private ChangeableText
            beatmapMetadataText,
            beatmapCreatorText,
            beatmapLengthText,
            beatmapHitObjectsText,
            beatmapDifficultyText;

    private boolean isSelectComplete = true;
    private AnimSprite scoringSwitcher = null;
    private FilterMenuFragment filterMenu = null;
    private GroupType groupType = GroupType.MapSet;

    private Timer previousSelectionTimer;
    private final long previousSelectionInterval = 1000;
    private boolean previousSelectionPerformed;
    private final LinkedList<BeatmapSetItem> previousSelectedItems = new LinkedList<>();
    private final LRUCache<String, RankedStatus> mapStatuses = new LRUCache<>(50);

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
        scoreScene = ss;
    }

    @Nullable
    public ArrayList<ScoreBoardItem> getBoard() {
        return board.getScoreBoardItems();
    }

    public boolean isBoardOnline() {
        return board.isShowOnlineScores();
    }

    public ArrayList<BeatmapSetItem> getMenuItems() {
        return items;
    }

    public void init(final Activity context, final Engine engine,
                     final GameScene pGame) {
        this.engine = engine;
        game = pGame;
        this.context = context;
    }

    public void loadFilter(IFilterMenu filterMenu) {
        setFilter(
            filterMenu.getFilter(),
            filterMenu.getOrder(),
            filterMenu.isFavoritesOnly(),
            filterMenu.getFavoriteFolder() == null ? null : DatabaseManager.getBeatmapCollectionsTable().getBeatmaps(filterMenu.getFavoriteFolder())
        );
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
        scene = new Scene();
        camY = 0;
        velocityY = 0;
        selectedItem = null;
        items = new ArrayList<>();
        selectedBeatmap = null;
        bgLoaded = true;
        SongMenuPool.getInstance().init();
        loadFilterFragment();

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

        board = new ScoreBoard(scene, backLayer, this);

//        float oy = 10;
        for (final BeatmapSetInfo i : LibraryManager.getLibrary()) {
            final BeatmapSetItem item = new BeatmapSetItem(this, i);
            items.add(item);
            item.attachToScene(scene, backLayer);
//            oy += item.getHeight();
        }
        sortOrder = SortOrder.Title;
        sort();

        if (items.size() == 0) {
            final Text text = new Text(0, 0, ResourceManager.getInstance()
                    .getFont("CaptionFont"), "There are no songs in library, try using chimu.moe",
                    HorizontalAlign.CENTER);
            text.setPosition(Config.getRES_WIDTH() / 2f - text.getWidth() / 2,
                    Config.getRES_HEIGHT() / 2f - text.getHeight() / 2);
            text.setScale(1.5f);
            text.setColor(0, 0, 0);
            scene.attachChild(text);
            return;
        }

        scene.setOnSceneTouchListener((pScene, evt) -> {
            if (evt.getX() < Config.getRES_WIDTH() / 5f * 2) {
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
                    if (camY <= -Config.getRES_HEIGHT() / 2f) {
                        camY = -Config.getRES_HEIGHT() / 2f;
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
        });

        scene.registerUpdateHandler(this);
        scene.setTouchAreaBindingEnabled(true);

        scrollbar = new ScrollBar(scene);

        final TextureRegion songSelectTopTexture = ResourceManager.getInstance().getTexture("songselect-top");
        final Sprite songSelectTop = new Sprite(0, 0, songSelectTopTexture);
        songSelectTop.setSize(songSelectTopTexture.getWidth() * songSelectTopTexture.getHeight() / 184f, 184);
        songSelectTop.setPosition(-1640, songSelectTop.getY());
        songSelectTop.setAlpha(0.6f);
        frontLayer.attachChild(songSelectTop);

        beatmapMetadataText = new ChangeableText(Utils.toRes(70), Utils.toRes(2),
                ResourceManager.getInstance().getFont("font"), "title", 1024);
        frontLayer.attachChild(beatmapMetadataText);

        beatmapCreatorText = new ChangeableText(Utils.toRes(70), beatmapMetadataText.getY() + beatmapMetadataText.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("middleFont"), "mapper", 1024);
        frontLayer.attachChild(beatmapCreatorText);

        beatmapLengthText = new ChangeableText(Utils.toRes(4), beatmapCreatorText.getY() + beatmapCreatorText.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("middleFont"), "beatmapInfo", 1024);
        frontLayer.attachChild(beatmapLengthText);

        beatmapHitObjectsText = new ChangeableText(Utils.toRes(4), beatmapLengthText.getY() + beatmapLengthText.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("middleFont"), "beatmapInfo2", 1024);
        frontLayer.attachChild(beatmapHitObjectsText);

        beatmapDifficultyText = new ChangeableText(Utils.toRes(4), beatmapHitObjectsText.getY() + beatmapHitObjectsText.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("smallFont"), "dimensionInfo", 1024);
        frontLayer.attachChild(beatmapDifficultyText);


        SkinLayout layoutBackButton = OsuSkin.get().getLayout("BackButton");
        SkinLayout layoutMods = null;

        if (!Multiplayer.isMultiplayer)
            layoutMods = OsuSkin.get().getLayout("ModsButton");

        SkinLayout layoutOptions = OsuSkin.get().getLayout("OptionsButton");
        SkinLayout layoutRandom = OsuSkin.get().getLayout("RandomButton");

        if (ResourceManager.getInstance().isTextureLoaded("menu-back-0")) {
            List<String> loadedBackTextures = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                if (ResourceManager.getInstance().isTextureLoaded("menu-back-" + i))
                    loadedBackTextures.add("menu-back-" + i);
            }
            backButton = new AnimSprite(0, 0, loadedBackTextures.size(), loadedBackTextures.toArray(new String[0])) {
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
                        if (selectedBeatmap == null) {
                            return true;
                        }
                        if (!moved) {
                            backButton.setScale(1f);
                            back();
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
                        if (selectedBeatmap == null) {
                            return true;
                        }
                        if (!moved) {
                            backButton.setScale(1f);
                            back();
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

        AnimSprite modSelection = null;

        if (!Multiplayer.isMultiplayer)
            modSelection = new AnimSprite(0, 0, 0,
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
                        if (!moved) {
                            velocityY = 0;
                            ModMenu.getInstance().show(scene, selectedBeatmap);
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
                    if (!moved) {
                        velocityY = 0;

                        if (filterMenu == null) loadFilterFragment();

                        filterMenu.showMenu(SongMenu.this);
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

                    if (previousSelectionTimer != null) {
                        previousSelectionTimer.cancel();
                    }

                    previousSelectionTimer = new Timer();
                    previousSelectionTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            if (!isSelectComplete) {
                                return;
                            }

                            BeatmapSetItem previousItem = previousSelectedItems.pollLast();
                            while (previousItem != null && previousItem.isDeleted()) {
                                previousItem = previousSelectedItems.pollLast();
                            }

                            if (previousItem == null) {
                                cancel();
                                return;
                            }

                            previousSelectionPerformed = true;

                            ResourceManager.getInstance().getSound("menuclick")
                                    .play();
                            previousItem.select(true, true);
                        }

                        @Override
                        public boolean cancel() {
                            previousSelectionTimer = null;
                            return super.cancel();
                        }
                    }, previousSelectionInterval, previousSelectionInterval);

                    previousSelectionPerformed = false;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setFrame(0);

                    if (previousSelectionTimer != null) {
                        previousSelectionTimer.cancel();
                    }

                    if (!isSelectComplete) {
                        return true;
                    }

                    if (!moved && !previousSelectionPerformed) {
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
                            } while (!items.get(index).isVisible());
                        }
                        if (!items.get(index).isVisible()) {
                            return true;
                        }
                        if (selectedItem == items.get(index)) {
                            return true;
                        }
                        ResourceManager.getInstance().getSound("menuclick")
                                .play();
                        items.get(index).select(true, true);
                    }

                    previousSelectionPerformed = false;
                    return true;
                }
                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50)) {
                    moved = true;
                    setFrame(0);

                    if (pSceneTouchEvent.isActionOutside() && previousSelectionTimer != null) {
                        previousSelectionTimer.cancel();
                    }
                }
                return false;
            }
        };

        final var difficultySwitcher = new DifficultyAlgorithmSwitcher();

        if (modSelection != null)
            modSelection.setScale(1.5f);

        optionSelection.setScale(1.5f);
        randomMap.setScale(1.5f);
        if (OsuSkin.get().isUseNewLayout()) {
            if (layoutBackButton != null) {
                layoutBackButton.baseApply(backButton);
            }
            if (layoutMods != null && modSelection != null) {
                layoutMods.baseApply(modSelection);
            }
            if (layoutOptions != null) {
                layoutOptions.baseApply(optionSelection);
            }
            if (layoutRandom != null) {
                layoutRandom.baseApply(randomMap);
            }
            backButton.setPosition(0, Config.getRES_HEIGHT() - backButton.getHeightScaled());

            if (modSelection != null) {
                modSelection.setPosition(backButton.getX() + backButton.getWidth(),
                                         Config.getRES_HEIGHT() - modSelection.getHeightScaled());
                optionSelection.setPosition(
                        modSelection.getX() + modSelection.getWidthScaled(),
                        Config.getRES_HEIGHT() - optionSelection.getHeightScaled());
            } else {
                optionSelection.setPosition(backButton.getX() + backButton.getWidth(),
                                         Config.getRES_HEIGHT() - optionSelection.getHeightScaled());
            }
            randomMap.setPosition(
                    optionSelection.getX() + optionSelection.getWidthScaled(),
                    Config.getRES_HEIGHT() - randomMap.getHeightScaled());
        } else {
            backButton.setPosition(0, Config.getRES_HEIGHT() - backButton.getHeight());

            if (modSelection != null) {
                modSelection.setPosition(backButton.getX() + backButton.getWidth(),
                                         Config.getRES_HEIGHT() - Utils.toRes(90));
                optionSelection.setPosition(
                        modSelection.getX() + modSelection.getWidthScaled(),
                        Config.getRES_HEIGHT() - Utils.toRes(90));
            } else {
                optionSelection.setPosition(backButton.getX() + backButton.getWidth(),
                                         Config.getRES_HEIGHT() - Utils.toRes(90));
            }
            randomMap.setPosition(
                    optionSelection.getX() + optionSelection.getWidthScaled(),
                    Config.getRES_HEIGHT() - Utils.toRes(90));
        }

        difficultySwitcher.setPosition(randomMap.getX() + randomMap.getWidthScaled() - 18, Config.getRES_HEIGHT() - difficultySwitcher.getHeightScaled());

        frontLayer.attachChild(backButton);
        scene.registerTouchArea(backButton);

        if (modSelection != null)
        {
            frontLayer.attachChild(modSelection);
            scene.registerTouchArea(modSelection);
        }
        frontLayer.attachChild(optionSelection);
        scene.registerTouchArea(optionSelection);
        frontLayer.attachChild(randomMap);
        scene.registerTouchArea(randomMap);
        frontLayer.attachChild(difficultySwitcher);
        scene.registerTouchArea(difficultySwitcher);

        if (OnlineScoring.getInstance().createSecondPanel() != null) {
            OnlinePanel panel = OnlineScoring.getInstance().getSecondPanel();
            panel.detachSelf();
            panel.setPosition(difficultySwitcher.getX() + difficultySwitcher.getWidthScaled() + 20, Config.getRES_HEIGHT() - 110);
            OnlineScoring.getInstance().loadAvatar(false);
            frontLayer.attachChild(panel);

            scoringSwitcher = new AnimSprite(Utils.toRes(5), Utils.toRes(10), 0,
                    "ranking_disabled", "ranking_enabled", "selection-ranked", "selection-approved",
                    "selection-loved", "selection-question") {
                @Override
                public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
                                             float pTouchAreaLocalX, float pTouchAreaLocalY) {
                    if (!pSceneTouchEvent.isActionDown()) return false;
                    toggleScoringSwitcher();
                    return true;
                }
            };

            scoringSwitcher.setFrame(0);
            scoringSwitcher.setPosition(10, 10);
            scene.registerTouchArea(scoringSwitcher);
            frontLayer.attachChild(scoringSwitcher);
        }

        // Preventing ModMenu to reload mod set
        if (!Multiplayer.isMultiplayer) {
            ModMenu.getInstance().reload();
        }
    }

    public void loadFilterFragment() {
        filterMenu = new FilterMenuFragment();
        filterMenu.loadConfig(context);
    }

    public void unloadFilterFragment() {
        scene.clearChildScene();
        filterMenu = null;
    }

    public void toggleScoringSwitcher() {
        if (board.isShowOnlineScores()) {
            board.setShowOnlineScores(false);
            board.init(selectedBeatmap);
            updateInfo(selectedBeatmap);
        } else if (OnlineManager.getInstance().isStayOnline()) {
            board.setShowOnlineScores(true);
            board.init(selectedBeatmap);
        }

        updateScoringSwitcherStatus(true);
    }

    public Scene getScene() {
        return scene;
    }
    public FilterMenuFragment getFilterMenu() { return filterMenu; }

    public void show() {
        engine.setScene(scene);
    }

    public void setFilter(final String filter, final SortOrder order,
                          final boolean favsOnly, List<String> limit) {
        String beatmapFilename = "";
        if (selectedBeatmap != null) {
            beatmapFilename = selectedBeatmap.getFilename();
        }
        if (!order.equals(sortOrder)) {
            sortOrder = order;
            tryReloadMenuItems(sortOrder);
            sort();
            reSelectItem(beatmapFilename);
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
        for (final BeatmapSetItem item : items) {
            item.applyFilter(lowerFilter, favsOnly, limit);
        }
        if (favsOnly != this.favsOnly) {
            this.favsOnly = favsOnly;
        } else {
            reSelectItem(beatmapFilename);
        }
        if (selectedItem != null && !selectedItem.isVisible()) {
            selectedItem = null;
            selectedBeatmap = null;
        }
    }

    public void sort() {
        if (!sortOrder.equals(filterMenu.getOrder())) {
            sortOrder = filterMenu.getOrder();
        }
        Collections.sort(items, (i1, i2) -> {
            String s1;
            String s2;
            switch (sortOrder) {
                case Artist:
                    s1 = i1.getFirstBeatmap().getArtist();
                    s2 = i2.getFirstBeatmap().getArtist();
                    break;
                case Creator:
                    s1 = i1.getFirstBeatmap().getCreator();
                    s2 = i2.getFirstBeatmap().getCreator();
                    break;
                case Date:
                    final Long int1 = i1.getFirstBeatmap().getDateImported();
                    final Long int2 = i2.getFirstBeatmap().getDateImported();
                    return int2.compareTo(int1);
                case Bpm:
                    final float bpm1 = i1.getFirstBeatmap().getBpmMax();
                    final float bpm2 = i2.getFirstBeatmap().getBpmMax();
                    return Float.compare(bpm2, bpm1);
                case DroidStars:
                    final float droid1 = i1.getFirstBeatmap().getStarRating(DifficultyAlgorithm.droid);
                    final float droid2 = i2.getFirstBeatmap().getStarRating(DifficultyAlgorithm.droid);
                    return Float.compare(droid2, droid1);
                case StandardStars:
                    final float standard1 = i1.getFirstBeatmap().getStarRating(DifficultyAlgorithm.standard);
                    final float standard2 = i2.getFirstBeatmap().getStarRating(DifficultyAlgorithm.standard);
                    return Float.compare(standard2, standard1);
                case Length:
                    final Long length1 = i1.getFirstBeatmap().getLength();
                    final Long length2 = i2.getFirstBeatmap().getLength();
                    return length2.compareTo(length1);
                default:
                    s1 = i1.getFirstBeatmap().getTitle();
                    s2 = i2.getFirstBeatmap().getTitle();
            }

            return s1.compareToIgnoreCase(s2);
        });
    }

    public void onUpdate(final float pSecondsElapsed) {
        secPassed += pSecondsElapsed;
        increaseVolume();
        increaseBackgroundLuminance(pSecondsElapsed);

        secondsSinceLastSelect += pSecondsElapsed;
        float oy = -camY;
        for (final BeatmapSetItem item : items) {
            final float cy = oy + Config.getRES_HEIGHT() / 2f + item.getHeight()
                    / 2;
            float ox = Config.getRES_WIDTH() / 1.85f + 200 * (float) Math.abs(Math.cos(cy * Math.PI
                    / (Config.getRES_HEIGHT() * 2)));
            ox = Utils.toRes(ox);
            item.setPos(ox, oy);
            oy += item.getHeight();
        }
        oy += camY;
        camY += velocityY * pSecondsElapsed;
        maxY = oy - Config.getRES_HEIGHT() / 2f;
        if (camY <= -Config.getRES_HEIGHT() / 2f && velocityY < 0
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

        expandSelectedItem(pSecondsElapsed);

        updateScrollbar(camY + Config.getRES_HEIGHT() / 2f, oy);
    }

    public void increaseVolume() {
        if (GlobalManager.getInstance().getSongService() != null) {
            synchronized (musicMutex) {
                if (GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING && GlobalManager.getInstance().getSongService().getVolume() < Config.getBgmVolume()) {
                    float vol = Math.min(1, GlobalManager.getInstance().getSongService().getVolume() + 0.01f);
                    GlobalManager.getInstance().getSongService().setVolume(vol);
                }
            }
        }
    }

    public void increaseBackgroundLuminance(final float pSecondsElapsed) {
        if (bg != null) {
            synchronized (backgroundMutex) {
                if (bg != null && bg.getRed() < 1) {
                    final float col = Math.min(1, bg.getRed() + pSecondsElapsed);
                    bg.setColor(col, col, col);
                }
            }
        }
    }

    public void expandSelectedItem(float pSecondsElapsed) {
        if (selectedItem != null) {
            if (selectedItem.percentAppeared < 1) {
                selectedItem.percentAppeared += 2 * pSecondsElapsed;
            } else {
                selectedItem.percentAppeared = 1;
            }
            selectedItem.update(pSecondsElapsed);
        }
    }

    public void updateScrollbar(final float vy, final float maxy) {
        scrollbar.setPosition(vy, maxy);
        scrollbar.setVisible(Math.abs(velocityY) > Utils.toRes(500));
    }

    public void reset() {
    }

    public void select(final BeatmapSetItem item) {
        secondsSinceLastSelect = 0;
        if (selectedItem != null) {
            selectedItem.deselect();

            if (!previousSelectionPerformed) {
                while (previousSelectedItems.size() >= 10) {
                    previousSelectedItems.pollFirst();
                }
                previousSelectedItems.addLast(selectedItem);
            }
        }

        selectedItem = item;
        velocityY = 0;
        selectedBeatmap = null;
        float height = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == selectedItem) {
                break;
            }
            height += items.get(i).getInitialHeight();
        }
        camY = height - Config.getRES_HEIGHT() / 2f;
        camY += item.getTotalHeight() / 2;
    }

    @SuppressLint("SimpleDateFormat")
    public void changeDimensionInfo(BeatmapInfo beatmapInfo) {
        if (beatmapInfo == null) {
            return;
        }
        float ar = beatmapInfo.getApproachRate();
        float od = beatmapInfo.getOverallDifficulty();
        float cs = beatmapInfo.getCircleSize();
        float hp = beatmapInfo.getHpDrainRate();
        float bpm_max = beatmapInfo.getBpmMax();
        float bpm_min = beatmapInfo.getBpmMin();
        long length = beatmapInfo.getLength();
        EnumSet<GameMod> mod = ModMenu.getInstance().getMod();

        beatmapDifficultyText.setColor(1, 1, 1);
        beatmapLengthText.setColor(1, 1, 1);

        if (mod.contains(GameMod.MOD_EASY)) {
            ar *= 0.5f;
            od *= 0.5f;
            cs -= 1f;
            hp *= 0.5f;
            beatmapDifficultyText.setColor(46 / 255f, 139 / 255f, 87 / 255f);
        }
        if (mod.contains(GameMod.MOD_HARDROCK) || mod.contains(GameMod.MOD_PRECISE)) {
            if (mod.contains(GameMod.MOD_HARDROCK)) {
                ar = Math.min(ar * 1.4f, 10);
                od = Math.min(od * 1.4f, 10);
                ++cs;
                hp = Math.min(hp * 1.4f, 10);
            }
            beatmapDifficultyText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
        }
        if (ModMenu.getInstance().getChangeSpeed() != 1) {
            float speed = ModMenu.getInstance().getSpeed();
            bpm_max *= speed;
            bpm_min *= speed;
            length /= speed;
            if (speed > 1) {
                beatmapLengthText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
                beatmapDifficultyText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
            } else if (speed < 1) {
                beatmapLengthText.setColor(46 / 255f, 139 / 255f, 87 / 255f);
                beatmapDifficultyText.setColor(46 / 255f, 139 / 255f, 87 / 255f);
            }
        } else {
            if (mod.contains(GameMod.MOD_DOUBLETIME)) {
                bpm_max *= 1.5f;
                bpm_min *= 1.5f;
                length *= 2 / 3f;
                beatmapLengthText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
                beatmapDifficultyText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
            }
            if (mod.contains(GameMod.MOD_NIGHTCORE)) {
                bpm_max *= 1.5f;
                bpm_min *= 1.5f;
                length *= 2 / 3f;
                beatmapLengthText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
                beatmapDifficultyText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
            }
            if (mod.contains(GameMod.MOD_HALFTIME)) {
                bpm_max *= 0.75f;
                bpm_min *= 0.75f;
                length *= 4 / 3f;
                beatmapLengthText.setColor(46 / 255f, 139 / 255f, 87 / 255f);
                beatmapDifficultyText.setColor(46 / 255f, 139 / 255f, 87 / 255f);
            }
        }
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            if (mod.contains(GameMod.MOD_EASY)) {
                ar *= 2f;
                ar -= 0.5f;
            }
            ar -= 0.5f;
            if (ModMenu.getInstance().getChangeSpeed() != 1) {
                ar -= ModMenu.getInstance().getSpeed() - 1.0f;
            } else if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)) {
                ar -= 0.5f;
            }
            od *= 0.5f;
            cs -= 1f;
            hp *= 0.5f;
            beatmapDifficultyText.setColor(46 / 255f, 139 / 255f, 87 / 255f);
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        String binfoStr = String.format(StringTable.get(R.string.binfoStr1), sdf.format(length),
                (bpm_min == bpm_max ? GameHelper.Round(bpm_min, 1) : GameHelper.Round(bpm_min, 1) + "-" + GameHelper.Round(bpm_max, 1)),
                beatmapInfo.getMaxCombo());
        if (length > 3600 * 1000) {
            sdf = new SimpleDateFormat("HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            binfoStr = String.format(StringTable.get(R.string.binfoStr1), sdf.format(length),
                    (bpm_min == bpm_max ? GameHelper.Round(bpm_min, 1) : GameHelper.Round(bpm_min, 1) + "-" + GameHelper.Round(bpm_max, 1)),
                    beatmapInfo.getMaxCombo());
        }
        beatmapLengthText.setText(binfoStr);

        final StringBuilder dimensionStringBuilder = new StringBuilder();
        if (ModMenu.getInstance().getChangeSpeed() != 1) {
            float speed = ModMenu.getInstance().getSpeed();
            ar = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(ar) / speed), 2);
            od = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(od) / speed), 2);
        } else if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)) {
            ar = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(ar) * 2 / 3), 2);
            od = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(od) * 2 / 3), 2);
        } else if (mod.contains(GameMod.MOD_HALFTIME)) {
            ar = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(ar) * 4 / 3), 2);
            od = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(od) * 4 / 3), 2);
        }
        float rawAR = ar;
        float rawOD = od;
        float rawCS = cs;
        float rawHP = hp;

        if (ModMenu.getInstance().isCustomAR()) {
            ar = ModMenu.getInstance().getCustomAR();
        }
        if (ModMenu.getInstance().isCustomOD()) {
            od = ModMenu.getInstance().getCustomOD();
        }
        if (ModMenu.getInstance().isCustomCS()) {
            cs = ModMenu.getInstance().getCustomCS();
        }
        if (ModMenu.getInstance().isCustomHP()) {
            hp = ModMenu.getInstance().getCustomHP();
        }

        if (ar != rawAR || od != rawOD || cs != rawCS || hp != rawHP) {
            beatmapDifficultyText.setColor(255 / 255f, 180 / 255f, 0 / 255f);
        }

        dimensionStringBuilder
                .append("AR: ").append(GameHelper.Round(ar, 2)).append(" ")
                .append("OD: ").append(GameHelper.Round(od, 2)).append(" ")
                .append("CS: ").append(GameHelper.Round(cs, 2)).append(" ")
                .append("HP: ").append(GameHelper.Round(hp, 2)).append(" ")
                .append("Stars: ");

        dimensionStringBuilder.append(GameHelper.Round(beatmapInfo.getStarRating(), 2));

        beatmapDifficultyText.setText(dimensionStringBuilder.toString());
    }

    public void reloadCurrentSelection() {
        updateInfo(selectedBeatmap);

        if (selectedItem != null) {
            selectedItem.reloadBeatmaps();
        }
    }

    public void updateInfo(BeatmapInfo beatmapInfo) {
        if (beatmapInfo == null) {
            return;
        }

        String tinfoStr = beatmapInfo.getArtistText() + " - " + beatmapInfo.getTitleText() + " [" + beatmapInfo.getVersion() + "]";
        String mapperStr = "Beatmap by " + beatmapInfo.getCreator();
        String binfoStr2 = String.format(StringTable.get(R.string.binfoStr2),
                beatmapInfo.getHitCircleCount(), beatmapInfo.getSliderCount(), beatmapInfo.getSpinnerCount(), beatmapInfo.getSetId());
        beatmapMetadataText.setText(tinfoStr);
        beatmapCreatorText.setText(mapperStr);
        beatmapHitObjectsText.setText(binfoStr2);
        changeDimensionInfo(beatmapInfo);
        Execution.async(() -> {
            try (var parser = new BeatmapParser(beatmapInfo.getPath())) {
                var data = parser.parse(true);

                if (data == null) {
                    setStarsDisplay(0);
                    return;
                }

                var newInfo = BeatmapInfo(data, beatmapInfo.getDateImported(), true);
                beatmapInfo.apply(newInfo);
                DatabaseManager.getBeatmapInfoTable().update(newInfo);

                changeDimensionInfo(beatmapInfo);

                var parameters = new DifficultyCalculationParameters();
                var modMenu = ModMenu.getInstance();

                parameters.setMods(convertLegacyMods(
                    modMenu.getMod(),
                    modMenu.isCustomCS() ? modMenu.getCustomCS() : null,
                    modMenu.isCustomAR() ? modMenu.getCustomAR() : null,
                    modMenu.isCustomOD() ? modMenu.getCustomOD() : null
                ));
                parameters.setCustomSpeedMultiplier(modMenu.getChangeSpeed());

                switch (Config.getDifficultyAlgorithm()) {
                    case droid -> {
                        var attributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(
                            data,
                            parameters
                        );

                        setStarsDisplay(GameHelper.Round(attributes.starRating, 2));
                    }

                    case standard -> {
                        var attributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(
                            data,
                            parameters
                        );

                        setStarsDisplay(GameHelper.Round(attributes.starRating, 2));
                    }
                }
            }
        });
    }

    @Override
    public void selectBeatmap(final BeatmapInfo beatmapInfo, boolean reloadBG) {

        // Playing corresponding audio for the selected track.
        var selectedAudio = selectedBeatmap != null ? selectedBeatmap : GlobalManager.getInstance().getSelectedBeatmap();

        if (selectedAudio == null || !Objects.equals(selectedAudio.getAudioFilename(), beatmapInfo.getAudioFilename())) {
            playMusic(beatmapInfo.getAudioPath(), beatmapInfo.getPreviewTime());
        }

        if (selectedBeatmap != null && selectedBeatmap.getFilename().equals(beatmapInfo.getFilename())) {
            synchronized (bgMutex) {
                if (!bgLoaded) {
                    return;
                }
            }

            ResourceManager.getInstance().getSound("menuhit").play();
            if (Multiplayer.isMultiplayer)
            {
                setMultiplayerRoomBeatmap(selectedBeatmap);
                back(false);
                return;
            }
            stopMusic();

            Replay.oldMod = ModMenu.getInstance().getMod();
            Replay.oldChangeSpeed = ModMenu.getInstance().getChangeSpeed();

            Replay.oldCustomAR = ModMenu.getInstance().getCustomAR();
            Replay.oldCustomOD = ModMenu.getInstance().getCustomOD();
            Replay.oldCustomCS = ModMenu.getInstance().getCustomCS();
            Replay.oldCustomHP = ModMenu.getInstance().getCustomHP();

            Replay.oldFLFollowDelay = ModMenu.getInstance().getFLfollowDelay();

            game.startGame(beatmapInfo, null);
            unload();
            return;
        }
        isSelectComplete = false;
        selectedBeatmap = beatmapInfo;
        GlobalManager.getInstance().setSelectedBeatmap(beatmapInfo);
        updateInfo(beatmapInfo);
        updateScoringSwitcherStatus(false);
        board.init(beatmapInfo);

        synchronized (backgroundMutex) {

            if (!reloadBG && (beatmapInfo.getBackgroundFilename() == null || backgroundPath.equals(beatmapInfo.getBackgroundPath()))) {
                isSelectComplete = true;
                return;
            }
            backgroundPath = beatmapInfo.getBackgroundPath();
            bg = null;
            bgLoaded = false;
            scene.setBackground(new ColorBackground(0, 0, 0));
        }

        Execution.async(() -> {
            synchronized (backgroundMutex) {
                final TextureRegion tex = Config.isSafeBeatmapBg() || beatmapInfo.getBackgroundFilename() == null?
                        ResourceManager.getInstance().getTexture("menu-background") :
                        ResourceManager.getInstance().loadBackground(backgroundPath);
                if (tex != null) {
                    float height = tex.getHeight();
                    height *= Config.getRES_WIDTH()
                            / (float) tex.getWidth();
                    bg = new Sprite(0,
                            (Config.getRES_HEIGHT() - height) / 2, Config
                            .getRES_WIDTH(), height, tex);
                    bg.setColor(0, 0, 0);
                }

                Execution.updateThread(() -> {
                    synchronized (backgroundMutex) {
                        if (bg == null) {
                            final TextureRegion tex1 = ResourceManager
                                    .getInstance().getTexture("menu-background");
                            float height = tex1.getHeight();
                            height *= Config.getRES_WIDTH()
                                    / (float) tex1.getWidth();
                            bg = new Sprite(
                                    0,
                                    (Config.getRES_HEIGHT() - height) / 2,
                                    Config.getRES_WIDTH(), height, tex1);
                            backgroundPath = "";
                        }
                        scene.setBackground(new SpriteBackground(bg));
                        synchronized (bgMutex) {
                            bgLoaded = true;
                        }
                    }
                });
            }

            isSelectComplete = true;
        });
    }

    public void stopScroll(final float y) {
        velocityY = 0;
        touchY = y;
        initalY = -1;
    }

    public void updateScore() {
        board.init(selectedBeatmap);
        if (selectedItem != null) {
            selectedItem.updateMarks();
        }
    }

    public void openScore(final int id, boolean showOnline, final String playerName) {
        if (showOnline) {
            engine.setScene(new LoadingScreen().getScene());
            ToastLogger.showTextId(com.edlplan.osudroidresource.R.string.online_loadrecord, false);

            Execution.async(() -> {
                try {
                    String scorePack = OnlineManager.getInstance().getScorePack(id);
                    String[] params = scorePack.split("\\s+");

                    if (params.length < 11) return;

                    StatisticV2 stat = new StatisticV2(params);
                    if (stat.isLegacySC()) {
                        stat.processLegacySC(selectedBeatmap);
                    }

                    stat.setPlayerName(playerName);
                    scoreScene.load(stat, null, null, OnlineManager.getReplayURL(id), null, selectedBeatmap);
                    engine.setScene(scoreScene.getScene());
                } catch (OnlineManagerException e) {
                    Debug.e("Cannot load play info: " + e.getMessage(), e);
                    engine.setScene(scene);
                }
            });
            return;
        }


        var stat = DatabaseManager.getScoreInfoTable().getScore(id).toStatisticV2();

        if (stat.isLegacySC()) {
            stat.processLegacySC(selectedBeatmap);
        }

        scoreScene.load(stat, null, null, stat.getReplayFilename(), null, selectedBeatmap);
        engine.setScene(scoreScene.getScene());
    }


    public void onScroll(final float where) {
        velocityY = 0;
        camY = where - Config.getRES_HEIGHT() / 2f;
    }

    public void unload() {
    }

    public void back() {
        back(true);
    }

    private void back(boolean resetMultiplayerBeatmap) {

        if (Multiplayer.isMultiplayer) {
            if (resetMultiplayerBeatmap) {
                resetMultiplayerRoomBeatmap();
            }

            RoomScene.INSTANCE.show();
            return;
        }

        GlobalManager.getInstance().getMainScene().show();
    }

    private void resetMultiplayerRoomBeatmap() {
        if (!Multiplayer.isMultiplayer) {
            return;
        }

        // Locking host from change beatmap before the server responses to beatmapChange
        RoomScene.awaitBeatmapChange = true;

        if (!Multiplayer.isConnected()) {
            return;
        }

        // Now we update the beatmap
        if (Multiplayer.room != null && Multiplayer.room.getPreviousBeatmap() != null) {
            var beatmap = Multiplayer.room.getPreviousBeatmap();

            RoomAPI.changeBeatmap(
                    beatmap.getMd5(),
                    beatmap.getTitle(),
                    beatmap.getArtist(),
                    beatmap.getVersion(),
                    beatmap.getCreator()
            );
        } else {
            RoomAPI.changeBeatmap();
        }
    }

    private void setMultiplayerRoomBeatmap(BeatmapInfo beatmapInfo) {
        if (!Multiplayer.isMultiplayer) {
            return;
        }

        // Locking host from change beatmap before the server responses to beatmapChange
        RoomScene.awaitBeatmapChange = true;

        if (!Multiplayer.isConnected()) {
            return;
        }

        // Now we update the beatmap
        if (beatmapInfo != null) {
            RoomAPI.changeBeatmap(
                    beatmapInfo.getMD5(),
                    beatmapInfo.getTitle(),
                    beatmapInfo.getArtist(),
                    beatmapInfo.getVersion(),
                    beatmapInfo.getCreator()
            );
        } else {
            RoomAPI.changeBeatmap();
        }
    }

    public void reloadScoreboard() {
        if (GlobalManager.getInstance().getEngine().getScene() != scene) {
            return;
        }

        board.init(selectedBeatmap);
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

    public void playMusic(final String filePath, final int previewTime) {
        if (!Config.isPlayMusicPreview()) {
            return;
        }

        Execution.async(() -> {
            synchronized (musicMutex) {
                if (GlobalManager.getInstance().getSongService() != null) {
                    GlobalManager.getInstance().getSongService().stop();
                }

                try {
                    GlobalManager.getInstance().getSongService().preLoad(filePath);
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
        });
    }

    public boolean isSelectAllowed() {
        if (!bgLoaded) {
            return false;
        }
        return secondsSinceLastSelect > 0.5f;
    }

    public void showPropertiesMenu(BeatmapSetItem item) {
        if (item == null) {
            if (selectedItem == null) {
                return;
            }
            item = selectedItem;
        }
        (new PropsMenuFragment()).show(SongMenu.this, item);
    }

    public void showDeleteScoreMenu(int scoreId) {
        (new ScoreMenuFragment()).show(scoreId);
    }

    public void select() {
        if (GlobalManager.getInstance().getSelectedBeatmap() != null) {
            BeatmapInfo beatmapInfo = GlobalManager.getInstance().getSelectedBeatmap();

            var i = items.size() - 1;
            while (i >= 0) {
                var item = items.get(i);
                if (item.getBeatmapSetInfo().getDirectory().equals(beatmapInfo.getSetDirectory())) {
                    secondsSinceLastSelect = 2;
                    item.select(false, true);
                    break;
                }
                --i;
            }
        }
    }

    public BeatmapInfo getSelectedBeatmap() {
        return selectedBeatmap;
    }

    private void tryReloadMenuItems(SortOrder order) {
        switch (order) {
            case Title:
            case Artist:
            case Creator:
            case Date:
            case Bpm:
                reloadMenuItems(GroupType.MapSet);
                break;
            case DroidStars:
            case StandardStars:
            case Length:
                reloadMenuItems(GroupType.SingleDiff);
                break;
        }
    }

    private void reloadMenuItems(GroupType type) {
        if (!groupType.equals(type)) {
            groupType = type;
//            float oy = 10;
            for (BeatmapSetItem item : items) {
                item.removeFromScene();
            }
            items.clear();
            switch (type) {
                case MapSet:
                    for (final BeatmapSetInfo i : LibraryManager.getLibrary()) {
                        final BeatmapSetItem item = new BeatmapSetItem(this, i);
                        items.add(item);
                        item.attachToScene(scene, backLayer);
//                        oy += item.getHeight();
                    }
                    break;
                case SingleDiff:
                    for (final BeatmapSetInfo i : LibraryManager.getLibrary()) {
                        for (int j = 0; j < i.getCount(); j++) {
                            final BeatmapSetItem item = new BeatmapSetItem(this, i, j);
                            items.add(item);
                            item.attachToScene(scene, backLayer);
//                            oy += item.getHeight();
                        }
                    }
                    break;
            }
            final String lowerFilter = filterMenu.getFilter().toLowerCase();
            final boolean favsOnly = filterMenu.isFavoritesOnly();

            var limit = DatabaseManager.getBeatmapCollectionsTable().getBeatmaps(filterMenu.getFavoriteFolder());
            for (final BeatmapSetItem item : items) {
                item.applyFilter(lowerFilter, favsOnly, limit);
            }
        }
    }

    public void setStarsDisplay(float star) {
        String str = beatmapDifficultyText.getText();
        String[] strs = str.split("Stars: ");
        if (strs.length == 2) {
            beatmapDifficultyText.setText(strs[0] + "Stars: " + star);
        }
    }

    private void reSelectItem(String beatmapFilename) {
        if (!beatmapFilename.isEmpty()) {
            if (selectedBeatmap.getFilename().equals(beatmapFilename) && items.size() > 1 && selectedItem != null && selectedItem.isVisible()) {
                velocityY = 0;
                float height = 0;
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i) == selectedItem) {
                        break;
                    }
                    height += items.get(i).getInitialHeight();
                }
                camY = height - Config.getRES_HEIGHT() / 2f;
                camY += items.get(0).getTotalHeight() / 2;
                return;
            }
            for (int i = items.size() - 1; i >= 0; i--) {
                BeatmapSetItem item = items.get(i);
                if (item == null || !item.isVisible()) continue;
                int beatmapId = item.tryGetCorrespondingBeatmapId(beatmapFilename);
                if (beatmapId >= 0) {
                    item.select(true, true);
                    if (beatmapId != 0) {
                        item.selectBeatmap(item.getBeatmapSpritesById(beatmapId), false);
                    }
                    break;
                }
            }
        }
    }

    private void updateScoringSwitcherStatus(boolean forceUpdate) {
        if (scoringSwitcher == null) {
            return;
        }

        if (selectedBeatmap == null || !board.isShowOnlineScores()) {
            scoringSwitcher.setFrame(0);
            return;
        }

        var md5 = selectedBeatmap.getMD5();

        if (!forceUpdate && mapStatuses.containsKey(md5)) {
            scoringSwitcher.setFrame(switch (Objects.requireNonNull(mapStatuses.get(md5))) {
                case ranked -> 2;
                case approved -> 3;
                case loved -> 4;
                default -> 5;
            });

            return;
        }

        scoringSwitcher.setFrame(1);

        Execution.async(() -> {
            try {
                var status = OnlineManager.getInstance().getBeatmapStatus(md5);

                if (!board.isShowOnlineScores() || status == null || scoringSwitcher == null || selectedBeatmap == null || !selectedBeatmap.getMD5().equals(md5)) {
                    return;
                }

                mapStatuses.put(md5, status);

                scoringSwitcher.setFrame(switch (status) {
                    case ranked -> 2;
                    case approved -> 3;
                    case loved -> 4;
                    default -> 5;
                });
            } catch (OnlineManagerException e) {
                Debug.e("Cannot get beatmap status: " + e.getMessage(), e);

                if (scoringSwitcher != null) {
                    scoringSwitcher.setFrame(1);
                }
            }
        });
    }

    /**
     * Called when the beatmap table changes. Most commonly during difficulty calculation.
     */
    public void onDifficultyCalculationEnd() {

        if (GlobalManager.getInstance().getEngine().getScene() != scene) {
            return;
        }

        Execution.updateThread(() -> {

            // If the sort order is related to difficulty, we need to reload the menu items.
            if (sortOrder == SortOrder.DroidStars || sortOrder == SortOrder.StandardStars) {
                reload();
                show();
                select();
            } else {
                reloadCurrentSelection();
            }

        });
    }


    public enum SortOrder {
        Title,
        Artist,
        Creator,
        Date,
        Bpm,
        DroidStars,
        StandardStars,
        Length
    }

    public enum GroupType {
        MapSet, SingleDiff
    }
}
