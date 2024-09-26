package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import kotlin.random.Random;
import ru.nsu.ccfit.zuev.osu.SecurityUtils;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.support.ProxySprite;
import com.edlplan.framework.support.osb.StoryboardSprite;
import com.edlplan.framework.utils.functionality.SmartIterator;
import com.reco1l.ibancho.RoomAPI;
import com.reco1l.osu.DifficultyCalculationManager;
import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.Execution;
import com.reco1l.osu.data.DatabaseManager;
import com.reco1l.osu.graphics.AnimatedSprite;
import com.reco1l.osu.graphics.BlankTextureRegion;
import com.reco1l.osu.graphics.ExtendedSprite;
import com.reco1l.osu.graphics.Modifiers;
import com.reco1l.osu.graphics.Origin;
import com.reco1l.osu.graphics.VideoSprite;
import com.reco1l.osu.graphics.ExtendedScene;
import com.reco1l.osu.playfield.FollowPointConnection;
import com.reco1l.osu.playfield.ScoreText;
import com.reco1l.osu.playfield.SliderTickSprite;
import com.reco1l.osu.ui.BlockAreaFragment;
import com.reco1l.osu.ui.entity.GameplayLeaderboard;
import com.reco1l.osu.multiplayer.Multiplayer;
import com.reco1l.osu.multiplayer.RoomScene;

import com.rian.osu.GameMode;
import com.rian.osu.beatmap.Beatmap;
import com.rian.osu.beatmap.constants.BeatmapCountdown;
import com.rian.osu.beatmap.hitobject.HitCircle;
import com.rian.osu.beatmap.hitobject.HitObject;
import com.rian.osu.beatmap.hitobject.HitSampleInfo;
import com.rian.osu.beatmap.hitobject.Spinner;
import com.rian.osu.beatmap.parser.BeatmapParser;
import com.rian.osu.beatmap.timings.EffectControlPoint;
import com.rian.osu.beatmap.timings.TimingControlPoint;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import com.rian.osu.difficulty.attributes.DifficultyAttributes;
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes;
import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes;
import com.rian.osu.difficulty.attributes.TimedDifficultyAttributes;
import com.rian.osu.difficulty.calculator.DifficultyCalculationParameters;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.TouchOptions;
import org.anddev.andengine.entity.modifier.LoopEntityModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.util.FPSCounter;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.modifier.ease.EaseQuadIn;
import org.anddev.andengine.util.modifier.ease.EaseQuadOut;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.effect.Metronome;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm;
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
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.menu.PauseMenu;
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem;
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osu.scoring.TouchType;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.SkinManager;

import static com.rian.osu.utils.ModConverter.convertLegacyMods;

