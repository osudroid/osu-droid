package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.edlplan.ui.fragment.ConfirmDialogFragment;
import com.reco1l.entity.Background;
import com.reco1l.entity.Spectrum;
import com.reco1l.ui.platform.UI;

import org.anddev.andengine.engine.handler.IUpdateHandler;
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
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.game.TimingPoint;
import ru.nsu.ccfit.zuev.osu.helper.ModifierFactory;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

/**
 * Created by Fuuko on 2015/4/24.
 */
public class MainScene implements IUpdateHandler, UI {
    public BeatmapInfo beatmapInfo;
    private Context context;
    private Scene scene;
    //    private ArrayList<BeatmapInfo> beatmaps;
    private Random random = new Random();
    private Replay replay = null;
    public TrackInfo selectedTrack;
    private BeatmapData beatmapData;
    private List<TimingPoint> timingPoints;
    private TimingPoint currentTimingPoint, lastTimingPoint, firstTimingPoint;

    private int particleBeginTime = 0;
    private boolean particleEnabled = false;
    private boolean isContinuousKiai = false;

    private ParticleSystem[] particleSystem = new ParticleSystem[2];

    //private BassAudioPlayer music;

    private boolean musicStarted;

    private float bpmLength = 1000;
    private float lastBpmLength = 0;
    private float offset = 0;
    private float beatPassTime = 0;
    private float lastBeatPassTime = 0;
    public boolean doChange = false;
    public boolean doStop = false;
    //    private int playIndex = 0;
//    private int lastPlayIndex = -1;
    public long lastHit = 0;
    private boolean isOnExitAnim = false;

    public Background background;
    public Spectrum spectrum;

    public void load(Context context) {
        this.context = context;
        Debug.i("Load: mainMenuLoaded()");
        scene = new Scene();
        spectrum = new Spectrum();
        background = new Background();

        background.draw(scene);
        spectrum.draw(scene);

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

        scene.setTouchAreaBindingEnabled(true);
        loadOnline();
        scene.registerUpdateHandler(this);
    }

    public void loadOnline() {
        Config.loadOnlineConfig(context);
        OnlineManager.getInstance().Init(context);
        OnlineScoring.getInstance().login();
    }

    public void musicControl(MusicOption option) {
        if (GlobalManager.getInstance().getSongService() == null || beatmapInfo == null) {
            return;
        }
        musicPlayer.currentOption = option;

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
            spectrum.clear(false);
            return;
        }

        if (GlobalManager.getInstance().getSongService() == null || !musicStarted || GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
            bpmLength = 1000;
            offset = 0;
        }

        boolean allowBounce = false;
        if (beatPassTime - lastBeatPassTime >= bpmLength - offset) {
            lastBeatPassTime = beatPassTime;
            offset = 0;
            allowBounce = true;
        }

        if (GlobalManager.getInstance().getSongService() != null) {
            if (!musicStarted) {
                if (firstTimingPoint != null) {
                    bpmLength = firstTimingPoint.getBeatLength() * 1000f;
                } else {
                    return;
                }
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

                int position = GlobalManager.getInstance().getSongService().getPosition();

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
                spectrum.update();

            } else {
                spectrum.clear(false);

                if (!doChange && !doStop && GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getPosition() >= GlobalManager.getInstance().getSongService().getLength()) {
                    musicControl(MusicOption.NEXT);
                }
            }
        }
        UI.mainMenu.update(pSecondsElapsed, allowBounce, bpmLength);
        musicPlayer.update();
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

            background.redraw(selectedTrack.getBackground());

            if (reloadMusic) {
                if (GlobalManager.getInstance().getSongService() != null) {
                    GlobalManager.getInstance().getSongService().preLoad(beatmapInfo.getMusic());
                    musicStarted = false;
                } else {
                    Log.w("nullpoint", "GlobalManager.getInstance().getSongService() is null while reload music (MainScene.loadTimeingPoints)");
                }
            }

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

        UI.mainMenu.playExitAnim();

        ScheduledExecutorService taskPool = Executors.newScheduledThreadPool(1);
        taskPool.schedule(new TimerTask() {
            @Override
            public void run() {
                if (GlobalManager.getInstance().getSongService() != null) {
                    GlobalManager.getInstance().getSongService().hideNotification();
                    GlobalManager.getInstance().getMainActivity().unbindService(GlobalManager.getInstance().getMainActivity().connection);
                    GlobalManager.getInstance().getMainActivity().stopService(new Intent(GlobalManager.getInstance().getMainActivity(), SongService.class));
                    musicStarted = false;
                }
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
