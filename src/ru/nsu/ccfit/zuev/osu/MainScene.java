package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.edlplan.ui.fragment.ConfirmDialogFragment;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.AccelerationInitializer;
import org.anddev.andengine.entity.particle.initializer.RotationInitializer;
import org.anddev.andengine.entity.particle.initializer.VelocityInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.entity.particle.modifier.ScaleModifier;
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
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseBounceOut;
import org.anddev.andengine.util.modifier.ease.EaseCubicOut;
import org.anddev.andengine.util.modifier.ease.EaseElasticOut;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.game.SongProgressBar;
import ru.nsu.ccfit.zuev.osu.game.TimingPoint;
import ru.nsu.ccfit.zuev.osu.helper.ModifierFactory;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

/**
 * Created by Fuuko on 2015/4/24.
 */
public class MainScene implements IUpdateHandler {
    public SongProgressBar progressBar;
    public BeatmapInfo beatmapInfo;
    private Context context;
    private Sprite logo, logoOverlay, play, options, exit, background, lastBackground;
    private Sprite music_nowplay;
    private Scene scene;
    private ChangeableText musicInfoText;
    //    private ArrayList<BeatmapInfo> beatmaps;
    private Random random = new Random();
    private Rectangle[] spectrum = new Rectangle[120];
    private float[] peakLevel = new float[120];
    private float[] peakDownRate = new float[120];
    private float[] peakAlpha = new float[120];
    private Replay replay = null;
    private TrackInfo selectedTrack;
    private BeatmapData beatmapData;
    private List<TimingPoint> timingPoints;
    private TimingPoint currentTimingPoint, lastTimingPoint, firstTimingPoint;

    private int particleBeginTime = 0;
    private boolean particleEnabled = false;
    private boolean isContinuousKiai = false;

    private ParticleSystem[] particleSystem = new ParticleSystem[2];

    //private BassAudioPlayer music;

    private boolean musicStarted;
    private BassSoundProvider hitsound;

    private float bpmLength = 1000;
    private float lastBpmLength = 0;
    private float offset = 0;
    private float beatPassTime = 0;
    private float lastBeatPassTime = 0;
    private boolean doChange = false;
    private boolean doStop = false;
    //    private int playIndex = 0;
//    private int lastPlayIndex = -1;
    private long lastHit = 0;
    private boolean isOnExitAnim = false;

    private boolean isMenuShowed = false;
    private boolean doMenuShow = false;
    private float showPassTime = 0, syncPassedTime = 0;
    private float menuBarX = 0, playY, optionsY, exitY;


