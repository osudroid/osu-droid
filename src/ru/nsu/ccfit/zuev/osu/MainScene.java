package ru.nsu.ccfit.zuev.osu;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.Execution;
import com.reco1l.osu.ui.entity.MainMenu;

import com.reco1l.osu.beatmaplisting.BeatmapListing;
import com.reco1l.osu.ui.MessageDialog;
import com.rian.osu.beatmap.parser.BeatmapParser;
import com.rian.osu.beatmap.timings.EffectControlPoint;
import com.rian.osu.beatmap.timings.TimingControlPoint;

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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.game.LinearSongProgress;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

/**
 * Created by Fuuko on 2015/4/24.
 */
public class MainScene implements IUpdateHandler {
    public LinearSongProgress progressBar;
    public BeatmapInfo beatmapInfo;
    private Context context;
    private Sprite logo, logoOverlay, background, lastBackground;
    private Sprite music_nowplay;
    private Scene scene;
    private ChangeableText musicInfoText;
    private final Rectangle[] spectrum = new Rectangle[120];
    private final float[] peakLevel = new float[120];
    private final float[] peakDownRate = new float[120];
    private final float[] peakAlpha = new float[120];
    private LinkedList<TimingControlPoint> timingControlPoints;
    private LinkedList<EffectControlPoint> effectControlPoints;
    private TimingControlPoint currentTimingPoint;
    private EffectControlPoint currentEffectPoint;

    private int particleBeginTime = 0;
    private boolean particleEnabled = false;
    private boolean isContinuousKiai = false;

    private final ParticleSystem[] particleSystem = new ParticleSystem[2];

    private boolean musicStarted;
    private BassSoundProvider hitsound;

    private double bpmLength = 1000;
    private double beatPassTime = 0;
    private boolean doChange = false;
    private boolean doStop = false;
    private long lastHit = 0;
    public boolean isOnExitAnim = false;

