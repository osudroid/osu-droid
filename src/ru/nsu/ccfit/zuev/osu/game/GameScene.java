package ru.nsu.ccfit.zuev.osu.game;

import static kotlinx.coroutines.JobKt.ensureActive;

import android.graphics.PointF;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;

import kotlin.Unit;
import kotlin.random.Random;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Job;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.SecurityUtils;

import com.acivev.VibratorManager;
import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.edlplan.osu.support.slider.SliderBody;
import com.edlplan.framework.support.ProxySprite;
import com.edlplan.framework.support.osb.StoryboardSprite;
import com.edlplan.framework.utils.functionality.SmartIterator;
import com.osudroid.beatmaps.BeatmapCache;
import com.osudroid.game.Cursor;
import com.osudroid.game.CursorEvent;
import com.osudroid.multiplayer.api.RoomAPI;
import com.osudroid.beatmaps.DifficultyCalculationManager;
import com.osudroid.data.BeatmapInfo;
import com.osudroid.ui.v2.GameLoaderScene;
import com.osudroid.data.DatabaseManager;
import com.osudroid.ui.v2.hud.elements.HUDLeaderboard;
import com.osudroid.ui.v2.modmenu.ModIcon;
import com.osudroid.utils.Execution;
import com.reco1l.andengine.Cameras;
import com.reco1l.andengine.UIEngine;
import com.reco1l.andengine.component.ComponentsKt;
import com.reco1l.andengine.shape.PaintStyle;
import com.reco1l.andengine.shape.UIBox;
import com.reco1l.andengine.sprite.UIAnimatedSprite;
import com.reco1l.andengine.sprite.UISprite;
import com.reco1l.andengine.modifier.Modifiers;
import com.reco1l.andengine.Anchor;
import com.reco1l.andengine.sprite.UIVideoSprite;
import com.reco1l.andengine.UIScene;
import com.osudroid.resources.R;
import com.osudroid.ui.v2.game.FollowPointConnection;
import com.osudroid.ui.v2.hud.GameplayHUD;
import com.osudroid.ui.v2.game.SliderTickSprite;
import com.osudroid.ui.v2.hud.elements.HUDPPCounter;
import com.osudroid.multiplayer.Multiplayer;

import com.reco1l.framework.Color4;
import com.osudroid.GameMode;
import com.osudroid.beatmaps.Beatmap;
import com.osudroid.beatmaps.ComboColor;
import com.osudroid.beatmaps.DroidPlayableBeatmap;
import com.osudroid.beatmaps.HitWindow;
import com.osudroid.beatmaps.constants.BeatmapCountdown;
import com.osudroid.beatmaps.constants.HitObjectType;
import com.osudroid.beatmaps.hitobjects.HitCircle;
import com.osudroid.beatmaps.hitobjects.HitObject;
import com.osudroid.beatmaps.hitobjects.Slider;
import com.osudroid.beatmaps.hitobjects.Spinner;
import com.osudroid.beatmaps.sections.BeatmapDifficulty;
import com.osudroid.beatmaps.timings.BreakPeriod;
import com.osudroid.beatmaps.timings.EffectControlPoint;
import com.osudroid.beatmaps.timings.TimingControlPoint;
import com.osudroid.difficulty.BeatmapDifficultyCalculator;
import com.osudroid.difficulty.attributes.DroidDifficultyAttributes;
import com.osudroid.difficulty.attributes.StandardDifficultyAttributes;
import com.osudroid.difficulty.attributes.TimedDifficultyAttributes;
import com.osudroid.difficulty.calculator.DroidPerformanceCalculationParameters;
import com.osudroid.difficulty.calculator.PerformanceCalculationParameters;
import com.osudroid.difficulty.calculator.StandardPerformanceCalculationParameters;
import com.osudroid.game.GameplayHitSampleInfo;
import com.osudroid.math.Interpolation;
import com.osudroid.mods.*;
import com.osudroid.ui.FPSCounter;
import com.osudroid.utils.ModHashMap;
import com.osudroid.utils.ModUtils;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.TouchOptions;
import org.anddev.andengine.engine.options.WakeLockOptions;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.LoopEntityModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.EntityBackground;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.effect.Metronome;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.AutoCursor;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorEntity;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.PauseMenu;
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem;
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osu.scoring.TouchType;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager;

public class GameScene implements GameObjectListener, IOnSceneTouchListener {
    public static final int CursorCount = 10;
    private final int maximumActiveCursorCount = 3;
    private final UIEngine engine;
    private Cursor[] cursors = new Cursor[CursorCount];
    public String audioFilePath = null;
    private UIScene scene;
    private UIScene bgScene, mgScene, fgScene;
    private Scene oldScene;
    private UIBox sceneBorder;
    private Shape beatmapBackground;
    private Beatmap parsedBeatmap;
    private DroidPlayableBeatmap playableBeatmap;
    private BeatmapInfo lastBeatmapInfo;
    private ScoringScene scoringScene;
    private TimingControlPoint[] timingControlPoints;
    private int timingControlPointIndex;
    private EffectControlPoint[] effectControlPoints;
    private int effectControlPointIndex;
    private TimingControlPoint activeTimingPoint;
    private EffectControlPoint activeEffectPoint;
    private int lastObjectId = -1;
    private float leadOut = 0;
    private HitObject[] objects;
    private int objectIndex;
    private ArrayList<Color4> comboColors;
    private boolean comboWasMissed = false;
    private boolean comboWas100 = false;
    private ArrayList<GameObject> activeObjects;
    private ArrayList<GameObject> expiredObjects;
    private final Set<GameObject> processedExpiredObjects = Collections.newSetFromMap(new IdentityHashMap<>());
    private GameObject judgeableObject;
    private BreakPeriod[] breakPeriods;
    private int breakPeriodIndex;
    private Metronome metronome;
    private float scale;
    public StatisticV2 stat;
    private boolean gameStarted;
    private float totalOffset;
    private int totalLength = Integer.MAX_VALUE;
    private boolean paused;
    private UISprite skipBtn;
    private float skipTime;
    private boolean musicStarted;
    private double distToNextObject;
    private CursorEntity[] cursorSprites;
    private AutoCursor autoCursor;
    private FlashLightEntity flashlightSprite;
    private int mainCursorId = -1;
    private Replay replay;
    private boolean replaying;
    private String replayFilePath;
    public float offsetSum;
    public int offsetRegs;
    private Rectangle dimRectangle = null;
    private ComboBurst comboBurst;
    private int failcount = 0;
    private Color4 sliderBorderColor;
    private SliderPath[] sliderPaths = null;
    private SliderBody.RenderPathCache[] sliderRenderPaths = null;
    private int sliderIndex = 0;
    private UISprite unrankedSprite;
    private final ArrayList<IModApplicableToTrackRate> rateAdjustingMods = new ArrayList<>();

    @Nullable
    private Job storyboardLoadingJob;
    private StoryboardSprite storyboardSprite;
    private ProxySprite storyboardOverlayProxy;

    public HitWindow hitWindow;
    private ModHashMap lastMods;

    private final AtomicInteger loadingRequestId = new AtomicInteger(0);
    private CompletableFuture<?> loadingPipeline;
    private Job gameLoadingJob;

    private PerformanceCalculationParameters performanceCalculationParameters;
    private TimedDifficultyAttributes<DroidDifficultyAttributes>[] droidTimedDifficultyAttributes;
    private TimedDifficultyAttributes<StandardDifficultyAttributes>[] standardTimedDifficultyAttributes;

    // Game

    /**
     * Whether the game is over.
     */
    private boolean isGameOver = false;

    /**
     * The break time animator.
     */
    private BreakAnimator breakAnimator;

    /**
     * The countdown animator.
     */
    public Countdown countdownAnimator;

    /**
     * Whether the game is ready to start.
     */
    public boolean isReadyToStart = false;


    // UI

    /**
     * The gameplay HUD
     */
    public GameplayHUD hud;

    /**
     * Whether the HUD editor mode is enabled.
     */
    public boolean isHUDEditorMode = false;

    /**
     * Whether the game started in HUD editor mode.
     */
    public boolean startedFromHUDEditor = false;


    // Timing

    /**
     * The time at which the last frame was rendered with respect to {@link SystemClock#uptimeMillis()}.
     * <br>
     * If 0, a frame has not been rendered yet.
     */
    private long previousFrameTime;

    /**
     * The start time of the first object in seconds.
     */
    public float firstObjectStartTime;

    /**
     * The end time of the last object in seconds.
     */
    public float lastObjectEndTime;

    /**
     * The initial {@link #elapsedTime} value when the game started, in seconds.
     */
    public float initialElapsedTime = 0;

    /**
     * The time passed since the game has started, in seconds.
     */
    public float elapsedTime = 0;


    // Video support

    /**
     * The current video loading {@link Job}.
     */
    @Nullable
    private Job videoLoadingJob;

    /**
     * Whether video is enabled.
     */
    private boolean videoEnabled;

    /**The video sprite*/
    private UIVideoSprite video;

    /**Video offset aka video start time in seconds*/
    private float videoOffset;

    /**Whether the video has started*/
    private boolean videoStarted;


    // Multiplayer

    /**Indicates the last time that the user pressed the back button, used to reset {@code backPressCount}*/
    private float lastBackPressTime = -1f;

    /**Indicates that the player has failed and the score shouldn't be submitted*/
    public boolean hasFailed = false;

    /**Indicates that the player has requested skip*/
    private boolean isSkipRequested = false;

    /**Real time elapsed in milliseconds since the game has started*/
    private long realTimeElapsed = 0;

    /**Real time elapsed in milliseconds since the latest statistic data was sent*/
    private long statisticDataTimeElapsed = 0;

    /**Last score data chunk sent to server, used to determine if the data was changed.*/
    private ScoreBoardItem lastScoreSent = null;


    public GameScene(final UIEngine engine) {
        this.engine = engine;
        scene = createMainScene();
        bgScene = new UIScene();
        fgScene = new UIScene();
        mgScene = new UIScene();
        scene.attachChild(bgScene);
        scene.attachChild(mgScene);
        scene.attachChild(fgScene);
    }

    public void setScoringScene(final ScoringScene sc) {
        scoringScene = sc;
    }

    public void setOldScene(final Scene oscene) {
        oldScene = oscene;
    }

    private void loadBackground() {
        if (dimRectangle != null) {
            dimRectangle.detachSelf();
            dimRectangle = null;
        }

        if (sceneBorder != null) {
            sceneBorder.detachSelf();
            sceneBorder = null;
        }

        if (storyboardSprite != null) {
            storyboardSprite.detachSelf();
            storyboardSprite = null;
        }

        if (video != null) {
            video.release();
            video.detachSelf();
            video = null;
        }

        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        TextureRegion textureRegion = Config.isSafeBeatmapBg() || playableBeatmap.getEvents().backgroundFilename == null
                ? ResourceManager.getInstance().getTexture("menu-background")
                : ResourceManager.getInstance().getTextureIfLoaded("::background");

        if (textureRegion == null) {
            Rectangle rectangle = new Rectangle(0f, 0f, Config.getRES_WIDTH(), Config.getRES_HEIGHT());

            Color4 backgroundColor = playableBeatmap.getEvents().getBackgroundColor();
            if (backgroundColor == null) {
                backgroundColor = new Color4(0, 0, 0);
            }
            ComponentsKt.setColor4(rectangle, backgroundColor);

            beatmapBackground = rectangle;
        } else {
            beatmapBackground = new Sprite(0, 0, textureRegion.getWidth(), textureRegion.getHeight(), textureRegion);
        }
    }

    public void loadStoryboard(BeatmapInfo beatmapInfo) {
        if (storyboardSprite != null) {
            return;
        }

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        float brightness = Config.getInt("bgbrightness", 25) / 100f;
        boolean isStoryboardEnabled = brightness > 0.02f && Config.getBoolean("enableStoryboard", false);

        if (!isStoryboardEnabled) {
            cancelStoryboardLoading();
            return;
        }

        if (storyboardLoadingJob != null && !storyboardLoadingJob.isCompleted()) {
            return;
        }

        storyboardLoadingJob = Execution.async(scope -> {
            StoryboardSprite storyboardSprite = this.storyboardSprite;
            this.storyboardSprite = null;

            if (storyboardSprite != null) {
                storyboardSprite.detachSelf();
            } else {
                storyboardSprite = new StoryboardSprite(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                ensureActive(scope.getCoroutineContext());
            }

            ProxySprite storyboardOverlayProxy = this.storyboardOverlayProxy;
            this.storyboardOverlayProxy = null;

            if (storyboardOverlayProxy != null) {
                storyboardOverlayProxy.detachSelf();
            } else {
                storyboardSprite.setOverlayDrawProxy(storyboardOverlayProxy = new ProxySprite(Config.getRES_WIDTH(), Config.getRES_HEIGHT()));
                ensureActive(scope.getCoroutineContext());
            }

            storyboardSprite.setTransparentBackground(videoEnabled && video != null);
            storyboardSprite.loadStoryboard(beatmapInfo.getPath());
            ensureActive(scope.getCoroutineContext());

            this.storyboardSprite = storyboardSprite;
            this.storyboardOverlayProxy = storyboardOverlayProxy;

            // The storyboard may only load after gameplay is started, in which case we must apply it immediately.
            Execution.updateThread(this::applyBackground);

            storyboardLoadingJob = null;
        });
    }

    private void cancelStoryboardLoading() {
        if (storyboardLoadingJob != null) {
            storyboardLoadingJob.cancel(new CancellationException("Storyboard loading job cancelled"));
            storyboardLoadingJob = null;
        }
    }

    public void loadVideo(BeatmapInfo beatmapInfo) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || video != null) {
            return;
        }

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        float brightness = Config.getInt("bgbrightness", 25) / 100f;
        var videoFilename = playableBeatmap.getEvents().videoFilename;
        videoEnabled = brightness > 0.02f && Config.getBoolean("enableVideo", false) && videoFilename != null;

        if (!videoEnabled) {
            cancelVideoLoading();
            return;
        }

        if (videoLoadingJob != null && !videoLoadingJob.isCompleted()) {
            return;
        }

