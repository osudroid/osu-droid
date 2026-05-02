package ru.nsu.ccfit.zuev.osu;

import static com.acivev.ui.EffectKt.addFireworksWithPeriod;
import static com.acivev.ui.EffectKt.addSnowfallWithPeriod;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.util.Log;

import com.acivev.VibratorManager;
import com.acivev.ui.MainMenuVisuals;
import com.edlplan.framework.easing.Easing;
import com.osudroid.beatmaps.BeatmapCache;
import com.osudroid.utils.Execution;
import com.reco1l.andengine.Anchor;
import com.reco1l.andengine.shape.UIBox;
import com.reco1l.andengine.sprite.UISprite;
import com.osudroid.ui.BannerManager;
import com.osudroid.ui.BannerManager.BannerSprite;
import com.osudroid.data.BeatmapInfo;
import com.osudroid.ui.MainMenu;

import com.osudroid.beatmaplisting.BeatmapListing;
import com.reco1l.andengine.ui.UIConfirmDialog;
import com.reco1l.framework.Color4;
import com.reco1l.osu.ui.HorizontalMessageDialog;
import com.rian.osu.beatmap.timings.EffectControlPoint;
import com.rian.osu.beatmap.timings.TimingControlPoint;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.IEntityModifier;
import org.andengine.entity.modifier.MoveXModifier;
import org.andengine.entity.modifier.ParallelEntityModifier;
import org.andengine.entity.modifier.RotationModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.particle.ParticleSystem;
import org.andengine.entity.particle.emitter.PointParticleEmitter;
import org.andengine.entity.particle.initializer.AccelerationParticleInitializer;
import org.andengine.entity.particle.initializer.RotationParticleInitializer;
import org.andengine.entity.particle.initializer.VelocityParticleInitializer;
import org.andengine.entity.particle.modifier.AlphaParticleModifier;
import org.andengine.entity.particle.modifier.ExpireParticleInitializer;
import org.andengine.entity.particle.modifier.ScaleParticleModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.scene.background.SpriteBackground;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.util.debug.Debug;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.modifier.IModifier;
import org.andengine.util.modifier.ease.EaseBounceOut;
import org.andengine.util.modifier.ease.EaseExponentialOut;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private Text musicInfoText;
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

    // === Visual enhancements ===
    private final MainMenuVisuals menuVisuals = new MainMenuVisuals();

    public void load(Context context) {
        this.context = context;
        Debug.i("Load: mainMenuLoaded()");
        VibratorManager.INSTANCE.init(context);
        scene = new Scene();
        final var vbo = GlobalManager.getInstance().getEngine().getVertexBufferObjectManager();
        scene.setOnAreaTouchTraversalFrontToBack();

        final TextureRegion tex = ResourceManager.getInstance().getTexture("menu-background");

        if (tex != null) {
            float height = tex.getHeight();
            height *= Config.getRES_WIDTH()
                    / (float) tex.getWidth();
            final Sprite menuBg = new Sprite(
                    0,
                    (Config.getRES_HEIGHT() - height) / 2,
                    Config.getRES_WIDTH(),
                    height, tex, vbo);
            scene.setBackground(new SpriteBackground(menuBg));
        } else {
            scene.setBackground(new Background(70 / 255f, 129 / 255f,
                    252 / 255f));
        }
        lastBackground = new Sprite(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT(), ResourceManager.getInstance().getTexture("emptyavatar"), vbo);

        addSnowfallWithPeriod(scene, context);
        addFireworksWithPeriod(scene, context);

        final TextureRegion logotex = ResourceManager.getInstance().getTexture("logo");
        logo = new Sprite((float) Config.getRES_WIDTH() / 2 - (float) logotex.getWidth() / 2, (float) Config.getRES_HEIGHT() / 2 - (float) logotex.getHeight() / 2, logotex, vbo) {
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

        logoOverlay = new Sprite((float) Config.getRES_WIDTH() / 2 - (float) logotex.getWidth() / 2, (float) Config.getRES_HEIGHT() / 2 - (float) logotex.getHeight() / 2, logotex, vbo);
        logoOverlay.setScale(1.07f);
        logoOverlay.setAlpha(0.2f);

        menu = new MainMenu(this);

        UIBox box = new UIBox() {

            {
                Text versionText = new Text(10f, 2f, ResourceManager.getInstance().getFont("smallFont"), "osu!droid " + BuildConfig.VERSION_NAME, vbo);
                attachChild(versionText, 0);

                setSize(versionText.getWidth() + 20f, versionText.getHeight() + 4f);
                setPosition(10f, Config.getRES_HEIGHT() - getHeight() - 10f);
                setColor(0f, 0f, 0f, 0.5f); // Black
                setCornerRadius(12f);
            }

            public boolean onAreaTouched(TouchEvent event, float localX, float localY) {
                if (event.isActionUp()) {
                    new HorizontalMessageDialog()
                        .setTitle("About")
                        .setMessage(
                                "<h1>osu!droid</h1>\n" +
                                "<h5>Version " + BuildConfig.VERSION_NAME + "</h5>\n" +
                                "<p>Made by osu!droid team<br>osu! is © peppy 2007-2026</p>\n" +
                                "<br>\n" +
                                "<a href=\"https://osu.ppy.sh\">Visit official osu! website ↗</a>\n" +
                                "<br>\n" +
                                "<br>\n" +
                                "<a href=\"https://osudroid.moe\">Visit official osu!droid website ↗</a>\n" +
                                "<br>\n" +
                                "<br>\n" +
                                "<a href=\"https://discord.gg/nyD92cE\">Join the official Discord server ↗</a>\n",
                            true
                        )
                        .addButton("Changelog", dialog -> {
                            dialog.dismiss();

                            try {
                                var intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://osudroid.moe/changelog/latest"));
                                context.startActivity(intent);
                            } catch (Exception e) {
                                android.util.Log.e("MainScene", "Failed to load changelog", e);
                            }

                            return null;
                        })
                        .addButton("Close", dialog -> {
                            dialog.dismiss();
                            return null;
                        })
                        .show();
                }
                return true;
            }
        };
        scene.attachChild(box);

        final Sprite music_prev = new Sprite(Config.getRES_WIDTH() - 50 * 6 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_prev"), vbo) {

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
                "music_play"), vbo) {

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
                "music_pause"), vbo) {

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
                "music_stop"), vbo) {

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
                "music_next"), vbo) {

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

    //TODO Fix Characters max Length
        musicInfoText = new Text(0, 0, ResourceManager.getInstance().getFont("font"), "", 256, new TextOptions(HorizontalAlign.RIGHT), vbo);

        final TextureRegion nptex = ResourceManager.getInstance().getTexture("music_np");
        music_nowplay = new Sprite(Utils.toRes(Config.getRES_WIDTH() - 500), 0, (float) (40 * nptex.getWidth()) / nptex.getHeight(), 40, nptex, vbo);

        menuVisuals.load(scene, vbo);

        TextureRegion starRegion = ResourceManager.getInstance().getTexture("star");

        {
            particleSystem[0] = new ParticleSystem<>(
                    (x, y) -> new Sprite(x, y, starRegion, vbo),
                    new PointParticleEmitter(-40, (float) (Config.getRES_HEIGHT() * 3) / 4), 32, 48, 128);

            particleSystem[0].addParticleInitializer(new VelocityParticleInitializer(150, 430, -480, -520));
            particleSystem[0].addParticleInitializer(new AccelerationParticleInitializer(10, 30));
            particleSystem[0].addParticleInitializer(new RotationParticleInitializer(0.0f, 360.0f));

            particleSystem[0].addParticleModifier(new ScaleParticleModifier(0.0f, 1.0f, 0.5f, 2.0f));
            particleSystem[0].addParticleModifier(new AlphaParticleModifier(0.0f, 1.0f, 1.0f, 0.0f));
            particleSystem[0].addParticleInitializer(new ExpireParticleInitializer(1.0f));

            particleSystem[0].setParticlesSpawnEnabled(false);

            scene.attachChild(particleSystem[0]);
        }

        {
            particleSystem[1] = new ParticleSystem<>(
                    (x, y) -> new Sprite(x, y, starRegion, vbo),
                    new PointParticleEmitter(Config.getRES_WIDTH(), (float) (Config.getRES_HEIGHT() * 3) / 4), 32, 48, 128);

            particleSystem[1].addParticleInitializer(new VelocityParticleInitializer(-150, -430, -480, -520));
            particleSystem[1].addParticleInitializer(new AccelerationParticleInitializer(-10, 30));
            particleSystem[1].addParticleInitializer(new RotationParticleInitializer(0.0f, 360.0f));

            particleSystem[1].addParticleModifier(new ScaleParticleModifier(0.0f, 1.0f, 0.5f, 2.0f));
            particleSystem[1].addParticleModifier(new AlphaParticleModifier(0.0f, 1.0f, 1.0f, 0.0f));
            particleSystem[1].addParticleInitializer(new ExpireParticleInitializer(1.0f));

            particleSystem[1].setParticlesSpawnEnabled(false);

            scene.attachChild(particleSystem[1]);
        }


        TextureRegion beatmapDownloaderTex = ResourceManager.getInstance().getTexture("beatmap_downloader");
        Sprite beatmapDownloader = new Sprite(Config.getRES_WIDTH() - beatmapDownloaderTex.getWidth(), (Config.getRES_HEIGHT() - beatmapDownloaderTex.getHeight()) / 2f, beatmapDownloaderTex, vbo) {
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
        scene.sortChildren(false);

        scene.registerTouchArea(logo);
        scene.registerTouchArea(box);
        scene.registerTouchArea(beatmapDownloader);
        scene.registerTouchArea(music_prev);
        scene.registerTouchArea(music_play);
        scene.registerTouchArea(music_pause);
        scene.registerTouchArea(music_stop);
        scene.registerTouchArea(music_next);
        scene.setTouchAreaBindingOnActionDownEnabled(true);

        if (BuildConfig.DEBUG) {
            ResourceManager.getInstance().loadHighQualityAsset("dev-build-overlay", "dev-build-overlay.png");

            UISprite debugOverlay = new UISprite(ResourceManager.getInstance().getTexture("dev-build-overlay"));
            debugOverlay.setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT());
            debugOverlay.setOrigin(Anchor.BottomCenter);
            scene.attachChild(debugOverlay);

            Text debugText = new Text(0, 0, ResourceManager.getInstance().getFont("smallFont"), "DEVELOPMENT BUILD", vbo);
            debugText.setColor(1f, 237f / 255f, 0f);
            debugText.setPosition((Config.getRES_WIDTH() - debugText.getWidth()) / 2f, Config.getRES_HEIGHT() - debugOverlay.getHeight() - 1f - debugText.getHeight());

            Text debugTextShadow = new Text(0, 0, ResourceManager.getInstance().getFont("smallFont"), "DEVELOPMENT BUILD", vbo);
            debugTextShadow.setColor(0f, 0f, 0f, 0.5f);
            debugTextShadow.setPosition((Config.getRES_WIDTH() - debugText.getWidth()) / 2f + 2f, Config.getRES_HEIGHT() - debugOverlay.getHeight() - 1f - debugText.getHeight() + 2f);

            scene.attachChild(debugTextShadow);
            scene.attachChild(debugText);
        }

        progressBar = new LinearSongProgress(scene, 0, 0, new PointF(Utils.toRes(Config.getRES_WIDTH() - 320), Utils.toRes(100)));
        progressBar.setProgressRectColor(new Color4(0.9f, 0.9f, 0.9f));
        progressBar.setProgressRectAlpha(0.8f);

        createOnlinePanel(scene);
        scene.registerUpdateHandler(this);

        hitsound = ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false);
    }

    public void loadBannerSprite() {

        if (!Config.isStayOnline()) {
            return;
        }

        BannerSprite sprite = BannerManager.loadBannerSprite();
        if (sprite != null) {
            sprite.setPosition(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
            sprite.setOrigin(Anchor.BottomRight);
            scene.attachChild(sprite);
        }
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

        menuVisuals.updateFrame(pSecondsElapsed, isContinuousKiai);

        if (isOnExitAnim) {
            menuVisuals.onExit();
            return;
        }

        if (GlobalManager.getInstance().getSongService() == null || !musicStarted || GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
            bpmLength = 1000;
        }

        if (doMenuShow && !isMenuShowed) {
            logo.registerEntityModifier(new MoveXModifier(0.3f, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, EaseExponentialOut.getInstance()));
            logoOverlay.registerEntityModifier(new MoveXModifier(0.3f, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, EaseExponentialOut.getInstance()));
            menuVisuals.onMenuShow();

            menu.attachButtons();
            menu.showFirstMenu();

            for (var button : menu.getButtons()) {
                button.clearEntityModifiers();
                button.setX(menuBarX - 100);
                button.setAlpha(0f);

                button.beginParallel((modifier) -> {
                    modifier.moveToX(menuBarX, 0.5f, Easing.OutElastic);
                    modifier.fadeTo(0.9f, 0.5f, Easing.OutCubic);
                    //noinspection DataFlowIssue
                    return null;
                });
            }

            isMenuShowed = true;
        }

        if (doMenuShow) {
            if (showPassTime > 10000f) {

                menu.showFirstMenu();

                for (var button : menu.getButtons()) {
                    // Do not allow the button to be pressed while it is disappearing.
                    scene.unregisterTouchArea(button);

                    button.clearEntityModifiers();
                    button.setX(menuBarX);
                    button.setAlpha(0.9f);

                    button.beginParallel((modifier) -> {
                        modifier.moveToX(menuBarX - 50, 1f, Easing.OutExpo);
                        modifier.fadeOut(1f, Easing.OutExpo);
                        //noinspection DataFlowIssue
                        return null;
                    }).after(IEntity::detachSelf);
                }

                logo.registerEntityModifier(new MoveXModifier(1f, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2,
                        EaseBounceOut.getInstance()));
                logoOverlay.registerEntityModifier(new MoveXModifier(1f, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, EaseBounceOut.getInstance()));
                menuVisuals.onMenuHide();
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
                logo.registerEntityModifier(new SequenceEntityModifier(new org.andengine.entity.modifier.ScaleModifier((float) (bpmLength / 1000 * 0.9f), 1f, 1.07f),
                        new org.andengine.entity.modifier.ScaleModifier((float) (bpmLength / 1000 * 0.07f), 1.07f, 1f)));
            }

            menuVisuals.onBeat(isContinuousKiai);
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
                }

                menuVisuals.updateAudio(peakLevel, peakAlpha, isContinuousKiai);
            } else {
                menuVisuals.clearBars();
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
                final var vbo = GlobalManager.getInstance().getEngine().getVertexBufferObjectManager();
                musicInfoText = new Text(Utils.toRes(Config.getRES_WIDTH() - 500), Utils.toRes(3),
                        ResourceManager.getInstance().getFont("font"), "None...", 256, new TextOptions(HorizontalAlign.RIGHT), vbo);
            }

            musicInfoText.setText(beatmapInfo.getArtistText() + " - " + beatmapInfo.getTitleText());

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
                            .getRES_WIDTH(), height, tex, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
                    lastBackground.registerEntityModifier(new org.andengine.entity.modifier.AlphaModifier(1.5f, 1, 0, new IEntityModifier.IEntityModifierListener() {
                        @Override
                        public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                            scene.attachChild(background, 0);
                            scene.sortChildren(false);
                        }

                        @Override
                        public void onModifierFinished(IModifier<IEntity> pModifier, final IEntity pItem) {
                            GlobalManager.getInstance().getMainActivity().runOnUpdateThread(pItem::detachSelf);
                            scene.sortChildren(false);
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

        try {
            var beatmap = BeatmapCache.getBeatmap(beatmapInfo, false);

            var timingControlPoints = new LinkedList<>(beatmap.getControlPoints().timing.controlPoints);
            var effectControlPoints = new LinkedList<>(beatmap.getControlPoints().effect.controlPoints);

            // Getting the first timing point is not always accurate - case in point is when the music is not reloaded.
            int position = GlobalManager.getInstance().getSongService() != null ?
                    GlobalManager.getInstance().getSongService().getPosition() : 0;

            TimingControlPoint currentTimingPoint = null;
            EffectControlPoint currentEffectPoint = null;

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

            this.timingControlPoints = timingControlPoints;
            this.effectControlPoints = effectControlPoints;
            this.currentTimingPoint = currentTimingPoint;
            this.currentEffectPoint = currentEffectPoint;

            bpmLength = currentTimingPoint.msPerBeat;
            beatPassTime = (position - currentTimingPoint.time) % bpmLength;
        } catch (IOException | IllegalArgumentException e) {
            Debug.e("Failed to load beatmap for timing points: " + e);
        }
    }

    public void showExitDialog() {
        if (isOnExitAnim) {
            return;
        }

        var exitDialog = new UIConfirmDialog();
        exitDialog.setTitle("Exit");
        exitDialog.setText(context.getString(com.osudroid.resources.R.string.dialog_exit_message));
        exitDialog.setOnConfirm(() -> {
            exit();
            return null;
        });
        exitDialog.show();
    }

    public void exit() {
        if (isOnExitAnim) {
            return;
        }
        isOnExitAnim = true;

        Execution.updateThread(menu::detachButtons);

        //ResourceManager.getInstance().loadSound("seeya", "sfx/seeya.wav", false).play();
        //Allow customize Seeya Sounds from Skins
        BassSoundProvider exitsound = ResourceManager.getInstance().getSound("seeya");
        if (exitsound != null) {
            exitsound.play();
        }

        Rectangle bg = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT(), GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        bg.setColor(0, 0, 0, 1.0f);
        bg.registerEntityModifier(new org.andengine.entity.modifier.AlphaModifier(3.0f, 0, 1));
        scene.attachChild(bg);
        logo.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                new org.andengine.entity.modifier.ScaleModifier(3.0f, 1f, 0.8f)
        ));
        logoOverlay.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                new org.andengine.entity.modifier.ScaleModifier(3.0f, 1f, 0.8f)
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

        if (!replay.load(replayFile, false) || replay.replayVersion < 3) {
            return;
        }

        BeatmapInfo beatmap = LibraryManager.findBeatmapByMD5(replay.getMd5());

        if (beatmap == null) {
            return;
        }

        GlobalManager.getInstance().getMainScene().setBeatmap(beatmap);
        StatisticV2 stat = replay.getStat();
        stat.migrateLegacyMods(beatmap.getBeatmapDifficulty());

        GlobalManager.getInstance().getSongMenu().select();
        ResourceManager.getInstance().loadBackground(beatmap.getBackgroundPath());
        GlobalManager.getInstance().getSongService().preLoad(beatmap.getAudioPath());
        GlobalManager.getInstance().getSongService().play();

        ScoringScene scorescene = GlobalManager.getInstance().getScoring();
        scorescene.load(stat, null, GlobalManager.getInstance().getSongService(), replayFile, null, beatmap);
        GlobalManager.getInstance().getEngine().setScene(scorescene.getScene());
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