    private boolean isMenuShowed = false;
    private boolean doMenuShow = false;
    private float showPassTime = 0;
    private float menuBarX = 0;

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
                        "osu!droid %s\nby osu!droid Team\nosu! is © peppy 2007-2024",
                        BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"
                )) {


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {

                    new MessageDialog()
                        .setMessage(context.getString(com.osudroid.resources.R.string.dialog_visit_osu_website))
                        .addButton("Yes", dialog -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://osu.ppy.sh"));
                            GlobalManager.getInstance().getMainActivity().startActivity(browserIntent);
                            dialog.dismiss();
                            return null;
                        })
                        .addButton("No", dialog -> {
                            dialog.dismiss();
                            return null;
                        })
                        .show();

                    return true;
                }
                return false;
            }
        };
        author.setPosition(10, Config.getRES_HEIGHT() - author.getHeight() - 10);

        final Text yasonline = new Text(720, 530, ResourceManager.getInstance().getFont("font"), "            Global Ranking\n   Provided by iBancho") {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {

                    new MessageDialog()
                        .setMessage(context.getString(com.osudroid.resources.R.string.dialog_visit_osudroid_website))
                        .addButton("Yes", dialog -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + OnlineManager.hostname));
                            GlobalManager.getInstance().getMainActivity().startActivity(browserIntent);
                            dialog.dismiss();
                            return null;
                        })
                        .addButton("No", dialog -> {
                            dialog.dismiss();
                            return null;
                        })
                        .show();

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

        TextureRegion beatmapDownloaderTex = ResourceManager.getInstance().getTexture("beatmap_downloader");
        Sprite beatmapDownloader = new Sprite(Config.getRES_WIDTH() - beatmapDownloaderTex.getWidth(), (Config.getRES_HEIGHT() - beatmapDownloaderTex.getHeight()) / 2f, beatmapDownloaderTex) {
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    doStop = true;
                    return true;
                }

                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    new BeatmapListing().show();
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

        menu.getSecond().setPosition(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 2.5f, (Config.getRES_HEIGHT() - menu.getSecond().getHeight()) / 2);
        menu.getFirst().setPosition(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 2.5f, menu.getSecond().getY() - menu.getFirst().getHeight() - 40 * Config.getRES_WIDTH() / 1024f);
        menu.getThird().setPosition(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 2.5f, menu.getSecond().getY() + menu.getThird().getHeight() + 40 * Config.getRES_WIDTH() / 1024f);

        menuBarX = menu.getFirst().getX();

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
        scene.attachChild(beatmapDownloader);

        scene.registerTouchArea(logo);
        scene.registerTouchArea(author);
        scene.registerTouchArea(beatmapDownloader);
        scene.registerTouchArea(yasonline);
        scene.registerTouchArea(music_prev);
        scene.registerTouchArea(music_play);
        scene.registerTouchArea(music_pause);
        scene.registerTouchArea(music_stop);
        scene.registerTouchArea(music_next);
        scene.setTouchAreaBindingEnabled(true);

        progressBar = new LinearSongProgress(null, scene, 0, 0, new PointF(Utils.toRes(Config.getRES_WIDTH() - 320), Utils.toRes(100)));
        progressBar.setProgressRectColor(new RGBColor(0.9f, 0.9f, 0.9f));
        progressBar.setProgressRectAlpha(0.8f);

        createOnlinePanel(scene);
        scene.registerUpdateHandler(this);

        hitsound = ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false);
    }

    private void createOnlinePanel(Scene scene) {
        Config.loadOnlineConfig(context);
        OnlineManager.getInstance().init();

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
                currentTimingPoint = null;
                LibraryManager.selectPreviousBeatmapSet();
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
                        GlobalManager.getInstance().getSongService().preLoad(beatmapInfo.getAudioPath());

                        if (currentTimingPoint != null) {
                            bpmLength = currentTimingPoint.msPerBeat;
                            beatPassTime = 0;
                        }
                    }
                    if (GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED && currentTimingPoint != null) {
                        bpmLength = currentTimingPoint.msPerBeat;
                        int position = GlobalManager.getInstance().getSongService().getPosition();
                        beatPassTime = (position - currentTimingPoint.time) % bpmLength;
                    }

                    GlobalManager.getInstance().getSongService().play();
                    doStop = false;
                }
            }
            break;
            case PAUSE: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
                    GlobalManager.getInstance().getSongService().pause();
                    bpmLength = 1000;
                    beatPassTime = 0;
                }
            }
            break;
            case STOP: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING || GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                    GlobalManager.getInstance().getSongService().stop();
                    bpmLength = 1000;
                    beatPassTime = 0;
                }
            }
            break;
            case NEXT: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING || GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                    GlobalManager.getInstance().getSongService().stop();
                }
                LibraryManager.selectNextBeatmapSet();
                currentTimingPoint = null;
                loadBeatmapInfo();
                loadTimingPoints(true);
                doChange = false;
                doStop = false;
            }
            break;
            case SYNC: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING && currentTimingPoint != null) {
                    int position = GlobalManager.getInstance().getSongService().getPosition();
                    beatPassTime = (position - currentTimingPoint.time) % bpmLength;
                }
            }
        }
    }

    @Override
    public void onUpdate(final float pSecondsElapsed) {
        beatPassTime += pSecondsElapsed * 1000;
        if (isOnExitAnim) {
            for (Rectangle specRectangle : spectrum) {
                specRectangle.setWidth(0);
                specRectangle.setAlpha(0);
            }
            return;
        }

        if (GlobalManager.getInstance().getSongService() == null || !musicStarted || GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
            bpmLength = 1000;
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

        if (beatPassTime >= bpmLength) {
            beatPassTime %= bpmLength;

            if (logo != null) {
                logo.registerEntityModifier(new SequenceEntityModifier(new org.anddev.andengine.entity.modifier.ScaleModifier((float) (bpmLength / 1000 * 0.9f), 1f, 1.07f),
                        new org.anddev.andengine.entity.modifier.ScaleModifier((float) (bpmLength / 1000 * 0.07f), 1.07f, 1f)));
            }
        }

        if (GlobalManager.getInstance().getSongService() != null) {
            if (!musicStarted) {
                if (currentTimingPoint == null) {
                    return;
                }

                bpmLength = currentTimingPoint.msPerBeat;
                beatPassTime = 0;
                progressBar.setStartTime(0);
                GlobalManager.getInstance().getSongService().play();
                GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
//				ToastLogger.showText("BPM: " + 60 / bpmLength * 1000 + " Offset: " + offset, false);
                musicStarted = true;
            }

            if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
//                syncPassedTime += pSecondsElapsed * 1000f;
                int position = GlobalManager.getInstance().getSongService().getPosition();
                progressBar.setTime(GlobalManager.getInstance().getSongService().getLength());
                progressBar.setPassedTime(position);
                progressBar.update(pSecondsElapsed * 1000);

                if (!timingControlPoints.isEmpty() && position > timingControlPoints.peek().time) {
                    while (!timingControlPoints.isEmpty() && position > timingControlPoints.peek().time) {
                        currentTimingPoint = timingControlPoints.pop();
                    }

                    bpmLength = currentTimingPoint.msPerBeat;
                    beatPassTime = (position - currentTimingPoint.time) % bpmLength;
                }

                if (!effectControlPoints.isEmpty() && position > effectControlPoints.peek().time) {
                    while (!effectControlPoints.isEmpty() && position > effectControlPoints.peek().time) {
                        currentEffectPoint = effectControlPoints.pop();
                    }

                    if (!isContinuousKiai && currentEffectPoint.isKiai) {
                        for (ParticleSystem particleSpout : particleSystem) {
                            particleSpout.setParticlesSpawnEnabled(true);
                        }
                        particleBeginTime = position;
                        particleEnabled = true;
                    }

                    isContinuousKiai = currentEffectPoint.isKiai;
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
        LibraryManager.shuffleLibrary();
        loadBeatmapInfo();
        loadTimingPoints(true);
    }

    public void loadBeatmapInfo() {
        if (LibraryManager.getSizeOfBeatmaps() != 0) {

            beatmapInfo = LibraryManager.getCurrentBeatmapSet().getBeatmap(0);

            if (musicInfoText == null) {
                musicInfoText = new ChangeableText(Utils.toRes(Config.getRES_WIDTH() - 500), Utils.toRes(3),
                        ResourceManager.getInstance().getFont("font"), "None...", HorizontalAlign.RIGHT, 35);
            }

            musicInfoText.setText(beatmapInfo.getArtistText() + " - " + beatmapInfo.getTitleText(), true);

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
        GlobalManager.getInstance().setSelectedBeatmap(beatmapInfo);
        if (beatmapInfo.getBackgroundFilename() != null) {
            try {
                final TextureRegion tex = Config.isSafeBeatmapBg() ?
                        ResourceManager.getInstance().getTexture("menu-background") :
                        ResourceManager.getInstance().loadBackground(beatmapInfo.getBackgroundPath());

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
                GlobalManager.getInstance().getSongService().preLoad(beatmapInfo.getAudioPath());
                musicStarted = false;
            } else {
                Log.w("nullpoint", "GlobalManager.getInstance().getSongService() is null while reload music (MainScene.loadTimeingPoints)");
            }
        }

        Arrays.fill(peakLevel, 0f);
        Arrays.fill(peakDownRate, 1f);
        Arrays.fill(peakAlpha, 0f);

        try (var parser = new BeatmapParser(beatmapInfo.getPath())) {
            var beatmap = parser.parse(false);

            if (beatmap != null) {
                timingControlPoints = new LinkedList<>(beatmap.getControlPoints().timing.controlPoints);
                effectControlPoints = new LinkedList<>(beatmap.getControlPoints().effect.controlPoints);

                // Getting the first timing point is not always accurate - case in point is when the music is not reloaded.
                int position = GlobalManager.getInstance().getSongService() != null ?
                        GlobalManager.getInstance().getSongService().getPosition() : 0;

                currentTimingPoint = null;
                currentEffectPoint = null;

                while (!timingControlPoints.isEmpty() && position > timingControlPoints.peek().time) {
                    currentTimingPoint = timingControlPoints.pop();
                }

                while (!effectControlPoints.isEmpty() && position > effectControlPoints.peek().time) {
                    currentEffectPoint = effectControlPoints.pop();
                }

                if (currentTimingPoint == null) {
                    currentTimingPoint = beatmap.getControlPoints().timing.defaultControlPoint;
                }

                if (currentEffectPoint == null) {
                    currentEffectPoint = beatmap.getControlPoints().effect.defaultControlPoint;
                }

                bpmLength = currentTimingPoint.msPerBeat;
                beatPassTime = (position - currentTimingPoint.time) % bpmLength;
            }
        }
    }

    public void showExitDialog() {

        new MessageDialog()
            .setTitle("Exit")
            .setMessage(context.getString(com.osudroid.resources.R.string.dialog_exit_message))
            .addButton("Yes", dialog -> {
                dialog.dismiss();
                exit();
                return null;
            })
            .addButton("No", dialog -> {
                dialog.dismiss();
                return null;
            })
            .show();
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
        bg.registerEntityModifier(new org.anddev.andengine.entity.modifier.AlphaModifier(3.0f, 0, 1));
        scene.attachChild(bg);
        logo.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                new org.anddev.andengine.entity.modifier.ScaleModifier(3.0f, 1f, 0.8f)
        ));
        logoOverlay.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                new org.anddev.andengine.entity.modifier.ScaleModifier(3.0f, 1f, 0.8f)
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

    public Scene getScene() {
        return scene;
    }

    public BeatmapInfo getBeatmapInfo() {
        return beatmapInfo;
    }

    public void setBeatmap(BeatmapInfo beatmapInfo) {

        LibraryManager.findBeatmapSetIndex(beatmapInfo);
        this.beatmapInfo = beatmapInfo;

        loadBeatmapInfo();
        loadTimingPoints(false);
        musicControl(MusicOption.SYNC);
    }

    public void watchReplay(String replayFile) {
        Replay replay = new Replay();
        if (replay.load(replayFile, false)) {
            if (replay.replayVersion >= 3) {
                //replay
                ScoringScene scorescene = GlobalManager.getInstance().getScoring();
                StatisticV2 stat = replay.getStat();
                BeatmapInfo beatmap = LibraryManager.findBeatmapByMD5(replay.getMd5());
                if (beatmap != null) {
                    GlobalManager.getInstance().getMainScene().setBeatmap(beatmap);
                    GlobalManager.getInstance().getSongMenu().select();
                    ResourceManager.getInstance().loadBackground(beatmap.getBackgroundPath());
                    GlobalManager.getInstance().getSongService().preLoad(beatmap.getAudioPath());
                    GlobalManager.getInstance().getSongService().play();
                    scorescene.load(stat, null, GlobalManager.getInstance().getSongService(), replayFile, null, beatmap);
                    GlobalManager.getInstance().getEngine().setScene(scorescene.getScene());
                }
            }
        }
    }

    public void show() {
        GlobalManager.getInstance().getSongService().setGaming(false);
        GlobalManager.getInstance().getEngine().setScene(getScene());
        if (GlobalManager.getInstance().getSelectedBeatmap() != null) {
            setBeatmap(GlobalManager.getInstance().getSelectedBeatmap());
        }
    }

    public enum MusicOption {PREV, PLAY, PAUSE, STOP, NEXT, SYNC}
}