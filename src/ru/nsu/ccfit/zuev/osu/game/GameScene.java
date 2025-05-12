package ru.nsu.ccfit.zuev.osu.game;

import static kotlinx.coroutines.JobKt.ensureActive;

import android.graphics.PointF;
import android.os.SystemClock;
import android.util.Log;

import kotlin.random.Random;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Job;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.SecurityUtils;

import com.acivev.VibratorManager;
import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.framework.support.ProxySprite;
import com.edlplan.framework.support.osb.StoryboardSprite;
import com.edlplan.framework.utils.functionality.SmartIterator;
import com.osudroid.multiplayer.api.RoomAPI;
import com.osudroid.beatmaps.DifficultyCalculationManager;
import com.osudroid.data.BeatmapInfo;
import com.osudroid.ui.v2.GameLoaderScene;
import com.osudroid.data.DatabaseManager;
import com.osudroid.ui.v2.modmenu.ModIcon;
import com.osudroid.utils.Execution;
import com.reco1l.andengine.sprite.AnimatedSprite;
import com.reco1l.andengine.sprite.ExtendedSprite;
import com.reco1l.andengine.Modifiers;
import com.reco1l.andengine.Anchor;
import com.reco1l.andengine.sprite.VideoSprite;
import com.reco1l.andengine.ExtendedScene;
import com.osudroid.ui.v2.game.FollowPointConnection;
import com.osudroid.ui.v2.hud.GameplayHUD;
import com.osudroid.ui.v2.game.SliderTickSprite;
import com.osudroid.ui.v2.hud.elements.HUDPPCounter;
import com.osudroid.ui.v1.BlockAreaFragment;
import com.osudroid.multiplayer.Multiplayer;
import com.osudroid.multiplayer.RoomScene;

import com.rian.osu.GameMode;
import com.rian.osu.beatmap.Beatmap;
import com.rian.osu.beatmap.DroidPlayableBeatmap;
import com.rian.osu.beatmap.HitWindow;
import com.rian.osu.beatmap.constants.BeatmapCountdown;
import com.rian.osu.beatmap.hitobject.HitCircle;
import com.rian.osu.beatmap.hitobject.HitObject;
import com.rian.osu.beatmap.hitobject.Slider;
import com.rian.osu.beatmap.hitobject.Spinner;
import com.rian.osu.beatmap.parser.BeatmapParser;
import com.rian.osu.beatmap.timings.EffectControlPoint;
import com.rian.osu.beatmap.timings.TimingControlPoint;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes;
import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes;
import com.rian.osu.difficulty.attributes.TimedDifficultyAttributes;
import com.rian.osu.gameplay.GameplayHitSampleInfo;
import com.rian.osu.mods.*;
import com.rian.osu.ui.FPSCounter;
import com.rian.osu.utils.ModHashMap;
import com.rian.osu.utils.ModUtils;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.TouchOptions;
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
import java.util.*;
import java.util.concurrent.CancellationException;

