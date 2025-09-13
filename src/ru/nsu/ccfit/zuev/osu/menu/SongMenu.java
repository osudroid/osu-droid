package ru.nsu.ccfit.zuev.osu.menu;

import static com.osudroid.data.BeatmapsKt.BeatmapInfo;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.fragment.SearchBarFragment;
import com.edlplan.ui.fragment.BeatmapPropertiesFragment;
import com.edlplan.ui.fragment.ScoreMenuFragment;
import com.osudroid.ui.v1.BeatmapAttributeDisplay;
import com.osudroid.utils.Execution;
import com.reco1l.andengine.UIScene;
import com.reco1l.framework.EasingKt;
import com.osudroid.multiplayer.api.RoomAPI;
import com.osudroid.data.BeatmapInfo;
import com.osudroid.data.BeatmapSetInfo;
import com.osudroid.data.DatabaseManager;
import com.reco1l.andengine.sprite.UIAnimatedSprite;
import com.reco1l.andengine.sprite.UISprite;
import com.osudroid.multiplayer.Multiplayer;
import com.osudroid.multiplayer.RoomScene;

import com.osudroid.ui.v2.modmenu.ModMenu;
import com.rian.osu.GameMode;
import com.rian.osu.beatmap.parser.BeatmapParser;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import com.rian.osu.math.Precision;
import com.rian.osu.mods.LegacyModConverter;
import com.rian.osu.mods.ModDifficultyAdjust;
import com.rian.osu.mods.ModNightCore;
import com.rian.osu.mods.ModPrecise;
import com.rian.osu.mods.ModReplayV6;
import com.rian.osu.utils.LRUCache;
import com.rian.osu.utils.ModUtils;

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
import java.util.concurrent.CancellationException;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;

import kotlinx.coroutines.Job;
import kotlinx.coroutines.JobKt;
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
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.SkinLayout;

