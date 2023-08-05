package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;
import android.os.SystemClock;

import com.edlplan.ext.EdExtensionHelper;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.support.ProxySprite;
import com.edlplan.framework.support.osb.StoryboardSprite;
import com.edlplan.framework.utils.functionality.SmartIterator;
import com.edlplan.osu.support.timing.TimingPoints;
import com.edlplan.osu.support.timing.controlpoint.ControlPoints;
import com.edlplan.ui.fragment.InGameSettingMenu;
import com.reco1l.api.ibancho.RoomAPI;
import com.reco1l.framework.lang.Execution;
import com.reco1l.framework.lang.execution.Async;
import com.reco1l.legacy.ui.multiplayer.Multiplayer;
import com.reco1l.legacy.ui.multiplayer.RoomScene;
import com.rian.difficultycalculator.attributes.TimedDifficultyAttributes;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration;
import com.rian.difficultycalculator.calculator.DifficultyCalculationParameters;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.FadeOutModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.LoopEntityModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.util.FPSCounter;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.modifier.IModifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.effect.Metronome;
import ru.nsu.ccfit.zuev.audio.serviceAudio.PlayMode;
import ru.nsu.ccfit.zuev.osu.BeatmapProperties;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.RGBAColor;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.AsyncTask;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.BeatmapCountdown;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.SampleBank;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser;
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.AutoCursor;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.Cursor;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorEntity;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calcuator;
import ru.nsu.ccfit.zuev.osu.helper.ModifierFactory;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.menu.PauseMenu;
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osu.scoring.TouchType;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.SkinManager;