    public void load(Context context) {
        this.context = context;
        Debug.i("Load: mainMenuLoaded()");
        scene = new Scene();

        final TextureRegion tex = ResourceManager.getInstance().getTexture("menu-background");
        
        if (tex != null) {
            float height = tex.getHeight();
            height *= Config.getRES_WIDTH()
                    / (float) tex.getWidth();
            final Sprite menuBg = new Sprite(
                    0,
                    (Config.getRES_HEIGHT() - height) / 2,
                    Config.getRES_WIDTH(),
                    height, tex);
            scene.setBackground(new SpriteBackground(menuBg));
        } else {
            scene.setBackground(new ColorBackground(70 / 255f, 129 / 255f,
                    252 / 255f));
        }
        lastBackground = new Sprite(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT(), ResourceManager.getInstance().getTexture("emptyavatar"));
        final TextureRegion logotex = ResourceManager.getInstance().getTexture("logo");
        logo = new Sprite(Config.getRES_WIDTH() / 2 - logotex.getWidth() / 2, Config.getRES_HEIGHT() / 2 - logotex.getHeight() / 2, logotex) {
            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (hitsound != null) {
                        hitsound.play();
                    }
                    Debug.i("logo down");
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    Debug.i("logo up");
                    Debug.i("doMenuShow " + doMenuShow + " isMenuShowed " + isMenuShowed + " showPassTime " + showPassTime);
                    if (doMenuShow == true && isMenuShowed == true) {
                        showPassTime = 20000;
                    }
                    if (doMenuShow == false && isMenuShowed == false && logo.getX() == (Config.getRES_WIDTH() - logo.getWidth()) / 2) {
                        doMenuShow = true;
                        showPassTime = 0;
                    }
                    Debug.i("doMenuShow " + doMenuShow + " isMenuShowed " + isMenuShowed + " showPassTime " + showPassTime);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        logoOverlay = new Sprite(Config.getRES_WIDTH() / 2 - logotex.getWidth() / 2, Config.getRES_HEIGHT() / 2 - logotex.getHeight() / 2, logotex);
        logoOverlay.setScale(1.07f);
        logoOverlay.setAlpha(0.2f);

        play = new Sprite(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 3,
                60 + 82 - 32, ResourceManager.getInstance().getTexture("play")) {


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    if (hitsound != null) {
                        hitsound.play();
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    if (isOnExitAnim) return true;
                    GlobalManager.getInstance().getSongService().setGaming(true);
                    if (GlobalManager.getInstance().getCamera().getRotation() == 0) {
                        Utils.setAccelerometerSign(1);
                    } else {
                        Utils.setAccelerometerSign(-1);
                    }
//                    final Intent intent = new Intent(
//                            MainManager.getInstance().getMainActivity(), OsuActivity.class);
//                    MainManager.getInstance().getMainActivity().startActivity(intent);
                    new AsyncTaskLoader().execute(new OsuAsyncCallback() {
                        public void run() {
                            GlobalManager.getInstance().getEngine().setScene(new LoadingScreen().getScene());
                            GlobalManager.getInstance().getMainActivity().checkNewSkins();
                            GlobalManager.getInstance().getMainActivity().checkNewBeatmaps();
                            if (!LibraryManager.getInstance().loadLibraryCache(GlobalManager.getInstance().getMainActivity(), true)) {
                                LibraryManager.getInstance().scanLibrary(GlobalManager.getInstance().getMainActivity());
                                System.gc();
                            }
                            GlobalManager.getInstance().getSongMenu().reload();
                            /* To fixed skin load bug in some Android 10
                            if (Build.VERSION.SDK_INT >= 29) {
                                String skinNow = Config.getSkinPath();
                                ResourceManager.getInstance().loadSkin(skinNow);
                            } */
                        }

                        public void onComplete() {

                            musicControl(MusicOption.PLAY);
                            GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getSongMenu().getScene());
                            GlobalManager.getInstance().getSongMenu().select();
                        }
                    });
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }

        };

        options = new Sprite(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 3,
                60 + 3 * 82 - 64, ResourceManager.getInstance().getTexture(
                "options")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    if (hitsound != null) {
                        hitsound.play();
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    if (isOnExitAnim) return true;
                    GlobalManager.getInstance().getSongService().setGaming(true);
                    // GlobalManager.getInstance().getSongService().setIsSettingMenu(true);
                    /* final Intent intent = new Intent(GlobalManager.getInstance().getMainActivity(),
                            SettingsMenu.class);
                    GlobalManager.getInstance().getMainActivity().startActivity(intent); */
                    GlobalManager.getInstance().getMainActivity().runOnUiThread(() ->
                        new SettingsMenu().show());
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        exit = new Sprite(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 3, 60
                + 5 * 82 - 128 + 32, ResourceManager.getInstance().getTexture(
                "exit")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    // if (hitsound != null) {
                    //     hitsound.play();
                    // }
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    if (GlobalManager.getInstance().getCamera().getRotation() == 0) {
                        Utils.setAccelerometerSign(1);
                    } else {
                        Utils.setAccelerometerSign(-1);
                    }
                    setColor(1, 1, 1);

                    showExitDialog();

                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        final Text author = new Text(10, 530, ResourceManager
                .getInstance().getFont("font"),
                String.format(
                        Locale.getDefault(),
                        "osu!droid %s\nby osu!droid Team\nosu! is \u00a9 peppy 2007-2021",
                        BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"
                        )) {


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    new ConfirmDialogFragment().setMessage(R.string.dialog_visit_osu_website_message).showForResult(
                        isAccepted -> {
                            if(isAccepted) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://osu.ppy.sh"));
                                GlobalManager.getInstance().getMainActivity().startActivity(browserIntent);
                            }
                        }
                    );
                    return true;
                }
                return false;
            }
        };
        author.setPosition(10, Config.getRES_HEIGHT() - author.getHeight() - 10);

        final Text yasonline = new Text(720, 530, ResourceManager
                .getInstance().getFont("font"),
                "  Performance Ranking\n   Provided by iBancho") {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    new ConfirmDialogFragment().setMessage(R.string.dialog_visit_osudroid_website_message).showForResult(
                        isAccepted -> {
                            if(isAccepted) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + OnlineManager.hostname));
                                GlobalManager.getInstance().getMainActivity().startActivity(browserIntent);
                            }
                        }
                    );
                    return true;
                }
                return false;
            }
        };
        yasonline.setPosition(Config.getRES_WIDTH() - yasonline.getWidth() - 40, Config.getRES_HEIGHT() - yasonline.getHeight() - 10);

        final Sprite music_prev = new Sprite(Config.getRES_WIDTH() - 50 * 6 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_prev")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    doChange = true;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    if (lastHit == 0) {
                        lastHit = System.currentTimeMillis();
                    } else {
                        if (System.currentTimeMillis() - lastHit <= 1000 && !isOnExitAnim) {
                            return true;
                        }
                    }
                    lastHit = System.currentTimeMillis();
                    musicControl(MusicOption.PREV);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        final Sprite music_play = new Sprite(Config.getRES_WIDTH() - 50 * 5 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_play")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    musicControl(MusicOption.PLAY);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        final Sprite music_pause = new Sprite(Config.getRES_WIDTH() - 50 * 4 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_pause")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    musicControl(MusicOption.PAUSE);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        final Sprite music_stop = new Sprite(Config.getRES_WIDTH() - 50 * 3 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_stop")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    doStop = true;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    musicControl(MusicOption.STOP);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        final Sprite music_next = new Sprite(Config.getRES_WIDTH() - 50 * 2 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_next")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    doChange = true;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    if (lastHit == 0) {
                        lastHit = System.currentTimeMillis();
                    } else {
                        if (System.currentTimeMillis() - lastHit <= 1000 && !isOnExitAnim) {
                            return true;
                        }
                    }
                    lastHit = System.currentTimeMillis();
                    musicControl(MusicOption.NEXT);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        musicInfoText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("font"), "", HorizontalAlign.RIGHT, 35);

        final TextureRegion nptex = ResourceManager.getInstance().getTexture("music_np");
        music_nowplay = new Sprite(Utils.toRes(Config.getRES_WIDTH() - 500), 0, 40 * nptex.getWidth() / nptex.getHeight(), 40, nptex);

        final Rectangle bgTopRect = new Rectangle(0, 0, Config.getRES_WIDTH(), Utils.toRes(120));
        bgTopRect.setColor(0, 0, 0, 0.3f);

        final Rectangle bgbottomRect = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Math.max(author.getHeight(), yasonline.getHeight()) + Utils.toRes(15));
        bgbottomRect.setPosition(0, Config.getRES_HEIGHT() - bgbottomRect.getHeight());
        bgbottomRect.setColor(0, 0, 0, 0.3f);

        for (int i = 0; i < 120; i++) {
            final float pX = Config.getRES_WIDTH() / 2;
            final float pY = Config.getRES_HEIGHT() / 2;

            spectrum[i] = new Rectangle(pX, pY, 260, 10);
            spectrum[i].setRotationCenter(0, 5);
            spectrum[i].setScaleCenter(0, 5);
            spectrum[i].setRotation(-220 + i * 3f);
            spectrum[i].setAlpha(0.0f);

            scene.attachChild(spectrum[i]);
        }

        LibraryManager.getInstance().loadLibraryCache((Activity) context, false);

        TextureRegion starRegion = ResourceManager.getInstance().getTexture("star");

        {
            particleSystem[0] = new ParticleSystem(new PointParticleEmitter(-40, Config.getRES_HEIGHT() * 3 / 4), 32, 48, 128, starRegion);
            particleSystem[0].setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            particleSystem[0].addParticleInitializer(new VelocityInitializer(150, 430, -480, -520));
            particleSystem[0].addParticleInitializer(new AccelerationInitializer(10, 30));
            particleSystem[0].addParticleInitializer(new RotationInitializer(0.0f, 360.0f));

            particleSystem[0].addParticleModifier(new ScaleModifier(0.5f, 2.0f, 0.0f, 1.0f));
            particleSystem[0].addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0.0f, 1.0f));
            particleSystem[0].addParticleModifier(new ExpireModifier(1.0f));

            particleSystem[0].setParticlesSpawnEnabled(false);

            scene.attachChild(particleSystem[0]);
        }

        {
            particleSystem[1] = new ParticleSystem(new PointParticleEmitter(Config.getRES_WIDTH(), Config.getRES_HEIGHT() * 3 / 4), 32, 48, 128, starRegion);
            particleSystem[1].setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            particleSystem[1].addParticleInitializer(new VelocityInitializer(-150, -430, -480, -520));
            particleSystem[1].addParticleInitializer(new AccelerationInitializer(-10, 30));
            particleSystem[1].addParticleInitializer(new RotationInitializer(0.0f, 360.0f));

            particleSystem[1].addParticleModifier(new ScaleModifier(0.5f, 2.0f, 0.0f, 1.0f));
            particleSystem[1].addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0.0f, 1.0f));
            particleSystem[1].addParticleModifier(new ExpireModifier(1.0f));

            particleSystem[1].setParticlesSpawnEnabled(false);

            scene.attachChild(particleSystem[1]);
        }

        play.setAlpha(0f);
        options.setAlpha(0f);
        exit.setAlpha(0f);

        logo.setPosition((Config.getRES_WIDTH() - logo.getWidth()) / 2, (Config.getRES_HEIGHT() - logo.getHeight()) / 2);
        logoOverlay.setPosition((Config.getRES_WIDTH() - logo.getWidth()) / 2, (Config.getRES_HEIGHT() - logo.getHeight()) / 2);
        options.setScale(Config.getRES_WIDTH() / 1024f);
        play.setScale(Config.getRES_WIDTH() / 1024f);
        exit.setScale(Config.getRES_WIDTH() / 1024f);
        options.setPosition(options.getX(), (Config.getRES_HEIGHT() - options.getHeight()) / 2);
        play.setPosition(play.getX(), options.getY() - play.getHeight() - 40 * Config.getRES_WIDTH() / 1024f);
        exit.setPosition(exit.getX(), options.getY() + options.getHeight() + 40 * Config.getRES_WIDTH() / 1024f);

        menuBarX = play.getX();
        playY = play.getScaleY();
        exitY = exit.getScaleY();

        scene.attachChild(lastBackground, 0);
        scene.attachChild(bgTopRect);
        scene.attachChild(bgbottomRect);
        scene.attachChild(author);
        scene.attachChild(yasonline);
        scene.attachChild(play);
        scene.attachChild(options);
        scene.attachChild(exit);
        scene.attachChild(logo);
        scene.attachChild(logoOverlay);
        scene.attachChild(music_nowplay);
        scene.attachChild(musicInfoText);
        scene.attachChild(music_prev);
        scene.attachChild(music_play);
        scene.attachChild(music_pause);
        scene.attachChild(music_stop);
        scene.attachChild(music_next);

        scene.registerTouchArea(logo);
        scene.registerTouchArea(author);
        scene.registerTouchArea(yasonline);
        scene.registerTouchArea(music_prev);
        scene.registerTouchArea(music_play);
        scene.registerTouchArea(music_pause);
        scene.registerTouchArea(music_stop);
        scene.registerTouchArea(music_next);
        scene.setTouchAreaBindingEnabled(true);

        progressBar = new SongProgressBar(null, scene, 0, 0, new PointF(Utils.toRes(Config.getRES_WIDTH() - 320), Utils.toRes(100)));
        progressBar.setProgressRectColor(new RGBAColor(0.9f, 0.9f, 0.9f, 0.8f));

        createOnlinePanel(scene);
        scene.registerUpdateHandler(this);

        String[] welcomeSounds = {"welcome", "welcome_piano"};
        int randNum = new Random().nextInt((1 - 0) + 1) + 0;
        String welcomeSound = welcomeSounds[randNum];
        ResourceManager.getInstance().loadSound(welcomeSound, String.format("sfx/%s.ogg", welcomeSound), false).play();
        hitsound = ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false);

        /*if (BuildConfig.DEBUG) {
            SupportSprite supportSprite = new SupportSprite(Config.getRES_WIDTH(), Config.getRES_HEIGHT()) {

                TextureQuad[] quads;

                {
                    Bitmap bitmap = Bitmap.createBitmap(4, 1, Bitmap.Config.ARGB_8888);
                    for (int i = 0; i < 4; i++) {
                        bitmap.setPixel(i, 0, Color.argb(i * 80 + 10, i * 80 + 10, i * 80 + 10, i * 80 + 10));
                    }
                    //bitmap.setPremultiplied(true);
                    TextureRegion region = TextureHelper.createRegion(bitmap);
                    quads = new TextureQuad[4];
                    {
                        TextureQuad quad = new TextureQuad();
                        quad.setTextureAndSize(region);
                        quad.enableScale().scale.set(10, 10);
                        quad.position.set(0, 0);
                        quads[0] = quad;
                    }
                    {
                        TextureQuad quad = new TextureQuad();
                        quad.setTextureAndSize(region);
                        quad.enableScale().scale.set(10, 10);
                        quad.position.set(640, 480);
                        quads[1] = quad;
                    }
                    {
                        TextureQuad quad = new TextureQuad();
                        quad.setTextureAndSize(region);
                        quad.enableScale().scale.set(10, 10);
                        quad.position.set(640, 0);
                        quads[2] = quad;
                    }
                    {
                        TextureQuad quad = new TextureQuad();
                        quad.setTextureAndSize(region);
                        quad.enableScale().scale.set(10, 10);
                        quad.position.set(0, 480);
                        quads[3] = quad;
                    }
                    *//*for (int i = 0; i < quads.length; i++) {
                        TextureQuad quad = new TextureQuad();
                        quad.setTextureAndSize(region);
                        quad.position.set((float) Math.random() * 1000, (float) Math.random() * 1000);
                        if (Math.random() > 0.2) {
                            //quad.enableColor().accentColor.set((float) Math.random(), (float) Math.random(), (float) Math.random(), 1);
                        }
                        if (Math.random() > 0.5) {
                            //quad.enableRotation().rotation.value = (float) (Math.PI * 2 * Math.random());
                        }
                        //if (Math.random() > 0.7) {
                        quad.enableScale().scale.set(10, 10);
                                    //.set((float) Math.random() * 5, (float) Math.random() * 5);
                        //}
                        quads[i] = quad;
                    }*//*
                }

                @Override
                protected void onSupportDraw(BaseCanvas canvas) {
                    super.onSupportDraw(canvas);
                    canvas.save();
                    float scale = Math.max(640 / canvas.getWidth(), 480 / canvas.getHeight());
                    Vec2 startOffset = new Vec2(canvas.getWidth() / 2, canvas.getHeight() / 2)
                            .minus(640 * 0.5f / scale, 480 * 0.5f / scale);

                    canvas.translate(startOffset.x, startOffset.y).expendAxis(scale);//.translate(64, 48);



                    TextureQuadBatch batch = TextureQuadBatch.getDefaultBatch();
                    for (TextureQuad quad : quads) {
                        batch.add(quad);
                    }


                    canvas.restore();
                }
            };
            scene.attachChild(supportSprite);
        }*/
    }

    private void createOnlinePanel(Scene scene) {
        Config.loadOnlineConfig(context);
        OnlineManager.getInstance().Init(context);

        if (OnlineManager.getInstance().isStayOnline()) {
            Debug.i("Stay online, creating panel");
            OnlineScoring.getInstance().createPanel();
            final OnlinePanel panel = OnlineScoring.getInstance().getPanel();
            panel.setPosition(5, 5);
            scene.attachChild(panel);
            scene.registerTouchArea(panel.rect);
        }

        OnlineScoring.getInstance().login();
    }

    public void reloadOnlinePanel() {
        scene.detachChild(OnlineScoring.getInstance().getPanel());
        createOnlinePanel(scene);
    }

    public void musicControl(MusicOption option) {
        if (GlobalManager.getInstance().getSongService() == null || beatmapInfo == null) {
            return;
        }
        switch (option) {
            case PREV: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING || GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                    GlobalManager.getInstance().getSongService().stop();
                }
                firstTimingPoint = null;
                LibraryManager.getInstance().getPrevBeatmap();
                loadBeatmapInfo();
                loadTimeingPoints(true);
                doChange = false;
                doStop = false;
            }
            break;
            case PLAY: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED || GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
                    if (GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
                        loadTimeingPoints(false);
                        GlobalManager.getInstance().getSongService().preLoad(beatmapInfo.getMusic());
                        if (firstTimingPoint != null) {
                            bpmLength = firstTimingPoint.getBeatLength() * 1000f;
                            if (lastTimingPoint != null) {
                                offset = lastTimingPoint.getTime() * 1000f % bpmLength;
                            }
                        }
                    }
                    if (GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                        bpmLength = lastBpmLength;
                        if (lastTimingPoint != null) {
                            int position = GlobalManager.getInstance().getSongService().getPosition();
                            offset = (position - lastTimingPoint.getTime() * 1000f) % bpmLength;
                        }
                    }
                    Debug.i("BPM: " + 60 / bpmLength * 1000 + " Offset: " + offset);