public class SongMenu implements IUpdateHandler, MenuItemListener,
        IScrollBarListener {
    public UIScene scene;
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
    private String backgroundPath = "";
    private ScoreBoard board;
    private Float touchY = null;
    private String filterText = "";
    private boolean favsOnly = false;
    @Nullable private List<String> limitC;
    private float maxY = 100500;
    private int pointerId = -1;
    private float initalY = -1;
    private float secPassed = 0, tapTime;
    private ScrollBar scrollbar;
    private boolean allowAutomaticPlaybackRestart = true;
    private ValueAnimator musicVolumeAnimator;

    private Job calculationJob,
                musicLoadingJob,
                backgroundLoadingJob,
                mapStatusJob;

    private ChangeableText
            beatmapMetadataText,
            beatmapCreatorText,
            beatmapLengthText,
            beatmapHitObjectsText,
            beatmapDifficultyText;

    private Scene.ITouchArea currentPressedButton;
    private UISprite scoringSwitcher = null;
    private SearchBarFragment searchBar = null;
    private GroupType groupType = GroupType.MapSet;

    private Timer previousSelectionTimer;
    private final long previousSelectionInterval = 1000;
    private boolean previousSelectionPerformed;
    private final LinkedList<BeatmapSetItem> previousSelectedItems = new LinkedList<>();
    private final LRUCache<String, RankedStatus> mapStatuses = new LRUCache<>(50);

    public SongMenu() {
    }

    public static void stopMusicStatic() {
        var songService = GlobalManager.getInstance().getSongService();

        if (songService != null) {
            songService.stop();
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
            filterMenu.getFavoriteFolder().isEmpty() ? null : DatabaseManager.getBeatmapCollectionsTable().getBeatmaps(filterMenu.getFavoriteFolder())
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
        scene = new UIScene();
        // This is needed for UIScene to behave on par with regular Scene, otherwise we would have weird scenarios such
        // as entities in the back layer having touch priority despite being rendered behind the front layer.
        scene.setOnAreaTouchTraversalBackToFront();
        camY = 0;
        velocityY = 0;
        selectedItem = null;
        items = new ArrayList<>();
        selectedBeatmap = null;
        SongMenuPool.getInstance().init();
        loadFilterFragment();
        updateMusicEffects();

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
                    .getFont("CaptionFont"), "There are no songs in library, try using the beatmap downloader.",
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
                ResourceManager.getInstance().getFont("smallFont"), "dimensionInfo", 1024) {
            private boolean moved = false;
            private float dx = 0, dy = 0;

            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (currentPressedButton == null) {
                        currentPressedButton = this;
                        moved = false;
                        dx = pTouchAreaLocalX;
                        dy = pTouchAreaLocalY;
                    }
                    return true;
                }

                if (pSceneTouchEvent.isActionUp()) {
                    if (currentPressedButton == this) {
                        currentPressedButton = null;
                        var selectedBeatmap = SongMenu.this.selectedBeatmap;

                        if (selectedBeatmap != null && !moved) {
                            var mods = ModMenu.INSTANCE.getEnabledMods();
                            new BeatmapAttributeDisplay(selectedBeatmap.getBeatmapDifficulty(), mods.values()).show();
                        }
                    }
                    return true;
                }

                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50) && currentPressedButton == this) {
                    currentPressedButton = null;
                    moved = true;
                }

                return false;
            }
        };

        frontLayer.attachChild(beatmapDifficultyText);
        scene.registerTouchArea(beatmapDifficultyText);

        var clickShortSound = ResourceManager.getInstance().getSound("click-short");
        var clickShortConfirmSound = ResourceManager.getInstance().getSound("click-short-confirm");

        SkinLayout layoutBackButton = OsuSkin.get().getLayout("BackButton");
        SkinLayout layoutMods = null;

        if (!Multiplayer.isMultiplayer)
            layoutMods = OsuSkin.get().getLayout("ModsButton");

        SkinLayout layoutOptions = OsuSkin.get().getLayout("OptionsButton");
        SkinLayout layoutRandom = OsuSkin.get().getLayout("RandomButton");

        var backButton = new UIAnimatedSprite("menu-back", true, OsuSkin.get().getAnimationFramerate()) {
            boolean moved = false;
            float dx = 0, dy = 0;
            boolean scaleWhenHold = true;

            {
                if (layoutBackButton != null) {
                    scaleWhenHold = layoutBackButton.property.optBoolean("scaleWhenHold", true);
                }

                setSize(getWidth(), getHeight());
            }

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (currentPressedButton == null) {
                        currentPressedButton = this;
                        if (scaleWhenHold) setScale(1.25f);
                        moved = false;
                        dx = pTouchAreaLocalX;
                        dy = pTouchAreaLocalY;
                        BassSoundProvider playSnd = ResourceManager.getInstance().getSound("menuback");
                        if (playSnd != null) {
                            playSnd.play();
                        }
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    if (currentPressedButton == this) {
                        currentPressedButton = null;

                        if (selectedBeatmap != null && !moved) {
                            setScale(1f);
                            back();
                        }
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50) && currentPressedButton == this) {
                    currentPressedButton = null;
                    setScale(1f);
                    moved = true;
                }
                return false;
            }
        };

        UISprite modSelection = null;

        if (!Multiplayer.isMultiplayer)
            modSelection = new UISprite() {
                boolean moved = false;
                private float dx = 0, dy = 0;

                {
                    setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-mods"));
                    setSize(getWidth(), getHeight());
                }

                @Override
                public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                             final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                    if (pSceneTouchEvent.isActionDown()) {
                        if (currentPressedButton == null) {
                            currentPressedButton = this;
                            setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-mods-over"));
                            moved = false;
                            dx = pTouchAreaLocalX;
                            dy = pTouchAreaLocalY;
                        }
                        return true;
                    }
                    if (pSceneTouchEvent.isActionUp()) {
                        if (currentPressedButton == this) {
                            currentPressedButton = null;

                            if (!moved) {
                                setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-mods"));
                                velocityY = 0;
                                if (clickShortConfirmSound != null) {
                                    clickShortConfirmSound.play();
                                }
                                ModMenu.INSTANCE.show();
                            }
                        }
                        return true;
                    }
                    if (pSceneTouchEvent.isActionOutside()
                            || pSceneTouchEvent.isActionMove()
                            && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                            pTouchAreaLocalY) > 50) && currentPressedButton == this) {
                        if (!moved && clickShortSound != null) {
                            clickShortSound.play();
                        }

                        currentPressedButton = null;
                        moved = true;
                        setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-mods"));
                    }
                    return false;
                }
            };

        var optionSelection = new UISprite() {
            boolean moved = false;
            private float dx = 0, dy = 0;

            {
                setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-options"));
                setSize(getWidth(), getHeight());
            }

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (currentPressedButton == null) {
                        currentPressedButton = this;
                        setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-options-over"));
                        moved = false;
                        dx = pTouchAreaLocalX;
                        dy = pTouchAreaLocalY;
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    if (currentPressedButton == this) {
                        currentPressedButton = null;

                        if (!moved) {
                            setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-options"));
                            velocityY = 0;
                            if (clickShortConfirmSound != null) {
                                clickShortConfirmSound.play();
                            }

                            searchBar.loadConfig(context);
                            searchBar.showMenu(SongMenu.this);
                        }
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50) && currentPressedButton == this) {
                    if (!moved && clickShortSound != null) {
                        clickShortSound.play();
                    }

                    currentPressedButton = null;
                    moved = true;
                    setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-options"));
                }
                return false;
            }
        };

        var randomMap = new UISprite() {
            boolean moved = false;
            private float dx = 0, dy = 0;

            {
                setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-random"));
                setSize(getWidth(), getHeight());
            }

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (currentPressedButton == null) {
                        currentPressedButton = this;

                        setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-random-over"));
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
                                BeatmapSetItem previousItem = previousSelectedItems.pollLast();
                                while (previousItem != null && previousItem.isDeleted()) {
                                    previousItem = previousSelectedItems.pollLast();
                                }

                                if (previousItem == null) {
                                    cancel();
                                    return;
                                }

                                previousSelectionPerformed = true;

                                if (clickShortConfirmSound != null) {
                                    clickShortConfirmSound.play();
                                }

                                previousItem.select();
                            }

                            @Override
                            public boolean cancel() {
                                previousSelectionTimer = null;
                                return super.cancel();
                            }
                        }, previousSelectionInterval, previousSelectionInterval);

                        previousSelectionPerformed = false;
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    if (currentPressedButton == this) {
                        currentPressedButton = null;

                        setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-random"));

                        if (previousSelectionTimer != null) {
                            previousSelectionTimer.cancel();
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
                            if (clickShortConfirmSound != null) {
                                clickShortConfirmSound.play();
                            }
                            items.get(index).select();
                        }

                        previousSelectionPerformed = false;
                    }

                    return true;
                }
                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50) && currentPressedButton == this) {
                    if (!moved && clickShortSound != null) {
                        clickShortSound.play();
                    }

                    currentPressedButton = null;
                    moved = true;
                    setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("selection-random"));

                    if (previousSelectionTimer != null) {
                        previousSelectionTimer.cancel();
                    }
                }
                return false;
            }
        };

        if (modSelection != null)
            modSelection.setScale(1.5f);

        optionSelection.setScale(1.5f);
        randomMap.setScale(1.5f);

        var isNewLayout = OsuSkin.get().isUseNewLayout();

        if (isNewLayout && layoutBackButton != null) {
            layoutBackButton.apply(backButton);
        } else {
            backButton.setPosition(0, Config.getRES_HEIGHT() - backButton.getHeightScaled());
        }

        if (modSelection != null) {
            if (isNewLayout && layoutMods != null) {
                layoutMods.apply(modSelection, backButton);
            } else {
                modSelection.setPosition(backButton.getX() + backButton.getWidthScaled(), Config.getRES_HEIGHT() - modSelection.getHeightScaled());
            }
        }

        if (isNewLayout && layoutOptions != null) {
            layoutOptions.apply(optionSelection, modSelection != null ? modSelection : backButton);
        } else {
            var prevButton = modSelection != null ? modSelection : backButton;
            optionSelection.setPosition(prevButton.getX() + prevButton.getWidthScaled(), Config.getRES_HEIGHT() - optionSelection.getHeightScaled());
        }

        if (isNewLayout && layoutRandom != null) {
            layoutRandom.apply(randomMap, optionSelection);
        } else {
            randomMap.setPosition(optionSelection.getX() + optionSelection.getWidthScaled(), Config.getRES_HEIGHT() - randomMap.getHeightScaled());
        }

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

        if (OnlineScoring.getInstance().createSecondPanel() != null) {
            OnlinePanel panel = OnlineScoring.getInstance().getSecondPanel();
            panel.detachSelf();
            panel.setPosition(randomMap.getX() + randomMap.getWidthScaled() + 20, Config.getRES_HEIGHT() - 110);
            OnlineScoring.getInstance().loadAvatar(false);
            frontLayer.attachChild(panel);

            scoringSwitcher = new UISprite() {
                @Override
                public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                    if (!pSceneTouchEvent.isActionDown()) return false;
                    toggleScoringSwitcher();
                    return true;
                }
            };
            scoringSwitcher.setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("ranking_disabled"));
            scoringSwitcher.setPosition(10, 10);
            if (!Multiplayer.isMultiplayer) {
                scene.registerTouchArea(panel.rect);
            }
            scene.registerTouchArea(scoringSwitcher);
            frontLayer.attachChild(scoringSwitcher);
        }
    }

    public void loadFilterFragment() {
        searchBar = new SearchBarFragment();
        searchBar.loadConfig(context);
    }

    public void toggleScoringSwitcher() {
        if (board.isShowOnlineScores()) {
            switch (Config.getBeatmapLeaderboardScoringMode()) {
                case SCORE -> Config.setBeatmapLeaderboardScoringMode(BeatmapLeaderboardScoringMode.PP);
                case PP -> board.setShowOnlineScores(false);
            }

            board.init(selectedBeatmap);
        } else if (OnlineManager.getInstance().isStayOnline()) {
            board.setShowOnlineScores(true);
            Config.setBeatmapLeaderboardScoringMode(BeatmapLeaderboardScoringMode.SCORE);
            board.init(selectedBeatmap);
        }

        updateScoringSwitcherStatus(true);
    }

    public UIScene getScene() {
        return scene;
    }
    public SearchBarFragment getSearchBar() { return searchBar; }

    public void show() {
        engine.setScene(scene);
    }

    public void setFilter(final String filter, final SortOrder order,
                          final boolean favsOnly, @Nullable List<String> limit) {
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
        }
    }

    public void sort() {
        if (!sortOrder.equals(searchBar.getOrder())) {
            sortOrder = searchBar.getOrder();
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
        increaseBackgroundLuminance(pSecondsElapsed);

        if (Config.isPlayMusicPreview()) {
            var songService = GlobalManager.getInstance().getSongService();
            if (allowAutomaticPlaybackRestart && songService != null) {
                int length = songService.getLength();

                // We want to restart playback.
                // Checking for 0 length here sounds counterintuitive, but BASS returns -1 for streams with finished
                // playback, even without automatic freeing flag. Weird...
                if (selectedBeatmap != null && length == 0) {
                    allowAutomaticPlaybackRestart = false;
                    playMusic(selectedBeatmap.getAudioPath(), selectedBeatmap.getPreviewTime());
                }
            }
        }

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

    public void increaseBackgroundLuminance(final float pSecondsElapsed) {
        if (bg != null && bg.getRed() < 1) {
            final float col = Math.min(1, bg.getRed() + pSecondsElapsed);
            bg.setColor(col, col, col);
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

        var mods = ModMenu.INSTANCE.getEnabledMods();
        boolean isPreciseMod = mods.contains(ModPrecise.class);
        float totalSpeedMultiplier = ModUtils.calculateRateWithMods(mods.values(), Double.POSITIVE_INFINITY);

        var difficulty = beatmapInfo.getBeatmapDifficulty().clone();
        ModUtils.applyModsToBeatmapDifficulty(difficulty, GameMode.Droid, mods.values(), true);

        // Round to 2 decimal places.
        difficulty.gameplayCS = GameHelper.Round(difficulty.gameplayCS, 2);
        difficulty.setAR(GameHelper.Round(difficulty.getAR(), 2));
        difficulty.od = GameHelper.Round(difficulty.od, 2);
        difficulty.hp = GameHelper.Round(difficulty.hp, 2);

        // Update texts
        float originalCS = beatmapInfo.getCircleSize();
        float originalAR = beatmapInfo.getApproachRate();
        float originalOD = beatmapInfo.getOverallDifficulty();
        float originalHP = beatmapInfo.getHpDrainRate();

        beatmapDifficultyText.setColor(1, 1, 1);
        beatmapLengthText.setColor(1, 1, 1);

        if (mods.contains(ModDifficultyAdjust.class)) {
            if (isPreciseMod) {
                beatmapDifficultyText.setColor(1, 120 / 255f, 0);
            } else {
                beatmapDifficultyText.setColor(1, 180 / 255f, 0);
            }
        } else if (
            (!Precision.almostEquals(originalCS, difficulty.gameplayCS) && originalCS < difficulty.gameplayCS) ||
            (!Precision.almostEquals(originalAR, difficulty.getAR()) && originalAR < difficulty.getAR()) ||
            (!Precision.almostEquals(originalOD, difficulty.od) && originalOD < difficulty.od) ||
            (!Precision.almostEquals(originalOD, difficulty.hp) && originalHP < difficulty.hp)
        ) {
            if (isPreciseMod) {
                beatmapDifficultyText.setColor(214 / 255f, 45 / 255f, 45 / 255f);
            } else {
                beatmapDifficultyText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
            }
        } else if (
            (!Precision.almostEquals(originalCS, difficulty.gameplayCS) && originalCS > difficulty.gameplayCS) ||
            (!Precision.almostEquals(originalAR, difficulty.getAR()) && originalAR > difficulty.getAR()) ||
            (!Precision.almostEquals(originalOD, difficulty.od) && originalOD > difficulty.od) ||
            (!Precision.almostEquals(originalOD, difficulty.hp) && originalHP > difficulty.hp)
        ) {
            if (isPreciseMod) {
                beatmapDifficultyText.setColor(158 / 255f, 108 / 255f, 65 / 255f);
            } else {
                beatmapDifficultyText.setColor(46 / 255f, 139 / 255f, 87 / 255f);
            }
        } else if (isPreciseMod) {
            beatmapDifficultyText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
        }

        if (totalSpeedMultiplier > 1) {
            beatmapLengthText.setColor(205 / 255f, 85 / 255f, 85 / 255f);
        } else if (totalSpeedMultiplier < 1) {
            beatmapLengthText.setColor(46 / 255f, 139 / 255f, 87 / 255f);
        }

        int minBpm = Math.round(beatmapInfo.getBpmMin() * totalSpeedMultiplier);
        int maxBpm = Math.round(beatmapInfo.getBpmMax() * totalSpeedMultiplier);
        int commonBpm = Math.round(beatmapInfo.getMostCommonBPM() * totalSpeedMultiplier);
        long length = (long) (beatmapInfo.getLength() / totalSpeedMultiplier);

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat(length > 3600 * 1000 ? "HH:mm:ss" : "mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        String binfoStr = String.format(StringTable.get(com.osudroid.resources.R.string.binfoStr1), sdf.format(length),
                (minBpm == maxBpm ? commonBpm : minBpm + "-" + maxBpm + " (" + commonBpm + ")"),
                beatmapInfo.getMaxCombo());

        beatmapLengthText.setText(binfoStr);

        String str = beatmapDifficultyText.getText();
        String[] strs = str.split("Stars: ");

        beatmapDifficultyText.setText(
            "AR: " + difficulty.getAR() + " " +
            "OD: " + difficulty.od + " " +
            "CS: " + difficulty.difficultyCS + " " +
            "HP: " + difficulty.hp + " " +
            "Stars: " + (strs.length == 2 ? strs[1] : GameHelper.Round(beatmapInfo.getStarRating(), 2))
        );
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
        String binfoStr2 = StringTable.format(com.osudroid.resources.R.string.binfoStr2,
                beatmapInfo.getHitCircleCount(),
                beatmapInfo.getSliderCount(),
                beatmapInfo.getSpinnerCount(),
                beatmapInfo.getSetId()
        );
        beatmapMetadataText.setText(tinfoStr);
        beatmapCreatorText.setText(mapperStr);
        beatmapHitObjectsText.setText(binfoStr2);
        changeDimensionInfo(beatmapInfo);
        setStarsDisplay(beatmapInfo.getStarRating());
        cancelCalculationJobs();

        calculationJob = Execution.async(scope -> {
            try (var parser = new BeatmapParser(beatmapInfo.getPath(), scope)) {
                var data = parser.parse(true);

                // Do not update if the beatmap has been changed.
                if (data != null && selectedBeatmap != null && !data.getMd5().equals(selectedBeatmap.getMD5())) {
                    return;
                }

                if (data == null) {
                    setStarsDisplay(0);
                    return;
                }

                var newInfo = BeatmapInfo(data, beatmapInfo.getDateImported(), true, scope);
                beatmapInfo.apply(newInfo);
                DatabaseManager.getBeatmapInfoTable().update(newInfo);

                changeDimensionInfo(beatmapInfo);

                // Copy the mods to avoid concurrent modification
                var mods = ModMenu.INSTANCE.getEnabledMods().deepCopy().values();

                var attributes = switch (Config.getDifficultyAlgorithm()) {
                    case droid -> BeatmapDifficultyCalculator.calculateDroidDifficulty(data, mods, scope);
                    case standard -> BeatmapDifficultyCalculator.calculateStandardDifficulty(data, mods, scope);
                };

                setStarsDisplay((float) attributes.starRating);
            }
        });
    }

    @Override
    public void selectBeatmap(final BeatmapInfo beatmapInfo, boolean reloadBG) {

        // Playing corresponding audio for the selected track.
        var selectedAudio = selectedBeatmap != null ? selectedBeatmap : GlobalManager.getInstance().getSelectedBeatmap();

        if (selectedAudio == null || !Objects.equals(selectedAudio.getAudioPath(), beatmapInfo.getAudioPath())) {
            playMusic(beatmapInfo.getAudioPath(), beatmapInfo.getPreviewTime());
        }

        if (selectedBeatmap != null && selectedBeatmap.getFilename().equals(beatmapInfo.getFilename())) {
            // Do not initiate gameplay if the player is still holding one of the menu buttons.
            if (currentPressedButton != null) {
                return;
            }

            ResourceManager.getInstance().getSound("menuhit").play();
            cancelCalculationJobs();
            cancelMapStatusLoadingJob();

            if (Multiplayer.isMultiplayer)
            {
                setMultiplayerRoomBeatmap(selectedBeatmap);
                back(false);
                return;
            }
            stopMusic();

            Execution.mainThread(() -> {
                ModMenu.INSTANCE.back();
                if (searchBar != null) {
                    searchBar.dismiss();
                }
            });

            game.startGame(beatmapInfo, null, ModMenu.INSTANCE.getEnabledMods());
            return;
        }
        selectedBeatmap = beatmapInfo;
        GlobalManager.getInstance().setSelectedBeatmap(beatmapInfo);
        cancelCalculationJobs();
        cancelMapStatusLoadingJob();
        updateInfo(beatmapInfo);
        updateScoringSwitcherStatus(false);
        board.init(beatmapInfo);

        if (!reloadBG && (beatmapInfo.getBackgroundFilename() == null || backgroundPath.equals(beatmapInfo.getBackgroundPath()))) {
            return;
        }
        backgroundPath = beatmapInfo.getBackgroundPath();
        bg = null;
        scene.setBackground(new ColorBackground(0, 0, 0));

        if (backgroundLoadingJob != null) {
            backgroundLoadingJob.cancel(new CancellationException("Background loading has been cancelled."));
        }

        backgroundLoadingJob = Execution.async(scope -> {
            JobKt.ensureActive(scope.getCoroutineContext());

            TextureRegion tex = Config.isSafeBeatmapBg() || beatmapInfo.getBackgroundFilename() == null?
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

            JobKt.ensureActive(scope.getCoroutineContext());

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

            JobKt.ensureActive(scope.getCoroutineContext());

            Execution.updateThread(() -> {
                if (selectedBeatmap != null && !selectedBeatmap.getFilename().equals(beatmapInfo.getFilename())) {
                    return;
                }

                scene.setBackground(new SpriteBackground(bg));
            });
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
        var difficulty = selectedBeatmap.getBeatmapDifficulty();

        if (showOnline) {
            engine.setScene(new LoadingScreen().getScene());
            ToastLogger.showText(com.osudroid.resources.R.string.online_loadrecord, false);

            cancelCalculationJobs();
            cancelMapStatusLoadingJob();

            Execution.async(() -> {
                try {
                    String scorePack = OnlineManager.getInstance().getScorePack(id);
                    String[] params = scorePack.split("\\s+");

                    if (params.length < 11) return;

                    StatisticV2 stat = new StatisticV2(params, difficulty);

                    stat.setPlayerName(playerName);
                    scoreScene.load(stat, null, null, OnlineManager.getReplayURL(id), null, selectedBeatmap);
                    engine.setScene(scoreScene.getScene());
                } catch (Exception e) {
                    Debug.e("Cannot load play info: " + e.getMessage(), e);
                    engine.setScene(scene);
                }
            });
            return;
        }

        var score = DatabaseManager.getScoreInfoTable().getScore(id);

        if (score == null) {
            ToastLogger.showText("Could not open score", true);
            return;
        }

        StatisticV2 stat;

        try {
            stat = score.toStatisticV2(difficulty);
        } catch (JSONException e1) {
            // When this happens, the mods are likely in the old format (that somehow was not converted during
            // migration). Convert them.
            var convertedMods = LegacyModConverter.convert(score.getMods());

            // Scores that are using the legacy mods format are guaranteed to use these mods.
            convertedMods.put(new ModReplayV6());

            score.setMods(convertedMods.serializeMods().toString());
            DatabaseManager.getScoreInfoTable().updateScore(score);

            try {
                stat = score.toStatisticV2(difficulty);
            } catch (JSONException e2) {
                ToastLogger.showText("Could not open score", true);
                return;
            }
        }

        scoreScene.load(stat, null, null, Config.getScorePath() + stat.getReplayFilename(), null, selectedBeatmap);
        engine.setScene(scoreScene.getScene());
    }


    public void onScroll(final float where) {
        velocityY = 0;
        camY = where - Config.getRES_HEIGHT() / 2f;
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

        resetMusicEffects();
        startMusicVolumeAnimation(0.5f);
        GlobalManager.getInstance().getMainScene().show();
    }

    private void resetMultiplayerRoomBeatmap() {
        if (!Multiplayer.isMultiplayer) {
            return;
        }

        // Locking host from change beatmap before the server responses to beatmapChange
        RoomScene.isWaitingForBeatmapChange = true;

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
        RoomScene.isWaitingForBeatmapChange = true;

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

    public void startMusicVolumeAnimation() {
        startMusicVolumeAnimation(0, 1);
    }

    public void startMusicVolumeAnimation(float initialVolume) {
        startMusicVolumeAnimation(initialVolume, 1);
    }

    public void startMusicVolumeAnimation(float initialVolume, float endVolume) {
        stopMusicVolumeAnimation();

        var songService = GlobalManager.getInstance().getSongService();
        if (songService == null || songService.getStatus() == Status.STOPPED) {
            return;
        }

        songService.setVolume(initialVolume * Config.getBgmVolume());

        if (initialVolume == endVolume) {
            return;
        }

        Execution.mainThread(() -> {
            musicVolumeAnimator = ValueAnimator.ofFloat(initialVolume, endVolume);
            // See https://github.com/ppy/osu/blob/790f863e0654fd563b57ab699d6be86895e756ab/osu.Game/Overlays/MusicController.cs#L529-L535
            // for the duration. Since SongService only supports one BASS channel at a time, we cannot apply part of the animation
            // to the previous song.
            musicVolumeAnimator.setDuration((long) (800 * Math.abs(endVolume - initialVolume)));
            musicVolumeAnimator.setInterpolator(input -> EasingKt.interpolate(Easing.Out, input));
            musicVolumeAnimator.addUpdateListener(animation -> {
                var animatorSongService = GlobalManager.getInstance().getSongService();
                if (animatorSongService == null) {
                    return;
                }

                if (animatorSongService.getStatus() == Status.STOPPED) {
                    musicVolumeAnimator.cancel();
                    return;
                }

                animatorSongService.setVolume(animation.getAnimatedFraction() * Config.getBgmVolume());
            });

            musicVolumeAnimator.start();
        });
    }

    public void stopMusicVolumeAnimation() {
        Execution.mainThread(() -> {
            if (musicVolumeAnimator != null) {
                musicVolumeAnimator.cancel();
            }
        });
    }

    public void stopMusic() {
        stopMusicVolumeAnimation();

        var songService = GlobalManager.getInstance().getSongService();

        if (songService != null) {
            GlobalManager.getInstance().getSongService().stop();
        }
    }

    public void playMusic(final String filePath, final int previewTime) {
        if (!Config.isPlayMusicPreview()) {
            return;
        }

        if (musicLoadingJob != null) {
            musicLoadingJob.cancel(new CancellationException("Music loading has been cancelled."));
        }

        stopMusicVolumeAnimation();
        allowAutomaticPlaybackRestart = false;
        musicLoadingJob = Execution.async(scope -> {
            var songService = GlobalManager.getInstance().getSongService();
            if (songService != null) {
                songService.stop();
            }

            try {
                JobKt.ensureActive(scope.getCoroutineContext());

                songService.preLoad(filePath);
                updateMusicEffects();

                JobKt.ensureActive(scope.getCoroutineContext());
                songService.play();
                startMusicVolumeAnimation();
                if (previewTime >= 0) {
                    songService.seekTo(previewTime);
                } else {
                    songService.seekTo((int) (songService.getLength() * 0.4f));
                }

                allowAutomaticPlaybackRestart = true;
            } catch (final Exception e) {
                if (e instanceof CancellationException) {
                    throw e;
                }

                Debug.e("LoadingMusic: " + e.getMessage(), e);
            }
        });
    }

    public void updateMusicEffects() {
        var songService = GlobalManager.getInstance().getSongService();
        if (songService == null) {
            return;
        }

        var enabledMods = ModMenu.INSTANCE.getEnabledMods();
        float speed = ModUtils.calculateRateWithMods(enabledMods.values(), Double.POSITIVE_INFINITY);
        boolean adjustPitch = Config.isShiftPitchInRateChange() || enabledMods.contains(ModNightCore.class);

        songService.setSpeed(speed);
        songService.setAdjustPitch(adjustPitch);
    }

    public void resetMusicEffects() {
        var songService = GlobalManager.getInstance().getSongService();
        if (songService == null) {
            return;
        }

        songService.setSpeed(1);
        songService.setAdjustPitch(false);
    }

    public boolean isSelectAllowed() {
        return true;
    }

    public void showPropertiesMenu(BeatmapSetItem item) {
        if (item == null) {
            if (selectedItem == null) {
                return;
            }
            item = selectedItem;
        }
        (new BeatmapPropertiesFragment()).show(SongMenu.this, item);
    }

    public void showDeleteScoreMenu(int scoreId) {
        (new ScoreMenuFragment()).show(selectedBeatmap, scoreId);
    }

    public void select() {
        if (GlobalManager.getInstance().getSelectedBeatmap() != null) {
            BeatmapInfo beatmapInfo = GlobalManager.getInstance().getSelectedBeatmap();

            var i = items.size() - 1;
            while (i >= 0) {
                var item = items.get(i);
                if (item.getBeatmapSetInfo().getDirectory().equals(beatmapInfo.getSetDirectory())) {
                    item.select();
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
            final String lowerFilter = searchBar.getFilter().toLowerCase();
            final boolean favsOnly = searchBar.isFavoritesOnly();

            var limit = DatabaseManager.getBeatmapCollectionsTable().getBeatmaps(searchBar.getFavoriteFolder());
            for (final BeatmapSetItem item : items) {
                item.applyFilter(lowerFilter, favsOnly, limit);
            }
        }
    }

    public void setStarsDisplay(float star) {
        String str = beatmapDifficultyText.getText();
        String[] strs = str.split("Stars: ");
        if (strs.length == 2) {
            beatmapDifficultyText.setText(strs[0] + "Stars: " + GameHelper.Round(star, 2));
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
                    item.select(false);
                    if (beatmapId != 0) {
                        item.selectBeatmap(item.getBeatmapSpritesById(beatmapId), false);
                    }
                    break;
                }
            }
        }
    }

    private void cancelCalculationJobs() {
        if (calculationJob != null) {
            calculationJob.cancel(new CancellationException("Difficulty calculation has been cancelled."));
        }

        ModMenu.INSTANCE.cancelCalculationJob();
    }

    private void cancelMapStatusLoadingJob() {
        if (mapStatusJob != null) {
            mapStatusJob.cancel(new CancellationException("Beatmap status check has been cancelled."));
        }
    }

    private void updateScoringSwitcherStatus(boolean forceUpdate) {
        if (scoringSwitcher == null) {
            return;
        }

        if (selectedBeatmap == null || !board.isShowOnlineScores()) {
            scoringSwitcher.setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("ranking_disabled"));
            return;
        }

        var md5 = selectedBeatmap.getMD5();
        var cachedStatus = mapStatuses.get(md5);

        if (!forceUpdate && cachedStatus != null) {
            scoringSwitcher.setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded(
                "selection-" + switch (cachedStatus) {
                    case ranked, approved, loved -> cachedStatus.name().toLowerCase();
                    default -> "question";
                }
            ));
            return;
        }

        var textureName = "ranking_enabled_" + switch (Config.getBeatmapLeaderboardScoringMode()) {
            case SCORE -> "score";
            case PP -> "pp";
        };

        scoringSwitcher.setTextureRegion(ResourceManager.getInstance().getTexture(textureName));

        cancelMapStatusLoadingJob();

        mapStatusJob = Execution.async(scope -> {
            try {
                JobKt.ensureActive(scope.getCoroutineContext());

                var status = OnlineManager.getInstance().getBeatmapStatus(md5);

                if (!board.isShowOnlineScores() || status == null || scoringSwitcher == null || selectedBeatmap == null || !selectedBeatmap.getMD5().equals(md5)) {
                    return;
                }

                mapStatuses.put(md5, status);

                JobKt.ensureActive(scope.getCoroutineContext());

                scoringSwitcher.setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded(
                    "selection-" + switch (status) {
                        case ranked, approved, loved -> status.name().toLowerCase();
                        default -> "question";
                    }
                ));
            } catch (OnlineManagerException e) {
                Debug.e("Cannot get beatmap status: " + e.getMessage(), e);

                JobKt.ensureActive(scope.getCoroutineContext());

                if (scoringSwitcher != null) {
                    scoringSwitcher.setTextureRegion(ResourceManager.getInstance().getTexture(textureName));
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