import javax.annotation.Nullable;
import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.effect.Metronome;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.AutoCursor;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.Cursor;
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
    private final Engine engine;
    private final Cursor[] cursors = new Cursor[CursorCount];
    private final boolean[] cursorIIsDown = new boolean[CursorCount];
    public String audioFilePath = null;
    private ExtendedScene scene;
    private ExtendedScene bgScene, mgScene, fgScene;
    private Scene oldScene;
    private Beatmap parsedBeatmap;
    private DroidPlayableBeatmap playableBeatmap;
    private BeatmapInfo lastBeatmapInfo;
    private ScoringScene scoringScene;
    private LinkedList<TimingControlPoint> timingControlPoints;
    private LinkedList<EffectControlPoint> effectControlPoints;
    private TimingControlPoint activeTimingPoint;
    private EffectControlPoint activeEffectPoint;
    private int lastObjectId = -1;
    private float leadOut = 0;
    private LinkedList<HitObject> objects;
    private ArrayList<RGBColor> comboColors;
    private boolean comboWasMissed = false;
    private boolean comboWas100 = false;
    private LinkedList<GameObject> activeObjects;
    private LinkedList<GameObject> expiredObjects;
    private Queue<BreakPeriod> breakPeriods = new LinkedList<>();
    private Metronome metronome;
    private float scale;
    public StatisticV2 stat;
    private boolean gameStarted;
    private float totalOffset;
    private int totalLength = Integer.MAX_VALUE;
    private boolean paused;
    private ExtendedSprite skipBtn;
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
    private RGBColor sliderBorderColor;
    private float lastActiveObjectHitTime = 0;
    private SliderPath[] sliderPaths = null;
    private LinePath[] sliderRenderPaths = null;
    private int sliderIndex = 0;
    private ExtendedSprite unrankedSprite;
    private final ArrayList<IModApplicableToTrackRate> rateAdjustingMods = new ArrayList<>();

    private StoryboardSprite storyboardSprite;

    private ProxySprite storyboardOverlayProxy;

    public HitWindow hitWindow;

    private Job loadingJob;
    private ModHashMap lastMods;
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

    /**
     * The current background loading job.
     */
    @Nullable
    private Job backgroundLoadingJob;


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

    /**The video sprite*/
    private VideoSprite video;

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

    /**The block area manager fragment used to manage the block area.*/
    private BlockAreaFragment blockAreaFragment;


    public GameScene(final Engine engine) {
        this.engine = engine;
        scene = createMainScene();
        bgScene = new ExtendedScene();
        fgScene = new ExtendedScene();
        mgScene = new ExtendedScene();
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

    private void loadBackground(BeatmapInfo beatmapInfo) {
        dimRectangle = null;
        videoStarted = false;
        videoOffset = playableBeatmap.getEvents().videoStartTime / 1000f;

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        float brightness = Config.getInt("bgbrightness", 25) / 100f;

        TextureRegion textureRegion = Config.isSafeBeatmapBg() || playableBeatmap.getEvents().backgroundFilename == null
                ? ResourceManager.getInstance().getTexture("menu-background")
                : ResourceManager.getInstance().getTextureIfLoaded("::background");

        if (textureRegion == null) {
            Rectangle rectangle = new Rectangle(0f, 0f, Config.getRES_WIDTH(), Config.getRES_HEIGHT());

            RGBColor backgroundColor = playableBeatmap.getEvents().backgroundColor;
            if (backgroundColor == null) {
                backgroundColor = new RGBColor(0, 0, 0);
            }
            backgroundColor.apply(rectangle);

            applyBackground(rectangle, brightness);
        } else {
            applyBackground(new Sprite(0, 0, textureRegion.getWidth(), textureRegion.getHeight(), textureRegion), brightness);
        }

        // Storyboard and video are loaded asynchronously.
        boolean isStoryboardEnabled = Config.getBoolean("enableStoryboard", false);
        boolean isVideoEnabled = Config.getBoolean("enableVideo", false) && playableBeatmap.getEvents().videoFilename != null;

        if (brightness > 0.02f && (isStoryboardEnabled || isVideoEnabled)) {

            if (backgroundLoadingJob != null) {
                backgroundLoadingJob.cancel(new CancellationException("Background loading job cancelled"));
                backgroundLoadingJob = null;
            }

            backgroundLoadingJob = Execution.async(scope -> {

                boolean videoLoaded = false;

                if (isVideoEnabled) {

                    if (video != null) {
                        video.release();
                        video = null;
                    }

                    try {
                        video = new VideoSprite(lastBeatmapInfo.getAbsoluteSetDirectory() + "/" + playableBeatmap.getEvents().videoFilename, engine);
                        video.setAlpha(0f);

                        ensureActive(scope.getCoroutineContext());
                        applyBackground(video, brightness);

                        videoLoaded = true;
                    } catch (Exception e) {
                        video = null;
                        Log.e("GameScene", "Error while loading video background.", e);
                    }
                }

                if (isStoryboardEnabled) {

                    StoryboardSprite storyboardSprite = this.storyboardSprite;
                    this.storyboardSprite = null;

                    if (storyboardSprite != null) {
                        storyboardSprite.detachSelf();
                    } else {
                        storyboardSprite = new StoryboardSprite(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                        ensureActive(scope.getCoroutineContext());
                    }
                    scene.attachChild(storyboardSprite, 0);

                    ProxySprite storyboardOverlayProxy = this.storyboardOverlayProxy;
                    this.storyboardOverlayProxy = null;

                    if (storyboardOverlayProxy != null) {
                        storyboardOverlayProxy.detachSelf();
                    } else {
                        storyboardSprite.setOverlayDrawProxy(storyboardOverlayProxy = new ProxySprite(Config.getRES_WIDTH(), Config.getRES_HEIGHT()));
                        ensureActive(scope.getCoroutineContext());
                    }

                    storyboardSprite.setTransparentBackground(videoLoaded);
                    storyboardSprite.setBrightness(brightness);
                    storyboardSprite.loadStoryboard(beatmapInfo.getPath());
                    ensureActive(scope.getCoroutineContext());

                    scene.attachChild(storyboardOverlayProxy, scene.getChildIndex(fgScene));

                    this.storyboardSprite = storyboardSprite;
                    this.storyboardOverlayProxy = storyboardOverlayProxy;
                }

                ensureActive(scope.getCoroutineContext());
                backgroundLoadingJob = null;
            });
        }
    }

    private void applyBackground(Shape background, float brigthness) {

        if (dimRectangle != null) {
            dimRectangle.detachSelf();
        } else {
            dimRectangle = new Rectangle(0f, 0f, 0f, 0f);
        }

        dimRectangle.setSize(background.getWidth(), background.getHeight());
        dimRectangle.setColor(0f, 0f, 0f, 1f - brigthness);
        background.attachChild(dimRectangle);

        var factor = Config.isKeepBackgroundAspectRatio()
                ? Config.getRES_HEIGHT() / background.getHeight()
                : Config.getRES_WIDTH() / background.getWidth();

        background.setScale(factor);
        background.setPosition((Config.getRES_WIDTH() - background.getWidth()) / 2f, (Config.getRES_HEIGHT() - background.getHeight()) / 2f);
        scene.setBackground(new EntityBackground(background));
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
            try (var parser = new BeatmapParser(beatmapInfo.getPath(), scope)) {
                if (parser.openFile()) {
                    parsedBeatmap = parser.parse(true, GameMode.Droid);
                } else {
                    Debug.e("startGame: cannot open file");
                    ToastLogger.showText(StringTable.format(com.osudroid.resources.R.string.message_error_open, beatmapInfo.getFilename()), true);
                    return false;
                }
            } catch (Exception e) {
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

        playableBeatmap = parsedBeatmap.createDroidPlayableBeatmap(mods.values());

        rateAdjustingMods.clear();

        for (var mod : mods.values()) {
            if (mod instanceof IModApplicableToTrackRate rateMod) {
                rateAdjustingMods.add(rateMod);
            }
        }

        // TODO skin manager
        BeatmapSkinManager.getInstance().loadBeatmapSkin(playableBeatmap.getBeatmapsetPath());

        breakPeriods = new LinkedList<>();
        for (var period : playableBeatmap.getEvents().breaks) {
            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }
            breakPeriods.add(new BreakPeriod(period.startTime / 1000f, period.endTime / 1000f));
        }

        totalOffset = Config.getOffset();
        var props = DatabaseManager.getBeatmapOptionsTable().getOptions(beatmapInfo.getSetDirectory());
        if (props != null) {
            totalOffset += props.getOffset();
        }
        totalOffset /= 1000;

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

        var firstObject = playableBeatmap.getHitObjects().objects.get(0);
        scale = firstObject.getScreenSpaceGameplayScale();

        GameHelper.setOverallDifficulty(playableBeatmap.getDifficulty().od);
        GameHelper.setHealthDrain(playableBeatmap.getDifficulty().hp);
        GameHelper.setSpeedMultiplier(playableBeatmap.speedMultiplier);

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        GlobalManager.getInstance().getSongService().preLoad(audioFilePath, GameHelper.getSpeedMultiplier(),
            GameHelper.getSpeedMultiplier() != 1f &&
                (Config.isShiftPitchInRateChange() || mods.contains(ModNightCore.class)));

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        totalLength = GlobalManager.getInstance().getSongService().getLength();
        objects = new LinkedList<>(playableBeatmap.getHitObjects().objects);
        activeObjects = new LinkedList<>();
        expiredObjects = new LinkedList<>();
        lastObjectId = -1;

        sliderBorderColor = BeatmapSkinManager.getInstance().getSliderColor();
        if (playableBeatmap.getColors().sliderBorderColor != null) {
            sliderBorderColor = playableBeatmap.getColors().sliderBorderColor;
        }

        if (OsuSkin.get().isForceOverrideSliderBorderColor()) {
            sliderBorderColor = OsuSkin.get().getSliderBorderColor();
        }

        comboColors = new ArrayList<>();
        for (RGBColor color : playableBeatmap.getColors().comboColors) {
            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }
            comboColors.add(new RGBColor(color.r() / 255, color.g() / 255, color.b() / 255));
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

        lastActiveObjectHitTime = 0;

        timingControlPoints = new LinkedList<>(playableBeatmap.getControlPoints().timing.controlPoints);
        effectControlPoints = new LinkedList<>(playableBeatmap.getControlPoints().effect.controlPoints);

        activeTimingPoint = timingControlPoints.poll();
        activeEffectPoint = effectControlPoints.poll();

        if (activeTimingPoint == null) {
            activeTimingPoint = playableBeatmap.getControlPoints().timing.defaultControlPoint;
        }

        if (activeEffectPoint == null) {
            activeEffectPoint = playableBeatmap.getControlPoints().effect.defaultControlPoint;
        }

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
        replay.setObjectCount(objects.size());
        replay.setBeatmap(beatmapInfo.getFullBeatmapsetName(), beatmapInfo.getFullBeatmapName(), parsedBeatmap.getMd5());

        if (replayFilePath != null) {
            // Replay decoding may be dependent on the used mods, so we must do this.
            var replayStat = new StatisticV2();
            replayStat.setMod(mods);
            replay.setStat(replayStat);

            replaying = replay.load(replayFilePath, true);
            if (!replaying) {
                ToastLogger.showText(com.osudroid.resources.R.string.replay_invalid, true);
                return false;
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
                    if (droidTimedDifficultyAttributes == null || mods != lastMods) {
                        droidTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateDroidTimedDifficulty(playableBeatmap, scope);
                    }
                }

                case standard -> {
                    if (standardTimedDifficultyAttributes == null || mods != lastMods) {
                        standardTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateStandardTimedDifficulty(
                            parsedBeatmap, mods.values(), scope
                        );
                    }
                }
            }
        }

        sliderIndex = 0;

        // Mod changes may require recalculating slider paths (i.e. Hard Rock)
        if (sliderPaths == null || sliderRenderPaths == null || (shouldParseBeatmap && mods != lastMods)) {
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

        scene = createMainScene();
        bgScene = new ExtendedScene();
        mgScene = new ExtendedScene();
        mgScene.setClipToBounds(true);
        fgScene = new ExtendedScene();
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

        ModHashMap modsToUse = mods != null ? mods.deepCopy() : lastMods;
        BeatmapInfo beatmapToUse = beatmapInfo != null ? beatmapInfo : lastBeatmapInfo;

        boolean isRestart = beatmapInfo == null && replayFile == null && mods == null;

        GameLoaderScene scene = new GameLoaderScene(this, beatmapToUse, modsToUse, isRestart);
        engine.getCamera().setHUD(null);
        engine.setScene(scene);

        cancelLoading();

        loadingJob = Execution.async((scope) -> {
            DifficultyCalculationManager.stopCalculation();

            boolean succeeded = false;

            try {
                succeeded = loadGame(beatmapToUse, rfile, modsToUse, scope);

                if (succeeded) {
                    prepareScene();
                }
            } finally {
                if (!succeeded) {
                    quit();
                }

                loadingJob = null;
            }
        });

        ResourceManager.getInstance().getSound("failsound").stop();
    }

    public void cancelLoading() {
        // Do not cancel loading in multiplayer.
        if (Multiplayer.isMultiplayer) {
            return;
        }

        if (loadingJob != null) {
            loadingJob.cancel(new CancellationException("Loading job cancelled"));
        }

        if (backgroundLoadingJob != null) {
            backgroundLoadingJob.cancel(new CancellationException("Background loading job cancelled"));
        }
    }

    private void prepareScene() {
        scene.setOnSceneTouchListener(this);
        if (GlobalManager.getInstance().getCamera() instanceof SmoothCamera) {
            SmoothCamera camera = (SmoothCamera) (GlobalManager.getInstance().getCamera());
            camera.setZoomFactorDirect(Config.getPlayfieldSize());
            if (Config.isShrinkPlayfieldDownwards()) {
                camera.setCenterDirect((float) Config.getRES_WIDTH() / 2, (float) Config.getRES_HEIGHT() / 2 * Config.getPlayfieldSize());
            }
        }

        stat = new StatisticV2();
        stat.setMod(lastMods);
        stat.migrateLegacyMods(parsedBeatmap.getDifficulty());
        stat.calculateModScoreMultiplier(parsedBeatmap.getDifficulty());
        stat.canFail = !stat.getMod().contains(ModNoFail.class)
                && !stat.getMod().contains(ModRelax.class)
                && !stat.getMod().contains(ModAutopilot.class)
                && !stat.getMod().contains(ModAutoplay.class);

        float difficultyScoreMultiplier = 1 + Math.min(parsedBeatmap.getDifficulty().od, 10) / 10f +
                Math.min(parsedBeatmap.getDifficulty().hp, 10) / 10f;

        // The maximum CS of osu!droid mapped to osu!standard is ~17.62.
        difficultyScoreMultiplier += (Math.min(parsedBeatmap.getDifficulty().gameplayCS, 17.62f) - 3) / 4f;

        stat.setDiffModifier(difficultyScoreMultiplier);
        stat.setBeatmapNoteCount(lastBeatmapInfo.getTotalHitObjectCount());
        stat.setBeatmapMaxCombo(lastBeatmapInfo.getMaxCombo());

        GameHelper.setHardRock(lastMods.ofType(ModHardRock.class));
        GameHelper.setDoubleTime(lastMods.ofType(ModDoubleTime.class));
        GameHelper.setNightCore(lastMods.ofType(ModNightCore.class));
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
        GameHelper.setGrow(lastMods.ofType(ModGrow.class));
        GameHelper.setDeflate(lastMods.ofType(ModDeflate.class));

        for (int i = 0; i < CursorCount; i++) {
            cursors[i] = new Cursor();
            cursors[i].mouseDown = false;
            cursors[i].mousePressed = false;
            cursors[i].mouseOldDown = false;
        }

        Arrays.fill(cursorIIsDown, false);

        comboWas100 = false;
        comboWasMissed = false;
        previousFrameTime = 0;

        hitWindow = playableBeatmap.getHitWindow();
        var firstObject = objects.peek();
        firstObjectStartTime = (float) firstObject.startTime / 1000;
        lastObjectEndTime = (float) objects.getLast().getEndTime() / 1000;

        float objectTimePreempt = (float) firstObject.timePreempt / 1000;
        float skipTargetTime = firstObjectStartTime - Math.max(2f, objectTimePreempt);

        elapsedTime = Math.min(0, skipTargetTime);
        skipTime = skipTargetTime - 1;

        // Some beatmaps specify a current lead-in time, which overrides the default lead-in time above.
        float leadIn = playableBeatmap.getGeneral().audioLeadIn / 1000f;
        if (leadIn > 0) {
            elapsedTime = Math.min(elapsedTime, firstObjectStartTime - leadIn);
        }

        // Ensure the video has time to start.
        if (video != null) {
            elapsedTime = Math.min(videoOffset, elapsedTime);
        }

        // Ensure user-defined offset has time to be applied.
        elapsedTime = Math.min(elapsedTime, firstObjectStartTime - objectTimePreempt - totalOffset);
        initialElapsedTime = elapsedTime;

        metronome = null;
        if ((Config.getMetronomeSwitch() == 1 && GameHelper.isNightCore())
                || (GameHelper.isMuted() && GameHelper.getMuted().isEnableMetronome())
                || Config.getMetronomeSwitch() == 2) {
            metronome = new Metronome();
        }

        distToNextObject = 0;

        // TODO passive objects
        if ((replaying || Config.isShowCursor()) && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
            cursorSprites = new CursorEntity[CursorCount];
            for (int i = 0; i < CursorCount; i++) {
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

        skipBtn = null;
        if (skipTime > 1) {
            skipBtn = new AnimatedSprite("play-skip", true, OsuSkin.get().getAnimationFramerate());
            skipBtn.setOrigin(Anchor.BottomRight);
            skipBtn.setPosition(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
            skipBtn.setAlpha(0.7f);
            fgScene.attachChild(skipBtn);
        }

        breakAnimator = new BreakAnimator(fgScene, stat, playableBeatmap.getGeneral().letterboxInBreaks, dimRectangle);

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
            unrankedSprite = new ExtendedSprite(ResourceManager.getInstance().getTexture("play-unranked"));
            unrankedSprite.setAnchor(Anchor.TopCenter);
            unrankedSprite.setOrigin(Anchor.Center);
            unrankedSprite.setPosition(0, 80);
            fgScene.attachChild(unrankedSprite);
        }

        if (GameHelper.isFlashlight()){
            var flashlight = lastMods.ofType(ModFlashlight.class);

            flashlightSprite = new FlashLightEntity(Objects.requireNonNull(flashlight).getFollowDelay());
            fgScene.attachChild(flashlightSprite, 0);
        }

        // HUD should be to the last so we ensure everything is initialized and ready to be used by
        // the HUD elements in their constructors.
        hud = new GameplayHUD();
        hud.setEditMode(isHUDEditorMode);
        hud.setSkinData(OsuSkin.get().getHUDSkinData());

        String playname = Config.getOnlineUsername();
        ChangeableText replayText = null;

        if (!Config.isHideReplayMarquee()) {
            replayText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("font"), "", 1000);
            replayText.setPosition(0, 140);
            replayText.setAlpha(0.7f);
            hud.attachChild(replayText, 0);
        }

        if (lastMods.contains(ModAutoplay.class) || replaying) {
            var metadata = playableBeatmap.getMetadata();
            playname = replaying ? GlobalManager.getInstance().getScoring().getReplayStat().getPlayerName() : "osu!";

            if (replayText != null) {
                replayText.setText("Watching " + playname + " play " + metadata.artist + " - " + metadata.title + " [" + metadata.version + "]");
                replayText.registerEntityModifier(new LoopEntityModifier(new MoveXModifier(40f, Config.getRES_WIDTH() + 5, -replayText.getWidth() - 5)));
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

        if (Multiplayer.isMultiplayer) {
            RoomAPI.INSTANCE.notifyBeatmapLoaded();
        } else {
            isReadyToStart = true;
        }
    }

    // This is used by the multiplayer system, is called once all players in the room completes beatmap loading.
    public void start() {

        if (skipTime <= 1)
            RoomScene.INSTANCE.getChat().dismiss();

        loadBackground(lastBeatmapInfo);

        leadOut = 0;
        musicStarted = false;

        // Handle input in its own thread
        var touchOptions = new TouchOptions();
        touchOptions.setRunOnUpdateThread(false);
        engine.getTouchController().applyTouchOptions(touchOptions);

        engine.setScene(scene);
        engine.getCamera().setHUD(hud.getParent());

        blockAreaFragment = new BlockAreaFragment();
        blockAreaFragment.show(false);

        if (isHUDEditorMode) {
            ToastLogger.showText("Press back to show HUD editor menu.", false);
        }
    }

    public RGBColor getComboColor(HitObject hitObject) {
        if (GameHelper.isSynesthesia()) {
            return ModSynesthesia.getColorFor(
                playableBeatmap.getControlPoints().getClosestBeatDivisor(hitObject.startTime)
            );
        }

        return comboColors.get(hitObject.getComboIndexWithOffsets() % comboColors.size());
    }

    private void update(final float dt) {
        elapsedTime += dt;
        previousFrameTime = SystemClock.uptimeMillis();

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
            float currentSpeedMultiplier = ModUtils.calculateRateWithTrackRateMods(rateAdjustingMods, mSecPassed);

            if (currentSpeedMultiplier != GameHelper.getSpeedMultiplier()) {
                GameHelper.setSpeedMultiplier(currentSpeedMultiplier);

                if (musicStarted) {
                    GlobalManager.getInstance().getSongService().setSpeed(currentSpeedMultiplier);
                }
            }
        }

        if (Config.isEnableStoryboard()) {
            if (storyboardSprite != null) {
                storyboardSprite.updateTime(mSecPassed);
            }
        }

        if (replaying) {
            int cIndex;
            for (int i = 0; i < replay.cursorIndex.length; i++) {
                if (replay.cursorMoves.size() <= i){
                    break;
                }

                cIndex = replay.cursorIndex[i];
                Replay.ReplayMovement movement = null;

                // Emulating moves
                while (
                        cIndex < replay.cursorMoves.get(i).size &&
                        (movement = replay.cursorMoves.get(i).movements[cIndex]).getTime() <= (elapsedTime + dt / 4) * 1000
                ) {
                    float mx = movement.getX();
                    float my = movement.getY();
                    if (movement.getTouchType() == TouchType.DOWN) {
                        cursors[i].mouseDown = true;
                        for (int j = 0; j < replay.cursorIndex.length; j++) {
                            cursors[j].mouseOldDown = false;
                        }
                        cursors[i].mousePos.x = mx;
                        cursors[i].mousePos.y = my;

                        replay.lastMoveIndex[i] = -1;
                        hud.onGameplayTouchDown(movement.getTime() / 1000f);
                    } else if (movement.getTouchType() == TouchType.MOVE) {
                        cursors[i].mousePos.x = mx;
                        cursors[i].mousePos.y = my;
                        replay.lastMoveIndex[i] = cIndex;
                    } else {
                        cursors[i].mouseDown = false;
                    }
                    replay.cursorIndex[i]++;
                    cIndex++;
                }
                // Interpolating cursor movements
                if (movement != null && movement.getTouchType() == TouchType.MOVE && replay.lastMoveIndex[i] >= 0) {
                    final int lIndex = replay.lastMoveIndex[i];
                    final Replay.ReplayMovement lastMovement = replay.cursorMoves.get(i).movements[lIndex];
                    float t = (elapsedTime * 1000 - movement.getTime()) / (lastMovement.getTime() - movement.getTime());
                    cursors[i].mousePos.x = lastMovement.getX() * t + movement.getX() * (1 - t);
                    cursors[i].mousePos.y = lastMovement.getY() * t + movement.getY() * (1 - t);
                }
            }
        }

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.update(dt);
        } else if (cursorSprites != null) {
            for (int i = 0; i < CursorCount; i++) {
                cursorSprites[i].update(dt);

                if (replaying) {
                    cursorSprites[i].setPosition(cursors[i].mousePos.x, cursors[i].mousePos.y);
                    cursorSprites[i].setShowing(cursors[i].mouseDown);
                }

                if (cursors[i].mouseDown && cursors[i].mousePressed) {
                    cursorSprites[i].click();
                }
            }
        }

        for (final Cursor c : cursors) {
            if (c.mouseDown && !c.mouseOldDown) {
                c.mousePressed = true;
                c.mouseOldDown = true;
            } else {
                c.mousePressed = false;
            }
        }
        if(GameHelper.isFlashlight()){
            if (!GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
                if (mainCursorId < 0){
                    int i = 0;
                    for (final Cursor c : cursors) {
                        if (c.mousePressed) {
                            mainCursorId = i;
                            flashlightSprite.onMouseMove(c.mousePos.x, c.mousePos.y);
                            break;
                        }
                        ++i;
                    }
                } else if (!cursors[mainCursorId].mouseDown) {
                    mainCursorId = -1;
                } else {
                    flashlightSprite.onMouseMove(
                            cursors[mainCursorId].mousePos.x, cursors[mainCursorId].mousePos.y
                    );
                }
            }
            flashlightSprite.onUpdate(stat.getCombo());
        }

        while (!timingControlPoints.isEmpty() && timingControlPoints.peek().time <= mSecPassed) {
            activeTimingPoint = timingControlPoints.poll();
        }

        while (!effectControlPoints.isEmpty() && effectControlPoints.peek().time <= mSecPassed) {
            activeEffectPoint = effectControlPoints.poll();
        }

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000);
        GameHelper.setKiai(activeEffectPoint.isKiai);
        GameHelper.setCurrentBeatTime(Math.max(0, elapsedTime - activeTimingPoint.time / 1000) % GameHelper.getBeatLength());

        if (!isGameOver) {

            if (!breakPeriods.isEmpty()) {
                if (!breakAnimator.isBreak() && breakPeriods.peek().getStart() <= elapsedTime) {
                    gameStarted = false;
                    breakAnimator.init(breakPeriods.peek().getLength());
                    if(GameHelper.isFlashlight()){
                        flashlightSprite.onBreak(true);
                    }

                    if (Multiplayer.isConnected())
                        RoomScene.INSTANCE.getChat().show();

                    hud.onBreakStateChange(true);
                    breakPeriods.poll();
                }
            }

            if (breakAnimator.isOver()) {

                // Ensure the chat is dismissed if it's still shown
                RoomScene.INSTANCE.getChat().dismiss();

                gameStarted = true;
                hud.onBreakStateChange(false);

                if(GameHelper.isFlashlight()){
                    flashlightSprite.onBreak(false);
                }
            }
        }

        if (objects.isEmpty() && activeObjects.isEmpty() && GameHelper.isFlashlight()) {
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
        while (!expiredObjects.isEmpty()) {
            var object = expiredObjects.poll();
            activeObjects.remove(object);
        }

        updatePassiveObjects(dt);
        if (Config.isRemoveSliderLock()) {
            updateLastActiveObjectHitTime();
        }
        updateActiveObjects(dt);

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.moveToObject(activeObjects.peek(), elapsedTime, this);
        }

        if (Config.isRemoveSliderLock()) {
            var downPressCursorCount = 0;

            for (int i = 0; i < CursorCount; i++) {
                if (cursorIIsDown[i])
                    downPressCursorCount++;
                cursorIIsDown[i] = false;
            }

            for (int i = 0; i < downPressCursorCount - 1; i++) {
                updateLastActiveObjectHitTime();
                tryHitActiveObjects(dt);
            }
        } else {
            tryHitActiveObjects(dt);
        }

        if (video != null && elapsedTime >= videoOffset)
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

        if (elapsedTime >= totalOffset && !musicStarted) {
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

        while (!objects.isEmpty()
                // This can be simplified, but it is necessary to prevent floating point errors (see how
                // GameplayHitCircle and GameplaySlider track their passed time, where startTime and timePreempt
                // are cast and converted to seconds individually).
                && elapsedTime >= (float) objects.peek().startTime / 1000 - (float) objects.peek().timePreempt / 1000) {
            gameStarted = true;
            final var obj = objects.poll();

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

            // Next object from the polled one, this returns null if the list is empty. That's why every
            // usage of this is done if condition 'objects.isEmpty()' is false. Ignore IDE warnings.
            final var nextObj = objects.peek();

            distToNextObject = nextObj != null ?
                Math.max(nextObj.startTime - obj.startTime, activeTimingPoint.msPerBeat / 2) / 1000 :
                0;

            hud.onHitObjectLifetimeStart(obj);

            final RGBColor comboColor = getComboColor(obj);

            if (obj instanceof HitCircle parsedCircle) {
                final var gameplayCircle = GameObjectPool.getInstance().getCircle();

                gameplayCircle.init(this, mgScene, parsedCircle, elapsedTime, comboColor);
                addObject(gameplayCircle);

                if (GameHelper.isAutoplay()) {
                    gameplayCircle.setAutoPlay();
                }

                gameplayCircle.setId(++lastObjectId);

                if (replaying) {
                    gameplayCircle.setReplayData(replay.objectData[gameplayCircle.getId()]);
                }

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

            } else if (obj instanceof Slider parsedSlider) {
                final var gameplaySlider = GameObjectPool.getInstance().getSlider();

                gameplaySlider.init(this, mgScene, stat, parsedSlider, playableBeatmap.getControlPoints(),
                        elapsedTime, comboColor, sliderBorderColor, getSliderPath(sliderIndex),
                        getSliderRenderPath(sliderIndex));

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

        if (shouldBePunished || (objects.isEmpty() && activeObjects.isEmpty() && leadOut > 2)) {

            // Reset the game to continue the HUD editor session.
            if (startedFromHUDEditor && isHUDEditorMode) {
                elapsedTime = initialElapsedTime;
                loadGame(lastBeatmapInfo, null, lastMods, null);
                stat.reset();
                skip(true);
                return;
            }

            scene = createMainScene();
            engine.getCamera().setHUD(null);
            BeatmapSkinManager.setSkinEnabled(false);
            GameObjectPool.getInstance().purge();
            timingControlPoints.clear();
            effectControlPoints.clear();
            objects.clear();
            activeObjects.clear();
            expiredObjects.clear();
            breakPeriods.clear();
            cursorSprites = null;
            playableBeatmap = null;
            lastMods = null;
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
            if (GlobalManager.getInstance().getCamera() instanceof SmoothCamera) {
                SmoothCamera camera = (SmoothCamera) (GlobalManager.getInstance().getCamera());
                camera.setZoomFactorDirect(1f);
                if (Config.isShrinkPlayfieldDownwards()) {
                    camera.setCenterDirect((float) Config.getRES_WIDTH() / 2, (float) Config.getRES_HEIGHT() / 2);
                }
            }

            if (blockAreaFragment != null) {
                blockAreaFragment.dismiss();
                blockAreaFragment = null;
            }

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
                        RoomScene.INSTANCE.getChat().show();

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

            // Handle input back in update thread
            var touchOptions = new TouchOptions();
            touchOptions.setRunOnUpdateThread(true);
            engine.getTouchController().applyTouchOptions(touchOptions);

            if (video != null) {
                video.release();
                video = null;
                videoStarted = false;
            }

        } else if (objects.isEmpty() && activeObjects.isEmpty()) {
            gameStarted = false;
            leadOut += dt;
        }

        if (elapsedTime > skipTime - 1f && skipBtn != null) {
            RoomScene.INSTANCE.getChat().dismiss();
            skipBtn.detachSelf();
            skipBtn = null;
        } else if (skipBtn != null) {

            var maxPos = new PointF(Config.getRES_WIDTH(), Config.getRES_HEIGHT());

            for (final Cursor c : cursors) {
                if (c.mouseDown && Utils.distance(c.mousePos, maxPos) < 250) {

                    if (Multiplayer.isConnected())
                    {
                        if (!isSkipRequested)
                        {
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

    private void updateLastActiveObjectHitTime() {
        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            var obj = activeObjects.get(i);
            if (!obj.isStartHit()) {
                lastActiveObjectHitTime = obj.getHitTime();
                break;
            }
        }
    }

    private void tryHitActiveObjects(float deltaTime) {
        // When replaying, judgements are processed when updating the objects' state.
        if (replaying) {
            return;
        }

        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            activeObjects.get(i).tryHit(deltaTime);
        }
    }

    private void updateActiveObjects(float deltaTime) {
        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            activeObjects.get(i).update(deltaTime);
        }
    }

    private void updatePassiveObjects(float deltaTime) {

        hud.onGameplayUpdate(this, deltaTime);

        breakAnimator.update(deltaTime);

        if (countdownAnimator != null) {
            countdownAnimator.update(deltaTime);
        }
    }

    public void skip() {
        skip(false);
    }

    public void skip(boolean force)
    {
        RoomScene.INSTANCE.getChat().dismiss();

        if (elapsedTime > skipTime - 1f && !force) {
            return;
        }

        SongService songService = GlobalManager.getInstance().getSongService();
        if (songService.getStatus() != Status.PLAYING) {
            songService.play();
            songService.setVolume(Config.getBgmVolume());
            totalLength = songService.getLength();
            musicStarted = true;
        }

        ResourceManager.getInstance().getSound("menuhit").play();

        float difference = Math.max(0, skipTime - elapsedTime);

        // Skip time may be negative in forced skips, which will cause desynchronization between game time and
        // audio time, so we cap it at 0.
        elapsedTime = Math.max(0, skipTime);
        int seekTime = (int) Math.ceil(elapsedTime * 1000);
        int videoSeekTime = seekTime - (int) (videoOffset * 1000);

        Execution.updateThread(() -> {

            updatePassiveObjects(difference);

            songService.seekTo(seekTime);
            if (songService.getStatus() != Status.PLAYING) {
                songService.play();
            }

            if (video != null) {
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
            if (objects != null) {
                objects.clear();
            }
            if (timingControlPoints != null) {
                timingControlPoints.clear();
            }
            if (effectControlPoints != null) {
                effectControlPoints.clear();
            }
            breakPeriods.clear();
            playableBeatmap = null;
            cursorSprites = null;
            lastMods = null;
            droidTimedDifficultyAttributes = null;
            standardTimedDifficultyAttributes = null;
            sliderPaths = null;
            sliderRenderPaths = null;
        });

        // osu!stable restarts the song back to preview time when the player is in the last 10 seconds *or* 2% of the beatmap.
        float mSecPassed = elapsedTime * 1000;
        var songMenu = GlobalManager.getInstance().getSongMenu();
        if (totalLength - mSecPassed > 10000 && mSecPassed / totalLength < 0.98f) {
            var songService = GlobalManager.getInstance().getSongService();

            if (songService != null) {
                songService.play();
                songMenu.startMusicVolumeAnimation(0.3f);
            }
        } else {
            songMenu.playMusic(lastBeatmapInfo.getAudioPath(), lastBeatmapInfo.getPreviewTime());
        }

        if (replaying) {
            replayFilePath = null;
        }
    }

    public void quit() {

        if (blockAreaFragment != null) {
            blockAreaFragment.dismiss();
            blockAreaFragment = null;
        }

        // Handle input back in update thread
        var touchOptions = new TouchOptions();
        touchOptions.setRunOnUpdateThread(true);
        engine.getTouchController().applyTouchOptions(touchOptions);

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

        onExit();
        if (GlobalManager.getInstance().getCamera() instanceof SmoothCamera) {
            SmoothCamera camera = (SmoothCamera) (GlobalManager.getInstance().getCamera());
            camera.setZoomFactorDirect(1f);
            if (Config.isShrinkPlayfieldDownwards()) {
                camera.setCenterDirect((float) Config.getRES_WIDTH() / 2, (float) Config.getRES_HEIGHT() / 2);
            }
        }
        scene = createMainScene();
        engine.getCamera().setHUD(null);

        if (Multiplayer.isMultiplayer)
        {
            RoomScene.INSTANCE.show();
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
                ResourceManager.getInstance().getCustomSound("combobreak", 1)
                        .play();
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
                            final boolean endCombo, byte forcedScore, RGBColor color) {
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
               flashlightSprite.onMouseMove(
                    cursors[mainCursorId].mousePos.x,
                    cursors[mainCursorId].mousePos.y
               );
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

    public void onSliderReverse(PointF pos, float ang, RGBColor color) {
        createBurstEffectSliderReverse(pos, ang, color);
    }

    public void onSliderHit(int id, final int score, final PointF judgementPos, final boolean endCombo,
                            RGBColor color, int type, boolean incrementCombo) {
        if (GameHelper.isFlashlight() && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
            int nearestCursorId = getNearestCursorId(judgementPos.x, judgementPos.y);
            if (nearestCursorId >= 0) {
                mainCursorId = nearestCursorId;
                flashlightSprite.onMouseMove(
                        cursors[mainCursorId].mousePos.x,
                        cursors[mainCursorId].mousePos.y
                );
            }
        }

        VibratorManager.INSTANCE.sliderVibration();

        if (score == 0) {
            createHitEffect(judgementPos, "hit0", color);
            registerHit(id, 0, endCombo);
            return;
        }

        if (score == -1) {
            if (stat.getCombo() > 30) {
                ResourceManager.getInstance().getCustomSound("combobreak", 1)
                        .play();
            }
            if (GameHelper.isSuddenDeath()) {
                stat.changeHp(-1.0f);
                gameover();
            }
            stat.registerHit(0, true, false);
            return;
        }

        String scoreName = "hit0";
        switch (score) {
            case 300:
                scoreName = registerHit(id, 300, endCombo, incrementCombo);
                break;
            case 100:
                scoreName = registerHit(id, 100, endCombo, incrementCombo);
                break;
            case 50:
                scoreName = registerHit(id, 50, endCombo, incrementCombo);
                break;
            case 30:
                scoreName = "sliderpoint30";
                stat.registerHit(30, false, false);
                break;
            case 10:
                scoreName = "sliderpoint10";
                stat.registerHit(10, false, false);
                break;
        }

        if (score > 10) {
            switch (type) {
                case GameObjectListener.SLIDER_START:
                    createBurstEffectSliderStart(judgementPos, color);
                    if (GameHelper.isAutoplay()) {
                        hud.onGameplayTouchDown((float) parsedBeatmap.getHitObjects().objects.get(id).startTime / 1000);
                    }
                    break;
                case GameObjectListener.SLIDER_END:
                    createBurstEffectSliderEnd(judgementPos, color);
                    break;
                case GameObjectListener.SLIDER_REPEAT:
                    break;
                default:
                    createBurstEffect(judgementPos, color);
            }
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

        String scoreName = switch (score) {
            case 300 -> registerHit(id, 300, endCombo);
            case 100 -> registerHit(id, 100, endCombo);
            case 50 -> registerHit(id, 50, endCombo);
            default -> "hit0";
        };

        createHitEffect(pos, scoreName, null);

        hud.onNoteHit(stat);
    }

    @Override
    public void playHitSamples(GameplayHitSampleInfo[] samples) {
        float volume = 1;
        var muted = GameHelper.getMuted();

        if (muted != null && muted.affectsHitSounds()) {
            volume = muted.volumeAt(stat.getCombo());
        }

        for (int i = 0; i < samples.length; ++i) {
            var sample = samples[i];
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


    public PointF getMousePos(final int index) {
        return cursors[index].mousePos;
    }


    public boolean isMouseDown(final int index) {
        return cursors[index].mouseDown;
    }


    public boolean isMousePressed(final GameObject object, final int index) {
        // EnumSet.contains() internally uses an iterator, and it can be expensive to use everytime we want to use this method.
        if (GameHelper.isAutoplay()) {
            return false;
        }
        if (Config.isRemoveSliderLock()){
            if(activeObjects.isEmpty()
                || Math.abs(object.getHitTime() - lastActiveObjectHitTime) > 0.001f) {
                return false;
            }
        }
        else if (activeObjects.isEmpty()
            || Math.abs(object.getHitTime()
            - activeObjects.peek().getHitTime()) > 0.001f) {
            return false;
        }
        return cursors[index].mousePressed;
    }

    @Override
    public double downFrameOffset(int index) {
        return cursors[index].mouseDownOffsetMS;
    }

    public void removeObject(final GameObject object) {
        expiredObjects.add(object);
    }


    private PointF applyCursorTrackCoordinates(Cursor cursor) {

        var rawX = cursor.mousePos.x;
        var rawY = cursor.mousePos.y;

        var width = Config.getRES_WIDTH();
        var height = Config.getRES_HEIGHT();

        if (GameHelper.isHardRock()) {
            rawY -= height / 2f;
            rawY *= -1;
            rawY += height / 2f;
        }
        rawY -= (height - Constants.MAP_ACTUAL_HEIGHT) / 2f;
        rawX -= (width - Constants.MAP_ACTUAL_WIDTH) / 2f;

        rawX *= Constants.MAP_WIDTH / (float) Constants.MAP_ACTUAL_WIDTH;
        rawY *= Constants.MAP_HEIGHT / (float) Constants.MAP_ACTUAL_HEIGHT;

        cursor.trackPos.x = rawX;
        cursor.trackPos.y = rawY;
        return cursor.trackPos;
    }


    public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent event) {
        if (replaying || isGameOver) {
            return false;
        }

        var id = event.getPointerID();
        if (id < 0 || id >= CursorCount) {
            return false;
        }

        var cursor = cursors[id];
        var sprite = !GameHelper.isAutoplay() && !GameHelper.isAutopilot() && cursorSprites != null
                ? cursorSprites[id]
                : null;

        cursor.mousePos.x = FMath.clamp(event.getX(), 0, Config.getRES_WIDTH());
        cursor.mousePos.y = FMath.clamp(event.getY(), 0, Config.getRES_HEIGHT());

        if (sprite != null) {
            sprite.setPosition(cursor.mousePos.x, cursor.mousePos.y);
        }

        float offset = previousFrameTime > 0
                ? (event.getMotionEvent().getEventTime() - previousFrameTime) * GameHelper.getSpeedMultiplier()
                : 0;
        int eventTime = (int) (elapsedTime * 1000 + offset);

        if (event.isActionDown()) {

            if (sprite != null) {
                sprite.setShowing(true);
            }

            if (!GameHelper.isAutoplay()) {
                hud.onGameplayTouchDown(eventTime / 1000f);
            }

            cursor.mouseDown = true;
            cursor.mouseDownOffsetMS = offset;

            for (var value : cursors)
                value.mouseOldDown = false;

            PointF gamePoint = applyCursorTrackCoordinates(cursor);
            if (replay != null) {
                replay.addPress(eventTime, gamePoint, id);
            }

        } else if (event.isActionMove()) {

            if (sprite != null) {
                sprite.setShowing(true);
            }

            PointF gamePoint = applyCursorTrackCoordinates(cursor);
            if (replay != null) {
                replay.addMove(eventTime, gamePoint, id);
            }

        } else if (event.isActionUp()) {

            if (sprite != null) {
                sprite.setShowing(false);
            }
            cursor.mouseDown = false;
            cursorIIsDown[id] = false;

            if (replay != null) {
                replay.addUp(eventTime, id);
            }

        } else {
            return false;
        }
        return true;
    }

    private void removeAllCursors() {
        int time = GlobalManager.getInstance().getSongService().getPosition();

        for (int i = 0; i < CursorCount; ++i) {
            var cursor = cursors[i];

            if (cursor.mouseDown) {
                cursor.mouseDown = false;

                if (replay != null) {
                    replay.addUp(time, i);
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
            // Setting a delay of 300ms for the player to tap back button again.
            if (lastBackPressTime > 0 && realTimeElapsed - lastBackPressTime > 300)
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

        if (blockAreaFragment != null) {
            blockAreaFragment.dismiss();
        }

        if (GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
            GlobalManager.getInstance().getSongService().pause();
        }
        paused = true;
        scene.setIgnoreUpdate(true);

        final PauseMenu menu = new PauseMenu(engine, this, false);
        hud.getParent().setChildScene(menu.getScene(), false, true, true);
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

        if (blockAreaFragment != null) {
            blockAreaFragment.dismiss();
            blockAreaFragment = null;
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
        ExtendedScene scene = this.scene;
        ExtendedScene mgScene = this.mgScene;
        ExtendedScene bgScene = this.bgScene;

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

                    if (video != null) {
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
                    if (video != null) {
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
                    hud.getParent().setChildScene(menu.getScene(), false, true, true);
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

        if (blockAreaFragment == null) {
            blockAreaFragment = new BlockAreaFragment();
        }
        blockAreaFragment.show();

        scene.setIgnoreUpdate(false);
        hud.getParent().getChildScene().back();
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

    private void createHitEffect(final PointF pos, final String name, RGBColor color) {

        var effect = GameObjectPool.getInstance().getEffect(name);
        var isAnimated = effect.hit instanceof AnimatedSprite animatedHit && animatedHit.getFrames().length > 1;

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

    private void createBurstEffect(final PointF pos, final RGBColor color) {
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

    private void createBurstEffectSliderStart(final PointF pos, final RGBColor color) {
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

    private void createBurstEffectSliderEnd(final PointF pos, final RGBColor color) {
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

    private void createBurstEffectSliderReverse(final PointF pos, float ang, final RGBColor color) {
        if (!Config.isBurstEffects() ||
                (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
                GameHelper.isTraceable())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("reversearrow");
        burst1.hit.setRotation(ang);
        applyBurstEffect(burst1, pos);
    }

    public int getCursorsCount() {
        return CursorCount;
    }


    public void registerAccuracy(final double acc) {
        offsetSum += (float) acc;
        offsetRegs++;

        stat.addHitOffset(acc);

        if (replaying) {
            scoringScene.getReplayStat().addHitOffset(acc);
        }

        hud.onAccuracyRegister((float) acc);
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

    private int getNearestCursorId(float pX, float pY){
        float distance = Float.POSITIVE_INFINITY, cursorDistance, dx, dy;
        int id = -1, i = 0;
        for (Cursor c : cursors) {
            if(c.mouseDown || c.mousePressed || c.mouseOldDown){
                dx = c.mousePos.x - pX;
                dy = c.mousePos.y - pY;
                cursorDistance = dx * dx + dy * dy;
                if(cursorDistance < distance){
                    id = i;
                    distance = cursorDistance;
                }
            }
            ++i;
        }
        return id;
    }

    private void calculateAllSliderPaths(final CoroutineScope scope) {
        if (playableBeatmap.getHitObjects().getSliderCount() == 0) {
            return;
        }

        sliderPaths = new SliderPath[playableBeatmap.getHitObjects().getSliderCount()];
        sliderRenderPaths = new LinePath[playableBeatmap.getHitObjects().getSliderCount()];
        sliderIndex = 0;

        for (var obj : playableBeatmap.getHitObjects().objects) {
            ensureActive(scope.getCoroutineContext());

            if (!(obj instanceof Slider slider)) {
                continue;
            }

            sliderPaths[sliderIndex] = GameHelper.convertSliderPath(slider);
            sliderRenderPaths[sliderIndex] = GameHelper.convertSliderPath(sliderPaths[sliderIndex]);
            ++sliderIndex;
        }

        sliderIndex = 0;
    }

    private SliderPath getSliderPath(int index) {
        if (sliderPaths != null && index < sliderPaths.length && index >= 0){
            return sliderPaths[index];
        }
        else {
            return null;
        }
    }

    private LinePath getSliderRenderPath(int index) {
        if (sliderRenderPaths != null && index < sliderRenderPaths.length && index >= 0) {
            return sliderRenderPaths[index];
        } else {
            return null;
        }
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
            while (!objects.isEmpty()){
                objects.poll();
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
        if (droidTimedDifficultyAttributes == null || objectId < 0 || objectId >= droidTimedDifficultyAttributes.length) {
            return 0;
        }

        var timedAttributes = droidTimedDifficultyAttributes[objectId];

        return BeatmapDifficultyCalculator.calculateDroidPerformance(timedAttributes.attributes, stat).total;
    }

    private double getStandardPPAt(int objectId) {
        if (standardTimedDifficultyAttributes == null || objectId < 0 || objectId >= standardTimedDifficultyAttributes.length) {
            return 0;
        }

        var timedAttributes = standardTimedDifficultyAttributes[objectId];

        return BeatmapDifficultyCalculator.calculateStandardPerformance(timedAttributes.attributes, stat).total;
    }

    private ExtendedScene createMainScene() {
        return new ExtendedScene() {
            @Override
            protected void onManagedUpdate(float secElapsed) {
                float maxDt = secElapsed * GameHelper.getSpeedMultiplier();
                float dt = maxDt;
                var songService = GlobalManager.getInstance().getSongService();

                if (songService.getStatus() == Status.PLAYING) {
                    // BASS may report the wrong position. When that happens, `dt` will
                    // be negative. In that case, we should ignore the update.
                    // See https://github.com/ppy/osu/issues/26879 for more information.
                    dt = songService.getPosition() / 1000f - (elapsedTime - totalOffset);
                } else if (!musicStarted) {
                    // Cap elapsed time at the music start time to prevent objects from progressing too far.
                    dt = Math.min(elapsedTime + dt, totalOffset) - elapsedTime;
                }

                // BASS may report the wrong position. When that happens, `dt` will either be negative or more than the
                // actual progressed time. To prevent such situation from happening, we keep `dt` between 0 and the
                // actual progressed time.
                // See https://github.com/ppy/osu/issues/26879 for more information.
                dt = FMath.clamp(dt, 0, maxDt);

                update(dt);
                super.onManagedUpdate(dt);
            }
        };
    }
}
