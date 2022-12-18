package com.reco1l;

import android.content.Intent;
import android.os.PowerManager;

import com.reco1l.andengine.scenes.LoaderScene;
import com.reco1l.andengine.scenes.SelectorScene;
import com.reco1l.andengine.scenes.MainScene;
import com.reco1l.andengine.scenes.SummaryScene;
import com.reco1l.interfaces.IReferences;

import java.util.Timer;
import java.util.TimerTask;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

// Created by Reco1l on 26/9/22 19:10

public final class Game implements IReferences {

    public static GameScene gameScene = GameScene.getInstance();
    public static MainScene mainScene = MainScene.getInstance();
    public static SelectorScene selectorScene = SelectorScene.getInstance();
    public static SummaryScene summaryScene = SummaryScene.getInstance();
    public static LoaderScene loaderScene = LoaderScene.getInstance();

    public static SongService songService = getSongService();

    //----------------------------------------------------------------------------------------//

    private static SongService getSongService() {
        return global.getSongService();
    }

    //----------------------------------------------------------------------------------------//

    public static void exit() {
        engine.setScene(mainScene);

        PowerManager.WakeLock wakeLock = activity.getWakeLock();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        mainScene.onExit();

        new Timer().schedule(new TimerTask() {
            public void run() {
                if (global.getSongService() != null) {
                    activity.unbindService(activity.connection);
                    activity.stopService(new Intent(activity, SongService.class));
                }
                activity.finish();
            }
        }, 3000);
    }

    public static void forcedExit() {
        if(engine.getScene() == gameScene.getScene()) {
            gameScene.quit();
        }
        Game.exit();
    }

    // TODO move this to Scoring scene
    public static void watchReplay(String path) {
        Replay replay = new Replay();

        if (replay.loadInfo(path)) {
            if (replay.replayVersion >= 3) {
                StatisticV2 stat = replay.getStat();
                TrackInfo track = library.findTrackByFileNameAndMD5(replay.getMapFile(), replay.getMd5());

                if (track != null) {
                    UI.beatmapCarrousel.setSelected(null, track);
                    musicManager.change(track.getBeatmap());
                    summaryScene.load(track, stat, path, true);
                }
            }
        }
    }
}