public class GameScene implements IUpdateHandler, GameObjectListener,
        IOnSceneTouchListener {
    public static final int CursorCount = 10;
    private final Engine engine;
    private final Cursor[] cursors = new Cursor[CursorCount];
    private final boolean[] cursorIIsDown = new boolean[CursorCount];
    private final StringBuilder strBuilder = new StringBuilder();
    public String audioFilePath = null;
    private ExtendedScene scene;
    private Scene bgScene, mgScene, fgScene;
    private Scene oldScene;
    private Beatmap parsedBeatmap;
    private Beatmap playableBeatmap;
    private BeatmapInfo lastBeatmapInfo;
    private ScoringScene scoringScene;
    private TimingControlPoint activeTimingPoint;
    private EffectControlPoint activeEffectPoint;
    private int lastObjectId = -1;
    private float secPassed = 0;
    private float leadOut = 0;
    private LinkedList<HitObject> objects;
    private ArrayList<RGBColor> comboColors;
    private boolean comboWasMissed = false;
    private boolean comboWas100 = false;
    private LinkedList<GameObject> activeObjects;
    private LinkedList<GameObject> passiveObjects;
    private LinkedList<GameObject> expiredObjects;
    private ScoreText comboText, accuracyText, scoreText;  //显示的文字  连击数  ACC  分数
    private Queue<BreakPeriod> breakPeriods = new LinkedList<>();
    private BreakAnimator breakAnimator;
    private ScoreBar scorebar;
    public GameplayLeaderboard scoreBoard;
    private HitErrorMeter hitErrorMeter;
    private Metronome metronome;
    private float scale;
    private float objectTimePreempt;
    private float difficultyStatisticsScoreMultiplier;
    public StatisticV2 stat;
    private boolean gameStarted;
    private float totalOffset;
    //private IMusicPlayer music = null;
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
    private float offsetSum;
    private int offsetRegs;
    private Rectangle dimRectangle = null;
    private ComboBurst comboBurst;
    private int failcount = 0;
    private RGBColor sliderBorderColor;
    private float lastActiveObjectHitTime = 0;
    private SliderPath[] sliderPaths = null;
    private int sliderIndex = 0;

    private StoryboardSprite storyboardSprite;

    private ProxySprite storyboardOverlayProxy;

    private DifficultyHelper difficultyHelper = DifficultyHelper.StdDifficulty;

    private List<TimedDifficultyAttributes<DroidDifficultyAttributes>> droidTimedDifficultyAttributes;
    private List<TimedDifficultyAttributes<StandardDifficultyAttributes>> standardTimedDifficultyAttributes;

    private final List<ChangeableText> counterTexts = new ArrayList<>(5);
    private ChangeableText fpsText;
    private ChangeableText avgOffsetText;
    private ChangeableText urText;
    private ChangeableText ppText;
    private ChangeableText memText;

    /**
     * The time at which the last frame was rendered with respect to {@link SystemClock#uptimeMillis()}.
     * <br>
     * If 0, a frame has not been rendered yet.
     */
    private long previousFrameTime;



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
        scene = new ExtendedScene();
        bgScene = new Scene();
        fgScene = new Scene();
        mgScene = new Scene();
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

    private void setBackground() {
        dimRectangle = null;

        if (video != null) {
            video.release();
            video = null;
        }

        Sprite bgSprite = null;

        if (Config.isVideoEnabled() && playableBeatmap.events.videoFilename != null
                // Unfortunately MediaPlayer API doesn't allow to change playback speed on APIs < 23, so in that case
                // the video will not be shown.
                && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || GameHelper.getSpeedMultiplier() == 1.0f)) {
            try {
                videoStarted = false;
                videoOffset = playableBeatmap.events.videoStartTime / 1000f;

                video = new VideoSprite(lastBeatmapInfo.getSetDirectory() + "/" + playableBeatmap.events.videoFilename, engine);
                video.setAlpha(0f);

                bgSprite = video;

                if (storyboardSprite != null) {
                    storyboardSprite.setTransparentBackground(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                video = null;
            }
        }

        // storyboard sprite will draw background and dimRectangle if needed, so skip here
        if (!Config.isEnableStoryboard() || storyboardSprite == null || !storyboardSprite.isStoryboardAvailable()) {
            if (bgSprite == null) {

                var tex = Config.isSafeBeatmapBg() || playableBeatmap.events.backgroundFilename == null
                        ? ResourceManager.getInstance().getTexture("menu-background")
                        : ResourceManager.getInstance().getTextureIfLoaded("::background");

                if (tex != null)
                    bgSprite = new Sprite(0, 0, tex);
            }

            if (bgSprite == null) {
                bgSprite = new Sprite(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT(), new BlankTextureRegion());

                if (playableBeatmap.events.backgroundColor != null)
                    playableBeatmap.events.backgroundColor.apply(bgSprite);
                else
                    bgSprite.setColor(0f, 0f, 0f);
            }


            dimRectangle = new Rectangle(0f, 0f, bgSprite.getWidth(), bgSprite.getHeight());
            dimRectangle.setColor(0f, 0f, 0f, 1.0f - Config.getBackgroundBrightness());
            bgSprite.attachChild(dimRectangle);
        } else {
            storyboardSprite.setBrightness(Config.getBackgroundBrightness());
        }

        if (bgSprite != null) {
            var factor = Config.isKeepBackgroundAspectRatio() ?
                    Config.getRES_HEIGHT() / bgSprite.getHeight()
                    :
                    Config.getRES_WIDTH() / bgSprite.getWidth();

            bgSprite.setScale(factor);
            bgSprite.setPosition((Config.getRES_WIDTH() - bgSprite.getWidth()) / 2f, (Config.getRES_HEIGHT() - bgSprite.getHeight()) / 2f);
            scene.setBackground(new SpriteBackground(bgSprite));
        }
    }

    private boolean loadGame(final BeatmapInfo beatmapInfo, final String rFile) {
        if (!SecurityUtils.verifyFileIntegrity(GlobalManager.getInstance().getMainActivity())) {
            ToastLogger.showTextId(R.string.file_integrity_tampered, true);
            return false;
        }

        if (rFile != null && rFile.startsWith("https://")) {
            this.replayFilePath = Config.getCachePath() + "/" +
                    MD5Calculator.getStringMD5(rFile) + ".odr";
            Debug.i("ReplayFile = " + replayFilePath);
            if (!OnlineFileOperator.downloadFile(rFile, this.replayFilePath)) {
                ToastLogger.showTextId(R.string.replay_cantdownload, true);
                return false;
            }
        } else
            this.replayFilePath = rFile;

        if (parsedBeatmap == null || !parsedBeatmap.md5.equals(beatmapInfo.getMD5())) {
            try (var parser = new BeatmapParser(beatmapInfo.getPath())) {
                if (parser.openFile()) {
                    parsedBeatmap = parser.parse(true, GameMode.Droid);
                } else {
                    Debug.e("startGame: cannot open file");
                    ToastLogger.showText(StringTable.format(R.string.message_error_open, beatmapInfo.getFilename()), true);
                    return false;
                }
            }
        }

        if (parsedBeatmap == null) {
            return false;
        }

        if (!parsedBeatmap.md5.equals(beatmapInfo.getMD5())) {
            ToastLogger.showTextId(R.string.file_integrity_tampered, true);
            return false;
        }

        if (parsedBeatmap.hitObjects.objects.isEmpty()) {
            ToastLogger.showText("Empty Beatmap", true);
            return false;
        }

        var modMenu = ModMenu.getInstance();
        var convertedMods = convertLegacyMods(
            modMenu.getMod(),
            modMenu.isCustomCS() ? modMenu.getCustomCS() : null,
            modMenu.isCustomAR() ? modMenu.getCustomAR() : null,
            modMenu.isCustomOD() ? modMenu.getCustomOD() : null,
            modMenu.isCustomHP() ? modMenu.getCustomHP() : null
        );

        playableBeatmap = parsedBeatmap.createPlayableBeatmap(GameMode.Droid, convertedMods, modMenu.getChangeSpeed());

        // TODO skin manager
        SkinManager.getInstance().loadBeatmapSkin(playableBeatmap.getBeatmapsetPath());

        breakPeriods = new LinkedList<>();
        for (var period : playableBeatmap.events.breaks) {
            breakPeriods.add(new BreakPeriod(period.startTime / 1000f, period.endTime / 1000f));
        }

        totalOffset = Config.getOffset();
        var props = DatabaseManager.getBeatmapOptionsTable().getOptions(beatmapInfo.getSetDirectory());
        if (props != null) {
            totalOffset += props.getOffset();
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

        scale = playableBeatmap.hitObjects.objects.get(0).getGameplayScale();
        objectTimePreempt = (float) GameHelper.ar2ms(playableBeatmap.difficulty.getAr()) / 1000f;

        difficultyStatisticsScoreMultiplier = 1 +
            Math.min(parsedBeatmap.difficulty.od, 10) / 10f + Math.min(parsedBeatmap.difficulty.hp, 10) / 10f;

        // The maximum CS of osu!droid mapped to osu!standard is ~17.62.
        difficultyStatisticsScoreMultiplier += (Math.min(parsedBeatmap.difficulty.gameplayCS, 17.62f) - 3) / 4f;

        GameHelper.setOverallDifficulty(playableBeatmap.difficulty.od);
        GameHelper.setHealthDrain(playableBeatmap.difficulty.hp);
        GameHelper.setObjectTimePreempt(objectTimePreempt);
        GameHelper.setSpeedMultiplier(modMenu.getSpeed());
        scene.setTimeMultiplier(GameHelper.getSpeedMultiplier());

        GlobalManager.getInstance().getSongService().preLoad(audioFilePath, GameHelper.getSpeedMultiplier(),
            GameHelper.getSpeedMultiplier() != 1f &&
                (modMenu.isEnableNCWhenSpeedChange() || modMenu.getMod().contains(GameMod.MOD_NIGHTCORE)));

        objects = new LinkedList<>(playableBeatmap.hitObjects.objects);
        activeObjects = new LinkedList<>();
        passiveObjects = new LinkedList<>();
        expiredObjects = new LinkedList<>();
        lastObjectId = -1;

        sliderBorderColor = SkinManager.getInstance().getSliderColor();
        if (playableBeatmap.colors.sliderBorderColor != null) {
            sliderBorderColor = playableBeatmap.colors.sliderBorderColor;
        }

        if (OsuSkin.get().isForceOverrideSliderBorderColor()) {
            sliderBorderColor = OsuSkin.get().getSliderBorderColor();
        }

        comboColors = new ArrayList<>();
        for (RGBColor color : playableBeatmap.colors.comboColors) {
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

        lastActiveObjectHitTime = 0;

        activeTimingPoint = playableBeatmap.controlPoints.timing.controlPointAt(Double.NEGATIVE_INFINITY);
        activeEffectPoint = playableBeatmap.controlPoints.effect.controlPointAt(Double.NEGATIVE_INFINITY);

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000);
        GameHelper.setKiai(activeEffectPoint.isKiai);
        GameHelper.setCurrentBeatTime(0);

        GameObjectPool.getInstance().purge();

        FollowPointConnection.getPool().renew(16);
        SliderTickSprite.getPool().renew(16);
        Modifiers.getPool().renew(16);

        // TODO replay
        offsetSum = 0;
        offsetRegs = 0;

        File beatmapFile = new File(beatmapInfo.getPath());
        replaying = false;
        replay = new Replay();
        replay.setObjectCount(objects.size());
        replay.setMap(beatmapFile.getParentFile().getName(), beatmapFile.getName(), parsedBeatmap.md5);

        if (replayFilePath != null) {
            replaying = replay.load(replayFilePath);
            if (!replaying) {
                ToastLogger.showTextId(R.string.replay_invalid, true);
                return false;
            }
        } else if (modMenu.getMod().contains(GameMod.MOD_AUTO)) {
            replay = null;
        }

        //TODO online
        if (!replaying)
            OnlineScoring.getInstance().startPlay(beatmapInfo, parsedBeatmap.md5);

        if (Config.isEnableStoryboard()) {
            storyboardSprite.loadStoryboard(beatmapInfo.getPath());
        }

        GameObjectPool.getInstance().preload();

        ppText = null;
        if (Config.isDisplayRealTimePPCounter()) {
            // Calculate timed difficulty attributes
            var parameters = new DifficultyCalculationParameters();

            parameters.setMods(convertedMods);
            parameters.setCustomSpeedMultiplier(modMenu.getChangeSpeed());

            switch (Config.getDifficultyAlgorithm()) {
                case droid ->
                    droidTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateDroidTimedDifficulty(
                        parsedBeatmap, parameters
                    );
                case standard ->
                    standardTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateStandardTimedDifficulty(
                        parsedBeatmap, parameters
                    );
            }
        }

        lastBeatmapInfo = beatmapInfo;
        calculateAllSliderPaths();

        // Resetting variables before starting the game.
        Multiplayer.finalData = null;
        hasFailed = false;
        lastBackPressTime = -1f;
        isSkipRequested = false;
        realTimeElapsed = 0;
        statisticDataTimeElapsed = 0;
        lastScoreSent = null;

        paused = false;
        gameStarted = false;
        return true;
    }

    public Scene getScene() {
        return scene;
    }

    public void restartGame() {
        startGame(null, null);
    }

    public void startGame(final BeatmapInfo beatmapInfo, final String replayFile) {
        scene = new ExtendedScene();
        if (Config.isEnableStoryboard()) {
            if (storyboardSprite == null || storyboardOverlayProxy == null) {
                storyboardSprite = new StoryboardSprite(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                storyboardOverlayProxy = new ProxySprite(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                storyboardSprite.setOverlayDrawProxy(storyboardOverlayProxy);
                scene.attachChild(storyboardSprite);
            }
            storyboardSprite.detachSelf();
            scene.attachChild(storyboardSprite);
        }
        bgScene = new Scene();
        mgScene = new Scene();
        fgScene = new Scene();
        scene.attachChild(bgScene);
        scene.attachChild(mgScene);
        if (storyboardOverlayProxy != null) {
            storyboardOverlayProxy.detachSelf();
            scene.attachChild(storyboardOverlayProxy);
        }
        scene.attachChild(fgScene);
        scene.setBackground(new ColorBackground(0, 0, 0));
        bgScene.setBackgroundEnabled(false);
        mgScene.setBackgroundEnabled(false);
        fgScene.setBackgroundEnabled(false);
        failcount = 0;
        mainCursorId = -1;
        final LoadingScreen screen = new LoadingScreen();
        engine.setScene(screen.getScene());

        final String rfile = beatmapInfo != null ? replayFile : this.replayFilePath;

        Execution.async(() -> {

            DifficultyCalculationManager.stopCalculation();

            if (loadGame(beatmapInfo != null ? beatmapInfo : lastBeatmapInfo, rfile)) {
                prepareScene();
            } else {
                ModMenu.getInstance().setMod(Replay.oldMod);
                ModMenu.getInstance().setChangeSpeed(Replay.oldChangeSpeed);
                ModMenu.getInstance().setFLfollowDelay(Replay.oldFLFollowDelay);

                ModMenu.getInstance().setCustomAR(Replay.oldCustomAR);
                ModMenu.getInstance().setCustomOD(Replay.oldCustomOD);
                ModMenu.getInstance().setCustomCS(Replay.oldCustomCS);
                ModMenu.getInstance().setCustomHP(Replay.oldCustomHP);

                quit();
            }
        });

        ResourceManager.getInstance().getSound("failsound").stop();
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
        setBackground();

        // Set up counter texts
        for (var text : counterTexts) {
            text.detachSelf();
        }

        counterTexts.clear();
        var counterTextFont = ResourceManager.getInstance().getFont("smallFont");

        if (Config.isShowFPS()) {
            fpsText = new ChangeableText(790, 520, counterTextFont, "00.00 FPS");
            counterTexts.add(fpsText);

            fgScene.registerUpdateHandler(new FPSCounter() {
                @Override
                public void onUpdate(float pSecondsElapsed) {
                    // Cancelling the speed multiplier for the FPS counter.
                    super.onUpdate(pSecondsElapsed / GameHelper.getSpeedMultiplier());

                    fpsText.setText(Math.round(getFPS()) + " FPS");
                }
            });
        }

        if (Config.isShowUnstableRate()) {
            urText = new ChangeableText(720, 480, counterTextFont, "00.00 UR    ");
            counterTexts.add(urText);
        }

        if (Config.isShowAverageOffset()) {
            avgOffsetText = new ChangeableText(720, 440, counterTextFont, "Avg offset: 0ms     ");
            counterTexts.add(avgOffsetText);
        }

        if (Config.isDisplayRealTimePPCounter()) {
            ppText = new ChangeableText(720, 400, counterTextFont,
                    Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid ? "0.00dpp" : "0.00pp", 15);
            counterTexts.add(ppText);
        }

        if (BuildConfig.DEBUG) {
            memText = new ChangeableText(780, 520, counterTextFont, "0/0 MB    ");
            counterTexts.add(memText);
        }

        updateCounterTexts();

        // Attach the counter texts
        for (var text : counterTexts) {
            fgScene.attachChild(text);
        }

        stat = new StatisticV2();
        stat.setMod(ModMenu.getInstance().getMod());
        stat.canFail = !stat.getMod().contains(GameMod.MOD_NOFAIL)
                && !stat.getMod().contains(GameMod.MOD_RELAX)
                && !stat.getMod().contains(GameMod.MOD_AUTOPILOT)
                && !stat.getMod().contains(GameMod.MOD_AUTO);

        stat.setDiffModifier(difficultyStatisticsScoreMultiplier);
        stat.setMaxObjectsCount(lastBeatmapInfo.getTotalHitObjectCount());
        stat.setMaxHighestCombo(lastBeatmapInfo.getMaxCombo());

        stat.setBeatmapCS(parsedBeatmap.difficulty.gameplayCS);
        stat.setBeatmapOD(parsedBeatmap.difficulty.od);

        stat.setCustomAR(ModMenu.getInstance().getCustomAR());
        stat.setCustomOD(ModMenu.getInstance().getCustomOD());
        stat.setCustomCS(ModMenu.getInstance().getCustomCS());
        stat.setCustomHP(ModMenu.getInstance().getCustomHP());

        stat.setChangeSpeed(ModMenu.getInstance().getChangeSpeed());
        stat.setFLFollowDelay(ModMenu.getInstance().getFLfollowDelay());

        GameHelper.setHardrock(stat.getMod().contains(GameMod.MOD_HARDROCK));
        GameHelper.setDoubleTime(stat.getMod().contains(GameMod.MOD_DOUBLETIME));
        GameHelper.setNightCore(stat.getMod().contains(GameMod.MOD_NIGHTCORE));
        GameHelper.setHalfTime(stat.getMod().contains(GameMod.MOD_HALFTIME));
        GameHelper.setHidden(stat.getMod().contains(GameMod.MOD_HIDDEN));
        GameHelper.setFlashLight(stat.getMod().contains(GameMod.MOD_FLASHLIGHT));
        GameHelper.setRelaxMod(stat.getMod().contains(GameMod.MOD_RELAX));
        GameHelper.setAutopilotMod(stat.getMod().contains(GameMod.MOD_AUTOPILOT));
        GameHelper.setAuto(stat.getMod().contains(GameMod.MOD_AUTO));
        GameHelper.setSuddenDeath(stat.getMod().contains(GameMod.MOD_SUDDENDEATH));
        GameHelper.setPerfect(stat.getMod().contains(GameMod.MOD_PERFECT));
        GameHelper.setScoreV2(stat.getMod().contains(GameMod.MOD_SCOREV2));
        GameHelper.setEasy(stat.getMod().contains(GameMod.MOD_EASY));
        difficultyHelper = stat.getMod().contains(GameMod.MOD_PRECISE) ?
                DifficultyHelper.HighDifficulty : DifficultyHelper.StdDifficulty;
        GameHelper.setDifficultyHelper(difficultyHelper);

        for (int i = 0; i < CursorCount; i++) {
            cursors[i] = new Cursor();
            cursors[i].mouseDown = false;
            cursors[i].mousePressed = false;
            cursors[i].mouseOldDown = false;
        }

        Arrays.fill(cursorIIsDown, false);

        comboWas100 = false;
        comboWasMissed = false;

        final int leadIn = playableBeatmap.general.audioLeadIn;
        previousFrameTime = 0;
        secPassed = -leadIn / 1000f;
        if (secPassed > -1) {
            secPassed = -1;
        }

        if (video != null && videoOffset < 0) {
            secPassed = Math.min(videoOffset, secPassed);
        }

        float firstObjStartTime = (float) objects.peek().startTime / 1000;
        skipTime = firstObjStartTime - objectTimePreempt - 1f;

        metronome = null;
        if ((Config.getMetronomeSwitch() == 1 && GameHelper.isNightCore())
                || Config.getMetronomeSwitch() == 2) {
            metronome = new Metronome();
        }

        secPassed -= Config.getOffset() / 1000f;
        if (secPassed > 0) {
            skipTime -= secPassed;
            secPassed = 0;
        }
        distToNextObject = 0;

        // TODO passive objects
        if ((replaying || Config.isShowCursor()) && !GameHelper.isAuto() && !GameHelper.isAutopilotMod()) {
            cursorSprites = new CursorEntity[CursorCount];
            for (int i = 0; i < CursorCount; i++) {
                cursorSprites[i] = new CursorEntity();
                cursorSprites[i].attachToScene(fgScene);
            }
        } else {
            cursorSprites = null;
        }

        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
            autoCursor = new AutoCursor();
            autoCursor.attachToScene(fgScene);
        }

        final var countdown = playableBeatmap.general.countdown;
        if (Config.isCorovans() && countdown != BeatmapCountdown.NoCountdown) {
            float cdSpeed = countdown.speed;
            skipTime -= cdSpeed * Countdown.COUNTDOWN_LENGTH;
            if (cdSpeed != 0 && firstObjStartTime - secPassed >= cdSpeed * Countdown.COUNTDOWN_LENGTH) {
                addPassiveObject(new Countdown(this, bgScene, cdSpeed, 0, firstObjStartTime - secPassed));
            }
        }

        if (!Config.isHideInGameUI()) {
            SongProgressBar progressBar = new SongProgressBar(this, fgScene,
                (float) objects.getLast().getEndTime() / 1000, firstObjStartTime,
                new PointF(0, Config.getRES_HEIGHT() - 7), Config.getRES_WIDTH(), 7);

            progressBar.setProgressRectColor(new RGBColor(153f / 255f, 204f / 255f, 51f / 255f));
            progressBar.setProgressRectAlpha(0.4f);
        }

        if (Config.getErrorMeter() == 1 || (Config.getErrorMeter() == 2 && replaying)) {
            hitErrorMeter = new HitErrorMeter(
                fgScene,
                new PointF(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() - 20),
                playableBeatmap.difficulty.od,
                12,
                difficultyHelper);
        }

        skipBtn = null;
        if (skipTime > 1) {
            skipBtn = new AnimatedSprite("play-skip", true, OsuSkin.get().getAnimationFramerate());
            skipBtn.setOrigin(Origin.BottomRight);
            skipBtn.setPosition(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
            skipBtn.setAlpha(0.7f);
            fgScene.attachChild(skipBtn);
        }

        breakAnimator = new BreakAnimator(this, fgScene, stat, playableBeatmap.general.letterboxInBreaks, dimRectangle);
        if (!Config.isHideInGameUI()) {
            scorebar = new ScoreBar(this, fgScene, stat);
            addPassiveObject(scorebar);

            scoreText = new ScoreText(OsuSkin.get().getScorePrefix());
            scoreText.setPosition(Config.getRES_WIDTH(), 0);
            scoreText.setOrigin(Origin.TopRight);
            scoreText.setText("0000000000");
            scoreText.setScale(0.9f);

            accuracyText = new ScoreText(OsuSkin.get().getScorePrefix());
            accuracyText.setPosition(Config.getRES_WIDTH(), 50);
            accuracyText.setOrigin(Origin.TopRight);
            accuracyText.setText("000.00%");
            accuracyText.setScale(0.6f);

            comboText = new ScoreText(OsuSkin.get().getComboPrefix(), Config.isAnimateComboText());
            comboText.setY(Config.getRES_HEIGHT());
            comboText.setOrigin(Origin.BottomLeft);
            comboText.setScaleCenter(Origin.BottomLeft);
            comboText.setScale(1.5f);
            comboText.setText("0x");

            fgScene.attachChild(comboText);
            fgScene.attachChild(accuracyText);
            fgScene.attachChild(scoreText);

            if (Config.isComboburst()) {
                comboBurst = new ComboBurst(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                comboBurst.attachAll(bgScene);
            }

            var mods = stat.getMod();
            var position = new PointF(Config.getRES_WIDTH() - 130, 130);
            float timeOffset = 0;

            for (var mod : mods) {

                var effect = GameObjectPool.getInstance().getEffect(GameMod.getTextureName(mod));

                effect.init(fgScene, position, scale, Modifiers.sequence(
                    Modifiers.scale(0.25f, 1.2f, 1f),
                    Modifiers.delay(2f - timeOffset),
                    Modifiers.parallel(
                        Modifiers.fadeOut(0.5f),
                        Modifiers.scale(0.5f, 1f, 1.5f)
                    )
                ));

                position.x -= 25f;
                timeOffset += 0.25f;
            }

        }

        Rectangle kiaiRect = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT());
        kiaiRect.setVisible(false);
        kiaiRect.setColor(1, 1, 1);
        bgScene.attachChild(kiaiRect, 0);

        Sprite unranked = new Sprite(0, 0, ResourceManager.getInstance().getTexture("play-unranked"));
        unranked.setPosition((float) Config.getRES_WIDTH() / 2 - unranked.getWidth() / 2, 80);
        unranked.setVisible(false);
        fgScene.attachChild(unranked);

        boolean hasUnrankedMod = SmartIterator.wrap(stat.getMod().iterator())
            .applyFilter(m -> m.unranked).hasNext();
        if (hasUnrankedMod
                || Config.isRemoveSliderLock()
                || ModMenu.getInstance().isCustomAR()
                || ModMenu.getInstance().isCustomOD()
                || ModMenu.getInstance().isCustomCS()
                || ModMenu.getInstance().isCustomHP()
                || !ModMenu.getInstance().isDefaultFLFollowDelay()) {
            unranked.setVisible(true);
        }

        String playname = Config.getOnlineUsername();

        ChangeableText replayText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("font"), "", 1000);
        replayText.setVisible(false);
        replayText.setPosition(0, 140);
        replayText.setAlpha(0.7f);
        fgScene.attachChild(replayText, 0);
        if (stat.getMod().contains(GameMod.MOD_AUTO) || replaying) {
            playname = replaying ? GlobalManager.getInstance().getScoring().getReplayStat().getPlayerName() : "osu!";
            replayText.setText("Watching " + playname + " play " + playableBeatmap.metadata.artist + " - " + playableBeatmap.metadata.title + " [" + playableBeatmap.metadata.version + "]");

            // We're cancelling the speed multiplier here because we don't want the replay text to move too fast or slow.
            var translationDuration = 40f * GameHelper.getSpeedMultiplier();

            replayText.registerEntityModifier(new LoopEntityModifier(new MoveXModifier(translationDuration, Config.getRES_WIDTH() + 5, -replayText.getWidth() - 5)));
            replayText.setVisible(!Config.isHideReplayMarquee());
        } else if (Multiplayer.room != null && Multiplayer.room.isTeamVersus()) {

            //noinspection DataFlowIssue
            playname = Multiplayer.player.getTeam().toString();

        }

        if (Config.isShowScoreboard()) {
            scoreBoard = new GameplayLeaderboard(playname, stat);
            fgScene.attachChild(scoreBoard);
        }

        if (GameHelper.isFlashLight()){
            flashlightSprite = new FlashLightEntity();
            fgScene.attachChild(flashlightSprite, 0);
        }

        // Returning here to avoid start the game instantly
        if (Multiplayer.isMultiplayer)
        {
            RoomAPI.INSTANCE.notifyBeatmapLoaded();
            return;
        }
        start();
    }

    // This is used by the multiplayer system, is called once all players in the room completes beatmap loading.
    public void start() {

        if (skipTime <= 1)
            RoomScene.INSTANCE.getChat().dismiss();

        leadOut = 0;
        musicStarted = false;

        // Handle input in its own thread
        var touchOptions = new TouchOptions();
        touchOptions.setRunOnUpdateThread(false);
        engine.getTouchController().applyTouchOptions(touchOptions);

        engine.setScene(scene);
        scene.registerUpdateHandler(this);

        blockAreaFragment = new BlockAreaFragment();
        blockAreaFragment.show(false);
    }

    public RGBColor getComboColor(int num) {
        return comboColors.get(num % comboColors.size());
    }

    @Override
    public void onUpdate(final float pSecondsElapsed) {
        previousFrameTime = SystemClock.uptimeMillis();

        float dt = pSecondsElapsed;
        if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
            //处理时间差过于庞大的情况
            final float offset = totalOffset / 1000f;
            final float realsecPassed = //Config.isSyncMusic() ?
                    GlobalManager.getInstance().getSongService().getPosition() / 1000.0f;// : realTime;
            final float criticalError = Config.isSyncMusic() ? 0.1f : 0.5f;
            final float normalError = Config.isSyncMusic() ? dt : 0.05f;

            if (secPassed + offset - realsecPassed > criticalError) {
                return;
            }

            if (Math.abs(secPassed + offset - realsecPassed) > normalError) {
                if (secPassed + offset > realsecPassed) {
                    dt /= 2f;
                } else {
                    dt *= 2f;
                }
            }
            secPassed += dt;
        }

        updateCounterTexts();

        if (Multiplayer.isMultiplayer)
        {
            long mSecElapsed = (long) (pSecondsElapsed * 1000);
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

        final float mSecPassed = secPassed * 1000;

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
                        (movement = replay.cursorMoves.get(i).movements[cIndex]).getTime() <= (secPassed + dt / 4) * 1000
                ) {
                    float mx = movement.getPoint().x;
                    float my = movement.getPoint().y;
                    if (movement.getTouchType() == TouchType.DOWN) {
                        cursors[i].mouseDown = true;
                        for (int j = 0; j < replay.cursorIndex.length; j++) {
                            cursors[j].mouseOldDown = false;
                        }
                        cursors[i].mousePos.x = mx;
                        cursors[i].mousePos.y = my;

                        replay.lastMoveIndex[i] = -1;
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
                    float t = (secPassed * 1000 - movement.getTime()) / (lastMovement.getTime() - movement.getTime());
                    cursors[i].mousePos.x = lastMovement.getPoint().x * t + movement.getPoint().x * (1 - t);
                    cursors[i].mousePos.y = lastMovement.getPoint().y * t + movement.getPoint().y * (1 - t);
                }
            }
        }

        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
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
        if(GameHelper.isFlashLight()){
            if (!GameHelper.isAuto() && !GameHelper.isAutopilotMod()) {
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

        activeTimingPoint = playableBeatmap.controlPoints.timing.controlPointAt(mSecPassed);
        activeEffectPoint = playableBeatmap.controlPoints.effect.controlPointAt(mSecPassed);

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000);
        GameHelper.setKiai(activeEffectPoint.isKiai);
        GameHelper.setCurrentBeatTime(Math.max(0, secPassed - activeTimingPoint.time / 1000) % GameHelper.getBeatLength());

        if (!breakPeriods.isEmpty()) {
            if (!breakAnimator.isBreak()
                    && breakPeriods.peek().getStart() <= secPassed) {
                gameStarted = false;
                breakAnimator.init(breakPeriods.peek().getLength());
                if(GameHelper.isFlashLight()){
                    flashlightSprite.onBreak(true);
                }

                if (Multiplayer.isConnected())
                    RoomScene.INSTANCE.getChat().show();

                if(scorebar != null) scorebar.setVisible(false);
                breakPeriods.poll();
            }
        }
        if (breakAnimator.isOver()) {

            // Ensure the chat is dismissed if it's still shown
            RoomScene.INSTANCE.getChat().dismiss();

            gameStarted = true;
            if(scorebar != null) scorebar.setVisible(true);
            if(GameHelper.isFlashLight()){
                flashlightSprite.onBreak(false);
            }
        }

        if (objects.isEmpty() && activeObjects.isEmpty() && GameHelper.isFlashLight()) {
            flashlightSprite.onBreak(true);
        }

        if (gameStarted) {
            double rate = 0.375;
            if (playableBeatmap.difficulty.hp > 0 && distToNextObject > 0) {
                rate = 1 + playableBeatmap.difficulty.hp / (2 * distToNextObject);
            }
            stat.changeHp((float) -rate * 0.01f * dt);

            if (stat.getHp() <= 0 && stat.canFail) {
                if (GameHelper.isEasy() && failcount < 3) {
                    failcount++;
                    stat.changeHp(1f);
                }
                else {
                    if (Multiplayer.isMultiplayer)
                    {
                        if (!hasFailed)
                            ToastLogger.showText("You failed but you can continue playing.", false);

                        hasFailed = true;
                    } else {
                        gameover();
                        return;
                    }
                }
            }
        }

        if (hitErrorMeter != null) {
            hitErrorMeter.update(dt);
        }

        if(!Config.isHideInGameUI()) {
            strBuilder.setLength(0);
            strBuilder.append(stat.getCombo());
            strBuilder.append('x');
            var comboStr = strBuilder.toString();
            comboText.setText(comboStr);

            strBuilder.setLength(0);
            float rawAccuracy = stat.getAccuracy() * 100f;
            strBuilder.append((int) rawAccuracy);
            if ((int) rawAccuracy < 10) {
                strBuilder.insert(0, '0');
            }
            strBuilder.append('.');
            rawAccuracy -= (int) rawAccuracy;
            rawAccuracy *= 100;
            if ((int) rawAccuracy < 10) {
                strBuilder.append('0');
            }
            strBuilder.append((int) rawAccuracy);
            strBuilder.append('%');
            accuracyText.setText(strBuilder.toString());
            strBuilder.setLength(0);
            strBuilder.append(stat.getTotalScoreWithMultiplier());
            while (strBuilder.length() < 8) {
                strBuilder.insert(0, '0');
            }

            scoreText.setText(strBuilder.toString());
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
            // Since we're going to remove them and same objects aren't added to both list we can
            // share the same list to remove them.
            activeObjects.remove(object);
            passiveObjects.remove(object);
        }

        updatePassiveObjects(dt);
        if (Config.isRemoveSliderLock()) {
            updateLastActiveObjectHitTime();
        }
        updateActiveObjects(dt);

        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
            autoCursor.moveToObject(activeObjects.peek(), secPassed, this);
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

        if (video != null && secPassed >= videoOffset)
        {
            if (!videoStarted) {
                video.getTexture().play();
                video.getTexture().setPlaybackSpeed(GameHelper.getSpeedMultiplier());
                videoStarted = true;
            }

            if (video.getAlpha() < 1.0f)
                video.setAlpha(Math.min(video.getAlpha() + 0.03f, 1.0f));
        }

        if (secPassed >= 0 && !musicStarted) {
            GlobalManager.getInstance().getSongService().play();
            GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
            totalLength = GlobalManager.getInstance().getSongService().getLength();
            musicStarted = true;
            secPassed = 0;
            return;
        }

        boolean shouldBePunished = false;

        while (!objects.isEmpty()
                && secPassed + objectTimePreempt > (float) objects.peek().startTime / 1000) {
            gameStarted = true;
            final var obj = objects.poll();

            if (obj.startTime > totalLength) {
                shouldBePunished = true;
            }

            // Next object from the polled one, this returns null if the list is empty. That's why every
            // usage of this is done if condition 'objects.isEmpty()' is false. Ignore IDE warnings.
            final var nextObj = objects.peek();

            distToNextObject = nextObj != null ?
                Math.max(nextObj.startTime - obj.startTime, activeTimingPoint.msPerBeat / 2) / 1000 :
                0;

            final RGBColor comboColor = getComboColor(obj.getComboIndexWithOffsets());

            if (obj instanceof HitCircle parsedCircle) {
                final var gameplayCircle = GameObjectPool.getInstance().getCircle();

                gameplayCircle.init(this, mgScene, parsedCircle, secPassed, comboColor);
                addObject(gameplayCircle);

                if (GameHelper.isAuto()) {
                    gameplayCircle.setAutoPlay();
                }

                gameplayCircle.setHitTime((float) obj.startTime / 1000);
                gameplayCircle.setId(++lastObjectId);

                if (replaying) {
                    gameplayCircle.setReplayData(replay.objectData[gameplayCircle.getId()]);
                }

            } else if (obj instanceof Spinner parsedSpinner) {
                final float rps = 2 + 2 * playableBeatmap.difficulty.od / 10f;
                final var gameplaySpinner = GameObjectPool.getInstance().getSpinner();

                gameplaySpinner.init(this, bgScene, parsedSpinner, rps, stat);
                addObject(gameplaySpinner);

                if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
                    gameplaySpinner.setAutoPlay();
                }

                gameplaySpinner.setId(++lastObjectId);
                if (replaying) {
                    gameplaySpinner.setReplayData(replay.objectData[gameplaySpinner.getId()]);
                }

            } else if (obj instanceof com.rian.osu.beatmap.hitobject.Slider parsedSlider) {
                final var gameplaySlider = GameObjectPool.getInstance().getSlider();

                gameplaySlider.init(this, mgScene, parsedSlider, secPassed,
                    comboColor, sliderBorderColor, (float) playableBeatmap.difficulty.sliderTickRate, playableBeatmap.controlPoints,
                    getSliderPath(sliderIndex++));

                addObject(gameplaySlider);

                if (GameHelper.isAuto()) {
                    gameplaySlider.setAutoPlay();
                }
                gameplaySlider.setHitTime((float) obj.startTime / 1000);
                gameplaySlider.setId(++lastObjectId);
                if (replaying) {
                    gameplaySlider.setReplayData(replay.objectData[gameplaySlider.getId()]);
                    if (gameplaySlider.getReplayData().tickSet == null)
                        gameplaySlider.getReplayData().tickSet = new BitSet();
                }
            }

            if (!(obj instanceof Spinner) && nextObj != null && !(nextObj instanceof Spinner) && !obj.isLastInCombo()) {
                FollowPointConnection.addConnection(bgScene, secPassed, obj, nextObj);
            }
        }

        // 节拍器
        if (metronome != null) {
            metronome.update(secPassed, activeTimingPoint);
        }

        //Status playerStatus = music.getStatus();
        Status playerStatus = GlobalManager.getInstance().getSongService().getStatus();

        if (playerStatus != Status.PLAYING) {
            secPassed += dt;
        }

        if (shouldBePunished || (objects.isEmpty() && activeObjects.isEmpty() && leadOut > 2)) {
            scene = new ExtendedScene();
            SkinManager.setSkinEnabled(false);
            GameObjectPool.getInstance().purge();
            objects.clear();
            activeObjects.clear();
            passiveObjects.clear();
            expiredObjects.clear();
            breakPeriods.clear();
            cursorSprites = null;
            playableBeatmap = null;
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

            if (scoringScene != null) {
                if (replaying) {
                    ModMenu.getInstance().setMod(Replay.oldMod);
                    ModMenu.getInstance().setChangeSpeed(Replay.oldChangeSpeed);
                    ModMenu.getInstance().setFLfollowDelay(Replay.oldFLFollowDelay);

                    ModMenu.getInstance().setCustomAR(Replay.oldCustomAR);
                    ModMenu.getInstance().setCustomOD(Replay.oldCustomOD);
                    ModMenu.getInstance().setCustomCS(Replay.oldCustomCS);
                    ModMenu.getInstance().setCustomHP(Replay.oldCustomHP);
                }

                if (replaying)
                    scoringScene.load(scoringScene.getReplayStat(), null, GlobalManager.getInstance().getSongService(), replayPath, null, lastBeatmapInfo);
                else {
                    if (stat.getMod().contains(GameMod.MOD_AUTO)) {
                        stat.setPlayerName("osu!");
                    }

                    if (Multiplayer.isConnected())
                    {
                        Multiplayer.log("Match ended, moving to results scene.");
                        RoomScene.INSTANCE.getChat().show();

                        Execution.async(() -> Execution.runSafe(() -> RoomAPI.submitFinalScore(stat.toJson())));

                        ToastLogger.showText("Loading room statistics...", false);
                    }
                    scoringScene.load(stat, lastBeatmapInfo, GlobalManager.getInstance().getSongService(), replayPath, parsedBeatmap.md5, null);
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

        if (secPassed > skipTime - 1f && skipBtn != null) {
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
        for (int i = 0, size = passiveObjects.size(); i < size; i++) {
            passiveObjects.get(i).update(deltaTime);
        }
    }

    public void skip()
    {
        RoomScene.INSTANCE.getChat().dismiss();

        if (secPassed > skipTime - 1f)
            return;

        if (GlobalManager.getInstance().getSongService().getStatus() != Status.PLAYING) {
            GlobalManager.getInstance().getSongService().play();
            GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
            totalLength = GlobalManager.getInstance().getSongService().getLength();
            musicStarted = true;
        }
        ResourceManager.getInstance().getSound("menuhit").play();
        float difference = skipTime - 0.5f - secPassed;

        secPassed = skipTime - 0.5f;
        int seekTime = (int) Math.ceil(secPassed * 1000);
        int videoSeekTime = seekTime - (int) (videoOffset * 1000);

        Execution.updateThread(() -> {

            updatePassiveObjects(difference);

            GlobalManager.getInstance().getSongService().seekTo(seekTime);
            if (video != null) {
                video.getTexture().seekTo(videoSeekTime);
            }

            if (skipBtn != null) {
                skipBtn.detachSelf();
                skipBtn = null;
            }
        });
    }

    private void onExit() {

        SkinManager.setSkinEnabled(false);
        GameObjectPool.getInstance().purge();
        GameHelper.purgeSliderPathPool();
        stopAllAuxiliarySamples();
        if (activeObjects != null) {
            activeObjects.clear();
        }
        if (passiveObjects != null) {
            passiveObjects.clear();
        }
        if (expiredObjects != null) {
            expiredObjects.clear();
        }
        if (objects != null) {
            objects.clear();
        }
        breakPeriods.clear();
        playableBeatmap = null;
        cursorSprites = null;
        scoreBoard = null;
        droidTimedDifficultyAttributes = null;
        standardTimedDifficultyAttributes = null;

        if (GlobalManager.getInstance().getSongService() != null) {
            GlobalManager.getInstance().getSongService().stop();
            GlobalManager.getInstance().getSongService().preLoad(audioFilePath);
            GlobalManager.getInstance().getSongService().play();
            GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
        }

        if (replaying) {
            replayFilePath = null;
            ModMenu.getInstance().setMod(Replay.oldMod);
            ModMenu.getInstance().setChangeSpeed(Replay.oldChangeSpeed);
            ModMenu.getInstance().setFLfollowDelay(Replay.oldFLFollowDelay);

            ModMenu.getInstance().setCustomAR(Replay.oldCustomAR);
            ModMenu.getInstance().setCustomOD(Replay.oldCustomOD);
            ModMenu.getInstance().setCustomCS(Replay.oldCustomCS);
            ModMenu.getInstance().setCustomHP(Replay.oldCustomHP);
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
        scene = new ExtendedScene();

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
        boolean writeReplay = objectId != -1 && replay != null && !replaying;
        if (score == 0) {
            if (stat.getCombo() > 30) {
                ResourceManager.getInstance().getCustomSound("combobreak", 1)
                        .play();
            }
            comboWasMissed = true;
            stat.registerHit(0, false, false);
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
                updatePPCounter(objectId);
            }
            return "hit0";
        }

        String scoreName = "hit300";
        if (score == 50) {
            stat.registerHit(50, false, false);
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
                stat.registerHit(100, true, false);
                scoreName = "hit100k";
            } else {
                stat.registerHit(100, false, false);
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
                    stat.registerHit(300, true, true);
                    scoreName = "hit300g";
                } else {
                    stat.registerHit(300, true, false);
                    scoreName = "hit300k";
                }
            } else {
                stat.registerHit(300, false, false);
                scoreName = "hit300";
            }
        }

        if (endCombo) {
            comboWas100 = false;
            comboWasMissed = false;
        }

        if (objectId != -1) {
            updatePPCounter(objectId);
        }

        return scoreName;
    }


    public void onCircleHit(int id, final float acc, final PointF pos,
                            final boolean endCombo, byte forcedScore, RGBColor color) {
        if (GameHelper.isAuto()) {
            autoCursor.click();
        }

        float accuracy = Math.abs(acc);
        boolean writeReplay = replay != null && !replaying;
        if (writeReplay) {
            short sacc = (short) (acc * 1000);
            replay.addObjectResult(id, sacc, null);
        }
        if(GameHelper.isFlashLight() && !GameHelper.isAuto() && !GameHelper.isAutopilotMod()){
           int nearestCursorId = getNearestCursorId(pos.x, pos.y);
           if (nearestCursorId >= 0) {
               mainCursorId = nearestCursorId;
               flashlightSprite.onMouseMove(
                    cursors[mainCursorId].mousePos.x,
                    cursors[mainCursorId].mousePos.y
               );
            }
        }

        //(30 - overallDifficulty) / 100f
        float overallDifficulty = playableBeatmap.difficulty.od;
        if (accuracy > difficultyHelper.hitWindowFor50(overallDifficulty) || forcedScore == ResultType.MISS.getId()) {
            createHitEffect(pos, "hit0", color);
            registerHit(id, 0, endCombo);
            return;
        }

        String scoreName;
        if (forcedScore == ResultType.HIT300.getId() ||
                forcedScore == 0 && accuracy <= difficultyHelper.hitWindowFor300(overallDifficulty)) {
            //(75 + 25 * (5 - overallDifficulty) / 5) / 1000)
            scoreName = registerHit(id, 300, endCombo);
        } else if (forcedScore == ResultType.HIT100.getId() ||
                forcedScore == 0 && accuracy <= difficultyHelper.hitWindowFor100(overallDifficulty)) {
            //(150 + 50 * (5 - overallDifficulty) / 5) / 1000)
            scoreName = registerHit(id, 100, endCombo);
        } else {
            scoreName = registerHit(id, 50, endCombo);
        }

        createBurstEffect(pos, color);
        createHitEffect(pos, scoreName, color);
    }

    public void onSliderReverse(PointF pos, float ang, RGBColor color) {
        createBurstEffectSliderReverse(pos, ang, color);
    }

    public void onSliderHit(int id, final int score, final PointF start,
                            final PointF end, final boolean endCombo, RGBColor color, int type) {
        if (GameHelper.isFlashLight() && !GameHelper.isAuto() && !GameHelper.isAutopilotMod()) {
            int nearestCursorId = getNearestCursorId(end.x, end.y);
            if (nearestCursorId >= 0) {
                mainCursorId = nearestCursorId;
                flashlightSprite.onMouseMove(
                        cursors[mainCursorId].mousePos.x,
                        cursors[mainCursorId].mousePos.y
                );
            }
        }

        if (score == 0) {
            createHitEffect(end, "hit0", color);
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
                scoreName = registerHit(id, 300, endCombo);
                break;
            case 100:
                scoreName = registerHit(id, 100, endCombo);
                stat.setPerfect(false);
                break;
            case 50:
                scoreName = registerHit(id, 50, endCombo);
                stat.setPerfect(false);
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
                    createBurstEffectSliderStart(end, color);
                    break;
                case GameObjectListener.SLIDER_END:
                    createBurstEffectSliderEnd(end, color);
                    break;
                case GameObjectListener.SLIDER_REPEAT:
                    break;
                default:
                    createBurstEffect(end, color);
            }
        }

        createHitEffect(end, scoreName, color);
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
    }

    @Override
    public void playSamples(final HitObject obj) {
        for (var sample : obj.getSamples()) {
            playSample(sample, false);
        }
    }

    @Override
    public void playAuxiliarySamples(final HitObject obj) {
        for (var sample : obj.getAuxiliarySamples()) {
            playSample(sample, true);
        }
    }

    @Override
    public void playSample(HitSampleInfo sample, boolean loop) {
        var resourceManager = ResourceManager.getInstance();
        BassSoundProvider snd = null;

        for (var name : sample.getLookupNames()) {
            snd = resourceManager.getCustomSound(name, false);

            if (snd != null) {
                break;
            }
        }

        if (snd == null) {
            return;
        }

        snd.setLooping(loop);
        snd.play(sample.volume / 100f);
    }

    @Override
    public void stopAuxiliarySamples(final HitObject obj) {
        for (var sample : obj.getAuxiliarySamples()) {
            stopSample(sample);
        }
    }

    @Override
    public void stopSample(HitSampleInfo sample) {
        var resourceManager = ResourceManager.getInstance();

        for (var name : sample.getLookupNames()) {
            var snd = resourceManager.getCustomSound(name, false);

            if (snd != null) {
                snd.stop();
            }
        }
    }

    private void stopAllAuxiliarySamples() {
        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            activeObjects.get(i).stopAuxiliarySamples();
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
        if (GameHelper.isAuto()) {
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

        if (GameHelper.isHardrock()) {
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
        if (replaying) {
            return false;
        }

        var id = event.getPointerID();
        if (id < 0 || id >= CursorCount) {
            return false;
        }

        var cursor = cursors[id];
        var sprite = !GameHelper.isAuto() && !GameHelper.isAutopilotMod() && cursorSprites != null
                ? cursorSprites[id]
                : null;

        cursor.mousePos.x = FMath.clamp(event.getX(), 0, Config.getRES_WIDTH());
        cursor.mousePos.y = FMath.clamp(event.getY(), 0, Config.getRES_HEIGHT());

        if (sprite != null) {
            sprite.setPosition(cursor.mousePos.x, cursor.mousePos.y);
        }

        var frameOffset = previousFrameTime > 0 ? (event.getMotionEvent().getEventTime() - previousFrameTime) * GameHelper.getSpeedMultiplier() : 0;
        var eventTime = (int) (secPassed * 1000 + frameOffset);

        if (event.isActionDown()) {

            if (sprite != null) {
                sprite.setShowing(true);
            }

            cursor.mouseDown = true;
            cursor.mouseDownOffsetMS = frameOffset;

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

    public void pause() {
        if (paused) {
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

        if (video != null && videoStarted) {
            video.getTexture().pause();
        }

        stopAllAuxiliarySamples();

        // Release all pressed cursors to avoid getting stuck at resume.
        if (!GameHelper.isAuto() && !GameHelper.isAutopilotMod() && !replaying) {
            var frameOffset = previousFrameTime > 0 ? (SystemClock.uptimeMillis() - previousFrameTime) * GameHelper.getSpeedMultiplier() : 0;
            var time = (int) (secPassed * 1000 + frameOffset);

            for (int i = 0; i < CursorCount; ++i) {
                var cursor = cursors[i];

                if (cursor.mouseDown) {
                    cursor.mouseDown = false;

                    if (replay != null)
                        replay.addUp(time, i);
                }
                if (cursorSprites != null)
                    cursorSprites[i].setShowing(false);
            }
        }

        if (blockAreaFragment != null) {
            blockAreaFragment.dismiss();
        }

        if (GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
            GlobalManager.getInstance().getSongService().pause();
        }
        paused = true;

        final PauseMenu menu = new PauseMenu(engine, this, false);
        scene.setChildScene(menu.getScene(), false, true, true);
    }

    public void gameover() {
        if (Multiplayer.isMultiplayer)
        {
            if (Multiplayer.isConnected())
            {
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

        if(scorebar != null) scorebar.flush();
        stopAllAuxiliarySamples();
        ResourceManager.getInstance().getSound("failsound").play();
        final PauseMenu menu = new PauseMenu(engine, this, true);
        gameStarted = false;

        if (video != null) {
            video.getTexture().pause();
        }

        if (GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
            GlobalManager.getInstance().getSongService().pause();
        }
        paused = true;
        scene.setChildScene(menu.getScene(), false, true, true);
    }

    public void resume() {
        if (!paused) {
            return;
        }

        if (blockAreaFragment == null) {
            blockAreaFragment = new BlockAreaFragment();
        }
        blockAreaFragment.show();

        scene.getChildScene().back();
        paused = false;
        if (stat.getHp() <= 0 && !stat.getMod().contains(GameMod.MOD_NOFAIL)) {
            quit();
            return;
        }

        if (video != null && videoStarted) {
            video.getTexture().play();
        }

        if (GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getStatus() != Status.PLAYING && secPassed > 0) {
            GlobalManager.getInstance().getSongService().play();
            GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
            totalLength = GlobalManager.getInstance().getSongService().getLength();
        }
    }

    public boolean isPaused() {
        return paused;
    }


    public void addPassiveObject(final GameObject object) {
        passiveObjects.add(object);
    }


    public void removePassiveObject(final GameObject object) {
        expiredObjects.add(object);
    }

    private void createHitEffect(final PointF pos, final String name, RGBColor color) {

        var effect = GameObjectPool.getInstance().getEffect(name);

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

            effect.init(
                mgScene,
                pos,
                scale * 1.6f,
                fadeSequence,
                Modifiers.scale(0.1f, scale * 1.6f, scale, null, EaseQuadIn.getInstance()),
                Modifiers.translateY(fadeOutDelay + fadeOutLength, -5f, 80f, null, EaseQuadIn.getInstance()),
                Modifiers.sequence(
                    Modifiers.rotation(fadeInLength, 0, rotation),
                    Modifiers.rotation(fadeOutDelay + fadeOutLength - fadeInLength, rotation, rotation * 2, null, EaseQuadIn.getInstance())
                )
            );

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
                Modifiers.scale(0.6f, scale * 0.8f, scale * 1.2f, null, EaseQuadOut.getInstance()),
                Modifiers.sequence(
                    Modifiers.fadeIn(0.2f),
                    Modifiers.delay(0.2f),
                    Modifiers.fadeOut(1f)
                )
            );
        }

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

    private void applyBurstEffect(GameEffect effect, PointF pos) {

        // Reference: https://github.com/ppy/osu/blob/c5893f245ce7a89d1900dbb620390823702481fe/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyMainCirclePiece.cs#L152-L174

        var fadeDuration = 0.24f;

        effect.init(mgScene, pos, scale,
            Modifiers.scale(fadeDuration, scale, scale * 1.4f, null, EaseQuadOut.getInstance()),
            Modifiers.fadeOut(fadeDuration)
        );
    }

    private void createBurstEffect(final PointF pos, final RGBColor color) {
        if (!Config.isBurstEffects() || GameHelper.isHidden())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("hitcircle");
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("hitcircleoverlay");
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderStart(final PointF pos, final RGBColor color) {
        if (!Config.isBurstEffects() || GameHelper.isHidden())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("sliderstartcircle");
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("sliderstartcircleoverlay");
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderEnd(final PointF pos, final RGBColor color) {
        if (!Config.isBurstEffects() || GameHelper.isHidden())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("sliderendcircle");
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("sliderendcircleoverlay");
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderReverse(final PointF pos, float ang, final RGBColor color) {
        if (!Config.isBurstEffects() || GameHelper.isHidden())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("reversearrow");
        burst1.hit.setRotation(ang);
        applyBurstEffect(burst1, pos);
    }

    public int getCursorsCount() {
        return CursorCount;
    }


    public void registerAccuracy(final double acc) {
        if (hitErrorMeter != null) {
            hitErrorMeter.putErrorResult((float) acc);
        }
        offsetSum += (float) acc;
        offsetRegs++;

        stat.addHitOffset(acc);

        if (replaying) {
            scoringScene.getReplayStat().addHitOffset(acc);
        }
    }


    public void onSliderEnd(int id, int accuracy, BitSet tickSet) {
        onTrackingSliders(false);
        if (GameHelper.isAuto()) {
            autoCursor.onSliderEnd();
        }
        if (replay != null && !replaying) {
            short acc = (short) (accuracy);
            replay.addObjectResult(id, acc, (BitSet) tickSet.clone());
        }
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        if (GameHelper.isAuto()) {
            autoCursor.onSliderTracking();
        }
        if (GameHelper.isFlashLight()) {
            flashlightSprite.onTrackingSliders(isTrackingSliders);
        }
    }

    public void onUpdatedAutoCursor(float pX, float pY) {
        if (GameHelper.isFlashLight()) {
            flashlightSprite.onMouseMove(pX, pY);
        }
    }

    public void updateAutoBasedPos(float pX, float pY) {
        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
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

    private void calculateAllSliderPaths() {
        if (playableBeatmap.hitObjects.getSliderCount() == 0) {
            return;
        }

        sliderPaths = new SliderPath[playableBeatmap.hitObjects.getSliderCount()];
        sliderIndex = 0;

        for (var obj : playableBeatmap.hitObjects.objects) {
            if (!(obj instanceof com.rian.osu.beatmap.hitobject.Slider slider)) {
                continue;
            }

            sliderPaths[sliderIndex++] = GameHelper.convertSliderPath(slider);
        }

        sliderIndex = 0;
    }

    private SliderPath getSliderPath(int index){
        if (sliderPaths != null && index < sliderPaths.length && index >= 0){
            return sliderPaths[index];
        }
        else {
            return null;
        }
    }

    public boolean getReplaying() {
        return replaying;
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

            replayFilePath = Config.getCorePath() + "Scores/" + odrFilename;
            replay.setStat(stat);
            replay.save(replayFilePath);

            if (stat.getTotalScoreWithMultiplier() > 0 && !stat.getMod().contains(GameMod.MOD_AUTO)) {
                stat.setReplayFilename(odrFilename);
                stat.setBeatmap(lastBeatmapInfo.getSetDirectory(), lastBeatmapInfo.getFilename());

                try {
                    DatabaseManager.getScoreInfoTable().insertScore(stat.toScoreInfo());
                } catch (Exception e) {
                    Log.e("GameScene", "Failed to save score to database", e);
                }
            }

            ToastLogger.showText(StringTable.get(R.string.message_save_replay_successful), true);
            replayFilePath = null;
            return true;
        }
        else{
            ToastLogger.showText(StringTable.get(R.string.message_save_replay_failed), true);
            return false;
        }
    }

    private void updateCounterTexts() {
        // We are not updating FPS text as it is handled by FPSCounter, as well
        // as PP text as it is updated in updatePPCounter.
        if (avgOffsetText != null) {
            float avgOffset = offsetRegs > 0 ? offsetSum / offsetRegs : 0;

            avgOffsetText.setText("Avg offset: " + (int) (avgOffset * 1000f) + "ms");
        }

        if (urText != null) {
            urText.setText(String.format(Locale.ENGLISH, "%.2f UR    ", stat != null ? stat.getUnstableRate() : 0));
        }

        if (BuildConfig.DEBUG) {
            var totalMemory = Runtime.getRuntime().totalMemory();
            var usedMemory = totalMemory - Runtime.getRuntime().freeMemory();

            memText.setText(usedMemory / 1024 / 1024 + "/" + totalMemory / 1024 / 1024 + " MB    ");
        }

        // Update counter text positions
        for (int i = 0; i < counterTexts.size(); ++i) {
            var text = counterTexts.get(i);

            text.setPosition(Config.getRES_WIDTH() - text.getWidth() - 5, Config.getRES_HEIGHT() - text.getHeight() - 10 - i * text.getHeight());
        }
    }

    private void updatePPCounter(int objectId) {
        if (ppText == null) {
            return;
        }

        var object = playableBeatmap.hitObjects.objects.get(objectId);
        double time = object.getEndTime();

        switch (Config.getDifficultyAlgorithm()) {
            case droid -> ppText.setText(String.format(Locale.ENGLISH, "%.2fdpp", getDroidPPAtTime(time)));
            case standard -> ppText.setText(String.format(Locale.ENGLISH, "%.2fpp", getStandardPPAtTime(time)));
        }
    }

    private double getDroidPPAtTime(double time) {
        var timedAttributes = getAttributeAtTime(droidTimedDifficultyAttributes, time);

        if (timedAttributes == null) {
            return 0;
        }

        return BeatmapDifficultyCalculator.calculateDroidPerformance(
            timedAttributes.attributes,
            stat
        ).total;
    }

    private double getStandardPPAtTime(double time) {
        var timedAttributes = getAttributeAtTime(standardTimedDifficultyAttributes, time);

        if (timedAttributes == null) {
            return 0;
        }

        return BeatmapDifficultyCalculator.calculateStandardPerformance(
            timedAttributes.attributes,
            stat
        ).total;
    }

    private <T extends DifficultyAttributes> TimedDifficultyAttributes<T> getAttributeAtTime(
        List<TimedDifficultyAttributes<T>> timedDifficultyAttributes, double time
    ) {
        if (timedDifficultyAttributes == null || timedDifficultyAttributes.isEmpty()) {
            return null;
        }

        if (time < timedDifficultyAttributes.get(0).time) {
            return null;
        }

        if (time >= timedDifficultyAttributes.get(timedDifficultyAttributes.size() - 1).time) {
            return timedDifficultyAttributes.get(timedDifficultyAttributes.size() - 1);
        }

        int l = 0;
        int r = timedDifficultyAttributes.size() - 2;

        while (l <= r) {
            int pivot = l + ((r - l) >> 1);
            var attributes = timedDifficultyAttributes.get(pivot);

            if (attributes.time < time) {
                l = pivot + 1;
            } else if (attributes.time > time) {
                r = pivot - 1;
            } else {
                return attributes;
            }
        }

        return timedDifficultyAttributes.get(l);
    }
}