        videoLoadingJob = Execution.async(scope -> {
            try {
                var video = new UIVideoSprite(beatmapInfo.getAbsoluteSetDirectory() + "/" + videoFilename, engine);
                video.setAlpha(0f);

                ensureActive(scope.getCoroutineContext());

                this.video = video;

                // The video may only load after gameplay is started, in which case we must apply it immediately.
                Execution.updateThread(this::applyBackground);
            } catch (Exception e) {
                video = null;
                Log.e("GameScene", "Error while loading video background.", e);
            }

            videoLoadingJob = null;
        });
    }

    private void cancelVideoLoading() {
        if (videoLoadingJob != null) {
            videoLoadingJob.cancel(new CancellationException("Video loading job cancelled"));
            videoLoadingJob = null;
        }
    }

    private void applyBackground() {
        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        float brightness = Config.getInt("bgbrightness", 25) / 100f;

        boolean isStoryboardEnabled = brightness > 0.02f && Config.getBoolean("enableStoryboard", false);
        float playfieldSize = Config.getPlayfieldSize();

        var storyboardSprite = this.storyboardSprite;
        var storyboardOverlayProxy = this.storyboardOverlayProxy;
        var video = this.video;
        var sceneBorder = this.sceneBorder;

        if (sceneBorder == null && Config.isDisplayPlayfieldBorder() && playfieldSize < 1f) {
            sceneBorder = new UIBox() {
                {
                    setAnchor(Anchor.Center);
                    setOrigin(Anchor.Center);
                    setPaintStyle(PaintStyle.Outline);
                    setLineWidth(5f);
                    setColor(1f, 1f, 1f);
                    setAlpha(Interpolation.linear(0.2f, 0.8f, brightness));
                    setSize(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                }
            };

            scene.attachChild(sceneBorder, 0);
            this.sceneBorder = sceneBorder;
        }

        var background = videoEnabled && video != null ? video : beatmapBackground;

        if (dimRectangle != null) {
            dimRectangle.detachSelf();
        } else {
            dimRectangle = new Rectangle(0f, 0f, 0f, 0f);
        }

        dimRectangle.setSize(background.getWidth(), background.getHeight());
        dimRectangle.setColor(0f, 0f, 0f, 1f - brightness);
        background.attachChild(dimRectangle);

        if (breakAnimator != null) {
            breakAnimator.setDimRectangle(dimRectangle);
        }

        var factor = Config.isKeepBackgroundAspectRatio()
                ? Config.getRES_HEIGHT() / background.getHeight()
                : Config.getRES_WIDTH() / background.getWidth();

        background.setScale(factor);
        background.setPosition((Config.getRES_WIDTH() - background.getWidth()) / 2f, (Config.getRES_HEIGHT() - background.getHeight()) / 2f);
        scene.setBackground(new EntityBackground(background));

        if (storyboardSprite != null) {
            if (isStoryboardEnabled) {
                storyboardSprite.setTransparentBackground(videoEnabled && video != null);
                storyboardSprite.setBrightness(brightness);

                if (!storyboardSprite.hasParent()) {
                    scene.attachChild(storyboardSprite, 0);
                }
            } else {
                storyboardSprite.detachSelf();
            }
        }

        if (storyboardOverlayProxy != null) {
            if (isStoryboardEnabled) {
                if (!storyboardOverlayProxy.hasParent()) {
                    scene.attachChild(storyboardOverlayProxy, scene.getChildIndex(fgScene));
                }
            } else {
                storyboardOverlayProxy.detachSelf();
            }
        }
    }

    private boolean loadGame(final BeatmapInfo beatmapInfo, final String rFile, final ModHashMap mods, @Nullable CoroutineScope scope) {
        if (!SecurityUtils.verifyFileIntegrity(GlobalManager.getInstance().getMainActivity())) {
            ToastLogger.showText(com.osudroid.resources.R.string.file_integrity_tampered, true);
            return false;
        }

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        if (rFile != null && rFile.startsWith("https://")) {
            this.replayFilePath = Config.getCachePath() + "/" +
                    MD5Calculator.getStringMD5(rFile) + ".odr";
            Debug.i("ReplayFile = " + replayFilePath);
            if (!OnlineFileOperator.downloadFile(rFile, this.replayFilePath)) {
                ToastLogger.showText(com.osudroid.resources.R.string.replay_cantdownload, true);
                return false;
            }
        } else
            this.replayFilePath = rFile;

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        boolean shouldParseBeatmap = parsedBeatmap == null || !parsedBeatmap.getMd5().equals(beatmapInfo.getMD5());

        if (shouldParseBeatmap) {
            try {
                parsedBeatmap = BeatmapCache.getBeatmap(beatmapInfo, true, GameMode.Droid, scope);
            } catch (IOException | IllegalArgumentException e) {
                Debug.e("startGame: " + e.getMessage());
                ToastLogger.showText(e.getMessage(), true);
                return false;
            }
        }

        if (parsedBeatmap == null) {
            return false;
        }

        if (!parsedBeatmap.getMd5().equals(beatmapInfo.getMD5())) {
            ToastLogger.showText(com.osudroid.resources.R.string.file_integrity_tampered, true);
            return false;
        }

        if (parsedBeatmap.getHitObjects().objects.isEmpty()) {
            ToastLogger.showText("Empty Beatmap", true);
            return false;
        }

        // Ensure that only relevant mods are applied.
        mods.values().removeIf(m -> !m.isRelevant());

        boolean differentPlayableBeatmap = shouldParseBeatmap || lastMods == null || !lastMods.equals(mods);

        var playableBeatmap = differentPlayableBeatmap
            ? parsedBeatmap.createDroidPlayableBeatmap(mods.values())
            : this.playableBeatmap;

        this.playableBeatmap = playableBeatmap;

        // Load backgrounds early to minimize waiting time.
        loadBackground();
        loadStoryboard(beatmapInfo);
        loadVideo(beatmapInfo);

        rateAdjustingMods.clear();

        for (var mod : mods.values()) {
            if (mod instanceof IModApplicableToTrackRate rateMod) {
                rateAdjustingMods.add(rateMod);
            }
        }

        // TODO skin manager
        BeatmapSkinManager.getInstance().loadBeatmapSkin(playableBeatmap.getBeatmapsetPath());

        var breaks = playableBeatmap.getEvents().breaks;

        if (shouldParseBeatmap || breakPeriods == null || breakPeriods.length != breaks.size()) {
            breakPeriods = new BreakPeriod[breaks.size()];
            System.arraycopy(breaks.toArray(), 0, breakPeriods, 0, breakPeriods.length);
        }

        try {
            var musicFile = new File(beatmapInfo.getAudioPath());

            if (!musicFile.exists()) {
                throw new FileNotFoundException(musicFile.getPath());
            }

            audioFilePath = musicFile.getPath();

        } catch (final Exception e) {
            Debug.e("Load Music: " + e.getMessage());
            ToastLogger.showText(e.getMessage(), true);
            return false;
        }

        var hitObjects = playableBeatmap.getHitObjects().objects;
        var firstObject = hitObjects.get(0);
        scale = firstObject.getScreenSpaceGameplayScale();

        GameHelper.setOverallDifficulty(playableBeatmap.getDifficulty().od);
        GameHelper.setHealthDrain(playableBeatmap.getDifficulty().hp);
        GameHelper.setSpeedMultiplier(ModUtils.calculateRateWithMods(mods.values(), Double.NEGATIVE_INFINITY));

        GameHelper.setOriginalTimePreempt((float) BeatmapDifficulty.difficultyRange(
            playableBeatmap.getDifficulty().getAR(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ));

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        GlobalManager.getInstance().getSongService().preLoad(audioFilePath, GameHelper.getSpeedMultiplier(),
            GameHelper.getSpeedMultiplier() != 1f &&
                (Config.isShiftPitchInRateChange() || mods.contains(ModNightCore.class) || mods.contains(ModOldNightCore.class)));

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        totalLength = GlobalManager.getInstance().getSongService().getLength();
        judgeableObject = null;
        breakPeriodIndex = 0;
        objectIndex = 0;
        lastObjectId = -1;
        hitWindow = playableBeatmap.getHitWindow();
        videoStarted = false;
        videoOffset = playableBeatmap.getEvents().videoStartTime / 1000f;

        if (shouldParseBeatmap || objects == null || objects.length != hitObjects.size()) {
            objects = new HitObject[hitObjects.size()];
        }

        if (differentPlayableBeatmap || objects[0] == null) {
            System.arraycopy(hitObjects.toArray(), 0, objects, 0, objects.length);
        }

        firstObjectStartTime = (float) firstObject.startTime / 1000;
        lastObjectEndTime = (float) objects[objects.length - 1].getEndTime() / 1000;

        int estimatedMaxActiveObjects = Math.max(10, estimateMaximumActiveObjects());

        if (activeObjects != null) {
            activeObjects.clear();
            activeObjects.ensureCapacity(estimatedMaxActiveObjects);
        } else {
            activeObjects = new ArrayList<>(estimatedMaxActiveObjects);
        }

        if (expiredObjects != null) {
            expiredObjects.clear();
            expiredObjects.ensureCapacity(estimatedMaxActiveObjects);
        } else {
            expiredObjects = new ArrayList<>(estimatedMaxActiveObjects);
        }

        float firstObjectTimePreempt = (float) firstObject.timePreempt / 1000;
        float skipTargetTime = firstObjectStartTime - Math.max(2f, firstObjectTimePreempt);

        elapsedTime = Math.min(0, skipTargetTime);
        skipTime = skipTargetTime - 1;

        // Some beatmaps specify a current lead-in time, which overrides the default lead-in time above.
        float leadIn = playableBeatmap.getGeneral().audioLeadIn / 1000f;
        if (leadIn > 0) {
            elapsedTime = Math.min(elapsedTime, firstObjectStartTime - leadIn);
        }

        // Ensure the video has time to start.
        // Even when video is not activated, apply offset anyway to ensure that everyone in multiplayer starts at the
        // same time regardless of the setting.
        elapsedTime = Math.min(elapsedTime, videoOffset);

        sliderBorderColor = null;
        if (playableBeatmap.getColors().getSliderBorderColor() != null) {
            sliderBorderColor = playableBeatmap.getColors().getSliderBorderColor();
        }

        if (OsuSkin.get().isForceOverrideSliderBorderColor()) {
            sliderBorderColor = OsuSkin.get().getSliderBorderColor();
        }

        if (sliderBorderColor == null) {
            sliderBorderColor = BeatmapSkinManager.getInstance().getSliderColor();
        }

        comboColors = new ArrayList<>();
        for (ComboColor comboColor : playableBeatmap.getColors().comboColors) {
            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            comboColors.add(comboColor.getColor());
        }

        if (comboColors.isEmpty() || Config.isUseCustomComboColors()) {
            comboColors.clear();
            comboColors.addAll(Arrays.asList(Config.getComboColors()));
        }
        if (OsuSkin.get().isForceOverrideComboColor()) {
            comboColors.clear();
            comboColors.addAll(OsuSkin.get().getComboColor());
        }

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        var timingControlPointManager = playableBeatmap.getControlPoints().timing;
        var effectControlPointManager = playableBeatmap.getControlPoints().effect;

        if (shouldParseBeatmap || timingControlPoints == null) {
            timingControlPoints = new TimingControlPoint[timingControlPointManager.controlPoints.size()];
            System.arraycopy(timingControlPointManager.controlPoints.toArray(), 0, timingControlPoints, 0, timingControlPoints.length);
        }

        if (shouldParseBeatmap || effectControlPoints == null) {
            effectControlPoints = new EffectControlPoint[effectControlPointManager.controlPoints.size()];
            System.arraycopy(effectControlPointManager.controlPoints.toArray(), 0, effectControlPoints, 0, effectControlPoints.length);
        }

        activeTimingPoint = timingControlPoints.length > 0 ? timingControlPoints[0] : timingControlPointManager.defaultControlPoint;
        activeEffectPoint = effectControlPoints.length > 0 ? effectControlPoints[0] : effectControlPointManager.defaultControlPoint;
        timingControlPointIndex = 0;
        effectControlPointIndex = 0;

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000);
        GameHelper.setKiai(activeEffectPoint.isKiai);
        GameHelper.setCurrentBeatTime(0);
        GameHelper.setSamplesMatchPlaybackRate(playableBeatmap.getGeneral().samplesMatchPlaybackRate);

        GameObjectPool.getInstance().purge();

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        FollowPointConnection.getPool().renew(16);
        SliderTickSprite.getPool().renew(16);

        // TODO replay
        offsetSum = 0;
        offsetRegs = 0;

        replaying = false;
        replay = new Replay(true);
        replay.setObjectCount(hitObjects.size());
        replay.setBeatmap(beatmapInfo.getFullBeatmapsetName(), beatmapInfo.getFullBeatmapName(), playableBeatmap.getMd5());

        if (replayFilePath != null) {
            // Replay decoding may be dependent on the used mods, so we must do this.
            var replayStat = new StatisticV2();
            replayStat.setMod(mods);
            replay.setStat(replayStat);

            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            replaying = replay.load(replayFilePath, true);

            if (!replaying) {
                ToastLogger.showText(com.osudroid.resources.R.string.replay_invalid, true);
                return false;
            }

            // In older versions, replay uploads are separated from scores, which means that they may not be uploaded
            // for reasons independent of score uploads (e.g., network failure). When this happens, replays may be very
            // off such that it causes gameplay to appear very wrong (e.g., a score has Hard Rock/Mirror mod while its
            // replay does not). While this can theoretically happen to any score data (not just mods), checking for
            // mods for the time being is enough to dislodge major inconsistencies in gameplay.
            // Checking for mod existence (not equality) is enough for this case since affected mods do not have
            // customizations.
            for (var mod : mods.values()) {
                if (!replay.getStat().getMod().containsKey(mod.getClass())) {
                    ToastLogger.showText(com.osudroid.resources.R.string.replay_invalid, true);
                    return false;
                }
            }

            GameHelper.setReplayVersion(replay.replayVersion);
        } else if (mods.contains(ModAutoplay.class)) {
            replay = null;
        }

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        GameObjectPool.getInstance().preload();

        if (isHUDEditorMode || OsuSkin.get().getHUDSkinData().hasElement(HUDPPCounter.class)) {
            // Calculate timed difficulty attributes
            switch (Config.getDifficultyAlgorithm()) {
                case droid -> {
                    performanceCalculationParameters = new DroidPerformanceCalculationParameters();

                    if (droidTimedDifficultyAttributes == null || differentPlayableBeatmap) {
                        droidTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateDroidTimedDifficulty(playableBeatmap, scope);
                    }
                }

                case standard -> {
                    performanceCalculationParameters = new StandardPerformanceCalculationParameters();

                    if (standardTimedDifficultyAttributes == null || differentPlayableBeatmap) {
                        standardTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateStandardTimedDifficulty(
                            parsedBeatmap, mods.values(), scope
                        );
                    }
                }
            }
        }

        sliderIndex = 0;

        if (sliderPaths == null || sliderRenderPaths == null || differentPlayableBeatmap) {
            calculateAllSliderPaths(scope);
        }

        lastMods = mods;
        lastBeatmapInfo = beatmapInfo;

        // Resetting variables before starting the game.
        Multiplayer.finalData = null;
        hasFailed = false;
        lastBackPressTime = -1f;
        isSkipRequested = false;
        realTimeElapsed = 0;
        statisticDataTimeElapsed = 0;
        leadOut = 0;
        musicStarted = false;
        lastScoreSent = null;
        isGameOver = false;

        paused = false;
        gameStarted = false;
        return true;
    }

    public Scene getScene() {
        return scene;
    }

    public void restartGame() {
        startGame(null, null, null);
    }


    public void startGame(BeatmapInfo beatmapInfo, String replayFile, ModHashMap mods) {
        startGame(beatmapInfo, replayFile, mods, false);
    }

    public void startGame(BeatmapInfo beatmapInfo, String replayFile, ModHashMap mods, boolean isHUDEditor) {
        isReadyToStart = false;
        isHUDEditorMode = isHUDEditor;
        startedFromHUDEditor = isHUDEditor;
        resetPlayfieldSizeScale();

        scene = createMainScene();
        bgScene = new UIScene();
        mgScene = new UIScene();
        mgScene.setClipToBounds(true);
        fgScene = new UIScene();
        scene.attachChild(bgScene);
        scene.attachChild(mgScene);
        scene.attachChild(fgScene);
        scene.setBackground(new ColorBackground(0, 0, 0));
        bgScene.setBackgroundEnabled(false);
        mgScene.setBackgroundEnabled(false);
        fgScene.setBackgroundEnabled(false);
        failcount = 0;
        mainCursorId = -1;

        final String rfile = beatmapInfo != null ? replayFile : this.replayFilePath;
        final int requestId = loadingRequestId.incrementAndGet();

        BeatmapInfo beatmapToUse = beatmapInfo != null ? beatmapInfo : lastBeatmapInfo;
        boolean isRestart = beatmapInfo == null && replayFile == null && mods == null;
        ModHashMap modsToUse;

        if (isHUDEditor) {
            modsToUse = new ModHashMap();
            modsToUse.put(ModAutoplay.class);
        } else {
            modsToUse = mods != null ? mods.deepCopy() : lastMods;
        }

        GameLoaderScene scene = new GameLoaderScene(this, beatmapToUse, modsToUse, isRestart);
        engine.setScene(scene);

        ResourceManager.getInstance().getSound("failsound").stop();

        var pipeline = cancelLoading(false)
                .thenCompose((ignored) -> DifficultyCalculationManager.stopCalculation())
                .thenRun(() -> {
                    if (requestId != loadingRequestId.get()) {
                        return;
                    }

                    gameLoadingJob = Execution.async((scope) -> {
                        boolean succeeded = false;
                        boolean cancelled = false;

                        try {
                            if (requestId != loadingRequestId.get()) {
                                return;
                            }

                            succeeded = loadGame(beatmapToUse, rfile, modsToUse, scope);

                            if (succeeded && requestId == loadingRequestId.get()) {
                                prepareScene();
                            }
                        } catch (CancellationException e) {
                            cancelled = true;
                            throw e;
                        } finally {
                            if (requestId == loadingRequestId.get()) {
                                if (!succeeded && !cancelled) {
                                    quit();
                                }

                                gameLoadingJob = null;
                            }
                        }
                    });
                });

        loadingPipeline = pipeline;

        pipeline.whenComplete((ignored, error) -> {
            if (loadingPipeline == pipeline) {
                loadingPipeline = null;
            }
        });
    }

    public CompletableFuture<Unit> cancelLoading() {
        return cancelLoading(true);
    }

    private CompletableFuture<Unit> cancelLoading(boolean invalidatePendingStart) {
        // Do not cancel loading in multiplayer.
        if (Multiplayer.isMultiplayer) {
            return CompletableFuture.completedFuture(Unit.INSTANCE);
        }

        if (invalidatePendingStart) {
            loadingRequestId.incrementAndGet();
        }

        var gameLoadingJob = this.gameLoadingJob;
        var storyboardLoadingJob = this.storyboardLoadingJob;
        var videoLoadingJob = this.videoLoadingJob;
        var loadingPipeline = this.loadingPipeline;

        this.gameLoadingJob = null;
        this.storyboardLoadingJob = null;
        this.videoLoadingJob = null;

        var jobCancellation = Execution.stopAsync(gameLoadingJob)
                .thenCompose((ignored) -> Execution.stopAsync(storyboardLoadingJob))
                .thenCompose((ignored) -> Execution.stopAsync(videoLoadingJob));

        var pipelineDrain = loadingPipeline != null
            ? loadingPipeline.exceptionally((error) -> null)
            : CompletableFuture.completedFuture(Unit.INSTANCE);

        return CompletableFuture.allOf(jobCancellation, pipelineDrain).thenApply((ignored) -> Unit.INSTANCE);
    }

    private void prepareScene() {
        scene.setOnSceneTouchListener(this);
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        stat = new StatisticV2();
        stat.setMod(lastMods);
        stat.migrateLegacyMods(parsedBeatmap.getDifficulty());
        stat.calculateModScoreMultiplier(parsedBeatmap);
        stat.canFail = !stat.getMod().contains(ModNoFail.class)
                && !stat.getMod().contains(ModRelax.class)
                && !stat.getMod().contains(ModAutopilot.class)
                && !stat.getMod().contains(ModAutoplay.class);

        float difficultyScoreMultiplier = 1 + Math.min(parsedBeatmap.getDifficulty().od, 10) / 10f +
                Math.min(parsedBeatmap.getDifficulty().hp, 10) / 10f;

        // The maximum CS of osu!droid mapped to osu!standard is ~17.62.
        difficultyScoreMultiplier += (Math.min(parsedBeatmap.getDifficulty().gameplayCS, 17.62f) - 3) / 4f;

        stat.setDiffModifier(difficultyScoreMultiplier);
        stat.setBeatmapNoteCount(objects.length);
        stat.setBeatmapMaxCombo(parsedBeatmap.getMaxCombo());

        GameHelper.setHardRock(lastMods.ofType(ModHardRock.class));
        GameHelper.setDoubleTime(lastMods.ofType(ModDoubleTime.class));
        GameHelper.setNightCore(lastMods.contains(ModNightCore.class) ? lastMods.ofType(ModNightCore.class) : lastMods.ofType(ModOldNightCore.class));
        GameHelper.setHalfTime(lastMods.ofType(ModHalfTime.class));
        GameHelper.setHidden(lastMods.ofType(ModHidden.class));
        GameHelper.setTraceable(lastMods.ofType(ModTraceable.class));
        GameHelper.setFlashlight(lastMods.ofType(ModFlashlight.class));
        GameHelper.setRelax(lastMods.ofType(ModRelax.class));
        GameHelper.setAutopilot(lastMods.ofType(ModAutopilot.class));
        GameHelper.setAutoplay(lastMods.ofType(ModAutoplay.class));
        GameHelper.setSuddenDeath(lastMods.ofType(ModSuddenDeath.class));
        GameHelper.setPerfect(lastMods.ofType(ModPerfect.class));
        GameHelper.setSynesthesia(lastMods.ofType(ModSynesthesia.class));
        GameHelper.setScoreV2(lastMods.ofType(ModScoreV2.class));
        GameHelper.setEasy(lastMods.ofType(ModEasy.class));
        GameHelper.setMuted(lastMods.ofType(ModMuted.class));
        GameHelper.setFreezeFrame(lastMods.ofType(ModFreezeFrame.class));
        GameHelper.setApproachDifferent(lastMods.ofType(ModApproachDifferent.class));

        int cursorCount = replaying && replay != null ? replay.cursorMoves.size() : CursorCount;

        cursors = new Cursor[cursorCount];

        for (int i = 0; i < cursorCount; i++) {
            cursors[i] = new Cursor();
        }

        comboWas100 = false;
        comboWasMissed = false;
        previousFrameTime = 0;

        metronome = null;
        if ((Config.getMetronomeSwitch() == 1 && GameHelper.isNightCore())
                || (GameHelper.isMuted() && GameHelper.getMuted().isEnableMetronome())
                || Config.getMetronomeSwitch() == 2) {
            metronome = new Metronome();
        }

        distToNextObject = 0;

        // TODO passive objects
        if ((replaying || Config.isShowCursor()) && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
            cursorSprites = new CursorEntity[cursorCount];
            for (int i = 0; i < cursorCount; i++) {
                cursorSprites[i] = new CursorEntity();
                cursorSprites[i].attachToScene(fgScene);
            }
        } else {
            cursorSprites = null;
        }

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor = new AutoCursor();
            autoCursor.attachToScene(fgScene);
        }

        final var countdown = playableBeatmap.getGeneral().countdown;
        if (Config.isCorovans() && countdown != BeatmapCountdown.NoCountdown) {
            float cdSpeed = countdown.speed;
            skipTime -= cdSpeed * Countdown.COUNTDOWN_LENGTH;
            if (cdSpeed != 0 && firstObjectStartTime - elapsedTime >= cdSpeed * Countdown.COUNTDOWN_LENGTH) {
                countdownAnimator = new Countdown(bgScene, cdSpeed, 0, firstObjectStartTime - elapsedTime);
            }
        }

        if (Config.isComboburst()) {
            comboBurst = new ComboBurst(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
            comboBurst.attachAll(bgScene);
        }

        var position = new PointF(Config.getRES_WIDTH() - 130, 130);
        float timeOffset = 0;

        for (var mod : lastMods.values()) {
            if (!mod.isUserPlayable()) {
                continue;
            }

            var icon = new ModIcon(mod);
            icon.setPosition(position.x, position.y);
            icon.setOrigin(Anchor.Center);
            icon.setSize(68, 66);
            icon.setScale(scale);
            icon.registerEntityModifier(Modifiers.sequence(
                IEntity::detachSelf,
                Modifiers.scale(0.25f, 1.2f, 1f),
                Modifiers.delay(2f - timeOffset),
                Modifiers.parallel(
                    Modifiers.fadeOut(0.5f),
                    Modifiers.scale(0.5f, 1f, 1.5f)
                )
            ));

            fgScene.attachChild(icon);

            position.x -= 25f;
            timeOffset += 0.25f;
        }

        boolean hasUnrankedMod = SmartIterator.wrap(lastMods.values().iterator()).applyFilter(m -> !m.isRanked()).hasNext();
        if (hasUnrankedMod || Config.isRemoveSliderLock()) {
            unrankedSprite = new UISprite(ResourceManager.getInstance().getTexture("play-unranked"));
            unrankedSprite.setAnchor(Anchor.TopCenter);
            unrankedSprite.setOrigin(Anchor.Center);
            unrankedSprite.setPosition(0, 80);
            fgScene.attachChild(unrankedSprite);
        }

        if (GameHelper.isFlashlight()) {
            flashlightSprite = new FlashLightEntity(GameHelper.getFlashlight());
            fgScene.attachChild(flashlightSprite, 0);
        }

        // HUD should be to the last so we ensure everything is initialized and ready to be used by
        // the HUD elements in their constructors.
        hud = new GameplayHUD();

        if (!replaying && !GameHelper.isAutoplay()) {
            // Since block areas are saved in device pixels, we need to map them to scaled pixels.
            final var displayMetrics = new DisplayMetrics();
            GlobalManager.getInstance().getMainActivity().getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

            final float ratio = (float) Config.getRES_WIDTH() / displayMetrics.widthPixels;

            for (final var area : DatabaseManager.getBlockAreaTable().getAll()) {
                // Attach the block area to the HUD so that it does not get scaled with the playfield.
                var areaBox = new UIBox() {
                    {
                        setCornerRadius(2f);
                        setColor(30f / 255f, 30f / 255f, 41f / 255f);
                        setAlpha(0.15f);

                        setPosition(
                            Interpolation.linear(0f, area.getX(), ratio),
                            Interpolation.linear(0f, area.getY(), ratio)
                        );

                        setSize(
                            Interpolation.linear(0f, area.getWidth(), ratio),
                            Interpolation.linear(0f, area.getHeight(), ratio)
                        );
                    }

                    @Override
                    public boolean onAreaTouched(TouchEvent event, float localX, float localY) {
                        if (event.isActionDown()) {
                            int id = event.getPointerID();

                            if (id >= 0 && id < getCursorsCount()) {
                                cursors[id].mouseBlocked = true;
                            }
                        }

                        return false;
                    }
                };

                hud.attachChild(areaBox);
            }
        }

        hud.setEditMode(isHUDEditorMode);
        hud.setSkinData(OsuSkin.get().getHUDSkinData());

        skipBtn = null;
        if (skipTime > 1) {
            float paddingBottom = Multiplayer.isConnected() ? Multiplayer.roomScene.getChat().getButtonHeight() : 0f;

            skipBtn = new UIAnimatedSprite("play-skip", true, OsuSkin.get().getAnimationFramerate());
            skipBtn.setOrigin(Anchor.BottomRight);
            skipBtn.setPosition(Config.getRES_WIDTH(), Config.getRES_HEIGHT() - paddingBottom);
            skipBtn.setAlpha(0.7f);
            hud.attachChild(skipBtn);
        }

        String playname = Config.getOnlineUsername();

        if (GameHelper.isAutoplay() || replaying) {
            var metadata = playableBeatmap.getMetadata();
            playname = replaying ? GlobalManager.getInstance().getScoring().getReplayStat().getPlayerName() : "osu!";

            if (!Config.isHideReplayMarquee()) {
                var replayText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("font"), "", 1000);
                replayText.setText("Watching " + playname + " play " + metadata.artist + " - " + metadata.title + " [" + metadata.version + "]");
                replayText.registerEntityModifier(new LoopEntityModifier(new MoveXModifier(40f, Config.getRES_WIDTH() + 5, -replayText.getWidth() - 5)));
                replayText.setPosition(0, 140);
                replayText.setAlpha(0.7f);
                hud.attachChild(replayText, 0);
            }
        } else if (Multiplayer.room != null && Multiplayer.room.isTeamVersus()) {
            //noinspection DataFlowIssue
            playname = Multiplayer.player.getTeam().toString();
        }
        stat.setPlayerName(playname);

        var counterTextFont = ResourceManager.getInstance().getFont("smallFont");

        if (Config.isShowFPS()) {
            var fpsCounter = new FPSCounter(counterTextFont);

            // Attach a dummy entity for computing FPS, as its frame rate is tied to the draw thread and not
            // the update thread.
            hud.attachChild(new Entity() {
                private long previousDrawTime;

                @Override
                protected void onManagedUpdate(float pSecondsElapsed) {
                    fpsCounter.setPosition(
                        Config.getRES_WIDTH() - fpsCounter.getWidthScaled() - 5,
                        Config.getRES_HEIGHT() - fpsCounter.getHeightScaled() - 10
                    );
                }

                @Override
                protected void onManagedDraw(GL10 pGL, Camera pCamera) {
                    long currentDrawTime = SystemClock.uptimeMillis();

                    fpsCounter.updateFps((currentDrawTime - previousDrawTime) / 1000f);

                    previousDrawTime = currentDrawTime;
                }
            });

            fpsCounter.setPosition(
                Config.getRES_WIDTH() - fpsCounter.getWidthScaled() - 5,
                Config.getRES_HEIGHT() - fpsCounter.getHeightScaled() - 10
            );

            hud.attachChild(fpsCounter);
        }

        breakAnimator = new BreakAnimator(fgScene, stat, hud);

        if (Multiplayer.isMultiplayer) {
            RoomAPI.INSTANCE.notifyBeatmapLoaded();
        } else {
            isReadyToStart = true;
        }
    }

    /**
     * Starts gameplay. This is used by the game loader once all necessary preprocessing is done.
     */
    public void start() {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        totalOffset = Config.getOffset();

        var props = DatabaseManager.getBeatmapOptionsTable().getOptions(lastBeatmapInfo.getSetDirectory());
        if (props != null) {
            totalOffset += props.getOffset();
        }

        totalOffset /= 1000;

        // Ensure user-defined offset has time to be applied.
        var firstObjectTimePreempt = (float) playableBeatmap.getHitObjects().objects.get(0).timePreempt / 1000;
        elapsedTime = Math.min(elapsedTime, firstObjectStartTime - firstObjectTimePreempt - getRateAdjustedOffset());
        initialElapsedTime = elapsedTime;

        if (skipTime <= 1 && Multiplayer.isConnected()) {
            Multiplayer.roomScene.getChat().hide();
        }

        applyPlayfieldSizeScale();
        applyBackground();

        if (!isHUDEditorMode && !Config.isShowScoreboard()) {
            hud.detachChild(e -> e instanceof HUDLeaderboard);
        }

        if (!isHUDEditorMode && !replaying && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
            // Enable historical event processing for more frequent ACTION_MOVE reports depending on user configuration.
            var touchOptions = new TouchOptions();
            touchOptions.setRunOnUpdateThread(true);
            touchOptions.setProcessHistoricalEvents(Config.isHighPrecisionInput());
            touchOptions.setUseRawPointer(Config.isHighPrecisionInput());

            var touchController = engine.getTouchController();
            touchController.applyTouchOptions(touchOptions);
            touchController.resetRawPointers();
        }

        // Disable screen dimming
        engine.getEngineOptions().setWakeLockOptions(WakeLockOptions.SCREEN_BRIGHT);
        GlobalManager.getInstance().getMainActivity().reapplyWakeLock();

        engine.setScene(scene);
        engine.getOverlay().attachChild(hud, 0);

        if (isHUDEditorMode) {
            ToastLogger.showText(R.string.hudEditor_back_for_menu, false);
        }
    }

    public Color4 getComboColor(HitObject hitObject) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap != null && GameHelper.isSynesthesia()) {
            return ModSynesthesia.getColorFor(playableBeatmap.getControlPoints().getClosestBeatDivisor(hitObject.startTime));
        }

        return comboColors.get(hitObject.getComboIndexWithOffsets() % comboColors.size());
    }

    private void update(final float dt) {
        if (!isReadyToStart) {
            return;
        }

        elapsedTime += dt;
        previousFrameTime = SystemClock.uptimeMillis();

        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        if (Multiplayer.isMultiplayer)
        {
            long mSecElapsed = (long) (dt / GameHelper.getSpeedMultiplier() * 1000);
            realTimeElapsed += mSecElapsed;
            statisticDataTimeElapsed += mSecElapsed;

            // Sending statistics data every 3000ms if data was changed
            if (statisticDataTimeElapsed > 3000)
            {
                statisticDataTimeElapsed %= 3000;

                if (Multiplayer.isConnected())
                {
                    var liveScore = stat.toBoardItem();

                    if (!Objects.equals(liveScore, lastScoreSent))
                    {
                        lastScoreSent = liveScore;
                        Execution.async(() -> Execution.runSafe(() -> RoomAPI.submitLiveScore(lastScoreSent.toJson())));
                    }
                }
            }
        }

        final float mSecPassed = elapsedTime * 1000;

        if (!isGameOver) {
            float currentSpeedMultiplier = getRateAt(mSecPassed);

            if (currentSpeedMultiplier != GameHelper.getSpeedMultiplier()) {
                GameHelper.setSpeedMultiplier(currentSpeedMultiplier);
                GlobalManager.getInstance().getSongService().setSpeed(currentSpeedMultiplier);
            }
        }

        if (storyboardSprite != null && storyboardSprite.hasParent()) {
            storyboardSprite.updateTime(mSecPassed);
        }

        if (replaying) {
            final float replayTimeMs = elapsedTime * 1000;
            final float replayCatchUpTimeMs = (elapsedTime + dt / 4) * 1000;
            int cIndex;

            for (int i = 0; i < replay.cursorIndex.length; i++) {
                if (replay.cursorMoves.size() <= i) {
                    break;
                }

                var moveArray = replay.cursorMoves.get(i);

                cIndex = replay.cursorIndex[i];
                Replay.ReplayMovement movement = null;

                // Emulating moves
                while (
                    cIndex < moveArray.size && (movement = moveArray.movements[cIndex]).getTime() <= replayCatchUpTimeMs
                ) {
                    var event = CursorEvent.obtain();

                    event.systemTime = movement.getTime();
                    event.trackTime = movement.getTime();
                    event.offset = 0;
                    event.position.set(movement.getX(), movement.getY());

                    if (movement.getTouchType() == TouchType.DOWN) {
                        event.action = TouchEvent.ACTION_DOWN;
                        replay.lastMoveIndex[i] = -1;
                        hud.onGameplayTouchDown(movement.getTime() / 1000f);
                    } else if (movement.getTouchType() == TouchType.MOVE) {
                        event.action = TouchEvent.ACTION_MOVE;
                        replay.lastMoveIndex[i] = cIndex;
                    } else {
                        event.action = TouchEvent.ACTION_UP;
                    }
                    cursors[i].addEvent(event);
                    replay.cursorIndex[i]++;
                    cIndex++;
                }

                // Interpolating cursor movements
                if (movement != null && movement.getTouchType() == TouchType.MOVE && replay.lastMoveIndex[i] >= 0) {
                    final int lIndex = replay.lastMoveIndex[i];
                    final Replay.ReplayMovement lastMovement = moveArray.movements[lIndex];
                    int movementTime = movement.getTime();
                    int lastMovementTime = lastMovement.getTime();
                    int duration = lastMovementTime - movementTime;

                    if (duration == 0) {
                        continue;
                    }

                    float t = (replayTimeMs - movementTime) / duration;

                    var event = CursorEvent.obtain();

                    // We don't exactly need systemTime to be accurate here since it's not used for anything important
                    // in replays.
                    event.systemTime = (long) mSecPassed;
                    event.trackTime = (long) mSecPassed;
                    event.offset = 0;
                    event.action = TouchEvent.ACTION_MOVE;
                    event.position.set(
                        lastMovement.getX() * t + movement.getX() * (1 - t),
                        lastMovement.getY() * t + movement.getY() * (1 - t)
                    );

                    cursors[i].addEvent(event);
                }
            }
        }

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.update(dt);
        } else if (cursorSprites != null) {
            for (int i = 0; i < cursorSprites.length; i++) {
                var sprite = cursorSprites[i];
                sprite.update(dt);

                var cursor = cursors[i];
                var latestEvent = cursor.getLatestEvent();

                if (replaying && latestEvent != null) {
                    sprite.setPosition(latestEvent.position.x, latestEvent.position.y);
                    sprite.setShowing(!latestEvent.isActionUp());
                }

                if (cursor.getLatestDownEvent() != null) {
                    sprite.click();
                }
            }
        }

        if (GameHelper.isFlashlight()) {
            if (!GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
                // Check if the main cursor is still valid.
                if (mainCursorId >= 0 && !cursors[mainCursorId].isMouseDown()) {
                    mainCursorId = -1;
                }

                // If no cursor is valid, check for the latest pressed cursor.
                if (mainCursorId < 0) {
                    int index = -1;
                    CursorEvent latestDownEvent = null;

                    for (int i = 0; i < cursors.length; ++i) {
                        var c = cursors[i];
                        var latestCursorDownEvent = c.getLatestDownEvent();

                        if (latestCursorDownEvent == null) {
                            continue;
                        }

                        if (latestDownEvent == null || latestDownEvent.systemTime < latestCursorDownEvent.systemTime) {
                            latestDownEvent = latestCursorDownEvent;
                            index = i;
                        }
                    }

                    if (latestDownEvent != null) {
                        mainCursorId = index;
                    }
                }

                if (mainCursorId != -1) {
                    var cursor = cursors[mainCursorId];
                    var latestNonUpEvent = cursor.getLatestNonUpEvent();

                    if (latestNonUpEvent != null) {
                        flashlightSprite.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y);
                    }
                }
            }

            flashlightSprite.onUpdate(stat.getCombo());
        }

        while (timingControlPointIndex + 1 < timingControlPoints.length) {
            var nextTimingPoint = timingControlPoints[timingControlPointIndex + 1];

            if (nextTimingPoint.time > mSecPassed) {
                break;
            }

            activeTimingPoint = nextTimingPoint;
            ++timingControlPointIndex;
        }

        while (effectControlPointIndex + 1 < effectControlPoints.length) {
            var nextEffectPoint = effectControlPoints[effectControlPointIndex + 1];

            if (nextEffectPoint.time > mSecPassed) {
                break;
            }

            activeEffectPoint = nextEffectPoint;
            ++effectControlPointIndex;
        }

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000);
        GameHelper.setKiai(activeEffectPoint.isKiai);
        GameHelper.setCurrentBeatTime(Math.max(0, elapsedTime - activeTimingPoint.time / 1000) % GameHelper.getBeatLength());

        if (!isGameOver) {

            if (breakPeriodIndex < breakPeriods.length) {
                if (!breakAnimator.isBreak() && breakPeriods[breakPeriodIndex].startTime / 1000 <= elapsedTime) {
                    var period = breakPeriods[breakPeriodIndex++];

                    gameStarted = false;
                    breakAnimator.init(period.getDuration() / 1000);
                    if(GameHelper.isFlashlight()){
                        flashlightSprite.onBreak(true);
                    }

                    if (Multiplayer.isConnected())
                        Multiplayer.roomScene.getChat().show();

                    hud.onBreakStateChange(true);
                }
            }

            if (breakAnimator.isOver()) {

                // Ensure the chat is dismissed if it's still shown
                if (Multiplayer.isConnected()) {
                    Multiplayer.roomScene.getChat().hide();
                }

                gameStarted = true;
                hud.onBreakStateChange(false);

                if(GameHelper.isFlashlight()){
                    flashlightSprite.onBreak(false);
                }
            }
        }

        if (objectIndex >= objects.length && activeObjects.isEmpty() && GameHelper.isFlashlight()) {
            flashlightSprite.onBreak(true);
        }

        if (gameStarted) {
            double rate = 0.375;
            if (playableBeatmap.getDifficulty().hp > 0 && distToNextObject > 0) {
                rate = 1 + playableBeatmap.getDifficulty().hp / (2 * distToNextObject);
            }
            stat.changeHp((float) -rate * 0.01f * dt);

            if (stat.getHp() <= 0 && stat.canFail) {
                if (GameHelper.isEasy() && failcount < 3) {
                    failcount++;
                    stat.changeHp(1f);
                } else {
                    if (Multiplayer.isMultiplayer) {
                        if (!hasFailed) {
                            ToastLogger.showText("You failed but you can continue playing.", false);
                        }
                        hasFailed = true;
                    } else {
                        gameover();
                        return;
                    }
                }
            }
        }

        if (comboBurst != null) {
            if (stat.getCombo() == 0) {
                comboBurst.breakCombo();
            } else {
                comboBurst.checkAndShow(stat.getCombo());
            }
        }

        // Clearing expired objects.
        processedExpiredObjects.clear();

        if (!expiredObjects.isEmpty()) {
            for (int i = 0, size = expiredObjects.size(); i < size; i++) {
                var obj = expiredObjects.get(i);

                if (processedExpiredObjects.add(obj)) {
                    obj.onExpire();
                }
            }

            activeObjects.removeAll(expiredObjects);
            expiredObjects.clear();
        }

        updatePassiveObjects(dt);
        updateActiveObjects(dt);

        if (judgeableObject != null && (GameHelper.isAutoplay() || GameHelper.isAutopilot())) {
            autoCursor.moveToObject(judgeableObject, elapsedTime, this);
        }

        if (videoEnabled && video != null && elapsedTime >= videoOffset)
        {
            if (!videoStarted) {
                video.play();
                // Some devices do not support custom playback speed for whatever reason.
                try {
                    video.setPlaybackSpeed(GameHelper.getSpeedMultiplier());
                } catch (Exception e) {
                    Log.e("GameScene", "Failed to change video playback speed.", e);
                    ToastLogger.showText(com.osudroid.resources.R.string.message_video_custom_speed_unsupported, false);
                }
                videoStarted = true;
            }

            if (video.getAlpha() < 1.0f)
                video.setAlpha(Math.min(video.getAlpha() + 0.03f, 1.0f));
        }

        if (elapsedTime >= getRateAdjustedOffset() && !musicStarted) {
            musicStarted = true;

            Execution.updateThread(() -> {
                // Start the music in the next update tick to ensure the most minimum time difference between the music
                // start and the game start.
                var songService = GlobalManager.getInstance().getSongService();

                songService.play();
                songService.setVolume(Config.getBgmVolume());
            });
        }

        boolean shouldBePunished = false;

        while (objectIndex < objects.length) {
            var obj = objects[objectIndex];

            // The casts can be simplified, but it is necessary to prevent floating point errors (see how
            // GameplayHitCircle and GameplaySlider track their passed time, where startTime and timePreempt
            // are cast and converted to seconds individually).
            float lifetimeStart = (float) obj.startTime / 1000 - (float) obj.timePreempt / 1000;
            float lifetimeDt = elapsedTime - lifetimeStart;

            if (lifetimeDt < 0) {
                break;
            }

            gameStarted = true;
            ++objectIndex;

            if (unrankedSprite != null) {
                unrankedSprite.registerEntityModifier(
                    Modifiers.sequence(IEntity::detachSelf,
                        Modifiers.delay(1.5f - elapsedTime),
                        Modifiers.parallel(
                            Modifiers.scale(0.5f, 1, 1.5f),
                            Modifiers.fadeOut(0.5f)
                        )
                    )
                );

                // Make it null to avoid multiple entity modifier registration
                unrankedSprite = null;
            }

            if (obj.startTime > totalLength) {
                shouldBePunished = true;
                break;
            }

            final var nextObj = objectIndex < objects.length ? objects[objectIndex] : null;

            distToNextObject = nextObj != null ?
                Math.max(nextObj.startTime - obj.startTime, activeTimingPoint.msPerBeat / 2) / 1000 :
                0;

            hud.onHitObjectLifetimeStart(obj);

            final Color4 comboColor = getComboColor(obj);

            GameObject gameObject = null;

            if (obj instanceof HitCircle parsedCircle) {
                final var gameplayCircle = GameObjectPool.getInstance().getCircle();

                gameplayCircle.init(this, mgScene, parsedCircle, comboColor);
                addObject(gameplayCircle);

                if (GameHelper.isAutoplay()) {
                    gameplayCircle.setAutoPlay();
                }

                gameplayCircle.setId(++lastObjectId);

                if (replaying) {
                    gameplayCircle.setReplayData(replay.objectData[gameplayCircle.getId()]);
                }

                gameObject = gameplayCircle;
            } else if (obj instanceof Spinner parsedSpinner) {
                final float rps = 2 + 2 * playableBeatmap.getDifficulty().od / 10f;
                final var gameplaySpinner = GameObjectPool.getInstance().getSpinner();

                gameplaySpinner.init(this, bgScene, parsedSpinner, rps, stat);
                addObject(gameplaySpinner);

                if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
                    gameplaySpinner.setAutoPlay();
                }

                gameplaySpinner.setId(++lastObjectId);
                if (replaying) {
                    gameplaySpinner.setReplayData(replay.objectData[gameplaySpinner.getId()]);
                }

                gameObject = gameplaySpinner;
            } else if (obj instanceof Slider parsedSlider) {
                final var gameplaySlider = GameObjectPool.getInstance().getSlider();

                gameplaySlider.init(this, mgScene, stat, parsedSlider, playableBeatmap.getControlPoints(),
                        comboColor, sliderBorderColor, getSliderPath(sliderIndex), getSliderRenderPath(sliderIndex));

                ++sliderIndex;
                addObject(gameplaySlider);

                if (GameHelper.isAutoplay()) {
                    gameplaySlider.setAutoPlay();
                }

                gameplaySlider.setId(++lastObjectId);
                if (replaying) {
                    gameplaySlider.setReplayData(replay.objectData[gameplaySlider.getId()]);
                    if (gameplaySlider.getReplayData().tickSet == null)
                        gameplaySlider.getReplayData().tickSet = new BitSet();
                }

                gameObject = gameplaySlider;
            }

            if (gameObject != null) {
                gameObject.updateAfterInit(lifetimeDt);
            }

            if (!(obj instanceof Spinner) && nextObj != null && !(nextObj instanceof Spinner) && !obj.isLastInCombo()) {
                FollowPointConnection.addConnection(bgScene, elapsedTime, obj, nextObj);
            }
        }

        var mutedMod = GameHelper.getMuted();

        // 节拍器
        if (metronome != null) {
            metronome.update(elapsedTime, activeTimingPoint);

            if (mutedMod != null) {
                metronome.setVolume(1 - mutedMod.volumeAt(stat.getCombo()));
            }
        }

        if (musicStarted && mutedMod != null) {
            GlobalManager.getInstance().getSongService().setVolume(
                Config.getBgmVolume() * mutedMod.volumeAt(stat.getCombo())
            );
        }

        if (shouldBePunished || (!isGameOver && objectIndex >= objects.length && activeObjects.isEmpty() && leadOut > 2)) {

            // Reset the game to continue the HUD editor session.
            if (startedFromHUDEditor && isHUDEditorMode) {
                elapsedTime = initialElapsedTime;
                loadGame(lastBeatmapInfo, null, lastMods, null);
                stat.reset();
                skip(true);
                return;
            }

            scene = createMainScene();
            BeatmapSkinManager.setSkinEnabled(false);
            GameObjectPool.getInstance().purge();
            timingControlPoints = null;
            effectControlPoints = null;
            objects = null;
            activeObjects.clear();
            expiredObjects.clear();
            breakPeriods = null;
            cursorSprites = null;
            this.playableBeatmap = null;
            performanceCalculationParameters = null;
            droidTimedDifficultyAttributes = null;
            standardTimedDifficultyAttributes = null;
            sliderPaths = null;
            sliderRenderPaths = null;
            String replayPath = null;
            stat.setTime(System.currentTimeMillis());
            if (replay != null && !replaying) {
                String ctime = String.valueOf(System.currentTimeMillis());
                replayPath = Config.getCorePath() + "Scores/"
                        + MD5Calculator.getStringMD5(lastBeatmapInfo.getFilename() + ctime)
                        + ctime.substring(0, Math.min(3, ctime.length())) + ".odr";
                replay.setStat(stat);
                replay.save(replayPath);
            }
            resetPlayfieldSizeScale();
            cancelStoryboardLoading();
            cancelVideoLoading();

            if (scoringScene != null && !startedFromHUDEditor) {
                if (replaying)
                    scoringScene.load(scoringScene.getReplayStat(), null, GlobalManager.getInstance().getSongService(), replayPath, null, lastBeatmapInfo);
                else {
                    if (stat.getMod().contains(ModAutoplay.class)) {
                        stat.setPlayerName("osu!");
                    }

                    if (Multiplayer.isConnected())
                    {
                        Multiplayer.log("Match ended, moving to results scene.");
                        Multiplayer.roomScene.getChat().show();

                        Execution.async(() -> Execution.runSafe(() -> RoomAPI.submitFinalScore(stat.toJson())));

                        ToastLogger.showText("Loading room statistics...", false);
                    }
                    scoringScene.load(stat, lastBeatmapInfo, GlobalManager.getInstance().getSongService(), replayPath, parsedBeatmap.getMd5(), null);
                }
                GlobalManager.getInstance().getSongService().setVolume(0.2f);
                engine.setScene(scoringScene.getScene());
            } else {
                engine.setScene(oldScene);
            }

            // Resume difficulty calculation.
            DifficultyCalculationManager.calculateDifficulties();

            // Disable historical event processing for more efficient ACTION_MOVE reports. Frequent reports are not
            // relevant outside gameplay.
            var touchOptions = new TouchOptions();
            touchOptions.setRunOnUpdateThread(true);
            touchOptions.setProcessHistoricalEvents(false);
            touchOptions.setUseRawPointer(false);
            engine.getTouchController().applyTouchOptions(touchOptions);

            // Enable screen dimming
            engine.getEngineOptions().setWakeLockOptions(WakeLockOptions.SCREEN_DIM);
            GlobalManager.getInstance().getMainActivity().reapplyWakeLock();

            if (video != null) {
                video.release();
                video = null;
                videoStarted = false;
            }

            parsedBeatmap = null;
        } else if (objectIndex >= objects.length && activeObjects.isEmpty()) {
            gameStarted = false;
            leadOut += dt;
        }

        if (elapsedTime > skipTime - 1f && skipBtn != null) {
            if (Multiplayer.isConnected()) {
                Multiplayer.roomScene.getChat().hide();
            }
            skipBtn.detachSelf();
            skipBtn = null;
        } else if (skipBtn != null) {
            for (int i = 0; i < cursors.length; ++i) {
                var latestDownEvent = cursors[i].getLatestNonUpEvent();

                if (latestDownEvent != null && Utils.squaredDistance(latestDownEvent.position.x, latestDownEvent.position.y,
                        Config.getRES_WIDTH(), Config.getRES_HEIGHT()) < 250 * 250) {
                    if (Multiplayer.isConnected()) {
                        if (!isSkipRequested) {
                            isSkipRequested = true;
                            ResourceManager.getInstance().getSound("menuhit").play();
                            skipBtn.setVisible(false);

                            Execution.async(RoomAPI.INSTANCE::requestSkip);
                            ToastLogger.showText("Skip requested", false);
                        }
                        return;
                    }
                    if (skipBtn != null) {
                        skipBtn.detachSelf();
                        skipBtn = null;
                    }
                    skip();
                    return;
                }
            }
        }
    }

    private void updateActiveObjects(float deltaTime) {
        judgeableObject = searchJudgeableObject(0);

        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            var obj = activeObjects.get(i);
            obj.update(deltaTime);

            // Advance to the next judgeable object if the current judgeable object is judged.
            // In remove slider lock mode, do this as soon as the current judgeable object is hit instead.
            if (isObjectHittable(obj) && (obj.isJudged() || (Config.isRemoveSliderLock() && obj.isStartHit()))) {
                judgeableObject = searchJudgeableObject(i + 1);
            }

            if (elapsedTime >= obj.getLifetimeEnd()) {
                expiredObjects.add(obj);
            }
        }
    }

    private void updatePassiveObjects(float deltaTime) {

        hud.onGameplayUpdate(this, deltaTime);

        breakAnimator.update(deltaTime);

        if (countdownAnimator != null) {
            countdownAnimator.update(deltaTime);
        }
    }

    @Nullable
    private GameObject searchJudgeableObject(int startIndex) {
        for (int i = startIndex, size = activeObjects.size(); i < size; i++) {
            var obj = activeObjects.get(i);

            if (!obj.isJudged()) {
                return obj;
            }
        }

        return null;
    }

    public void skip() {
        skip(false);
    }

    public void skip(boolean force)
    {
        if (Multiplayer.isConnected()) {
            Multiplayer.roomScene.getChat().hide();
        }

        if (elapsedTime > skipTime - 1f && !force) {
            return;
        }

        ResourceManager.getInstance().getSound("menuhit").play();

        float difference = skipTime - elapsedTime;
        elapsedTime = skipTime;

        double elapsedTimeMs = Math.ceil(elapsedTime * 1000);

        // Seek times may be negative in forced skips, which are not supported by music and video.
        int musicSeekTime = Math.max(0, (int) (elapsedTimeMs - totalOffset * getRateAt(elapsedTimeMs) * 1000));
        int videoSeekTime = Math.max(0, (int) (elapsedTimeMs - videoOffset * 1000));

        Execution.updateThread(() -> {
            updatePassiveObjects(difference);

            var songService = GlobalManager.getInstance().getSongService();

            if (elapsedTime >= getRateAdjustedOffset() && !musicStarted) {
                songService.play();
                songService.setVolume(Config.getBgmVolume());
                musicStarted = true;
            }

            songService.seekTo(musicSeekTime);

            if (videoEnabled && video != null) {
                video.seekTo(videoSeekTime);
            }

            if (skipBtn != null) {
                skipBtn.detachSelf();
                skipBtn = null;
            }
        });
    }

    private void onExit() {

        Execution.updateThread(() -> {
            BeatmapSkinManager.setSkinEnabled(false);
            GameObjectPool.getInstance().purge();
            stopLoopingSamples();
            if (activeObjects != null) {
                activeObjects.clear();
            }
            if (expiredObjects != null) {
                expiredObjects.clear();
            }
            breakPeriods = null;
            objects = null;
            timingControlPoints = null;
            effectControlPoints = null;
            parsedBeatmap = null;
            playableBeatmap = null;
            cursorSprites = null;
            lastMods = null;
            performanceCalculationParameters = null;
            droidTimedDifficultyAttributes = null;
            standardTimedDifficultyAttributes = null;
            sliderPaths = null;
            sliderRenderPaths = null;
        });

        cancelStoryboardLoading();
        cancelVideoLoading();

        float mSecPassed = elapsedTime * 1000;
        var selectedBeatmap = GlobalManager.getInstance().getSelectedBeatmap();
        var songService = GlobalManager.getInstance().getSongService();
        var songMenu = GlobalManager.getInstance().getSongMenu();

        if (songService != null && selectedBeatmap != null) {
            // osu!stable restarts the song back to preview time when the player is in the last 10 seconds *or* 2% of the beatmap.
            boolean continuePreview = mSecPassed < totalLength - 10000 && mSecPassed / totalLength < 0.98f;
            int previewTime = continuePreview ? songService.getPosition() : selectedBeatmap.getPreviewTime();

            songMenu.playMusic(selectedBeatmap.getAudioPath(), previewTime);

            if (continuePreview) {
                songMenu.startMusicVolumeAnimation(0.3f);
            }
        }

        if (replaying) {
            replayFilePath = null;
        }
    }

    public void quit() {
        // Disable historical event processing for more efficient ACTION_MOVE reports, since frequent reports are
        // not that relevant outside gameplay.
        var touchOptions = new TouchOptions();
        touchOptions.setRunOnUpdateThread(true);
        touchOptions.setProcessHistoricalEvents(false);
        touchOptions.setUseRawPointer(false);

        var touchController = engine.getTouchController();
        touchController.applyTouchOptions(touchOptions);
        touchController.resetRawPointers();

        // Enable screen dimming
        engine.getEngineOptions().setWakeLockOptions(WakeLockOptions.SCREEN_DIM);
        GlobalManager.getInstance().getMainActivity().reapplyWakeLock();

        if (storyboardSprite != null) {
            storyboardSprite.detachSelf();
            storyboardOverlayProxy.detachSelf();
            storyboardSprite.releaseStoryboard();
            storyboardOverlayProxy.setDrawProxy(null);
            storyboardSprite = null;
        }

        if (video != null) {
            video.release();
            video = null;
            videoStarted = false;
        }

        if (sceneBorder != null) {
            sceneBorder.detachSelf();
            sceneBorder = null;
        }

        onExit();
        resetPlayfieldSizeScale();
        scene = createMainScene();

        if (Multiplayer.isMultiplayer)
        {
            Multiplayer.roomScene.show();
            return;
        }
        ResourceManager.getInstance().getSound("failsound").stop();
        engine.setScene(oldScene);

        // Resume difficulty calculation.
        DifficultyCalculationManager.calculateDifficulties();
    }


    public void reset() {
    }

    //CB打击处理
    private String registerHit(final int objectId, final int score, final boolean endCombo) {
        return registerHit(objectId, score, endCombo, true);
    }

    private String registerHit(final int objectId, final int score, final boolean endCombo, final boolean incrementCombo) {

        if (isGameOver) {
            return "hit0";
        }

        boolean writeReplay = objectId != -1 && replay != null && !replaying;
        if (score == 0) {
            if (stat.getCombo() > 30) {
                var sound = ResourceManager.getInstance().getCustomSound("combobreak", 1);
                if (sound != null) {
                    sound.play();
                }
            }
            comboWasMissed = true;
            stat.registerHit(0, false, false, incrementCombo);
            if (writeReplay) replay.addObjectScore(objectId, ResultType.MISS);
            if (GameHelper.isPerfect()) {
                gameover();

                if (!Multiplayer.isMultiplayer)
                    restartGame();
            }
            if (GameHelper.isSuddenDeath()) {
                stat.changeHp(-1.0f);
                gameover();
            }
            if (objectId != -1) {
                updatePPValue(objectId);
            }
            return "hit0";
        }

        String scoreName = "hit300";
        if (score == 50) {
            stat.registerHit(50, false, false, incrementCombo);
            if (writeReplay) replay.addObjectScore(objectId, ResultType.HIT50);
            scoreName = "hit50";
            comboWas100 = true;
            if(GameHelper.isPerfect()){
                gameover();

                if (!Multiplayer.isMultiplayer)
                    restartGame();
            }
        } else if (score == 100) {
            comboWas100 = true;
            if (writeReplay) replay.addObjectScore(objectId, ResultType.HIT100);
            if (endCombo && !comboWasMissed) {
                stat.registerHit(100, true, false, incrementCombo);
                scoreName = "hit100k";
            } else {
                stat.registerHit(100, false, false, incrementCombo);
                scoreName = "hit100";
            }
            if(GameHelper.isPerfect()){
                gameover();
                if (!Multiplayer.isMultiplayer)
                    restartGame();
            }
        } else if (score == 300) {
            if (writeReplay) replay.addObjectScore(objectId, ResultType.HIT300);
            if (endCombo && !comboWasMissed) {
                if (!comboWas100) {
                    stat.registerHit(300, true, true, incrementCombo);
                    scoreName = "hit300g";
                } else {
                    stat.registerHit(300, true, false, incrementCombo);
                    scoreName = "hit300k";
                }
            } else {
                stat.registerHit(300, false, false, incrementCombo);
                scoreName = "hit300";
            }
        }

        if (endCombo) {
            comboWas100 = false;
            comboWasMissed = false;
        }

        if (objectId != -1) {
            updatePPValue(objectId);
        }

        return scoreName;
    }


    public void onCircleHit(int id, final float acc, final PointF pos,
                            final boolean endCombo, byte forcedScore, Color4 color) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        if (GameHelper.isAutoplay()) {
            autoCursor.click();
            hud.onGameplayTouchDown((float) parsedBeatmap.getHitObjects().objects.get(id).startTime / 1000);
        }

        float accuracy = Math.abs(acc);
        boolean writeReplay = replay != null && !replaying;
        if (writeReplay) {
            short sacc = (short) (acc * 1000);
            replay.addObjectResult(id, sacc, null);
        }
        if(GameHelper.isFlashlight() && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()){
           int nearestCursorId = getNearestCursorId(pos.x, pos.y);
           if (nearestCursorId >= 0) {
               mainCursorId = nearestCursorId;
               var latestNonUpEvent = cursors[mainCursorId].getLatestNonUpEvent();

               if (latestNonUpEvent != null) {
                   flashlightSprite.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y);
               }
            }
        }
        VibratorManager.INSTANCE.circleVibration();

        if (accuracy > playableBeatmap.getHitWindow().getMehWindow() / 1000 || forcedScore == ResultType.MISS.getId()) {
            createHitEffect(pos, "hit0", color);
            registerHit(id, 0, endCombo);
            return;
        }

        String scoreName;
        if (forcedScore == ResultType.HIT300.getId() ||
                forcedScore == 0 && accuracy <= playableBeatmap.getHitWindow().getGreatWindow() / 1000) {
            scoreName = registerHit(id, 300, endCombo);
        } else if (forcedScore == ResultType.HIT100.getId() ||
                forcedScore == 0 && accuracy <= playableBeatmap.getHitWindow().getOkWindow() / 1000) {
            scoreName = registerHit(id, 100, endCombo);
        } else {
            scoreName = registerHit(id, 50, endCombo);
        }

        createBurstEffect(pos, color);
        createHitEffect(pos, scoreName, color);

        hud.onNoteHit(stat);
    }

    public void onSliderReverse(PointF pos, float ang, Color4 color) {
        createBurstEffectSliderReverse(pos, ang, color);
    }

    public void onSliderHit(int id, final int score, final PointF judgementPos, final boolean endCombo,
                            Color4 color, int type, boolean incrementCombo) {
        if (GameHelper.isFlashlight() && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
            int nearestCursorId = getNearestCursorId(judgementPos.x, judgementPos.y);
            if (nearestCursorId >= 0) {
                mainCursorId = nearestCursorId;
                var latestNonUpEvent = cursors[mainCursorId].getLatestNonUpEvent();

                if (latestNonUpEvent != null) {
                    flashlightSprite.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y);
                }
            }
        }

        VibratorManager.INSTANCE.sliderVibration();

        // Whole slider was missed.
        if (score == 0) {
            createHitEffect(judgementPos, "hit0", color);
            registerHit(id, 0, endCombo);
            return;
        }

        // Nested object was missed.
        if (score == -1) {
            if (stat.getCombo() > 30) {
                var sound = ResourceManager.getInstance().getCustomSound("combobreak", 1);
                if (sound != null) {
                    sound.play();
                }
            }
            if (GameHelper.isSuddenDeath()) {
                stat.changeHp(-1.0f);
                gameover();
            }
            stat.registerHit(0, true, false);
            return;
        }

        String scoreName = "hit0";

        switch (type) {
            case GameObjectListener.SLIDER_START:
                if (incrementCombo) {
                    scoreName = "sliderpoint30";
                    stat.registerHit(30, false, false);
                    stat.addSliderHeadHit();
                    createBurstEffectSliderStart(judgementPos, color);
                    if (GameHelper.isAutoplay()) {
                        hud.onGameplayTouchDown((float) parsedBeatmap.getHitObjects().objects.get(id).startTime / 1000);
                    }
                }
                break;

            case GameObjectListener.SLIDER_REPEAT:
                if (incrementCombo) {
                    scoreName = "sliderpoint30";
                    stat.registerHit(30, false, false);
                    stat.addSliderRepeatHit();
                }
                break;

            case GameObjectListener.SLIDER_TICK:
                if (incrementCombo) {
                    scoreName = "sliderpoint10";
                    stat.registerHit(10, false, false);
                    stat.addSliderTickHit();
                }
                break;

            case GameObjectListener.SLIDER_END:
                // Slider end hit is tied to the final result of the slider.
                scoreName = registerHit(id, score, endCombo, incrementCombo);

                if (incrementCombo) {
                    stat.addSliderEndHit();
                    createBurstEffectSliderEnd(judgementPos, color);
                }
                break;
        }

        createHitEffect(judgementPos, scoreName, color);

        hud.onNoteHit(stat);
    }

    @Override
    public void onSpinnerStart(int id) {
        if (GameHelper.isAutoplay()) {
            autoCursor.click();
            hud.onGameplayTouchDown((float) parsedBeatmap.getHitObjects().objects.get(id).startTime / 1000);
        }
    }

    public void onSpinnerHit(int id, final int score, final boolean endCombo, int totalScore) {
        if (score == 1000) {
            stat.registerHit(score, false, false);
            return;
        }

        if (replay != null && !replaying) {
            short acc = (short) (totalScore * 4);
            switch (score) {
                case 300:
                    acc += 3;
                    break;
                case 100:
                    acc += 2;
                    break;
                case 50:
                    acc += 1;
                    break;
            }
            replay.addObjectResult(id, acc, null);
        }

        final PointF pos = new PointF((float) Config.getRES_WIDTH() / 2,
                (float) Config.getRES_HEIGHT() / 2);

        VibratorManager.INSTANCE.spinnerVibration();

        if (score == 0) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "hit0");
            effect.init(
                    scene,
                    pos,
                    scale,
                    Modifiers.sequence(
                        Modifiers.fadeIn(0.15f),
                        Modifiers.delay(0.35f),
                        Modifiers.fadeOut(0.25f)
                    )
            );
            registerHit(id, 0, endCombo);
            return;
        }

        String scoreName;

        // Simulate a hit for hit error meter registration.
        float accuracy = (float) switch (score) {
            case 300 -> {
                scoreName = registerHit(id, 300, endCombo);
                yield 0;
            }

            case 100 -> {
                scoreName = registerHit(id, 100, endCombo);
                yield hitWindow.getGreatWindow() + 1;
            }

            case 50 -> {
                scoreName = registerHit(id, 50, endCombo);
                yield hitWindow.getOkWindow() + 1;
            }

            default -> {
                scoreName = "hit0";
                yield hitWindow.getMehWindow() + 1;
            }
        } / 1000;

        createHitEffect(pos, scoreName, null);

        hud.onAccuracyRegister(HitObjectType.Spinner, accuracy);
        hud.onNoteHit(stat);
    }

    @Override
    public void playHitSamples(List<GameplayHitSampleInfo> samples) {
        float volume = 1;
        var muted = GameHelper.getMuted();

        if (muted != null && muted.affectsHitSounds()) {
            volume = muted.volumeAt(stat.getCombo());
        }

        for (int i = 0, size = samples.size(); i < size; ++i) {
            var sample = samples.get(i);
            sample.setVolume(volume);
            sample.play();
        }
    }

    private void stopLoopingSamples() {
        if (activeObjects == null) {
            return;
        }

        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            activeObjects.get(i).stopLoopingSamples();
        }
    }

    public void addObject(final GameObject object) {
        activeObjects.add(object);
    }

    @Override
    public boolean isObjectHittable(GameObject object) {
        return object == judgeableObject;
    }

    @Override
    public Cursor getCursor(int index) {
        return cursors[index];
    }

    public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent event) {
        float offset = previousFrameTime > 0
                ? (event.getMotionEvent().getEventTime() - previousFrameTime) * GameHelper.getSpeedMultiplier()
                : 0;
        int eventTime = (int) (elapsedTime * 1000 + offset);

        if (replaying || isGameOver) {
            return false;
        }

        var id = event.getPointerID();
        if (id < 0 || id >= getCursorsCount()) {
            return false;
        }

        var cursor = cursors[id];

        if (event.isActionDown()) {
            if (cursor.mouseBlocked) {
                cursor.mouseBlocked = false;
                return true;
            }

            int activeCursorCount = 0;

            for (int i = 0; i < cursors.length; ++i) {
                if (activeCursorCount >= maximumActiveCursorCount) {
                    break;
                }

                if (cursors[i].isMouseDown()) {
                    ++activeCursorCount;
                }
            }

            if (activeCursorCount >= maximumActiveCursorCount) {
                return false;
            }
        }

        var sprite = !GameHelper.isAutoplay() && !GameHelper.isAutopilot() && cursorSprites != null
                ? cursorSprites[id]
                : null;

        var cursorEvent = CursorEvent.obtain(event);

        cursorEvent.trackTime = elapsedTime * 1000;
        cursorEvent.offset = offset;

        if (sprite != null) {
            sprite.setPosition(cursorEvent.position.x, cursorEvent.position.y);
        }

        if (event.isActionDown()) {

            if (sprite != null) {
                sprite.setShowing(true);
            }

            if (!GameHelper.isAutoplay()) {
                hud.onGameplayTouchDown(eventTime / 1000f);
            }

            cursor.addEvent(cursorEvent);

            if (replay != null) {
                replay.addPress(eventTime, cursorEvent.trackPosition, id);
            }
        } else if (cursor.isMouseDown() && event.isActionMove()) {

            if (sprite != null) {
                sprite.setShowing(true);
            }

            cursor.addEvent(cursorEvent);

            if (replay != null) {
                replay.addMove(eventTime, cursorEvent.trackPosition, id);
            }

        } else if (cursor.isMouseDown() && event.isActionUp()) {

            if (sprite != null) {
                sprite.setShowing(false);
            }

            cursor.addEvent(cursorEvent);

            if (replay != null) {
                replay.addUp(eventTime, id);
            }

        } else if (event.isActionCancel() || event.isActionOutside()) {
            removeAllCursors();
        } else {
            return false;
        }
        return true;
    }

    private void removeAllCursors() {
        long currentTime = SystemClock.uptimeMillis();
        float offset = previousFrameTime > 0
                ? (currentTime - previousFrameTime) * GameHelper.getSpeedMultiplier()
                : 0;
        float time = elapsedTime * 1000 + offset;

        for (int i = 0; i < cursors.length; ++i) {
            var cursor = cursors[i];

            if (cursor.isMouseDown()) {
                var upEvent = CursorEvent.obtain();

                upEvent.systemTime = currentTime;
                upEvent.trackTime = time;
                upEvent.action = TouchEvent.ACTION_UP;

                cursor.addEvent(upEvent);

                if (replay != null) {
                    replay.addUp((int) time, i);
                }
            }

            if (cursorSprites != null) {
                cursorSprites[i].setShowing(false);
            }
        }
    }

    public void pause() {

        if (paused) {
            return;
        }

        if (isHUDEditorMode) {
            hud.onBackPress();
            return;
        }

        if (Multiplayer.isMultiplayer)
        {
            // Setting a delay of 300ms minimum for the player to tap back button again.
            if (lastBackPressTime > 0 && realTimeElapsed - lastBackPressTime > Math.max(300, Config.getBackButtonPressTime() * 1.5f))
            {
                // Room being null can happen when the player disconnects from socket while playing
                if (Multiplayer.isConnected())
                    Execution.async(() -> Execution.runSafe(() -> RoomAPI.submitFinalScore(stat.toJson())));

                Multiplayer.log("Player left the match.");
                quit();
                return;
            }

            lastBackPressTime = realTimeElapsed;
            ToastLogger.showText("Tap twice to exit to room.", false);
            return;
        }

        if (isGameOver) {
            // Finishing the game over animation now.
            GlobalManager.getInstance().getSongService().setFrequencyForcefully(101);
            return;
        }

        if (video != null && videoStarted) {
            video.pause();
        }

        Execution.updateThread(this::stopLoopingSamples);

        if (!GameHelper.isAutoplay() && !GameHelper.isAutopilot() && !replaying) {
            removeAllCursors();
        }

        if (GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
            GlobalManager.getInstance().getSongService().pause();
        }
        paused = true;
        scene.setIgnoreUpdate(true);

        final PauseMenu menu = new PauseMenu(engine, this, false);
        UIEngine.getCurrent().getOverlay().setChildScene(menu.getScene(), false, true, true);
    }

    public void gameover() {

        if (isGameOver) {
            return;
        }
        isGameOver = true;

        if (!replaying) {
            removeAllCursors();
        }

        if (Multiplayer.isMultiplayer) {
            if (Multiplayer.isConnected()) {
                Multiplayer.log("Player has lost, moving to room scene.");
                Execution.async(() -> Execution.runSafe(() -> RoomAPI.submitFinalScore(stat.toJson())));
            }
            quit();
            return;
        }

        stopLoopingSamples();
        SongService songService = GlobalManager.getInstance().getSongService();

        if (GameHelper.isPerfect()) {
            if (video != null) {
                video.pause();
            }
            songService.pause();
            paused = true;
            scene.setIgnoreUpdate(true);
            return;
        }

        ResourceManager.getInstance().getSound("failsound").play();
        gameStarted = false;

        float initialFrequency = songService.getFrequency();

        // Locally saving the scenes references to avoid unexpected behavior when the scene is changed.
        UIScene scene = this.scene;
        UIScene mgScene = this.mgScene;
        UIScene bgScene = this.bgScene;

        // Wind down animation for failing based on osu!stable behavior.
        engine.registerUpdateHandler(new IUpdateHandler() {
            private float elapsedTime;

            private void applyEffectToScene(Scene scene) {
                if (scene.getAlpha() > 0) {
                    scene.setAlpha(Math.max(0, scene.getAlpha() - 0.007f));
                }

                for (int i = 0; i < scene.getChildCount(); i++) {
                    IEntity entity = scene.getChild(i);

                    entity.setPosition(entity.getX(), entity.getY() < 0f ? entity.getY() * 0.6f : entity.getY() * 1.01f);

                    if (entity.getRotation() == 0) {
                        entity.setRotation(entity.getRotation() + (float) Random.Default.nextDouble(-0.02, 0.02) * 180 / FMath.Pi);
                    } else if (entity.getRotation() > 0) {
                        entity.setRotation(entity.getRotation() + 0.01f * 180 / FMath.Pi);
                    } else {
                        entity.setRotation(entity.getRotation() - 0.01f * 180 / FMath.Pi);
                    }
                }
            }

            @Override
            public void onUpdate(float pSecondsElapsed) {

                // Ensure this update handler is removed under unexpected circumstances.
                if (engine.getScene() != scene) {
                    engine.unregisterUpdateHandler(this);
                    return;
                }

                elapsedTime += pSecondsElapsed;

                // In osu!stable, the update is capped to 60 FPS. This means in higher framerates, the animations
                // need to be slowed down to match 60 FPS.
                float sixtyFPS = 1 / 60f;

                if (elapsedTime < sixtyFPS) {
                    return;
                }

                elapsedTime -= sixtyFPS;

                if (songService.getFrequency() > 101) {

                    applyEffectToScene(mgScene);
                    applyEffectToScene(bgScene);

                    float decreasedFrequency = Math.max(101, songService.getFrequency() - 300);
                    float decreasedSpeed = GameHelper.getSpeedMultiplier() * (1 - (initialFrequency - decreasedFrequency) / initialFrequency);

                    if (videoEnabled && video != null) {
                        // In some devices this can throw an exception, unfortunately there's no
                        // documentation that explains how to avoid that scenario. Thanks Google.
                        try {
                            video.setPlaybackSpeed(decreasedSpeed);
                        } catch (Exception e) {
                            Log.e("GameScene", "Failed to change video playback speed during game over animation.", e);
                        }
                    }

                    songService.setFrequencyForcefully(decreasedFrequency);
                } else {
                    if (videoEnabled && video != null) {
                        video.pause();
                    }

                    // Ensure music frequency is reset back to what it was.
                    songService.setFrequencyForcefully(initialFrequency);

                    if (songService.getStatus() == Status.PLAYING) {
                        songService.pause();
                    }

                    paused = true;

                    scene.setIgnoreUpdate(true);
                    engine.unregisterUpdateHandler(this);

                    PauseMenu menu = new PauseMenu(engine, GameScene.this, true);
                    UIEngine.getCurrent().getOverlay().setChildScene(menu.getScene(), false, true, true);
                }
            }

            @Override
            public void reset() {
            }
        });
    }

    public void resume() {
        if (!paused) {
            return;
        }

        scene.setIgnoreUpdate(false);
        UIEngine.getCurrent().getOverlay().getChildScene().back();
        paused = false;

        if (stat.getHp() <= 0 && !stat.getMod().contains(ModNoFail.class)) {
            quit();
            return;
        }

        if (video != null && videoStarted) {
            video.play();
        }

        if (GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getStatus() != Status.PLAYING && elapsedTime > 0) {
            GlobalManager.getInstance().getSongService().play();
            GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
            totalLength = GlobalManager.getInstance().getSongService().getLength();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    private void createHitEffect(final PointF pos, final String name, Color4 color) {

        var effect = GameObjectPool.getInstance().getEffect(name);
        var isAnimated = effect.hit instanceof UIAnimatedSprite animatedHit && animatedHit.getFrames().length > 1;

        // Reference https://github.com/ppy/osu/blob/ebf637bd3c33f1c886f6bfc81aa9ea2132c9e0d2/osu.Game/Skinning/LegacyJudgementPieceOld.cs

        var fadeInLength = 0.12f;
        var fadeOutLength = 0.6f;
        var fadeOutDelay = 0.5f;

        var fadeSequence = Modifiers.sequence(
            Modifiers.fadeIn(fadeInLength),
            Modifiers.delay(fadeOutDelay),
            Modifiers.fadeOut(fadeOutLength)
        );

        if (name.equals("hit0")) {
            var rotation = (float) Random.Default.nextDouble(8.6 * 2) - 8.6f;

            if (isAnimated) {
                // Legacy judgements don't play any transforms if they are an animation.
                effect.init(
                    mgScene,
                    pos,
                    scale,
                    fadeSequence
                );
            } else {
                effect.init(
                    mgScene,
                    pos,
                    scale * 1.6f,
                    fadeSequence,
                    Modifiers.scale(0.1f, scale * 1.6f, scale, null, Easing.InQuad),
                    Modifiers.translateY(fadeOutDelay + fadeOutLength, -5f, 80f, null, Easing.InQuad),
                    Modifiers.sequence(
                        Modifiers.rotation(fadeInLength, 0, rotation),
                        Modifiers.rotation(fadeOutDelay + fadeOutLength - fadeInLength, rotation, rotation * 2, null, Easing.InQuad)
                    )
                );
            }

            return;
        }

        if (Config.isHitLighting() && !name.equals("sliderpoint10") && !name.equals("sliderpoint30") && ResourceManager.getInstance().getTexture("lighting") != null) {

            // Reference https://github.com/ppy/osu/blob/a7e110f6693beca6f6e6a20efb69a6913d58550e/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuJudgement.cs#L71-L88

            var light = GameObjectPool.getInstance().getEffect("lighting");
            light.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_DST_ALPHA);
            light.setColor(color);
            light.init(
                bgScene,
                pos,
                scale * 0.8f,
                Modifiers.scale(0.6f, scale * 0.8f, scale * 1.2f, null, Easing.OutQuad),
                Modifiers.sequence(
                    Modifiers.fadeIn(0.2f),
                    Modifiers.delay(0.2f),
                    Modifiers.fadeOut(1f)
                )
            );
        }

        // Legacy judgements don't play any transforms if they are an animation.
        if (isAnimated) {
            effect.init(
                mgScene,
                pos,
                scale,
                fadeSequence
            );
        } else {
            effect.init(
                mgScene,
                pos,
                scale * 0.6f,
                fadeSequence,
                Modifiers.sequence(
                    Modifiers.scale(fadeInLength * 0.8f, scale * 0.6f, scale * 1.1f),
                    Modifiers.delay(fadeInLength * 0.2f),
                    Modifiers.scale(fadeInLength * 0.2f, scale * 1.1f, scale * 0.9f),

                    // stable dictates scale of 0.9->1 over time 1.0 to 1.4, but we are already at 1.2.
                    // so we need to force the current value to be correct at 1.2 (0.95) then complete the
                    // second half of the transform.
                    Modifiers.scale(fadeInLength * 0.2f, scale * 0.95f, scale)
                )
            );
        }
    }

    private void applyBurstEffect(GameEffect effect, PointF pos) {

        // Reference: https://github.com/ppy/osu/blob/c5893f245ce7a89d1900dbb620390823702481fe/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyMainCirclePiece.cs#L152-L174

        var fadeDuration = 0.24f;

        effect.init(mgScene, pos, scale,
            Modifiers.scale(fadeDuration, scale, scale * 1.4f, null, Easing.OutQuad),
            Modifiers.fadeOut(fadeDuration)
        );
    }

    private void createBurstEffect(final PointF pos, final Color4 color) {
        if (!Config.isBurstEffects() ||
                (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
                GameHelper.isTraceable())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("hitcircle");
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("hitcircleoverlay");
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderStart(final PointF pos, final Color4 color) {
        if (!Config.isBurstEffects() ||
                (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
                GameHelper.isTraceable())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("sliderstartcircle");
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("sliderstartcircleoverlay");
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderEnd(final PointF pos, final Color4 color) {
        if (!Config.isBurstEffects() ||
                (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
                GameHelper.isTraceable())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("sliderendcircle");
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("sliderendcircleoverlay");
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderReverse(final PointF pos, float ang, final Color4 color) {
        if (!Config.isBurstEffects() ||
                (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
                GameHelper.isTraceable())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("reversearrow");
        burst1.hit.setRotation(ang);
        applyBurstEffect(burst1, pos);
    }

    public int getCursorsCount() {
        return cursors.length;
    }


    public void registerAccuracy(HitObjectType type, final double acc) {
        double mehWindow = hitWindow.getMehWindow() / 1000;

        if (-mehWindow <= acc && acc <= mehWindow) {
            offsetSum += (float) acc;
            offsetRegs++;

            if (type != HitObjectType.Spinner) {
                stat.addHitOffset(acc);
            }

            if (replaying) {
                scoringScene.getReplayStat().addHitOffset(acc);
            }
        }

        hud.onAccuracyRegister(type, (float) acc);
    }


    public void onSliderEnd(int id, int accuracy, BitSet tickSet) {
        onTrackingSliders(false);
        if (GameHelper.isAutoplay()) {
            autoCursor.onSliderEnd();
        }
        if (replay != null && !replaying) {
            short acc = (short) (accuracy);
            replay.addObjectResult(id, acc, (BitSet) tickSet.clone());
        }
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        if (GameHelper.isAutoplay()) {
            autoCursor.onSliderTracking();
        }
        if (GameHelper.isFlashlight()) {
            flashlightSprite.onTrackingSliders(isTrackingSliders);
        }
    }

    public void onUpdatedAutoCursor(float pX, float pY) {
        if (GameHelper.isFlashlight()) {
            flashlightSprite.onMouseMove(pX, pY);
        }
    }

    public void updateAutoBasedPos(float pX, float pY) {
        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.setPosition(pX, pY, this);
        }
    }

    private int getNearestCursorId(float pX, float pY) {
        float nearestDistance = Float.POSITIVE_INFINITY;
        int id = -1;

        for (int i = 0; i < cursors.length; ++i) {
            var latestEvent = cursors[i].getLatestNonUpEvent();

            if (latestEvent != null) {
                float distance = Utils.squaredDistance(pX, pY, latestEvent.position.x, latestEvent.position.y);

                if (distance < nearestDistance) {
                    id = i;
                    nearestDistance = distance;
                }
            }
        }

        return id;
    }

    private void calculateAllSliderPaths(@Nullable final CoroutineScope scope) {
        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || playableBeatmap.getHitObjects().getSliderCount() == 0) {
            return;
        }

        var sliderPaths = new SliderPath[playableBeatmap.getHitObjects().getSliderCount()];
        var sliderRenderPaths = new SliderBody.RenderPathCache[playableBeatmap.getHitObjects().getSliderCount()];
        int index = 0;

        for (var obj : playableBeatmap.getHitObjects().objects) {
            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            if (!(obj instanceof Slider slider)) {
                continue;
            }

            sliderPaths[index] = GameHelper.convertSliderPath(slider, scope);

            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            float scale = slider.getScreenSpaceGameplayScale();
            float sliderBodyWidth = OsuSkin.get().getSliderBodyWidth() * scale;
            float sliderBorderWidth = OsuSkin.get().getSliderBorderWidth() * scale;
            float sliderHintWidth = OsuSkin.get().getSliderHintWidth() * scale;
            boolean isHintVisible = OsuSkin.get().isSliderHintEnable() &&
                slider.getDistance() > OsuSkin.get().getSliderHintShowMinLength();

            sliderRenderPaths[index] = SliderBody.createCache(
                GameHelper.convertSliderPath(sliderPaths[index], scope),
                sliderBodyWidth,
                sliderBorderWidth,
                isHintVisible,
                sliderHintWidth,
                Config.isSnakingInSliders() || Config.isSnakingOutSliders()
            );

            ++index;
        }


        this.sliderPaths = sliderPaths;
        this.sliderRenderPaths = sliderRenderPaths;
    }

    private SliderPath getSliderPath(int index) {
        if (sliderPaths != null && index < sliderPaths.length && index >= 0){
            return sliderPaths[index];
        }
        else {
            return null;
        }
    }

    private SliderBody.RenderPathCache getSliderRenderPath(int index) {
        if (sliderRenderPaths == null) {
            throw new IllegalStateException(
                "Slider render paths have not been calculated; sliderRenderPaths is null, index=" + index
            );
        }

        if (index < 0 || index >= sliderRenderPaths.length) {
            throw new IndexOutOfBoundsException(
                "Invalid slider render path index " + index + " for sliderRenderPaths.length=" + sliderRenderPaths.length
            );
        }

        var renderPath = sliderRenderPaths[index];

        if (renderPath == null) {
            throw new IllegalStateException(
                "Slider render path at index " + index + " is null after calculation; sliderRenderPaths.length=" + sliderRenderPaths.length
            );
        }

        return renderPath;
    }

    public boolean getReplaying() {
        return replaying;
    }

    public @Nullable DroidPlayableBeatmap getPlayableBeatmap() {
        return playableBeatmap;
    }

    public boolean saveFailedReplay() {
        stat.setTime(System.currentTimeMillis());
        if (replay != null && !replaying) {
            //write misses to replay
            for (GameObject obj : activeObjects) {
                stat.registerHit(0, false, false);
                replay.addObjectScore(obj.getId(), ResultType.MISS);
            }
            while (objectIndex < objects.length) {
                ++objectIndex;
                stat.registerHit(0, false, false);
                replay.addObjectScore(++lastObjectId, ResultType.MISS);
            }

            var currentTime = String.valueOf(System.currentTimeMillis());
            var odrFilename = MD5Calculator.getStringMD5(lastBeatmapInfo.getFilename() + currentTime) + currentTime.substring(0, Math.min(3, currentTime.length())) + ".odr";

            replayFilePath = Config.getScorePath() + odrFilename;
            replay.setStat(stat);
            replay.save(replayFilePath);

            if (stat.getTotalScoreWithMultiplier() > 0 && !stat.getMod().contains(ModAutoplay.class)) {
                stat.setReplayFilename(odrFilename);
                stat.setBeatmapMD5(lastBeatmapInfo.getMD5());

                try {
                    DatabaseManager.getScoreInfoTable().insertScore(stat.toScoreInfo());
                } catch (Exception e) {
                    Log.e("GameScene", "Failed to save score to database", e);
                }
            }

            ToastLogger.showText(StringTable.get(com.osudroid.resources.R.string.message_save_replay_successful), true);
            replayFilePath = null;
            return true;
        }
        else{
            ToastLogger.showText(StringTable.get(com.osudroid.resources.R.string.message_save_replay_failed), true);
            return false;
        }
    }

    private void updatePPValue(int objectId) {
        if (Config.isHideInGameUI() || !isHUDEditorMode && !OsuSkin.get().getHUDSkinData().hasElement(HUDPPCounter.class)) {
            return;
        }

        double pp = switch (Config.getDifficultyAlgorithm()) {
            case droid -> getDroidPPAt(objectId);
            case standard -> getStandardPPAt(objectId);
        };

        stat.setPP(pp);
    }

    private double getDroidPPAt(int objectId) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || droidTimedDifficultyAttributes == null ||
                performanceCalculationParameters == null || objectId < 0 ||
                objectId >= droidTimedDifficultyAttributes.length) {
            return 0;
        }

        var timedAttributes = droidTimedDifficultyAttributes[objectId];

        performanceCalculationParameters.populate(playableBeatmap, stat);

        return BeatmapDifficultyCalculator.calculateDroidPerformance(
            timedAttributes.attributes, (DroidPerformanceCalculationParameters) performanceCalculationParameters
        ).total;
    }

    private double getStandardPPAt(int objectId) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || standardTimedDifficultyAttributes == null ||
                performanceCalculationParameters == null || objectId < 0 ||
                objectId >= standardTimedDifficultyAttributes.length) {
            return 0;
        }

        var timedAttributes = standardTimedDifficultyAttributes[objectId];

        performanceCalculationParameters.populate(playableBeatmap, stat);

        return BeatmapDifficultyCalculator.calculateStandardPerformance(
            timedAttributes.attributes, (StandardPerformanceCalculationParameters) performanceCalculationParameters
        ).total;
    }

    private UIScene createMainScene() {
        return new UIScene() {
            // Reused buffer to avoid allocations.
            private final float[] fastPathSurfaceCoords = new float[2];

            // Stable fallback cache per pointer (surface space).
            private final boolean[] fastPathHasStableSnapshot = new boolean[CursorCount];
            private final float[] fastPathLastStableX = new float[CursorCount];
            private final float[] fastPathLastStableY = new float[CursorCount];

            private boolean isInterpolating;

            @Override
            protected void onManagedDraw(GL10 pGL, Camera pCamera) {
                applyRawPointerFastPath(pCamera);

                super.onManagedDraw(pGL, pCamera);
            }

            @Override
            protected void onManagedUpdate(float secElapsed) {
                var songService = GlobalManager.getInstance().getSongService();
                float speedMultiplier = GameHelper.getSpeedMultiplier();
                float dt = secElapsed * speedMultiplier;

                if (songService.getStatus() == Status.PLAYING) {
                    // BASS may report the wrong position. When that happens, `dt` will either be negative or more than the
                    // actual progressed time. To prevent that situation from happening, we keep `dt` between thresholds.
                    // They serve as a buffer zone to allow audio and gameplay time to synchronize in cases where one is
                    // behind or ahead of the other.
                    // See https://github.com/ppy/osu/issues/26879 for more information.
                    float minDt = dt / 2;
                    float maxDt = dt * 2;

                    float audioElapsedTime = (float) songService.getPositionPrecise() / 1000;
                    float gameElapsedTime = elapsedTime - getRateAdjustedOffset();
                    float timeDifference = gameElapsedTime - audioElapsedTime;

                    // In some cases, the audio can be behind the gameplay time so far it would cause gameplay to
                    // completely desynchronize. In that case, we do not let gameplay progress at all until the audio
                    // catches up.
                    if (timeDifference <= 0.1f * speedMultiplier) {
                        float minimumSynchronizationTime = Config.getMinimumGameplaySynchronizationTime() * speedMultiplier / 1000;
                        // Sync gameplay with audio if the difference is too large.
                        if (!isGameOver && timeDifference >= minimumSynchronizationTime) {
                            if (minimumSynchronizationTime > 0) {
                                Log.i("GameScene",
                                        "Synchronizing gameplay time with audio time at " +
                                        audioElapsedTime +
                                        "s audio time and " +
                                        gameElapsedTime +
                                        "s gameplay time. Difference: " + timeDifference + "s");
                            }
                            dt = -timeDifference;
                        }

                        dt = FMath.clamp(dt, minDt, maxDt);
                    } else {
                        dt = 0;
                    }
                } else if (!musicStarted) {
                    float realElapsedTime = elapsedTime + dt;
                    float targetElapsedTime = Math.min(realElapsedTime, getRateAdjustedOffset());
                    float newElapsedTime;

                    if (isInterpolating) {
                        newElapsedTime = Interpolation.dampContinuously(realElapsedTime, targetElapsedTime, 0.08f, secElapsed);

                        // If the difference is more than ~2 frames at 60 FPS, snap to the target time.
                        if (Math.abs(targetElapsedTime - newElapsedTime) > 1f / 60f * 2f * speedMultiplier) {
                            newElapsedTime = targetElapsedTime;
                            isInterpolating = false;
                        }
                    } else {
                        newElapsedTime = targetElapsedTime;

                        // Only interpolate on second frame onwards.
                        if (previousFrameTime > 0) {
                            isInterpolating = true;
                        }
                    }

                    dt = Math.max(0, newElapsedTime - elapsedTime);
                }

                update(dt);

                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < cursors.length; ++i) {
                    cursors[i].reset(previousFrameTime, elapsedTime * 1000);
                }

                super.onManagedUpdate(dt);
            }

            private void applyRawPointerFastPath(final Camera camera) {
                var touchController = engine.getTouchController();

                if (touchController == null || !touchController.isUseRawPointers()) {
                    return;
                }

                if (replaying || GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
                    return;
                }

                var sprites = cursorSprites;

                if (sprites == null) {
                    return;
                }

                // Use update thread's active state to determine visibility of the sprites to respect the maximum
                // active cursor limitation.
                int count = Math.min(Math.min(getCursorsCount(), sprites.length), touchController.getRawPointerCapacity());
                int updatePathActiveCount = 0;

                for (int i = 0; i < count; ++i) {
                    var sprite = sprites[i];

                    if (sprite == null) {
                        continue;
                    }

                    var cursor = cursors[i];
                    boolean isUpdatePathDown = cursor != null && cursor.isMouseDown();

                    sprite.setShowing(isUpdatePathDown);

                    if (!isUpdatePathDown) {
                        fastPathHasStableSnapshot[i] = false;
                        continue;
                    }

                    if (updatePathActiveCount >= maximumActiveCursorCount) {
                        continue;
                    }

                    ++updatePathActiveCount;

                    if (tryReadRawPointer(i)) {
                        fastPathLastStableX[i] = fastPathSurfaceCoords[0];
                        fastPathLastStableY[i] = fastPathSurfaceCoords[1];
                        fastPathHasStableSnapshot[i] = true;
                    } else if (fastPathHasStableSnapshot[i]) {
                        // Revert to latest stable coordinates if read fails.
                        fastPathSurfaceCoords[0] = fastPathLastStableX[i];
                        fastPathSurfaceCoords[1] = fastPathLastStableY[i];
                    } else {
                        // No stable sample yet. Keep update thread position.
                        continue;
                    }

                    // Per underlying implementation, this is thread-safe since the camera is never rotated (thus the
                    // shared array is never used). When this is not the case, this must be revisited.
                    float[] sceneCoords = Cameras.convertSurfaceToSceneCoordinates(camera, fastPathSurfaceCoords);

                    sprite.setPosition(sceneCoords[0], sceneCoords[1]);
                }
            }

            private boolean tryReadRawPointer(int pointerId) {
                var touchController = engine.getTouchController();

                if (touchController == null) {
                    return false;
                }

                for (int attempt = 0; attempt < 2; ++attempt) {
                    int versionBefore = touchController.getRawPointerVersion(pointerId);

                    // An odd version means the main thread is updating this pointer, so we wait.
                    if ((versionBefore & 1) != 0) {
                        continue;
                    }

                    float x = touchController.getRawPointerSurfaceX(pointerId);
                    float y = touchController.getRawPointerSurfaceY(pointerId);

                    int versionAfter = touchController.getRawPointerVersion(pointerId);

                    if (versionBefore == versionAfter && (versionAfter & 1) == 0) {
                        // Successfully read a consistent snapshot.
                        fastPathSurfaceCoords[0] = x;
                        fastPathSurfaceCoords[1] = y;
                        return true;
                    }
                }

                return false;
            }
        };
    }

    private void applyPlayfieldSizeScale() {
        // IMPORTANT: This MUST be called only when the game scene is displayed, otherwise it will scale the currently
        // displayed scene (the one before gameplay starts), which is not what we want.
        if (!(GlobalManager.getInstance().getCamera() instanceof SmoothCamera camera)) {
            return;
        }

        float playfieldSize = Config.getPlayfieldSize();
        float playfieldHorizontalPosition = Config.getPlayfieldHorizontalPosition();
        float playfieldVerticalPosition = Config.getPlayfieldVerticalPosition();

        camera.setZoomFactorDirect(playfieldSize);

        camera.setCenterDirect(
            Config.getRES_WIDTH() * Interpolation.linear(
                Interpolation.linear(0f, 0.5f, playfieldSize),
                Interpolation.linear(1f, 0.5f, playfieldSize),
                1 - playfieldHorizontalPosition
            ),
            Config.getRES_HEIGHT() * Interpolation.linear(
                Interpolation.linear(0f, 0.5f, playfieldSize),
                Interpolation.linear(1f, 0.5f, playfieldSize),
                1 - playfieldVerticalPosition
            )
        );
    }

    private void resetPlayfieldSizeScale() {
        if (!(GlobalManager.getInstance().getCamera() instanceof SmoothCamera camera)) {
            return;
        }

        camera.setZoomFactorDirect(1f);
        camera.setCenterDirect(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
    }

    private int estimateMaximumActiveObjects() {
        if (objects == null) {
            return 0;
        }

        // Estimate the maximum number of simultaneously active objects to pre-size the lists and minimize
        // array reallocations.
        var lifetimeEnds = new PriorityQueue<Double>(Math.max(1, objects.length / 4));
        int estimatedMaxActiveObjects = 0;

        for (var object : objects) {
            double lifetimeStart = object.startTime - object.timePreempt;

            // Remove all objects that have expired by the time this object's lifetime starts.
            while (!lifetimeEnds.isEmpty() && lifetimeEnds.peek() <= lifetimeStart) {
                lifetimeEnds.poll();
            }

            double lifetimeEnd;

            if (object instanceof HitCircle) {
                var hitWindow = object.hitWindow;
                lifetimeEnd = object.startTime + (hitWindow != null ? hitWindow.getMehWindow() : 0);
            } else if (object instanceof Slider slider) {
                lifetimeEnd = Math.max(object.startTime + slider.getHead().hitWindow.getMehWindow(), object.getEndTime());
            } else {
                lifetimeEnd = object.getEndTime();
            }

            lifetimeEnds.add(lifetimeEnd);
            estimatedMaxActiveObjects = Math.max(estimatedMaxActiveObjects, lifetimeEnds.size());
        }

        return estimatedMaxActiveObjects;
    }

    private float getRateAt(double time) {
        return ModUtils.calculateRateWithTrackRateMods(rateAdjustingMods, time);
    }

    private float getRateAdjustedOffset() {
        return totalOffset * GameHelper.getSpeedMultiplier();
    }
}