public class GameScene implements IUpdateHandler, GameObjectListener,
        IOnSceneTouchListener {
    public static final int CursorCount = 10;
    private final Engine engine;
    private final Cursor[] cursors = new Cursor[CursorCount];
    private final boolean[] cursorIIsDown = new boolean[CursorCount];
    private final StringBuilder strBuilder = new StringBuilder();
    public String filePath = null;
    private Scene scene;
    private Scene bgScene, mgScene, fgScene;
    private Scene oldScene;
    private BeatmapData beatmapData;
    private TrackInfo lastTrack;
    private ScoringScene scoringScene;
    private TimingPoint currentTimingPoint;
    private TimingPoint soundTimingPoint;
    private TimingPoint firstTimingPoint;
    private Queue<TimingPoint> timingPoints;
    private Queue<TimingPoint> activeTimingPoints;
    private String trackMD5;
    private int lastObjectId = -1;
    private float secPassed = 0;
    private float leadOut = 0;
    private LinkedList<GameObjectData> objects;
    private ArrayList<RGBColor> combos;
    private int comboNum; // use this to show combo color
    private int currentComboNum;
    private boolean comboWasMissed = false;
    private boolean comboWas100 = false;
    private Queue<GameObject> activeObjects;
    private LinkedList<GameObject> passiveObjects = new LinkedList<GameObject>();
    private GameScoreText comboText, accText, scoreText;  //显示的文字  连击数  ACC  分数
    private GameScoreTextShadow scoreShadow;
    private Queue<BreakPeriod> breakPeriods = new LinkedList<BreakPeriod>();
    private BreakAnimator breakAnimator;
    private ScoreBar scorebar;
    private SongProgressBar progressBar;
    public DuringGameScoreBoard scoreBoard;
    private HitErrorMeter hitErrorMeter;
    private Metronome metronome;
    private boolean isFirst = true;
    private float scale;
    private float approachRate;
    private float rawDifficulty;
    private float overallDifficulty;
    private float rawDrain;
    private float drain;
    public StatisticV2 stat;
    private boolean gameStarted;
    private float totalOffset;
    //private IMusicPlayer music = null;
    private int totalLength = Integer.MAX_VALUE;
    private boolean loadComplete;
    private boolean paused;
    private Sprite skipBtn;
    private float skipTime;
    private boolean musicStarted;
    private float distToNextObject;
    private float timeMultiplier = 1.0f;
    private CursorEntity[] cursorSprites;
    private AutoCursor autoCursor;
    private FlashLightEntity flashlightSprite;
    private int mainCursorId = -1;
    private Replay replay;
    private boolean replaying;
    private String replayFile;
    private float avgOffset;
    private int offsetRegs;
    private Rectangle kiaiRect = null;
    private Sprite bgSprite = null;
    private Sprite unranked;
    private ChangeableText replayText;
    private String title, artist, version;
    private ComboBurst comboBurst;
    private int failcount = 0;
    private float lastObjectHitTime = 0;
    private SliderPath[] sliderPaths = null;
    private int sliderIndex = 0;

    private StoryboardSprite storyboardSprite;
    private ProxySprite storyboardOverlayProxy;

    private DifficultyHelper difficultyHelper = DifficultyHelper.StdDifficulty;

    private List<TimedDifficultyAttributes> timedDifficultyAttributes = new ArrayList<>();
    private ChangeableText ppText;

    private long previousFrameTime;

    private boolean mIsAuto;

    // Multiplayer

    /**Count of times that user pressed the back button*/
    private int backPressCount = 0;

    /**Indicates the last time that the user pressed the back button, used to reset {@code backPressCount}*/
    private float lastTimeBackPress = -1f;

    /**Indicates that the player has failed and the score shouldn't be submitted*/
    public boolean hasFailed = false;

    /**Indicates that the player has requested skip*/
    private boolean isSkipRequested = false;

    /**Last time the live score was sent to server, this is used to send the score every 3s (real time)*/
    private long realTimeElapsed = 0;

    // End multiplayer

    public GameScene(final Engine engine) {
        this.engine = engine;
        scene = new Scene();
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
        boolean bgset = false;
        bgSprite = null;
        if (storyboardSprite != null) {
            if (storyboardSprite.isStoryboardAvailable()) {
                storyboardSprite.setBrightness(Config.getBackgroundBrightness());
                return;
            }
        }

        if (beatmapData.events.backgroundFilename != null) {
            bgset = true;
            final TextureRegion tex = Config.isSafeBeatmapBg() ? ResourceManager.getInstance().getTexture("menu-background") : ResourceManager.getInstance().getTextureIfLoaded("::background");
            if (tex != null) {
                float brightness = Config.getBackgroundBrightness();
                float height = tex.getHeight();
                height *= Config.getRES_WIDTH() / (float) tex.getWidth();
                bgSprite = new Sprite(0, (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH(), height, tex);
                bgSprite.setColor(brightness, brightness, brightness);
                scene.setBackground(new SpriteBackground(bgSprite));
            }
        }

        if (!bgset && beatmapData.events.backgroundColor != null) {
            final float bright = Config.getBackgroundBrightness();
            scene.setBackground(new ColorBackground(
                    beatmapData.events.backgroundColor.r() * bright / 255f,
                    beatmapData.events.backgroundColor.g() * bright / 255f,
                    beatmapData.events.backgroundColor.b() * bright / 255f
            ));
        }
    }

    private boolean loadGame(final TrackInfo track, final String rFile) {
        InGameSettingMenu.getInstance().dismiss();
        if (rFile != null && rFile.startsWith("https://")) {
            this.replayFile = Config.getCachePath() + "/" +
                    MD5Calcuator.getStringMD5(rFile) + ".odr";
            Debug.i("ReplayFile = " + replayFile);
            if (!OnlineFileOperator.downloadFile(rFile, this.replayFile)) {
                ToastLogger.showTextId(R.string.replay_cantdownload, true);
                return false;
            }
        } else
            this.replayFile = rFile;

        // Avoid parsing beatmap if the track didn't change.
        if (track != lastTrack) {
            BeatmapParser parser = new BeatmapParser(track.getFilename());
            if (parser.openFile()) {
                beatmapData = parser.parse(true);
            } else {
                Debug.e("startGame: cannot open file");
                ToastLogger.showText(
                        StringTable.format(R.string.message_error_open,
                                track.getFilename()), true);
                return false;
            }
        }

        if (beatmapData == null) {
            return false;
        }

        // TODO skin manager
        SkinManager.getInstance().loadBeatmapSkin(beatmapData.getFolder());

        breakPeriods = new LinkedList<>();
        for (final BreakPeriod period : beatmapData.events.breaks) {
            breakPeriods.add(new BreakPeriod(period.getStart() / 1000f, (period.getStart() + period.getLength()) / 1000f));
        }

        totalOffset = Config.getOffset();
        String beatmapName = track.getFilename();
        beatmapName = beatmapName.substring(0, beatmapName.lastIndexOf('/'));
        final BeatmapProperties props = PropertiesLibrary.getInstance()
                .getProperties(beatmapName);
        if (props != null) {
            totalOffset += props.getOffset();
        }

        try {
            final File musicFile = new File(beatmapData.getFolder(),
                    beatmapData.general.audioFilename);

            if (!musicFile.exists()) {
                throw new FileNotFoundException(musicFile.getPath());
            }

            /*
            if (musicFile.getName().endsWith(".ogg")) {
                totalOffset += Config.getOggOffset();
            } */

            //music = new BassAudioPlayer(musicFile.getPath());
            filePath = musicFile.getPath();

        } catch (final Exception e) {
            Debug.e("Load Music: " + e.getMessage());
            ToastLogger.showText(e.getMessage(), true);
            return false;
        }

        title = beatmapData.metadata.title;
        artist = beatmapData.metadata.artist;
        version = beatmapData.metadata.version;


        scale = (float) ((Config.getRES_HEIGHT() / 480.0f)
                * (54.42 - beatmapData.difficulty.cs * 4.48)
                * 2 / GameObjectSize.BASE_OBJECT_SIZE)
                + 0.5f * Config.getScaleMultiplier();


        float rawApproachRate = beatmapData.difficulty.ar;
        approachRate = (float) GameHelper.ar2ms(rawApproachRate) / 1000f;

        overallDifficulty = beatmapData.difficulty.od;
        drain = beatmapData.difficulty.hp;
        rawDifficulty = overallDifficulty;
        rawDrain = drain;

        if (ModMenu.getInstance().getMod().contains(GameMod.MOD_EASY)) {
            scale += 0.125f;
            drain *= 0.5f;
            overallDifficulty *= 0.5f;
            approachRate = (float) GameHelper.ar2ms(rawApproachRate / 2f) / 1000f;
        }

        GameHelper.setHardrock(false);
        if (ModMenu.getInstance().getMod().contains(GameMod.MOD_HARDROCK)) {
            scale -= 0.125f;
            drain = Math.min(1.4f * drain, 10f);
            overallDifficulty = Math.min(1.4f * overallDifficulty, 10f);
            approachRate = (float) GameHelper.ar2ms(Math.min(1.4f * rawApproachRate, 10f)) / 1000f;
            GameHelper.setHardrock(true);
        }

        timeMultiplier = 1f;
        GameHelper.setDoubleTime(false);
        GameHelper.setNightCore(false);
        GameHelper.setHalfTime(false);
        GameHelper.setSpeedUp(false);

        GlobalManager.getInstance().getSongService().preLoad(filePath, PlayMode.MODE_NONE);
        GameHelper.setTimeMultiplier(1f);
        //Speed Change
        if (ModMenu.getInstance().getChangeSpeed() != 1.00f){
            timeMultiplier = ModMenu.getInstance().getSpeed();
            GlobalManager.getInstance().getSongService().preLoad(filePath, timeMultiplier, ModMenu.getInstance().isEnableNCWhenSpeedChange());
            GameHelper.setTimeMultiplier(1 / timeMultiplier);
        } else if (ModMenu.getInstance().getMod().contains(GameMod.MOD_DOUBLETIME)) {
            GlobalManager.getInstance().getSongService().preLoad(filePath, PlayMode.MODE_DT);
            /*music.setUseSoftDecoder(1);
            music.setDecoderMultiplier(150);*/
            timeMultiplier = 1.5f;
            GameHelper.setDoubleTime(true);
            GameHelper.setTimeMultiplier(2 / 3f);
        } else if (ModMenu.getInstance().getMod().contains(GameMod.MOD_NIGHTCORE)) {
            GlobalManager.getInstance().getSongService().preLoad(filePath, PlayMode.MODE_NC);
            /*music.setUseSoftDecoder(2);
            music.setDecoderMultiplier(150);*/
            timeMultiplier = 1.5f;
            GameHelper.setNightCore(true);
            GameHelper.setTimeMultiplier(2 / 3f);
        } else if (ModMenu.getInstance().getMod().contains(GameMod.MOD_HALFTIME)) {
            GlobalManager.getInstance().getSongService().preLoad(filePath, PlayMode.MODE_HT);
            /*music.setUseSoftDecoder(1);
            music.setDecoderMultiplier(75);*/
            timeMultiplier = 0.75f;
            GameHelper.setHalfTime(true);
            GameHelper.setTimeMultiplier(4 / 3f);
        }

        if (ModMenu.getInstance().getMod().contains(GameMod.MOD_REALLYEASY)) {
            scale += 0.125f;
            drain *= 0.5f;
            overallDifficulty *= 0.5f;
            float ar = (float)GameHelper.ms2ar(approachRate * 1000f);
            if (ModMenu.getInstance().getMod().contains(GameMod.MOD_EASY)) {
                ar *= 2;
                ar -= 0.5f;
            }
            ar -= (timeMultiplier - 1.0f) + 0.5f;
            approachRate = (float)(GameHelper.ar2ms(ar) / 1000f);
        }

        if (ModMenu.getInstance().getMod().contains(GameMod.MOD_SMALLCIRCLE)) {
            scale -= (float) ((Config.getRES_HEIGHT() / 480.0f) * (4 * 4.48)
            * 2 / GameObjectSize.BASE_OBJECT_SIZE);
        }
        //Force AR
        if (ModMenu.getInstance().isEnableForceAR()){
            approachRate = (float) GameHelper.ar2ms(ModMenu.getInstance().getForceAR()) / 1000f * timeMultiplier;
        }

        GameHelper.setRelaxMod(ModMenu.getInstance().getMod().contains(GameMod.MOD_RELAX));
        GameHelper.setAutopilotMod(ModMenu.getInstance().getMod().contains(GameMod.MOD_AUTOPILOT));
        GameHelper.setAuto(ModMenu.getInstance().getMod().contains(GameMod.MOD_AUTO));

        GameHelper.setStackLeniency(beatmapData.general.stackLeniency);
        if (scale < 0.001f){
            scale = 0.001f;
        }
        GameHelper.setSpeed((float) beatmapData.difficulty.sliderMultiplier * 100);
        GameHelper.setTickRate((float) beatmapData.difficulty.sliderTickRate);
        GameHelper.setScale(scale);
        GameHelper.setDifficulty(overallDifficulty);
        GameHelper.setDrain(drain);
        GameHelper.setApproachRate(approachRate);

        // Parsing hit objects
        objects = new LinkedList<>();
        for (final String s : beatmapData.rawHitObjects) {
            objects.add(new GameObjectData(s));
        }

        if (objects.size() <= 0) {
            ToastLogger.showText("Empty Beatmap", true);
            return false;
        }

        activeObjects = new LinkedList<>();
        passiveObjects = new LinkedList<>();
        lastObjectId = -1;

        GameHelper.setSliderColor(SkinManager.getInstance().getSliderColor());
        if (beatmapData.colors.sliderBorderColor != null) {
            GameHelper.setSliderColor(beatmapData.colors.sliderBorderColor);
        }

        if (OsuSkin.get().isForceOverrideSliderBorderColor()) {
            GameHelper.setSliderColor(new RGBColor(OsuSkin.get().getSliderBorderColor()));
        }

        combos = new ArrayList<>();
        for (RGBColor color : beatmapData.colors.comboColors) {
            combos.add(new RGBColor(color.r() / 255, color.g() / 255, color.b() / 255));
        }

        if (combos.isEmpty() || Config.isUseCustomComboColors()) {
            combos.clear();
            combos.addAll(Arrays.asList(Config.getComboColors()));
        }
        if (OsuSkin.get().isForceOverrideComboColor()) {
            combos.clear();
            combos.addAll(OsuSkin.get().getComboColor());
        }
        comboNum = -1;
        currentComboNum = 0;
        lastObjectHitTime = 0;
        final SampleBank defSound = beatmapData.general.sampleBank;
        if (defSound == SampleBank.soft) {
            TimingPoint.setDefaultSound("soft");
        } else {
            TimingPoint.setDefaultSound("normal");
        }
        timingPoints = new LinkedList<>();
        activeTimingPoints = new LinkedList<>();
        currentTimingPoint = null;
        for (final String s : beatmapData.rawTimingPoints) {
            final TimingPoint tp = new TimingPoint(s.split("[,]"),
                    currentTimingPoint);
            timingPoints.add(tp);
            if (!tp.wasInderited()) {
                currentTimingPoint = tp;
            }
        }

        if (currentTimingPoint == null) {
            // No uninherited timing points, use default uninherited timing point.
            currentTimingPoint = new TimingPoint(
                    // https://osu.ppy.sh/wiki/en/Client/File_formats/Osu_%28file_format%29#timing-points
                    new String[] {"0", "1000", "4", "0", "0", "100", "1", "0"},
                    null
            );

            for (final String s : beatmapData.rawTimingPoints) {
                final TimingPoint tp = new TimingPoint(s.split("[,]"),
                        currentTimingPoint);
                timingPoints.add(tp);
                if (!tp.wasInderited()) {
                    currentTimingPoint = tp;
                }
            }
        }

        GameHelper.controlPoints = new ControlPoints();
        GameHelper.controlPoints.load(TimingPoints.parse(beatmapData.rawTimingPoints));
        currentTimingPoint = timingPoints.peek();
        firstTimingPoint = currentTimingPoint;
        soundTimingPoint = currentTimingPoint;
        if (soundTimingPoint != null) {
            GameHelper.setTimingOffset(soundTimingPoint.getTime());
            GameHelper.setBeatLength(soundTimingPoint.getBeatLength() * GameHelper.getSpeed() / 100f);
            GameHelper.setTimeSignature(soundTimingPoint.getSignature());
            GameHelper.setKiai(soundTimingPoint.isKiai());
        } else {
            GameHelper.setTimingOffset(0);
            GameHelper.setBeatLength(1);
            GameHelper.setTimeSignature(4);
            GameHelper.setKiai(false);
        }
        GameHelper.setInitalBeatLength(GameHelper.getBeatLength());

        GameObjectPool.getInstance().purge();
        SpritePool.getInstance().purge();
        ModifierFactory.clear();

        // TODO replay
        avgOffset = 0;
        offsetRegs = 0;

        File trackFile = new File(track.getFilename());
        trackMD5 = track.getMD5();
        replaying = false;
        replay = new Replay();
        replay.setObjectCount(objects.size());
        replay.setMap(trackFile.getParentFile().getName(), trackFile.getName(), trackMD5);

        if (replayFile != null) {
            replaying = replay.load(replayFile);
            if (!replaying) {
                ToastLogger.showTextId(R.string.replay_invalid, true);
                return false;
            } else
                replay.countMarks(overallDifficulty);
        } else if (ModMenu.getInstance().getMod().contains(GameMod.MOD_AUTO)) {
            replay = null;
        }

        //TODO online
        if (!replaying)
            OnlineScoring.getInstance().startPlay(track, trackMD5);

        if (Config.isEnableStoryboard()) {
            storyboardSprite.loadStoryboard(track.getFilename());
        }

        System.gc();
        GameObjectPool.getInstance().preload();

        ppText = null;
        if (Config.isDisplayRealTimePPCounter()) {
            // Calculate timed difficulty attributes
            DifficultyCalculationParameters parameters = new DifficultyCalculationParameters();

            parameters.mods = ModMenu.getInstance().getMod().clone();
            parameters.customSpeedMultiplier = ModMenu.getInstance().getChangeSpeed();

            if (ModMenu.getInstance().isEnableForceAR()) {
                parameters.forcedAR = ModMenu.getInstance().getForceAR();
            }

            timedDifficultyAttributes = BeatmapDifficultyCalculator.calculateTimedDifficulty(
                    beatmapData,
                    parameters
            );
        } else {
            timedDifficultyAttributes.clear();
        }

        lastTrack = track;
        if (Config.isCalculateSliderPathInGameStart()){
            stackNotes();
            calculateAllSliderPaths();
        }

        // Resetting variables before starting the game.
        Multiplayer.clearLeaderboard();
        hasFailed = false;
        backPressCount = 0;
        lastTimeBackPress = -1f;
        isSkipRequested = false;
        realTimeElapsed = 0;

        paused = false;
        gameStarted = false;
        return true;
    }

    public Scene getScene() {
        return scene;
    }

    public void restartGame() {
        if (!replaying) {
            EdExtensionHelper.onRestartGame(lastTrack);
        }
        startGame(null, null);
    }

    public void startGame(final TrackInfo track, final String replayFile) {
        GameHelper.updateGameid();
        if (!replaying) {
            EdExtensionHelper.onStartGame(track);
        }

        scene = new Scene();
        if (Config.isEnableStoryboard()) {
            if (storyboardSprite == null) {
                storyboardSprite = new StoryboardSprite(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                storyboardOverlayProxy = new ProxySprite(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                storyboardSprite.setOverlayDrawProxy(storyboardOverlayProxy);
            } else {
                storyboardSprite.detachSelf();
            }

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
        isFirst = true;
        failcount = 0;
        mainCursorId = -1;
        final LoadingScreen screen = new LoadingScreen();
        engine.setScene(screen.getScene());

        final String rfile = track != null ? replayFile : this.replayFile;

        new AsyncTask() {
            @Override
            public void run() {
                loadComplete = loadGame(track != null ? track : lastTrack, rfile);
            }

            @Override
            public void onComplete() {
                if (loadComplete) {
                    prepareScene();
                } else {
                    ModMenu.getInstance().setMod(Replay.oldMod);
                    ModMenu.getInstance().setChangeSpeed(Replay.oldChangeSpeed);
                    ModMenu.getInstance().setForceAR(Replay.oldForceAR);
                    ModMenu.getInstance().setEnableForceAR(Replay.oldEnableForceAR);
                    ModMenu.getInstance().setFLfollowDelay(Replay.oldFLFollowDelay);
                    quit();
                }
            }
        }.execute();

        ResourceManager.getInstance().getSound("failsound").stop();
    }

    private void prepareScene() {
        scene.setOnSceneTouchListener(this);
        // bgScene.attachChild(mVideo);
        if (GlobalManager.getInstance().getCamera() instanceof SmoothCamera) {
            SmoothCamera camera = (SmoothCamera) (GlobalManager.getInstance().getCamera());
            camera.setZoomFactorDirect(Config.getPlayfieldSize());
            if (Config.isShrinkPlayfieldDownwards()) {
                camera.setCenterDirect(Config.getRES_WIDTH() / 2, Config.getRES_HEIGHT() / 2 * Config.getPlayfieldSize());
            }
        }
        setBackground();

        if (Config.isShowFPS() || Config.isDisplayRealTimePPCounter()) {
            final Font font = ResourceManager.getInstance().getFont(
                    "smallFont");
            final ChangeableText fpsText = new ChangeableText(Utils.toRes(790),
                    Utils.toRes(520), font, "00.00 FPS");
            final ChangeableText urText = new ChangeableText(Utils.toRes(720),
                    Utils.toRes(480), font, "00.00 UR    ");
            final ChangeableText accText = new ChangeableText(Utils.toRes(720),
                    Utils.toRes(440), font, "Avg offset: 0ms     ");
            fpsText.setPosition(Config.getRES_WIDTH() - fpsText.getWidth() - 5, Config.getRES_HEIGHT() - fpsText.getHeight() - 10);
            accText.setPosition(Config.getRES_WIDTH() - accText.getWidth() - 5, fpsText.getY() - accText.getHeight());
            urText.setPosition(Config.getRES_WIDTH() - urText.getWidth() - 5, accText.getY() - urText.getHeight());
            fgScene.attachChild(fpsText);
            fgScene.attachChild(accText);
            fgScene.attachChild(urText);

            if (Config.isDisplayRealTimePPCounter()) {
                ppText = new ChangeableText(Utils.toRes(720),
                        Utils.toRes(440), font, "0.00pp", 100);
                fgScene.attachChild(ppText);
            }

            ChangeableText memText = null;
            if (BuildConfig.DEBUG) {
                memText = new ChangeableText(Utils.toRes(780),
                        Utils.toRes(520), font, "0 MB/0 MB    ");
                fgScene.attachChild(memText);
            }

            final ChangeableText fmemText = memText;
            fgScene.registerUpdateHandler(new FPSCounter() {
                int elapsedInt = 0;
                @Override
                public void onUpdate(final float pSecondsElapsed) {
                    super.onUpdate(pSecondsElapsed);
                    elapsedInt++;
                    fpsText.setText(Math.round(this.getFPS()) + " FPS");
                    if (offsetRegs != 0 && elapsedInt > 200) {
                        float mean = avgOffset / offsetRegs;
                        accText.setText("Avg offset: "
                                + (int) (mean * 1000f)
                                + "ms");
                        elapsedInt = 0;
                    }
                    urText.setText(String.format(Locale.ENGLISH, "%.2f UR    ", stat.getUnstableRate()));

                    fpsText.setPosition(Config.getRES_WIDTH() - fpsText.getWidth() - 5, Config.getRES_HEIGHT() - fpsText.getHeight() - 10);
                    accText.setPosition(Config.getRES_WIDTH() - accText.getWidth() - 5, fpsText.getY() - accText.getHeight());
                    urText.setPosition(Config.getRES_WIDTH() - urText.getWidth() - 5, accText.getY() - urText.getHeight());
                    if (ppText != null) {
                        ppText.setPosition(Config.getRES_WIDTH() - ppText.getWidth() - 5, urText.getY() - ppText.getHeight());
                    }

                    if (fmemText != null) {
                        Runtime runtime = Runtime.getRuntime();
                        fmemText.setText(
                            ((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024) + " MB"
                            + "/" + (runtime.totalMemory() / 1024 / 1024) + " MB    ");
                        fmemText.setPosition(
                                Config.getRES_WIDTH() - fmemText.getWidth() - 5,
                                (ppText != null ? ppText : urText).getY() - fmemText.getHeight()
                        );
                    }

                }
            });
        }

        stat = new StatisticV2();
        stat.setMod(ModMenu.getInstance().getMod());
        mIsAuto = stat.getMod() != null && stat.getMod().contains(GameMod.MOD_AUTO);

        float multiplier = 1 + rawDifficulty / 10f + rawDrain / 10f;
        multiplier += (beatmapData.difficulty.cs - 3) / 4f;

        stat.setDiffModifier(multiplier);
        stat.setMaxObjectsCount(lastTrack.getTotalHitObjectCount());
        stat.setMaxHighestCombo(lastTrack.getMaxCombo());
        stat.setEnableForceAR(ModMenu.getInstance().isEnableForceAR());
        stat.setForceAR(ModMenu.getInstance().getForceAR());
        stat.setChangeSpeed(ModMenu.getInstance().getChangeSpeed());
        stat.setFLFollowDelay(ModMenu.getInstance().getFLfollowDelay());

        GameHelper.setHardrock(stat.getMod().contains(GameMod.MOD_HARDROCK));
        GameHelper.setDoubleTime(stat.getMod().contains(GameMod.MOD_DOUBLETIME));
        GameHelper.setNightCore(stat.getMod().contains(GameMod.MOD_NIGHTCORE));
        GameHelper.setSpeedUp(stat.getMod().contains(GameMod.MOD_SPEEDUP));
        GameHelper.setHalfTime(stat.getMod().contains(GameMod.MOD_HALFTIME));
        GameHelper.setHidden(stat.getMod().contains(GameMod.MOD_HIDDEN));
        GameHelper.setFlashLight(stat.getMod().contains(GameMod.MOD_FLASHLIGHT));
        GameHelper.setRelaxMod(stat.getMod().contains(GameMod.MOD_RELAX));
        GameHelper.setAutopilotMod(stat.getMod().contains(GameMod.MOD_AUTOPILOT));
        GameHelper.setSuddenDeath(stat.getMod().contains(GameMod.MOD_SUDDENDEATH));
        GameHelper.setPerfect(stat.getMod().contains(GameMod.MOD_PERFECT));
        GameHelper.setScoreV2(stat.getMod().contains(GameMod.MOD_SCOREV2));
        difficultyHelper = stat.getMod().contains(GameMod.MOD_PRECISE) ?
                DifficultyHelper.HighDifficulty : DifficultyHelper.StdDifficulty;
        GameHelper.setDifficultyHelper(difficultyHelper);

        for (int i = 0; i < CursorCount; i++) {
            cursors[i] = new Cursor();
            cursors[i].mouseDown = false;
            cursors[i].mousePressed = false;
            cursors[i].mouseOldDown = false;
        }

        for (int i = 0; i < CursorCount; i++) {
            cursorIIsDown[i] = false;
        }

        comboWas100 = false;
        comboWasMissed = false;

        final int leadIn = beatmapData.general.audioLeadIn;
        secPassed = -leadIn / 1000f;
        if (secPassed > -1) {
            secPassed = -1;
        }

        if (!objects.isEmpty()) {
            skipTime = objects.peek().getTime() - approachRate - 1f;
        } else {
            skipTime = 0;
        }

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

        final BeatmapCountdown countdown = beatmapData.general.countdown;
        if (Config.isCorovans() && countdown != null) {
            float cdSpeed = countdown.speed;
            skipTime -= cdSpeed * Countdown.COUNTDOWN_LENGTH;
            if (cdSpeed != 0 && objects.peek().getTime() - secPassed >= cdSpeed * Countdown.COUNTDOWN_LENGTH) {
                addPassiveObject(new Countdown(this, bgScene, cdSpeed, 0, objects.peek().getTime() - secPassed));
            }
        }

        float lastObjectTime = 0;
        if (!objects.isEmpty())
            lastObjectTime = objects.getLast().getTime();

        if(!Config.isHideInGameUI()) { 
            progressBar = new SongProgressBar(this, fgScene, lastObjectTime, objects
                    .getFirst().getTime(), new PointF(0, Config.getRES_HEIGHT() - 7), Config.getRES_WIDTH(), 7);
            progressBar.setProgressRectColor(new RGBAColor(153f / 255f, 204f / 255f, 51f / 255f, 0.4f));
        }

        if (Config.getErrorMeter() == 1
                || (Config.getErrorMeter() == 2 && replaying)) {
            hitErrorMeter = new HitErrorMeter(
                    fgScene,
                    new PointF(Config.getRES_WIDTH() / 2, Config.getRES_HEIGHT() - 20),
                    overallDifficulty,
                    12,
                    difficultyHelper);
        }

        skipBtn = null;
        if (skipTime > 1) {
            final TextureRegion tex;
            if (ResourceManager.getInstance().isTextureLoaded("play-skip-0")) {
                List<String> loadedSkipTextures = new ArrayList<>();
                for (int i = 0; i < 60; i++) {
                    if (ResourceManager.getInstance().isTextureLoaded("play-skip-" + i))
                        loadedSkipTextures.add("play-skip-" + i);
                }
                tex = ResourceManager.getInstance().getTexture("play-skip-0");
                skipBtn = new AnimSprite(Config.getRES_WIDTH() - tex.getWidth(),
                        Config.getRES_HEIGHT() - tex.getHeight(), loadedSkipTextures.size(),
                        loadedSkipTextures.toArray(new String[loadedSkipTextures.size()]));
            } else {
                tex = ResourceManager.getInstance().getTexture("play-skip");
                skipBtn = new Sprite(Config.getRES_WIDTH() - tex.getWidth(),
                        Config.getRES_HEIGHT() - tex.getHeight(), tex);
            }
            skipBtn.setAlpha(0.7f);
            fgScene.attachChild(skipBtn);
        }
        GameHelper.setGlobalTime(0);

        float effectOffset = 155 - 25;
        breakAnimator = new BreakAnimator(this, fgScene, stat, beatmapData.general.letterboxInBreaks, bgSprite);
        if(!Config.isHideInGameUI()){
            scorebar = new ScoreBar(this, fgScene, stat);
            addPassiveObject(scorebar);
            final TextureRegion scoreDigitTex = ResourceManager.getInstance()
                    .getTexture("score-0");
            accText = new GameScoreText(OsuSkin.get().getScorePrefix(), Config.getRES_WIDTH()
                    - scoreDigitTex.getWidth() * 4.75f, 50,
                    "000.00%", 0.6f);
            comboText = new GameScoreText(OsuSkin.get().getComboPrefix(), Utils.toRes(2), Config.getRES_HEIGHT()
                    - Utils.toRes(95), "0000x", 1.5f);
            comboText.changeText(new StringBuilder("0****"));
            scoreText = new GameScoreText(OsuSkin.get().getScorePrefix(), Config.getRES_WIDTH()
                    - scoreDigitTex.getWidth() * 7.25f, 0, "0000000000", 0.9f);
            comboText.attachToScene(fgScene);
            accText.attachToScene(fgScene);
            scoreText.attachToScene(fgScene);
            if (Config.isComplexAnimations()) {
                scoreShadow = new GameScoreTextShadow(0, Config.getRES_HEIGHT()
                        - Utils.toRes(90), "0000x", 1.5f);
                scoreShadow.attachToScene(bgScene);
                passiveObjects.add(scoreShadow);
            }
            if (stat.getMod().contains(GameMod.MOD_AUTO)) {
                final Sprite autoIcon = new Sprite(Utils.toRes(Config.getRES_WIDTH() - 140),
                        Utils.toRes(100), ResourceManager.getInstance().getTexture(
                        "selection-mod-autoplay"));
                bgScene.attachChild(autoIcon);
                effectOffset += 25;
            } else if (stat.getMod().contains(GameMod.MOD_RELAX)) {
                final Sprite autoIcon = new Sprite(Utils.toRes(Config.getRES_WIDTH() - 140),
                        Utils.toRes(98), ResourceManager.getInstance().getTexture(
                        "selection-mod-relax"));
                bgScene.attachChild(autoIcon);
                effectOffset += 25;
            } else if (stat.getMod().contains(GameMod.MOD_AUTOPILOT)) {
                final Sprite autoIcon = new Sprite(Utils.toRes(Config.getRES_WIDTH() - 140),
                        Utils.toRes(98), ResourceManager.getInstance().getTexture(
                        "selection-mod-relax2"));
                bgScene.attachChild(autoIcon);
                effectOffset += 25;
            }
    
            if (Config.isComboburst()) {
                comboBurst = new ComboBurst(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                comboBurst.attachAll(bgScene);
            }
        }

        float timeOffset = 0;
        if (stat.getMod().contains(GameMod.MOD_SCOREV2)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-scorev2");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2), new ParallelEntityModifier(
                            ModifierFactory.newFadeOutModifier(0.5f),
                            ModifierFactory.newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_EASY)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-easy");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2), new ParallelEntityModifier(
                            ModifierFactory.newFadeOutModifier(0.5f),
                            ModifierFactory.newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        } else if (stat.getMod().contains(GameMod.MOD_HARDROCK)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-hardrock");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2), new ParallelEntityModifier(
                            ModifierFactory.newFadeOutModifier(0.5f),
                            ModifierFactory.newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_NOFAIL)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-nofail");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_HIDDEN)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-hidden");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }

        if (stat.getMod().contains(GameMod.MOD_DOUBLETIME)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-doubletime");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_NIGHTCORE)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-nightcore");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_HALFTIME)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-halftime");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_PRECISE)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-precise");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_SUDDENDEATH)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-suddendeath");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        else if (stat.getMod().contains(GameMod.MOD_PERFECT)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-perfect");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_FLASHLIGHT)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-flashlight");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_SMALLCIRCLE)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-smallcircle");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }
        if (stat.getMod().contains(GameMod.MOD_REALLYEASY)) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "selection-mod-reallyeasy");
            effect.init(
                    fgScene,
                    new PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset), Utils
                            .toRes(130)),
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, 1.2f, 1), ModifierFactory
                            .newDelayModifier(2 - timeOffset),
                            new ParallelEntityModifier(ModifierFactory
                                    .newFadeOutModifier(0.5f), ModifierFactory
                                    .newScaleModifier(0.5f, 1, 1.5f))));
            effectOffset += 25;
            timeOffset += 0.25f;
        }

        kiaiRect = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT());
        kiaiRect.setVisible(false);
        kiaiRect.setColor(1, 1, 1);
        bgScene.attachChild(kiaiRect, 0);

        unranked = new Sprite(0, 0, ResourceManager.getInstance().getTexture("play-unranked"));
        unranked.setPosition(Config.getRES_WIDTH() / 2 - unranked.getWidth() / 2, 80);
        unranked.setVisible(false);
        fgScene.attachChild(unranked);

        boolean hasUnrankedMod = SmartIterator.wrap(stat.getMod().iterator())
            .applyFilter(m -> m.unranked).hasNext();
        if (hasUnrankedMod
                || Config.isRemoveSliderLock()
                || ModMenu.getInstance().isEnableForceAR()
                || !ModMenu.getInstance().isDefaultFLFollowDelay()) {
            unranked.setVisible(true);
        }

        String playname = null;
        replayText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("font"), "", 1000);
        replayText.setVisible(false);
        replayText.setPosition(0, 140);
        replayText.setAlpha(0.7f);
        fgScene.attachChild(replayText, 0);
        if (stat.getMod().contains(GameMod.MOD_AUTO) || replaying) {
            playname = replaying ? GlobalManager.getInstance().getScoring().getReplayStat().getPlayerName() : "osu!";
            replayText.setText("Watching " + playname + " play " + artist + " - " + title + " [" + version + "]");
            replayText.registerEntityModifier(new LoopEntityModifier(new MoveXModifier(40,
                    Config.getRES_WIDTH() + 5, -replayText.getWidth() - 5)));
            replayText.setVisible(!Config.isHideReplayMarquee());
        }
        if (Config.isShowScoreboard()) {
            scoreBoard = new DuringGameScoreBoard(fgScene, stat, playname);
            addPassiveObject(scoreBoard);
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

        leadOut = 0;
        musicStarted = false;
        engine.setScene(scene);
        scene.registerUpdateHandler(this);
    }

    // This is used by the multiplayer system, is called once all players in the room completes beatmap loading.
    public void start() {

        if (skipTime <= 1)
            RoomScene.INSTANCE.getChat().dismiss();

        leadOut = 0;
        musicStarted = false;
        engine.setScene(scene);
        scene.registerUpdateHandler(this);
    }

    public RGBColor getComboColor(int num) {
        return combos.get(num % combos.size());
    }

    // TODO update

    public void onUpdate(final float pSecondsElapsed) {
        previousFrameTime = SystemClock.uptimeMillis();
        Utils.clearSoundMask();
        float dt = pSecondsElapsed * timeMultiplier;
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

        if (Multiplayer.isMultiplayer)
        {
            realTimeElapsed += pSecondsElapsed * 1000;

            // Sending statistics data every 3000ms
            if (realTimeElapsed > 3000 && !breakAnimator.isBreak())
            {
                realTimeElapsed = 0;

                Execution.async(() -> {
                    // This can happen when the player disconnects while playing
                    if (Multiplayer.isConnected)
                        RoomAPI.submitLiveScore(stat.toJson(true));
                    return null;
                });
            }

            // Setting a delay of 300ms for the player to tap back button again.
            if (lastTimeBackPress > 0f && lastTimeBackPress - pSecondsElapsed * 1000f > 300)
            {
                lastTimeBackPress = -1f;
                backPressCount = 0;
            }
        }

        float gtime;
        if (soundTimingPoint == null || soundTimingPoint.getTime() > secPassed) {
            gtime = 0;
        } else {
            gtime = (secPassed - firstTimingPoint.getTime())
                    % (GameHelper.getKiaiTickLength());
        }
        GameHelper.setGlobalTime(gtime);

        if (Config.isEnableStoryboard()) {
            if (storyboardSprite != null) {
                storyboardSprite.updateTime(secPassed * 1000);
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
                cursorSprites[i].setPosition(cursors[i].mousePos.x, cursors[i].mousePos.y);
                cursorSprites[i].update(dt);
                if (cursors[i].mouseDown) {
                    cursorSprites[i].setShowing(true);
                    if (cursors[i].mousePressed) {
                        cursorSprites[i].click();
                    }
                } else {
                    cursorSprites[i].setShowing(false);
                }
            }
        }

        for (final Cursor c : cursors) {
            if (c.mouseDown == true && c.mouseOldDown == false) {
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
                } else if(!cursors[mainCursorId].mouseDown){
                    mainCursorId = -1;
                } else if(cursors[mainCursorId].mouseDown){
                    flashlightSprite.onMouseMove(
                            cursors[mainCursorId].mousePos.x, cursors[mainCursorId].mousePos.y
                    );
                }
            }
            flashlightSprite.onUpdate(stat.getCombo());
        }

        while (timingPoints.isEmpty() == false
                && timingPoints.peek().getTime() <= secPassed + approachRate) {
            currentTimingPoint = timingPoints.poll();
            activeTimingPoints.add(currentTimingPoint);
        }
        while (activeTimingPoints.isEmpty() == false
                && activeTimingPoints.peek().getTime() <= secPassed) {
            soundTimingPoint = activeTimingPoints.poll();
            if (!soundTimingPoint.inherited) {
                GameHelper.setBeatLength(soundTimingPoint.getBeatLength());
                GameHelper.setTimingOffset(soundTimingPoint.getTime());
            }
            GameHelper.setTimeSignature(soundTimingPoint.getSignature());
            GameHelper.setKiai(soundTimingPoint.isKiai());
        }

        if (!breakPeriods.isEmpty()) {
            if (!breakAnimator.isBreak()
                    && breakPeriods.peek().getStart() <= secPassed) {
                gameStarted = false;
                breakAnimator.init(breakPeriods.peek().getLength());
                if(GameHelper.isFlashLight()){
                    flashlightSprite.onBreak(true);
                }

                if (Multiplayer.isConnected)
                    RoomScene.INSTANCE.getChat().show();

                if(scorebar != null) scorebar.setVisible(false);
                breakPeriods.poll();
            }
        }
        if (breakAnimator.isOver()) {
            gameStarted = true;
            if(scorebar != null) scorebar.setVisible(true);
            if(GameHelper.isFlashLight()){
                flashlightSprite.onBreak(false);
            }
        }

        if (objects.isEmpty() && activeObjects.isEmpty()) {
            if (GameHelper.isFlashLight()) {
                flashlightSprite.onBreak(true);
            }
        }

        if (gameStarted) {
            float rate = 0.375f;
            if (drain > 0 && distToNextObject > 0) {
                rate = 1 + drain / (2f * distToNextObject);
            }
            stat.changeHp(-rate * 0.01f * dt);
            if (stat.getHp() <= 0
                    && stat.getMod().contains(GameMod.MOD_NOFAIL) == false
                    && stat.getMod().contains(GameMod.MOD_RELAX) == false
                    && stat.getMod().contains(GameMod.MOD_AUTOPILOT) == false
                    && stat.getMod().contains(GameMod.MOD_AUTO) == false) {
                if (stat.getMod().contains(GameMod.MOD_EASY) && failcount < 3) {
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
            //连击数////////////////////////
            final StringBuilder comboBuilder = new StringBuilder();
            comboBuilder.setLength(0);
            comboBuilder.append(stat.getCombo());
            while (comboBuilder.length() < 5) {
                comboBuilder.append('*');
            }
            if (Config.isComplexAnimations()) {
                scoreShadow.changeText(comboBuilder);
                scoreShadow.registerEntityModifier(new DelayModifier(0.2f, new IEntityModifier.IEntityModifierListener() {
                    @Override
                    public void onModifierStarted(IModifier<IEntity> iModifier, IEntity iEntity) { 
                    }
                    @Override
                    public void onModifierFinished(IModifier<IEntity> iModifier, IEntity iEntity) {
                        //当CB数字阴影缩小完成的时候 更改CB数字
                        comboText.changeText(comboBuilder);
                    }
                }));
            } else {
                comboText.changeText(comboBuilder);
            }

            //连击数////////////////////////
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
            if (strBuilder.length() < 6) {
                strBuilder.insert(0, '*');
            }
            accText.changeText(strBuilder);
            strBuilder.setLength(0);
            strBuilder.append(stat.getAutoTotalScore());
            while (strBuilder.length() < 8) {
                strBuilder.insert(0, '0');
            }
            int scoreTextOffset = 0;
            while (strBuilder.length() < 10) {
                strBuilder.insert(0, '*');
                scoreTextOffset++;
            }
    
            scoreText.setPosition(Config.getRES_WIDTH()
                    - ResourceManager.getInstance().getTextureWithPrefix(OsuSkin.get().getScorePrefix(), "0").getWidth() * (9.25f - scoreTextOffset), 0);
            scoreText.changeText(strBuilder);
        }

        if (comboBurst != null) {
            if (stat.getCombo() == 0) {
                comboBurst.breakCombo();
            } else {
                comboBurst.checkAndShow(stat.getCombo());
            }
        }

        for (final GameObject obj : passiveObjects) {
            obj.update(dt);
        }

        if (Config.isRemoveSliderLock()){
            GameObject lastObject = getLastTobeclickObject();
            if (lastObject != null) {
                lastObjectHitTime = getLastTobeclickObject().getHitTime();
            }
        }

        for (final GameObject obj : activeObjects) {
            obj.update(dt);
        }

        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
            autoCursor.moveToObject(activeObjects.peek(), secPassed, approachRate, this);
        }

        int clickCount = 0;
        for (final boolean c : cursorIIsDown){
            if (c == true) clickCount++;
        }
        for (int i = 0; i < CursorCount; i++) {
            cursorIIsDown[i] = false;
        }

        for (int i = 0; i < clickCount - 1; i++){
            if (Config.isRemoveSliderLock()){
                GameObject lastObject = getLastTobeclickObject();
                if (lastObject != null) {
                    lastObjectHitTime = getLastTobeclickObject().getHitTime();
                }
            }
            for (final GameObject obj : activeObjects) {
                obj.tryHit(dt);
            }
        }

        if (secPassed >= 0 && musicStarted == false) {
            GlobalManager.getInstance().getSongService().play();
            GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
            totalLength = GlobalManager.getInstance().getSongService().getLength();
            musicStarted = true;
            secPassed = 0;
            return;
        }
//        if (secPassed >= 0) {
//            if (!mVideo.getIsPlaying()) {
//                mVideo.play();
//            }
//            if (mVideo.getAlpha() < 1.0f) {
//                float alpha = mVideo.getAlpha();
//                mVideo.setAlpha(Math.min(alpha + 0.03f, 1.0f));
//            }
//        }

        boolean shouldBePunished = false;

        while (objects.isEmpty() == false
                && secPassed + approachRate > objects.peek().getTime()) {
            gameStarted = true;
            final GameObjectData data = objects.poll();
            final String[] params = data.getData();

            final PointF pos = data.getPos();
            // Fix matching error on new beatmaps
            final int objDefine = Integer.parseInt(params[3]);

            final float time = data.getRawTime();
            if (time > totalLength) {
                shouldBePunished = true;
            }

            // Stack notes
            // If Config.isCalculateSliderPathInGameStart(), do this in stackNotes()
            if (Config.isCalculateSliderPathInGameStart() == false &&
                objects.isEmpty() == false && (objDefine & 1) > 0) {
                if (objects.peek().getTime() - data.getTime() < 2f * GameHelper.getStackLeniency()
                        && Utils.squaredDistance(pos, objects.peek().getPos()) < scale) {
                    objects.peek().setPosOffset(
                            data.getPosOffset() + Utils.toRes(4) * scale);
                }
            }
            // If this object is silder and isCalculateSliderPathInGameStart(), the pos is += in calculateAllSliderPaths()
            if (Config.isCalculateSliderPathInGameStart() == false || (objDefine & 2) <= 0){
                pos.x += data.getPosOffset();
                pos.y += data.getPosOffset();
            }
            if (objects.isEmpty() == false) {
                distToNextObject = objects.peek().getTime() - data.getTime();
                if (soundTimingPoint != null
                        && distToNextObject < soundTimingPoint.getBeatLength() / 2) {
                    distToNextObject = soundTimingPoint.getBeatLength() / 2;
                }
            } else {
                distToNextObject = 0;
            }
            // Calculate combo color
            int comboCode = objDefine;
            if (comboCode == 12) {
                currentComboNum = 0;
            } else if (comboNum == -1) {
                comboNum = 1;
                currentComboNum = 0;
            } else if ((comboCode & 4) > 0) {
                currentComboNum = 0;
                if (comboCode / 15 > 0) {
                    comboCode /= 15;
                    for (int i = 0; true; i++) {
                        if (comboCode >> i == 1) {
                            comboNum = i;
                            break;
                        }
                    }
                } else {
                    comboNum = (comboNum + 1) % combos.size();
                }
            }

            if ((objDefine & 1) > 0) {
                final RGBColor col = getComboColor(comboNum);
                final HitCircle circle = GameObjectPool.getInstance().getCircle();
                String tempSound = null;
                if (params.length > 5) {
                    tempSound = params[5];
                }

                circle.init(this, mgScene, pos, data.getTime() - secPassed,
                        col.r(), col.g(), col.b(), scale, currentComboNum,
                        Integer.parseInt(params[4]), tempSound, isFirst);
                circle.setEndsCombo(objects.isEmpty()
                        || objects.peek().isNewCombo());
                addObject(circle);
                isFirst = false;
                if (objects.isEmpty() == false
                        && objects.peek().isNewCombo() == false) {
                    final FollowTrack track = GameObjectPool.getInstance()
                            .getTrack();
                    PointF end;
                    if (objects.peek().getTime() > data.getTime()) {
                        end = data.getEnd();
                    } else {
                        end = data.getPos();
                    }
                    track.init(this, bgScene, end, objects.peek().getPos(),
                            objects.peek().getTime() - secPassed, approachRate,
                            scale);
                }
                if (stat.getMod().contains(GameMod.MOD_AUTO)) {
                    circle.setAutoPlay();
                }
                circle.setHitTime(data.getTime());

                if (objects.isEmpty() == false) {
                    if (objects.peek().getTime() > data.getTime()) {
                        currentComboNum++;
                    }
                }

                circle.setId(++lastObjectId);
                if (replaying) {
                    circle.setReplayData(replay.objectData[circle.getId()]);
                }

            } else if ((objDefine & 8) > 0) {
                final float endTime = Integer.parseInt(params[5]) / 1000.0f;
                final float rps = 2 + 2 * overallDifficulty / 10f;
                final Spinner spinner = GameObjectPool.getInstance().getSpinner();
                String tempSound = null;
                if (params.length > 6) {
                    tempSound = params[6];
                }
                spinner.init(this, bgScene, (data.getTime() - secPassed) / timeMultiplier,
                        (endTime - data.getTime()) / timeMultiplier, rps, Integer.parseInt(params[4]),
                        tempSound, stat);
                spinner.setEndsCombo(objects.isEmpty()
                        || objects.peek().isNewCombo());
                addObject(spinner);
                isFirst = false;

                if (stat.getMod().contains(GameMod.MOD_AUTO) ||
                        stat.getMod().contains(GameMod.MOD_AUTOPILOT)) {
                    spinner.setAutoPlay();
                }

                spinner.setId(++lastObjectId);
                if (replaying) {
                    spinner.setReplayData(replay.objectData[spinner.getId()]);
                }

            } else if ((objDefine & 2) > 0) {
                final RGBColor col = getComboColor(comboNum);
                final String soundspec = params.length > 8 ? params[8] : null;
                final Slider slider = GameObjectPool.getInstance().getSlider();
                String tempSound = null;
                if (params.length > 9) {
                    tempSound = params[9];
                }
                if (Config.isCalculateSliderPathInGameStart()){
                    SliderPath sliderPath = getSliderPath(sliderIndex);
                    slider.init(this, mgScene, pos, data.getPosOffset(), data.getTime() - secPassed,
                        col.r(), col.g(), col.b(), scale, currentComboNum,
                        Integer.parseInt(params[4]),
                        Integer.parseInt(params[6]),
                        Float.parseFloat(params[7]), params[5],
                        currentTimingPoint, soundspec, tempSound, isFirst, Double.parseDouble(params[2]),
                        sliderPath);
                    sliderIndex++;
                }
                else{
                    slider.init(this, mgScene, pos, data.getPosOffset(), data.getTime() - secPassed,
                    col.r(), col.g(), col.b(), scale, currentComboNum,
                    Integer.parseInt(params[4]),
                    Integer.parseInt(params[6]),
                    Float.parseFloat(params[7]), params[5],
                    currentTimingPoint, soundspec, tempSound, isFirst, Double.parseDouble(params[2]));
                }
                slider.setEndsCombo(objects.isEmpty()
                        || objects.peek().isNewCombo());
                addObject(slider);
                isFirst = false;

                if (objects.isEmpty() == false
                        && objects.peek().isNewCombo() == false) {
                    final FollowTrack track = GameObjectPool.getInstance()
                            .getTrack();
                    PointF end;
                    if (objects.peek().getTime() > data.getTime()) {
                        end = data.getEnd();
                    } else {
                        end = data.getPos();
                    }
                    track.init(this, bgScene, end, objects.peek().getPos(),
                            objects.peek().getTime() - secPassed, approachRate,
                            scale);
                }
                if (stat.getMod().contains(GameMod.MOD_AUTO)) {
                    slider.setAutoPlay();
                }
                slider.setHitTime(data.getTime());


                if (objects.isEmpty() == false) {
                    if (objects.peek().getTime() > data.getTime()) {
                        currentComboNum++;
                    }
                }

                slider.setId(++lastObjectId);
                if (replaying) {
                    slider.setReplayData(replay.objectData[slider.getId()]);
                    if (slider.getReplayData().tickSet == null)
                        slider.getReplayData().tickSet = new BitSet();
                }
            }
        }

        // 节拍器
        if (metronome != null) {
            metronome.update(secPassed);
        }

        //Status playerStatus = music.getStatus();
        Status playerStatus = GlobalManager.getInstance().getSongService().getStatus();

        if (playerStatus != Status.PLAYING) {
            secPassed += dt;
        }

        if (shouldBePunished || (objects.isEmpty() && activeObjects.isEmpty() && leadOut > 2)) {
            scene = new Scene();
            SkinManager.setSkinEnabled(false);
            GameObjectPool.getInstance().purge();
            SpritePool.getInstance().purge();
            passiveObjects.clear();
            breakPeriods.clear();
            cursorSprites = null;
            String replayFile = null;
            stat.setTime(System.currentTimeMillis());
            if (replay != null && replaying == false) {
                String ctime = String.valueOf(System.currentTimeMillis());
                replayFile = Config.getCorePath() + "Scores/"
                        + MD5Calcuator.getStringMD5(lastTrack.getFilename() + ctime)
                        + ctime.substring(0, Math.min(3, ctime.length())) + ".odr";
                replay.setStat(stat);
                replay.save(replayFile);
            }
            if (GlobalManager.getInstance().getCamera() instanceof SmoothCamera) {
                SmoothCamera camera = (SmoothCamera) (GlobalManager.getInstance().getCamera());
                camera.setZoomFactorDirect(1f);
                if (Config.isShrinkPlayfieldDownwards()) {
                    camera.setCenterDirect(Config.getRES_WIDTH() / 2, Config.getRES_HEIGHT() / 2);
                }
            }
            if (scoringScene != null) {
                if (replaying) {
                    ModMenu.getInstance().setMod(Replay.oldMod);
                    ModMenu.getInstance().setChangeSpeed(Replay.oldChangeSpeed);
                    ModMenu.getInstance().setForceAR(Replay.oldForceAR);
                    ModMenu.getInstance().setEnableForceAR(Replay.oldEnableForceAR);
                    ModMenu.getInstance().setFLfollowDelay(Replay.oldFLFollowDelay);
                }

                if (replaying)
                    scoringScene.load(scoringScene.getReplayStat(), null, GlobalManager.getInstance().getSongService(), replayFile, null, lastTrack);
                else {
                    if (stat.getMod().contains(GameMod.MOD_AUTO)) {
                        stat.setPlayerName("osu!");
                    }

                    EdExtensionHelper.onEndGame(lastTrack, stat);
                    if (storyboardSprite != null) {
                        storyboardSprite.releaseStoryboard();
                        storyboardSprite = null;
                        storyboardOverlayProxy.setDrawProxy(null);
                    }

                    if (Multiplayer.isConnected)
                    {
                        Multiplayer.log("Match ended, moving to results scene.");
                        Async.run(() -> RoomAPI.submitFinalScore(stat.toJson(false)));
                    }
                    scoringScene.load(stat, lastTrack, GlobalManager.getInstance().getSongService(), replayFile, trackMD5, null);
                }
                GlobalManager.getInstance().getSongService().setVolume(0.2f);
                engine.setScene(scoringScene.getScene());

                if (Multiplayer.isConnected)
                {
                    RoomScene.INSTANCE.getChat().show();
                    ToastLogger.showText("Loading room statistics...", false);
                }

            } else {
                engine.setScene(oldScene);
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
            for (final Cursor c : cursors) {
                if (c.mouseDown
                        && Utils.distance(
                        c.mousePos,
                        new PointF(Config.getRES_WIDTH(), Config
                                .getRES_HEIGHT())) < 250) {

                    if (Multiplayer.isConnected)
                    {
                        if (!isSkipRequested)
                        {
                            isSkipRequested = true;
                            ResourceManager.getInstance().getSound("menuhit").play();
                            skipBtn.setVisible(false);

                            Async.run(RoomAPI.INSTANCE::requestSkip);
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
        final float difference = skipTime - 0.5f - secPassed;
        for (final GameObject obj : passiveObjects) {
            obj.update(difference);
        }
        GlobalManager.getInstance().getSongService().seekTo((int) Math.ceil((skipTime - 0.5f) * 1000));
        secPassed = skipTime - 0.5f;

        Execution.glThread(() -> {
            if (skipBtn != null) {
                skipBtn.detachSelf();
                skipBtn = null;
            }
            return null;
        });
    }

    private void onExit() {

        //游戏退出

        if (!replaying) {
            EdExtensionHelper.onExitGame(lastTrack);
        }

        SkinManager.setSkinEnabled(false);
        GameObjectPool.getInstance().purge();
        SpritePool.getInstance().purge();
        passiveObjects.clear();
        breakPeriods.clear();
        cursorSprites = null;
        scoreBoard = null;

        if (GlobalManager.getInstance().getSongService() != null) {
            GlobalManager.getInstance().getSongService().stop();
            GlobalManager.getInstance().getSongService().preLoad(filePath);
            GlobalManager.getInstance().getSongService().play();
            GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
        }

        /*try {
            android.os.Debug.dumpHprofData(Environment.getExternalStorageDirectory().getPath()+"/dd.hprof");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        if (replaying) {
            replayFile = null;
            ModMenu.getInstance().setMod(Replay.oldMod);
            ModMenu.getInstance().setChangeSpeed(Replay.oldChangeSpeed);
            ModMenu.getInstance().setForceAR(Replay.oldForceAR);
            ModMenu.getInstance().setEnableForceAR(Replay.oldEnableForceAR);
            ModMenu.getInstance().setFLfollowDelay(Replay.oldFLFollowDelay);
        }
    }

    public void quit() {

        //游戏中退出，通过暂停菜单

        if (!replaying) {
            EdExtensionHelper.onQuitGame(lastTrack);
        }

        if (storyboardSprite != null) {
            storyboardSprite.releaseStoryboard();
            storyboardSprite = null;
            storyboardOverlayProxy.setDrawProxy(null);
        }

        onExit();
        ResourceManager.getInstance().getSound("failsound").stop();
        if (GlobalManager.getInstance().getCamera() instanceof SmoothCamera) {
            SmoothCamera camera = (SmoothCamera) (GlobalManager.getInstance().getCamera());
            camera.setZoomFactorDirect(1f);
            if (Config.isShrinkPlayfieldDownwards()) {
                camera.setCenterDirect(Config.getRES_WIDTH() / 2, Config.getRES_HEIGHT() / 2);
            }
        }
        scene = new Scene();

        if (Multiplayer.isMultiplayer)
        {
            RoomScene.INSTANCE.show();
            return;
        }
        engine.setScene(oldScene);
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
            if (endCombo && comboWasMissed == false) {
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
        if (accuracy > difficultyHelper.hitWindowFor50(overallDifficulty) || forcedScore == ResultType.MISS.getId()) {
            createHitEffect(pos, "hit0", color);
            registerHit(id, 0, endCombo);
            return;
        }

        String scoreName = "hit300";
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
            createHitEffect(start, "hit0", color);
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

        final PointF pos = new PointF(Config.getRES_WIDTH() / 2,
                Config.getRES_HEIGHT() / 2);
        if (score == 0) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "hit0");
            effect.init(
                    scene,
                    pos,
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newFadeInModifier(0.15f), ModifierFactory
                            .newDelayModifier(0.35f), ModifierFactory
                            .newFadeOutModifier(0.25f)));
            registerHit(id, 0, endCombo);
            return;
        }

        String scoreName = "hit0";
        switch (score) {
            case 300:
                scoreName = registerHit(id, 300, endCombo);
                break;
            case 100:
                scoreName = registerHit(id, 100, endCombo);
                break;
            case 50:
                scoreName = registerHit(id, 50, endCombo);
                break;
        }

        if (Config.isHitLighting() &&
                ResourceManager.getInstance().getTexture("lighting") != null) {
            final GameEffect light = GameObjectPool.getInstance().getEffect(
                    "lighting");
            light.init(
                    mgScene,
                    pos,
                    scale,
                    new FadeOutModifier(0.7f),
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, scale, 1.5f * scale),
                            ModifierFactory.newScaleModifier(0.45f,
                                    scale * 1.5f, 2f * scale)));
        }

        GameEffect effect = GameObjectPool.getInstance().getEffect(scoreName);
        effect.init(
                mgScene,
                pos,
                scale,
                new SequenceEntityModifier(ModifierFactory.newScaleModifier(
                        0.15f, 1.0f * scale, 1.2f * scale), ModifierFactory
                        .newScaleModifier(0.05f, 1.2f * scale, 1.0f * scale),
                        ModifierFactory.newAlphaModifier(1f, 1, 0)));

        pos.y /= 2f;
        effect = GameObjectPool.getInstance().getEffect("spinner-osu");
        effect.init(mgScene, pos, 1, ModifierFactory.newFadeOutModifier(1.5f));
    }

    public void playSound(final String name, final int sampleSet, final int addition) {
        if (addition > 0 && !name.equals("hitnormal") && addition < Constants.SAMPLE_PREFIX.length) {
            playSound(Constants.SAMPLE_PREFIX[addition], name);
            return;
        }
        if (sampleSet > 0 && sampleSet < Constants.SAMPLE_PREFIX.length) {
            playSound(Constants.SAMPLE_PREFIX[sampleSet], name);
        } else {
            playSound(soundTimingPoint.getHitSound(), name);
        }
    }

    public void playSound(final String prefix, final String name) {
        final String fullName = prefix + "-" + name;
        BassSoundProvider snd;
        if (soundTimingPoint.getCustomSound() == 0) {
            snd = ResourceManager.getInstance().getSound(fullName);
        } else {
            snd = ResourceManager.getInstance().getCustomSound(fullName,
                    soundTimingPoint.getCustomSound());
        }
        if(snd == null) {
            return;
        }
        if (name.equals("sliderslide") || name.equals("sliderwhistle")) {
            snd.setLooping(true);
        }
        if (name.equals("hitnormal")) {
            snd.play(soundTimingPoint.getVolume() * 0.8f);
            return;
        }
        if (name.equals("hitwhistle")
                || name.equals("hitclap")) {
            snd.play(soundTimingPoint.getVolume() * 0.85f);
            return;
        }
        snd.play(soundTimingPoint.getVolume());
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
        if (mIsAuto) {
            return false;
        }
        if (Config.isRemoveSliderLock()){
            if(activeObjects.isEmpty()
                || Math.abs(object.getHitTime() - lastObjectHitTime) > 0.001f) {
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

    private GameObject getLastTobeclickObject(){
        for (GameObject note : activeObjects)
        {
            if (!note.isStartHit())
            {
                return note;
            }
        }
        return null;
    }

    @Override
    public double downFrameOffset(int index) {
        return cursors[index].mouseDownOffsetMS;
    }

    public void removeObject(final GameObject object) {
        activeObjects.remove(object);
    }

    public boolean onSceneTouchEvent(final Scene pScene,
                                     final TouchEvent pSceneTouchEvent) {
        if (pSceneTouchEvent.getPointerID() < 0
                || pSceneTouchEvent.getPointerID() >= CursorCount) {
            Debug.e("Invalid pointerID: " + pSceneTouchEvent.getPointerID());
            return false;
        }
        if (replaying) {
            return false;
        }
        final int i = pSceneTouchEvent.getPointerID();
        float pTouchX = FMath.clamp(pSceneTouchEvent.getX(), 0, Config.getRES_WIDTH());
        float pTouchY = FMath.clamp(pSceneTouchEvent.getY(), 0, Config.getRES_HEIGHT());
        if (pSceneTouchEvent.isActionDown()) {
            cursors[i].mouseDown = true;
            cursors[i].mouseDownOffsetMS = (pSceneTouchEvent.getMotionEvent().getEventTime() - previousFrameTime) * timeMultiplier;
            for (int j = 0; j < cursors.length; j++)
                cursors[j].mouseOldDown = false;
            PointF gamePoint = Utils.realToTrackCoords(new PointF(pTouchX, pTouchY));
            cursors[i].mousePos.x = pTouchX;
            cursors[i].mousePos.y = pTouchY;
            if (replay != null) {
                replay.addPress(secPassed, gamePoint, i);
            }
            cursorIIsDown[i] = true;
        } else if (pSceneTouchEvent.isActionMove()) {
            PointF gamePoint = Utils.realToTrackCoords(new PointF(pTouchX, pTouchY));
            cursors[i].mousePos.x = pTouchX;
            cursors[i].mousePos.y = pTouchY;
            if (replay != null) {
                replay.addMove(secPassed, gamePoint, i);
            }
        } else if (pSceneTouchEvent.isActionUp()) {
            cursors[i].mouseDown = false;
            if (replay != null) {
                replay.addUp(secPassed, i);
            }
        } else {
            return false;
        }
        return true;
    }


    public void stopSound(final String name) {
        final String prefix = soundTimingPoint.getHitSound() + "-";
        final BassSoundProvider snd = ResourceManager.getInstance().getSound(prefix + name);
        if (snd != null) {
            snd.stop();
        }
    }

    public void pause() {
        if (paused) {
            return;
        }

        if (Multiplayer.isMultiplayer)
        {
            if (backPressCount < 1 && GlobalManager.getInstance().getMainActivity().hasWindowFocus())
            {
                backPressCount++;
                ToastLogger.showText("Tap twice to exit to room.", false);
                return;
            }

            // Room being null can happen when the player disconnects from socket while playing
            if (Multiplayer.isConnected)
                Async.run(() -> RoomAPI.submitFinalScore(stat.toJson(false)));

            Multiplayer.log("Player left the match.");
            RoomScene.INSTANCE.show();
            return;
        }

        if (!replaying) {
            EdExtensionHelper.onPauseGame(lastTrack);
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
            if (Multiplayer.isConnected)
            {
                Multiplayer.log("Player has lost, moving to room scene.");

                Async.run(() -> RoomAPI.submitFinalScore(stat.toJson(false)));
                RoomScene.INSTANCE.show();
            }
            else RoomScene.INSTANCE.back();
            return;
        }

        if (!replaying) {
            EdExtensionHelper.onGameover(lastTrack);
        }

        if(scorebar != null) scorebar.flush();
        ResourceManager.getInstance().getSound("failsound").play();
        final PauseMenu menu = new PauseMenu(engine, this, true);
        gameStarted = false;
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

        scene.getChildScene().back();
        paused = false;
        if (stat.getHp() <= 0 && !stat.getMod().contains(GameMod.MOD_NOFAIL)
                && !stat.getMod().contains(GameMod.MOD_RELAX)
                && !stat.getMod().contains(GameMod.MOD_AUTOPILOT)) {
            quit();
            return;
        }

        if (!replaying) {
            EdExtensionHelper.onResume(lastTrack);
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
        passiveObjects.remove(object);
    }

    private void createHitEffect(final PointF pos, final String name, RGBColor color) {
        final GameEffect effect = GameObjectPool.getInstance().getEffect(name);
        if (name.equals("hit0")) {
            if(GameHelper.isSuddenDeath()){
                effect.init(
                    mgScene,
                    pos,
                    scale * 3,
                    new SequenceEntityModifier(ModifierFactory
                            .newFadeInModifier(0.15f), ModifierFactory
                            .newDelayModifier(0.35f), ModifierFactory
                            .newFadeOutModifier(0.25f)));
                return;
            }
            effect.init(
                    mgScene,
                    pos,
                    scale,
                    new SequenceEntityModifier(ModifierFactory
                            .newFadeInModifier(0.15f), ModifierFactory
                            .newDelayModifier(0.35f), ModifierFactory
                            .newFadeOutModifier(0.25f)));
            return;
        }

        if (Config.isHitLighting()
                && name.equals("sliderpoint10") == false
                && name.equals("sliderpoint30") == false
                && ResourceManager.getInstance().getTexture("lighting") != null) {
            final GameEffect light = GameObjectPool.getInstance().getEffect("lighting");
            light.setColor(color);
            light.init(
                    bgScene,
                    pos,
                    scale,
                    ModifierFactory.newFadeOutModifier(1f),
                    new SequenceEntityModifier(ModifierFactory
                            .newScaleModifier(0.25f, scale, 1.5f * scale),
                            ModifierFactory.newScaleModifier(0.45f,
                                    scale * 1.5f, 1.9f * scale),
                            ModifierFactory.newScaleModifier(0.3f, scale * 1.9f, scale * 2f)
                    ));
            light.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_DST_ALPHA);
        }

        effect.init(
                mgScene,
                pos,
                scale,
                new SequenceEntityModifier(ModifierFactory.newScaleModifier(
                        0.15f, 1.0f * scale, 1.2f * scale), ModifierFactory
                        .newScaleModifier(0.05f, 1.2f * scale, 1.0f * scale),
                        ModifierFactory.newAlphaModifier(0.5f, 1, 0)));
    }

    private void createBurstEffect(final PointF pos, final RGBColor color) {
        if (!Config.isComplexAnimations() || !Config.isBurstEffects() || stat.getMod().contains(GameMod.MOD_HIDDEN))
            return;
        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("hitcircle");
        burst1.init(mgScene, pos, scale,
                ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale),
                ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0)
        );
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("hitcircleoverlay");
        burst2.init(mgScene, pos, scale,
                ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale),
                ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0)
        );

    }

    private void createBurstEffectSliderStart(final PointF pos, final RGBColor color) {
        if (!Config.isComplexAnimations() || !Config.isBurstEffects() || stat.getMod().contains(GameMod.MOD_HIDDEN))
            return;
        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("sliderstartcircle");
        burst1.init(mgScene, pos, scale,
                ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale),
                ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0)
        );
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("sliderstartcircleoverlay");
        burst2.init(mgScene, pos, scale,
                ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale),
                ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0)
        );

    }

    private void createBurstEffectSliderEnd(final PointF pos, final RGBColor color) {
        if (!Config.isComplexAnimations() || !Config.isBurstEffects() || stat.getMod().contains(GameMod.MOD_HIDDEN))
            return;
        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("sliderendcircle");
        burst1.init(mgScene, pos, scale,
                ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale),
                ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0)
        );
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("sliderendcircleoverlay");
        burst2.init(mgScene, pos, scale,
                ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale),
                ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0)
        );

    }

    private void createBurstEffectSliderReverse(final PointF pos, float ang, final RGBColor color) {
        if (!Config.isComplexAnimations() || !Config.isBurstEffects() || stat.getMod().contains(GameMod.MOD_HIDDEN)) 
            return;
        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("reversearrow");
        burst1.hit.setRotation(ang);
        burst1.init(mgScene, pos, scale,
                ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale),
                ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0)
        );

    }

    public int getCursorsCount() {
        return CursorCount;
    }


    public void registerAccuracy(final float acc) {
        if (hitErrorMeter != null) {
            hitErrorMeter.putErrorResult(acc);
        }
        avgOffset += acc;
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
            if(c.mouseDown == true || c.mousePressed == true || c.mouseOldDown == true){
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

    private void stackNotes(){
        // Stack notes
        int i = 0;
        for (GameObjectData data : objects){
            final PointF pos = data.getPos();
            final String[] params = data.getData();
            final int objDefine = Integer.parseInt(params[3]);
            if (objects.isEmpty() == false && (objDefine & 1) > 0 && i + 1 < objects.size()) {
                if (objects.get(i + 1).getTime() - data.getTime() < 2f * GameHelper.getStackLeniency()
                        && Utils.squaredDistance(pos, objects.get(i + 1).getPos()) < scale) {
                    objects.get(i + 1).setPosOffset(
                            data.getPosOffset() + Utils.toRes(4) * scale);
                }
            }
            i++;
        }
    }

    private void calculateAllSliderPaths(){
        if (objects.isEmpty()){
            return;
        }
        else {
            if (lastTrack.getSliderCount() <= 0){
                return;
            }
            sliderPaths = new SliderPath[lastTrack.getSliderCount()];
            for (SliderPath path : sliderPaths){
                path = null;
            }
            int i = 0;
            sliderIndex = 0;
            for (GameObjectData data : objects){
                final String[] params = data.getData();
                final int objDefine = Integer.parseInt(params[3]);
                //is slider
                if ((objDefine & 2) > 0) {
                    final PointF pos = data.getPos();
                    final float length = Float.parseFloat(params[7]);
                    final float offset = data.getPosOffset();
                    pos.x += data.getPosOffset();
                    pos.y += data.getPosOffset();
                    if (length < 0){
                        sliderPaths[sliderIndex] = GameHelper.calculatePath(Utils.realToTrackCoords(pos),
                        params[5].split("[|]"), 0, offset);
                    }
                    else{
                        sliderPaths[sliderIndex] = GameHelper.calculatePath(Utils.realToTrackCoords(pos),
                        params[5].split("[|]"), length, offset);
                    }
                    sliderIndex++;
                }
                i++;
            }
            sliderIndex = 0;
        }
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
        if (replay != null && replaying == false) {
            //write misses to replay
            for (GameObject obj : activeObjects) {
                stat.registerHit(0, false, false);
                replay.addObjectScore(obj.getId(), ResultType.MISS);
            }
            while (objects.isEmpty() == false){
                objects.poll();
                stat.registerHit(0, false, false);
                replay.addObjectScore(++lastObjectId, ResultType.MISS);
            }
            //save replay
            String ctime = String.valueOf(System.currentTimeMillis());
            replayFile = Config.getCorePath() + "Scores/"
                    + MD5Calcuator.getStringMD5(lastTrack.getFilename() + ctime)
                    + ctime.substring(0, Math.min(3, ctime.length())) + ".odr";
            replay.setStat(stat);
            replay.save(replayFile);
            ScoreLibrary.getInstance().addScore(lastTrack.getFilename(), stat, replayFile);
            ToastLogger.showText(StringTable.get(R.string.message_save_replay_successful), true);
            replayFile = null;
            return true;
        }
        else{
            ToastLogger.showText(StringTable.get(R.string.message_save_replay_failed), true);
            return false;
        }
    }

    private void updatePPCounter(int objectId) {
        if (ppText == null) {
            return;
        }

        HitObject object = beatmapData.hitObjects.getObjects().get(objectId);
        double time = object.getStartTime();

        if (object instanceof HitObjectWithDuration) {
            time = ((HitObjectWithDuration) object).getEndTime();
        }

        ppText.setText(String.format(Locale.ENGLISH, "%.2fpp", getPPAtTime(time)));
    }

    private double getPPAtTime(double time) {
        TimedDifficultyAttributes timedAttributes = getAttributeAtTime(time);

        if (timedAttributes == null) {
            return 0;
        }

        return BeatmapDifficultyCalculator.calculatePerformance(
                timedAttributes.attributes,
                stat
        ).total;
    }

    private TimedDifficultyAttributes getAttributeAtTime(double time) {
        if (timedDifficultyAttributes.isEmpty()) {
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
            TimedDifficultyAttributes attributes = timedDifficultyAttributes.get(pivot);

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
