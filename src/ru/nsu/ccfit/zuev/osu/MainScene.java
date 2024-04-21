package ru.nsu.ccfit.zuev.osu;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import com.edlplan.ui.fragment.ConfirmDialogFragment;
import com.reco1l.framework.lang.Execution;
import com.reco1l.legacy.ui.MainMenu;

import com.reco1l.legacy.ui.beatmapdownloader.BeatmapListing;
import com.rian.osu.beatmap.parser.BeatmapParser;
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
import ru.nsu.ccfit.zuev.osu.game.SongProgressBar;
import ru.nsu.ccfit.zuev.osu.game.TimingPoint;
import ru.nsu.ccfit.zuev.osu.helper.ModifierFactory;
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
    private Sprite logo, logoOverlay, background, lastBackground;
    private Sprite music_nowplay;
    private Scene scene;
    private ChangeableText musicInfoText;
    private final Random random = new Random();
    private final Rectangle[] spectrum = new Rectangle[120];
    private final float[] peakLevel = new float[120];
    private final float[] peakDownRate = new float[120];
    private final float[] peakAlpha = new float[120];
    private List<TimingPoint> timingPoints;
    private TimingPoint currentTimingPoint, lastTimingPoint, firstTimingPoint;

    private int particleBeginTime = 0;
    private boolean particleEnabled = false;
    private boolean isContinuousKiai = false;

    private final ParticleSystem[] particleSystem = new ParticleSystem[2];

    //private BassAudioPlayer music;

    private boolean musicStarted;
    private BassSoundProvider hitsound;

    private double bpmLength = 1000;
    private double lastBpmLength = 0;
    private double offset = 0;
    private float beatPassTime = 0;
    private float lastBeatPassTime = 0;
    private boolean doChange = false;
    private boolean doStop = false;
    private long lastHit = 0;
    public boolean isOnExitAnim = false;

    private boolean isMenuShowed = false;
    private boolean doMenuShow = false;
    private float showPassTime = 0, syncPassedTime = 0;
    private float menuBarX = 0, playY, exitY;

    private MainMenu menu;


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
        logo = new Sprite((float) Config.getRES_WIDTH() / 2 - (float) logotex.getWidth() / 2, (float) Config.getRES_HEIGHT() / 2 - (float) logotex.getHeight() / 2, logotex) {
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
                    if (doMenuShow && isMenuShowed) {
                        showPassTime = 20000;
                    }
                    if (!doMenuShow && !isMenuShowed && logo.getX() == (Config.getRES_WIDTH() - logo.getWidth()) / 2) {
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

        logoOverlay = new Sprite((float) Config.getRES_WIDTH() / 2 - (float) logotex.getWidth() / 2, (float) Config.getRES_HEIGHT() / 2 - (float) logotex.getHeight() / 2, logotex);
        logoOverlay.setScale(1.07f);
        logoOverlay.setAlpha(0.2f);

        menu = new MainMenu(this);

        final Text author = new Text(10, 530, ResourceManager
                .getInstance().getFont("font"),
                String.format(
                        Locale.getDefault(),
                        "osu!droid %s\nby osu!droid Team\nosu! is © peppy 2007-2023",
                        BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"
                )) {


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    new ConfirmDialogFragment().setMessage(R.string.dialog_visit_osu_website_message).showForResult(
                            isAccepted -> {
                                if (isAccepted) {
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
                "            Global Ranking\n   Provided by iBancho") {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    new ConfirmDialogFragment().setMessage(R.string.dialog_visit_osudroid_website_message).showForResult(
                            isAccepted -> {
                                if (isAccepted) {
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
        music_nowplay = new Sprite(Utils.toRes(Config.getRES_WIDTH() - 500), 0, (float) (40 * nptex.getWidth()) / nptex.getHeight(), 40, nptex);

        final Rectangle bgTopRect = new Rectangle(0, 0, Config.getRES_WIDTH(), Utils.toRes(120));
        bgTopRect.setColor(0, 0, 0, 0.3f);

        final Rectangle bgbottomRect = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Math.max(author.getHeight(), yasonline.getHeight()) + Utils.toRes(15));
        bgbottomRect.setPosition(0, Config.getRES_HEIGHT() - bgbottomRect.getHeight());
        bgbottomRect.setColor(0, 0, 0, 0.3f);

        for (int i = 0; i < 120; i++) {
            final float pX = (float) Config.getRES_WIDTH() / 2;
            final float pY = (float) Config.getRES_HEIGHT() / 2;

            spectrum[i] = new Rectangle(pX, pY, 260, 10);
            spectrum[i].setRotationCenter(0, 5);
            spectrum[i].setScaleCenter(0, 5);
            spectrum[i].setRotation(-220 + i * 3f);
            spectrum[i].setAlpha(0.0f);

            scene.attachChild(spectrum[i]);
        }

        LibraryManager.INSTANCE.loadLibraryCache(false);

        TextureRegion starRegion = ResourceManager.getInstance().getTexture("star");

        {
            particleSystem[0] = new ParticleSystem(new PointParticleEmitter(-40, (float) (Config.getRES_HEIGHT() * 3) / 4), 32, 48, 128, starRegion);
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
            particleSystem[1] = new ParticleSystem(new PointParticleEmitter(Config.getRES_WIDTH(), (float) (Config.getRES_HEIGHT() * 3) / 4), 32, 48, 128, starRegion);
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

        TextureRegion chimuTex = ResourceManager.getInstance().getTexture("chimu");
        Sprite chimu = new Sprite(Config.getRES_WIDTH() - chimuTex.getWidth(), (Config.getRES_HEIGHT() - chimuTex.getHeight()) / 2f, chimuTex) {
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    doStop = true;
                    return true;
                }

                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    musicControl(MusicOption.STOP);
                    BeatmapListing.INSTANCE.show();
                    return true;
                }

                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY);
            }
        };

        menu.getFirst().setAlpha(0f);
        menu.getSecond().setAlpha(0f);
        menu.getThird().setAlpha(0f);

        logo.setPosition((Config.getRES_WIDTH() - logo.getWidth()) / 2, (Config.getRES_HEIGHT() - logo.getHeight()) / 2);
        logoOverlay.setPosition((Config.getRES_WIDTH() - logo.getWidth()) / 2, (Config.getRES_HEIGHT() - logo.getHeight()) / 2);

        menu.getSecond().setScale(Config.getRES_WIDTH() / 1024f);
        menu.getFirst().setScale(Config.getRES_WIDTH() / 1024f);
        menu.getThird().setScale(Config.getRES_WIDTH() / 1024f);

        menu.getSecond().setPosition(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 3f, (Config.getRES_HEIGHT() - menu.getSecond().getHeight()) / 2);
        menu.getFirst().setPosition(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 3f, menu.getSecond().getY() - menu.getFirst().getHeight() - 40 * Config.getRES_WIDTH() / 1024f);
        menu.getThird().setPosition(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 3f, menu.getSecond().getY() + menu.getSecond().getHeight() + 40 * Config.getRES_WIDTH() / 1024f);

        menuBarX = menu.getFirst().getX();
        playY = menu.getFirst().getScaleY();
        exitY = menu.getThird().getScaleY();

        scene.attachChild(lastBackground, 0);
        scene.attachChild(bgTopRect);
        scene.attachChild(bgbottomRect);
        scene.attachChild(author);
        scene.attachChild(yasonline);

        menu.attachButtons();

        scene.attachChild(logo);
        scene.attachChild(logoOverlay);
        scene.attachChild(music_nowplay);
        scene.attachChild(musicInfoText);
        scene.attachChild(music_prev);
        scene.attachChild(music_play);
        scene.attachChild(music_pause);
        scene.attachChild(music_stop);
        scene.attachChild(music_next);
        scene.attachChild(chimu);

        scene.registerTouchArea(logo);
        scene.registerTouchArea(author);
        scene.registerTouchArea(chimu);
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

        hitsound = ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false);
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
        // IndexOutOfBoundsException 141 fix
        Execution.updateThread(() -> {
            scene.detachChild(OnlineScoring.getInstance().getPanel());
            createOnlinePanel(scene);
        });
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
                LibraryManager.INSTANCE.getPrevBeatmap();
                loadBeatmapInfo();
                loadTimingPoints(true);
                doChange = false;
                doStop = false;
            }
            break;
            case PLAY: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED || GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
                    if (GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
                        loadTimingPoints(false);
                        GlobalManager.getInstance().getSongService().preLoad(beatmapInfo.getMusic());
                        if (firstTimingPoint != null) {
                            bpmLength = firstTimingPoint.getBeatLength() * 1000f;
                            if (lastTimingPoint != null) {
                                offset = lastTimingPoint.getTime() * 1000f % bpmLength;
                            }
                        }
                    }
                    if (GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                        if (lastBpmLength > 0) {
                            bpmLength = lastBpmLength;
                        }
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
                LibraryManager.INSTANCE.getNextBeatmap();
                firstTimingPoint = null;
                loadBeatmapInfo();
                loadTimingPoints(true);
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

        if (doMenuShow && !isMenuShowed) {
            logo.registerEntityModifier(new MoveXModifier(0.3f, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, EaseExponentialOut.getInstance()));
            logoOverlay.registerEntityModifier(new MoveXModifier(0.3f, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, EaseExponentialOut.getInstance()));
            for (Rectangle rectangle : spectrum) {
                rectangle.registerEntityModifier(new MoveXModifier(0.3f, (float) Config.getRES_WIDTH() / 2, (float) Config.getRES_WIDTH() / 3, EaseExponentialOut.getInstance()));
            }
            menu.getFirst().registerEntityModifier(new ParallelEntityModifier(
                    new MoveXModifier(0.5f, menuBarX - 100, menuBarX, EaseElasticOut.getInstance()),
                    new org.anddev.andengine.entity.modifier.AlphaModifier(0.5f, 0, 0.9f, EaseCubicOut.getInstance())));
            menu.getSecond().registerEntityModifier(new ParallelEntityModifier(
                    new MoveXModifier(0.5f, menuBarX - 100, menuBarX, EaseElasticOut.getInstance()),
                    new org.anddev.andengine.entity.modifier.AlphaModifier(0.5f, 0, 0.9f, EaseCubicOut.getInstance())));
            menu.getThird().registerEntityModifier(new ParallelEntityModifier(
                    new MoveXModifier(0.5f, menuBarX - 100, menuBarX, EaseElasticOut.getInstance()),
                    new org.anddev.andengine.entity.modifier.AlphaModifier(0.5f, 0, 0.9f, EaseCubicOut.getInstance())));
            scene.registerTouchArea(menu.getFirst());
            scene.registerTouchArea(menu.getSecond());
            scene.registerTouchArea(menu.getThird());
            isMenuShowed = true;
        }

        if (doMenuShow) {
            if (showPassTime > 10000f) {

                menu.showFirstMenu();
                scene.unregisterTouchArea(menu.getFirst());
                scene.unregisterTouchArea(menu.getSecond());
                scene.unregisterTouchArea(menu.getThird());

                menu.getFirst().registerEntityModifier(new ParallelEntityModifier(
                        new MoveXModifier(1f, menuBarX, menuBarX - 50, EaseExponentialOut.getInstance()),
                        new org.anddev.andengine.entity.modifier.AlphaModifier(1f, 0.9f, 0, EaseExponentialOut.getInstance())));
                menu.getSecond().registerEntityModifier(new ParallelEntityModifier(
                        new MoveXModifier(1f, menuBarX, menuBarX - 50, EaseExponentialOut.getInstance()),
                        new org.anddev.andengine.entity.modifier.AlphaModifier(1f, 0.9f, 0, EaseExponentialOut.getInstance())));
                menu.getThird().registerEntityModifier(new ParallelEntityModifier(
                        new MoveXModifier(1f, menuBarX, menuBarX - 50, EaseExponentialOut.getInstance()),
                        new org.anddev.andengine.entity.modifier.AlphaModifier(1f, 0.9f, 0, EaseExponentialOut.getInstance())));

                logo.registerEntityModifier(new MoveXModifier(1f, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2,
                        EaseBounceOut.getInstance()));
                logoOverlay.registerEntityModifier(new MoveXModifier(1f, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, EaseBounceOut.getInstance()));

                for (Rectangle rectangle : spectrum) {
                    rectangle.registerEntityModifier(new MoveXModifier(1f, (float) Config.getRES_WIDTH() / 3, (float) Config.getRES_WIDTH() / 2, EaseBounceOut.getInstance()));
                }
                isMenuShowed = false;
                doMenuShow = false;
                showPassTime = 0;
            } else {
                showPassTime += pSecondsElapsed * 1000f;
            }
        }

        if (beatPassTime - lastBeatPassTime >= bpmLength - offset) {
            lastBeatPassTime = beatPassTime;
            offset = 0;
            if (logo != null) {
                logo.registerEntityModifier(new SequenceEntityModifier(new org.anddev.andengine.entity.modifier.ScaleModifier((float) (bpmLength / 1000 * 0.9f), 1f, 1.07f),
                        new org.anddev.andengine.entity.modifier.ScaleModifier((float) (bpmLength / 1000 * 0.07f), 1.07f, 1f)));
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
        LibraryManager.INSTANCE.shuffleLibrary();
        loadBeatmapInfo();
        loadTimingPoints(true);
    }

    public void loadBeatmapInfo() {
        if (LibraryManager.INSTANCE.getSizeOfBeatmaps() != 0) {
            beatmapInfo = LibraryManager.INSTANCE.getBeatmap();
            Log.w("MainMenuActivity", "Next song: " + beatmapInfo.getMusic() + ", Start at: " + beatmapInfo.getPreviewTime());

            if (musicInfoText == null) {
                musicInfoText = new ChangeableText(Utils.toRes(Config.getRES_WIDTH() - 500), Utils.toRes(3),
                        ResourceManager.getInstance().getFont("font"), "None...", HorizontalAlign.RIGHT, 35);
            }
            if (beatmapInfo.getArtistUnicode() != null && beatmapInfo.getTitleUnicode() != null && !Config.isForceRomanized()) {
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

    public void loadTimingPoints(boolean reloadMusic) {
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
            TrackInfo selectedTrack = trackInfos.get(trackIndex);
            GlobalManager.getInstance().setSelectedTrack(selectedTrack);

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
                                GlobalManager.getInstance().getMainActivity().runOnUpdateThread(pItem::detachSelf);
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

            try (var parser = new BeatmapParser(selectedTrack.getFilename())) {
                var beatmap = parser.parse(false);

                if (beatmap != null) {
                    timingPoints = new LinkedList<>();

                    for (final String s : beatmap.rawTimingPoints) {
                        final TimingPoint tp = new TimingPoint(s.split(","), currentTimingPoint);
                        timingPoints.add(tp);
                        if (!tp.wasInderited() || currentTimingPoint == null) {
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
    }

    public void showExitDialog() {
        GlobalManager.getInstance().getMainActivity().runOnUiThread(() -> new ConfirmDialogFragment().setMessage(R.string.dialog_exit_message).showForResult(
                isAccepted -> {
                    if (isAccepted) {
                        exit();
                    }
                }
        ));
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

        scene.unregisterTouchArea(menu.getFirst());
        scene.unregisterTouchArea(menu.getSecond());
        scene.unregisterTouchArea(menu.getThird());

        menu.getFirst().setAlpha(0);
        menu.getSecond().setAlpha(0);
        menu.getThird().setAlpha(0);

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

        if (GlobalManager.getInstance().getSongService() != null) {
            GlobalManager.getInstance().getSongService().stop();
        }

        ScheduledExecutorService taskPool = Executors.newScheduledThreadPool(1);
        taskPool.schedule(new TimerTask() {
            @Override
            public void run() {
                GlobalManager.getInstance().getMainActivity().finish();
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
        int playIndex = LibraryManager.INSTANCE.findBeatmap(info);
        Debug.i("index " + playIndex);
        loadBeatmapInfo();
        loadTimingPoints(false);
        musicControl(MusicOption.SYNC);
    }

    public void watchReplay(String replayFile) {
        Replay replay = new Replay();
        if (replay.loadInfo(replayFile)) {
            if (replay.replayVersion >= 3) {
                //replay
                ScoringScene scorescene = GlobalManager.getInstance().getScoring();
                StatisticV2 stat = replay.getStat();
                TrackInfo track = LibraryManager.INSTANCE.findTrackByFileNameAndMD5(replay.getMapFile(), replay.getMd5());
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