//						ToastLogger.showText("BPM: " + 60 / bpmLength * 1000 + " Offset: " + offset, false);
                    GlobalManager.getInstance().getSongService().play();
                    doStop = false;
                }
            }
            break;
            case PAUSE: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
                    GlobalManager.getInstance().getSongService().pause();
                    lastBpmLength = bpmLength;
                    bpmLength = 1000;
                }
            }
            break;
            case STOP: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING || GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                    GlobalManager.getInstance().getSongService().stop();
                    lastBpmLength = bpmLength;
                    bpmLength = 1000;
                }
            }
            break;
            case NEXT: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING || GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                    GlobalManager.getInstance().getSongService().stop();
                }
                LibraryManager.getInstance().getNextBeatmap();
                firstTimingPoint = null;
                loadBeatmapInfo();
                loadTimeingPoints(true);
                doChange = false;
                doStop = false;
            }
            break;
            case SYNC: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
                    if (lastTimingPoint != null) {
                        int position = GlobalManager.getInstance().getSongService().getPosition();
                        offset = (position - lastTimingPoint.getTime() * 1000f) % bpmLength;
                    }
                    Debug.i("BPM: " + 60 / bpmLength * 1000 + " Offset: " + offset);
                }
            }
        }
    }

    @Override
    public void onUpdate(final float pSecondsElapsed) {
        beatPassTime += pSecondsElapsed * 1000f;
        if (isOnExitAnim) {
            for (Rectangle specRectangle : spectrum) {
                specRectangle.setWidth(0);
                specRectangle.setAlpha(0);
            }
            return;
        }

        if (GlobalManager.getInstance().getSongService() == null || !musicStarted || GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
            bpmLength = 1000;
            offset = 0;
        }

        if (doMenuShow == true && isMenuShowed == false) {
            logo.registerEntityModifier(new MoveXModifier(0.3f, Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, EaseExponentialOut.getInstance()));
            logoOverlay.registerEntityModifier(new MoveXModifier(0.3f, Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, EaseExponentialOut.getInstance()));
            for (int i = 0; i < spectrum.length; i++) {
                spectrum[i].registerEntityModifier(new MoveXModifier(0.3f, Config.getRES_WIDTH() / 2, Config.getRES_WIDTH() / 3, EaseExponentialOut.getInstance()));
            }
            play.registerEntityModifier(new ParallelEntityModifier(
                    new MoveXModifier(0.5f, menuBarX - 100, menuBarX, EaseElasticOut.getInstance()),
                    new org.anddev.andengine.entity.modifier.AlphaModifier(0.5f, 0, 0.9f, EaseCubicOut.getInstance())));
            options.registerEntityModifier(new ParallelEntityModifier(
                    new MoveXModifier(0.5f, menuBarX - 100, menuBarX, EaseElasticOut.getInstance()),
                    new org.anddev.andengine.entity.modifier.AlphaModifier(0.5f, 0, 0.9f, EaseCubicOut.getInstance())));
            exit.registerEntityModifier(new ParallelEntityModifier(
                    new MoveXModifier(0.5f, menuBarX - 100, menuBarX, EaseElasticOut.getInstance()),
                    new org.anddev.andengine.entity.modifier.AlphaModifier(0.5f, 0, 0.9f, EaseCubicOut.getInstance())));
            scene.registerTouchArea(play);
            scene.registerTouchArea(options);
            scene.registerTouchArea(exit);
            isMenuShowed = true;
        }

        if (doMenuShow == true && isMenuShowed == true) {
            if (showPassTime > 10000f) {
                scene.unregisterTouchArea(play);
                scene.unregisterTouchArea(options);
                scene.unregisterTouchArea(exit);
                play.registerEntityModifier(new ParallelEntityModifier(
                        new MoveXModifier(1f, menuBarX, menuBarX - 50, EaseExponentialOut.getInstance()),
                        new org.anddev.andengine.entity.modifier.AlphaModifier(1f, 0.9f, 0, EaseExponentialOut.getInstance())));
                options.registerEntityModifier(new ParallelEntityModifier(
                        new MoveXModifier(1f, menuBarX, menuBarX - 50, EaseExponentialOut.getInstance()),
                        new org.anddev.andengine.entity.modifier.AlphaModifier(1f, 0.9f, 0, EaseExponentialOut.getInstance())));
                exit.registerEntityModifier(new ParallelEntityModifier(
                        new MoveXModifier(1f, menuBarX, menuBarX - 50, EaseExponentialOut.getInstance()),
                        new org.anddev.andengine.entity.modifier.AlphaModifier(1f, 0.9f, 0, EaseExponentialOut.getInstance())));
                logo.registerEntityModifier(new MoveXModifier(1f, Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, Config.getRES_WIDTH() / 2 - logo.getWidth() / 2,
                        EaseBounceOut.getInstance()));
                logoOverlay.registerEntityModifier(new MoveXModifier(1f, Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, Config.getRES_WIDTH() / 2 - logo.getWidth() / 2,
                        EaseBounceOut.getInstance()));
                for (int i = 0; i < spectrum.length; i++) {
                    spectrum[i].registerEntityModifier(new MoveXModifier(1f, Config.getRES_WIDTH() / 3, Config.getRES_WIDTH() / 2, EaseBounceOut.getInstance()));
                }
                isMenuShowed = false;
                doMenuShow = false;
                showPassTime = 0;
            } else {
                showPassTime += pSecondsElapsed * 1000f;
            }
        }

//        if (offset != 0) {
//            beatPassTime += offset;
//            offset = 0;
//        }

        if (beatPassTime - lastBeatPassTime >= bpmLength - offset) {
            lastBeatPassTime = beatPassTime;
            offset = 0;
            if (logo != null) {
//				logo.clearEntityModifiers();
                logo.registerEntityModifier(new SequenceEntityModifier(new org.anddev.andengine.entity.modifier.ScaleModifier(bpmLength / 1000 * 0.9f, 1f, 1.07f),
                        new org.anddev.andengine.entity.modifier.ScaleModifier(bpmLength / 1000 * 0.07f, 1.07f, 1f)));
            }
        }

        if (GlobalManager.getInstance().getSongService() != null) {
            if (!musicStarted) {
                if (firstTimingPoint != null) {
                    bpmLength = firstTimingPoint.getBeatLength() * 1000f;
                } else {
                    return;
                }
                progressBar.setStartTime(0);
                GlobalManager.getInstance().getSongService().play();
                GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
                if (lastTimingPoint != null) {
                    offset = lastTimingPoint.getTime() * 1000f % bpmLength;
                }
                Debug.i("BPM: " + 60 / bpmLength * 1000 + " Offset: " + offset);
//				ToastLogger.showText("BPM: " + 60 / bpmLength * 1000 + " Offset: " + offset, false);
                musicStarted = true;
            }

            if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
//                syncPassedTime += pSecondsElapsed * 1000f;
                int position = GlobalManager.getInstance().getSongService().getPosition();
                progressBar.setTime(GlobalManager.getInstance().getSongService().getLength());
                progressBar.setPassedTime(position);
                progressBar.update(pSecondsElapsed * 1000);

//                if (syncPassedTime > bpmLength * 8) {
//                    musicControl(MusicOption.SYNC);
//                    syncPassedTime = 0;
//                }

                if (currentTimingPoint != null && position > currentTimingPoint.getTime() * 1000) {
                    if (!isContinuousKiai && currentTimingPoint.isKiai()) {
                        for (ParticleSystem particleSpout : particleSystem) {
                            particleSpout.setParticlesSpawnEnabled(true);
                        }
                        particleBeginTime = position;
                        particleEnabled = true;
                    }
                    isContinuousKiai = currentTimingPoint.isKiai();

                    if (timingPoints.size() > 0) {
                        currentTimingPoint = timingPoints.remove(0);
                        if (!currentTimingPoint.wasInderited()) {
                            lastTimingPoint = currentTimingPoint;
                            bpmLength = currentTimingPoint.getBeatLength() * 1000;
                            offset = lastTimingPoint.getTime() * 1000f % bpmLength;
                            Debug.i("BPM: " + 60 / bpmLength * 1000 + " Offset: " + offset);
//							ToastLogger.showText("BPM: " + 60 / bpmLength * 1000 + " Offset: " + offset, false);
                        }
                    } else {
                        currentTimingPoint = null;
                    }
                }

                if (particleEnabled && (position - particleBeginTime > 2000)) {
                    for (ParticleSystem particleSpout : particleSystem) {
                        particleSpout.setParticlesSpawnEnabled(false);
                    }
                    particleEnabled = false;
                }

                int windowSize = 240;
                int spectrumWidth = 120;
                float[] fft = GlobalManager.getInstance().getSongService().getSpectrum();
                if (fft == null) return;
                for (int i = 0, leftBound = 0; i < spectrumWidth; i++) {
                    float peak = 0;
                    int rightBound = (int) Math.pow(2., i * 9. / (windowSize - 1));
                    if (rightBound <= leftBound) rightBound = leftBound + 1;
                    if (rightBound > 511) rightBound = 511;

                    for (; leftBound < rightBound; leftBound++) {
                        if (peak < fft[1 + leftBound])
                            peak = fft[1 + leftBound];
                    }

                    float initialAlpha = 0.4f;
                    float gradient = 20;
                    float currPeakLevel = peak * 500;

                    if (currPeakLevel > peakLevel[i]) {
                        peakLevel[i] = currPeakLevel;
                        peakDownRate[i] = peakLevel[i] / gradient;
                        peakAlpha[i] = initialAlpha;

                    } else {
                        peakLevel[i] = Math.max(peakLevel[i] - peakDownRate[i], 0f);
                        peakAlpha[i] = Math.max(peakAlpha[i] - initialAlpha / gradient, 0f);
                    }

                    spectrum[i].setWidth(250f + peakLevel[i]);
                    spectrum[i].setAlpha(peakAlpha[i]);
                }
            } else {
                for (Rectangle specRectangle : spectrum) {
                    specRectangle.setWidth(0);
                    specRectangle.setAlpha(0);
                }
                if (!doChange && !doStop && GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getPosition() >= GlobalManager.getInstance().getSongService().getLength()) {
                    musicControl(MusicOption.NEXT);
                }
            }
        }
    }

    @Override
    public void reset() {

    }

    public void loadBeatmap() {
        LibraryManager.getInstance().shuffleLibrary();
        loadBeatmapInfo();
        loadTimeingPoints(true);
    }

    public void loadBeatmapInfo() {
        if (LibraryManager.getInstance().getSizeOfBeatmaps() != 0) {
            beatmapInfo = LibraryManager.getInstance().getBeatmap();
            Log.w("MainMenuActivity", "Next song: " + beatmapInfo.getMusic() + ", Start at: " + beatmapInfo.getPreviewTime());

            if (musicInfoText == null) {
                musicInfoText = new ChangeableText(Utils.toRes(Config.getRES_WIDTH() - 500), Utils.toRes(3),
                        ResourceManager.getInstance().getFont("font"), "None...", HorizontalAlign.RIGHT, 35);
            }
            if (beatmapInfo.getArtistUnicode() != null && beatmapInfo.getTitleUnicode() != null && Config.isForceRomanized() == false) {
                musicInfoText.setText(beatmapInfo.getArtistUnicode() + " - " + beatmapInfo.getTitleUnicode(), true);
            } else if (beatmapInfo.getArtist() != null && beatmapInfo.getTitle() != null) {
                musicInfoText.setText(beatmapInfo.getArtist() + " - " + beatmapInfo.getTitle(), true);
            } else {
                musicInfoText.setText("Failure to load QAQ", true);
            }
            try {
                musicInfoText.setPosition(Utils.toRes(Config.getRES_WIDTH() - 500 + 470 - musicInfoText.getWidth()), musicInfoText.getY());
                music_nowplay.setPosition(Utils.toRes(Config.getRES_WIDTH() - 500 + 470 - musicInfoText.getWidth() - 130), 0);
            } catch (NullPointerException e) {
                musicInfoText.setPosition(Utils.toRes(Config.getRES_WIDTH() - 500 + 470 - 200), 5);
                music_nowplay.setPosition(Utils.toRes(Config.getRES_WIDTH() - 500 + 470 - 200 - 130), 0);
            }
        }
    }

    public void loadTimeingPoints(boolean reloadMusic) {
        if (beatmapInfo == null) {
            return;
        }

        for (ParticleSystem particleSpout : particleSystem) {
            particleSpout.setParticlesSpawnEnabled(false);
        }
        particleEnabled = false;

        ArrayList<TrackInfo> trackInfos = beatmapInfo.getTracks();
        if (trackInfos != null && trackInfos.size() > 0) {
            int trackIndex = random.nextInt(trackInfos.size());
            selectedTrack = trackInfos.get(trackIndex);

            if (selectedTrack.getBackground() != null) {
                try {
                    final TextureRegion tex = Config.isSafeBeatmapBg() ?
                        ResourceManager.getInstance().getTexture("menu-background") :
                        ResourceManager.getInstance().loadBackground(selectedTrack.getBackground());

                    if (tex != null) {
                        float height = tex.getHeight();
                        height *= Config.getRES_WIDTH()
                                / (float) tex.getWidth();
                        background = new Sprite(0,
                                (Config.getRES_HEIGHT() - height) / 2, Config
                                .getRES_WIDTH(), height, tex);
                        lastBackground.registerEntityModifier(new org.anddev.andengine.entity.modifier.AlphaModifier(1.5f, 1, 0, new IEntityModifier.IEntityModifierListener() {
                            @Override
                            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                                scene.attachChild(background, 0);
                            }

                            @Override
                            public void onModifierFinished(IModifier<IEntity> pModifier, final IEntity pItem) {
                                GlobalManager.getInstance().getMainActivity().runOnUpdateThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // TODO Auto-generated method stub
                                        pItem.detachSelf();
                                    }
                                });
                            }
                        }));
                        lastBackground = background;
                    }
                } catch (Exception e) {
                    Debug.e(e.toString());
                    lastBackground.setAlpha(0);
                }
            } else {
                lastBackground.setAlpha(0);
            }

            if (reloadMusic) {
                if (GlobalManager.getInstance().getSongService() != null) {
                    GlobalManager.getInstance().getSongService().preLoad(beatmapInfo.getMusic());
                    musicStarted = false;
                } else {
                    Log.w("nullpoint", "GlobalManager.getInstance().getSongService() is null while reload music (MainScene.loadTimeingPoints)");
                }
            }

            Arrays.fill(peakLevel, 0f);
            Arrays.fill(peakDownRate, 1f);
            Arrays.fill(peakAlpha, 0f);

            OSUParser parser = new OSUParser(selectedTrack.getFilename());
            if (parser.openFile()) {
                beatmapData = parser.readData();

                timingPoints = new LinkedList<TimingPoint>();
                currentTimingPoint = null;
                for (final String s : beatmapData.getData("TimingPoints")) {
                    final TimingPoint tp = new TimingPoint(s.split("[,]"), currentTimingPoint);
                    timingPoints.add(tp);
                    if (tp.wasInderited() == false || currentTimingPoint == null) {
                        currentTimingPoint = tp;
                    }
                }
                firstTimingPoint = timingPoints.remove(0);
                currentTimingPoint = firstTimingPoint;
                lastTimingPoint = currentTimingPoint;
                bpmLength = firstTimingPoint.getBeatLength() * 1000f;
            }
        }
    }

    public void showExitDialog() {
        GlobalManager.getInstance().getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                new ConfirmDialogFragment().setMessage(R.string.dialog_exit_message).showForResult(
                    isAccepted -> {
                        if (isAccepted) {
                            exit();
                        }
                    }
                );
            }
        });
    }

    public void exit() {
        if (isOnExitAnim) {
            return;
        }
        isOnExitAnim = true;

        PowerManager.WakeLock wakeLock = GlobalManager.getInstance().getMainActivity().getWakeLock();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        scene.unregisterTouchArea(play);
        scene.unregisterTouchArea(options);
        scene.unregisterTouchArea(exit);

        play.setAlpha(0);
        options.setAlpha(0);
        exit.setAlpha(0);

        //ResourceManager.getInstance().loadSound("seeya", "sfx/seeya.wav", false).play();
        //Allow customize Seeya Sounds from Skins
        BassSoundProvider exitsound = ResourceManager.getInstance().getSound("seeya");
        if (exitsound != null) {
            exitsound.play();
        }
        
        Rectangle bg = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT());
        bg.setColor(0, 0, 0, 1.0f);
        bg.registerEntityModifier(ModifierFactory.newAlphaModifier(3.0f, 0, 1));
        scene.attachChild(bg);
        logo.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                ModifierFactory.newScaleModifier(3.0f, 1f, 0.8f)
        ));
        logoOverlay.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                ModifierFactory.newScaleModifier(3.0f, 1f, 0.8f)
        ));

        ScheduledExecutorService taskPool = Executors.newScheduledThreadPool(1);
        taskPool.schedule(new TimerTask() {
            @Override
            public void run() {
                if (GlobalManager.getInstance().getSongService() != null) {
                    GlobalManager.getInstance().getSongService().hideNotifyPanel();
                    if (Build.VERSION.SDK_INT > 10)
                        GlobalManager.getInstance().getMainActivity().unregisterReceiver(GlobalManager.getInstance().getMainActivity().onNotifyButtonClick);
                    GlobalManager.getInstance().getMainActivity().unbindService(GlobalManager.getInstance().getMainActivity().connection);
                    GlobalManager.getInstance().getMainActivity().stopService(new Intent(GlobalManager.getInstance().getMainActivity(), SongService.class));
                    musicStarted = false;
                }
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 3000, TimeUnit.MILLISECONDS);
    }

    public void restart() {
        MainActivity mActivity = GlobalManager.getInstance().getMainActivity();
        mActivity.runOnUiThread(() -> new ConfirmDialogFragment().setMessage(R.string.dialog_dither_confirm).showForResult(
                isAccepted -> {
                    if (isAccepted) {
                        Intent mIntent = new Intent(mActivity, MainActivity.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        mActivity.startActivity(mIntent);
                        System.exit(0);
                    }
                }
        ));
    }

    public Scene getScene() {
        return scene;
    }

    public BeatmapInfo getBeatmapInfo() {
        return beatmapInfo;
    }

    public void setBeatmap(BeatmapInfo info) {
        int playIndex = LibraryManager.getInstance().findBeatmap(info);
        Debug.i("index " + playIndex);
        loadBeatmapInfo();
        loadTimeingPoints(false);
        musicControl(MusicOption.SYNC);
    }

    public void watchReplay(String replayFile) {
        replay = new Replay();
        if (replay.loadInfo(replayFile)) {
            if (replay.replayVersion >= 3) {
                //replay
                ScoringScene scorescene = GlobalManager.getInstance().getScoring();
                StatisticV2 stat = replay.getStat();
                TrackInfo track = LibraryManager.getInstance().findTrackByFileNameAndMD5(replay.getMapFile(), replay.getMd5());
                if (track != null) {
                    GlobalManager.getInstance().getMainScene().setBeatmap(track.getBeatmap());
                    GlobalManager.getInstance().getSongMenu().select();
                    ResourceManager.getInstance().loadBackground(track.getBackground());
                    GlobalManager.getInstance().getSongService().preLoad(track.getBeatmap().getMusic());
                    GlobalManager.getInstance().getSongService().play();
                    scorescene.load(stat, null, ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance().getSongService(), replayFile, null, track);
                    GlobalManager.getInstance().getEngine().setScene(scorescene.getScene());
                }
            }
        }
    }

    public void show() {
        GlobalManager.getInstance().getSongService().setGaming(false);
        GlobalManager.getInstance().getEngine().setScene(getScene());
        if (GlobalManager.getInstance().getSelectedTrack() != null) {
            setBeatmap(GlobalManager.getInstance().getSelectedTrack().getBeatmap());
        }
    }

    public enum MusicOption {PREV, PLAY, PAUSE, STOP, NEXT, SYNC}
}
