package com.reco1l;

import android.content.Intent;
import android.os.PowerManager;

import com.reco1l.andengine.scenes.ListScene;
import com.reco1l.andengine.scenes.MainScene;
import com.reco1l.interfaces.IReferences;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

// Created by Reco1l on 26/9/22 19:10

public final class Game implements IReferences {

    public static MainScene mainScene = global.getMainScene();
    public static ListScene listScene = global.getSongMenu();

    public static SongService songService = getSongService();

    //----------------------------------------------------------------------------------------//

    private static SongService getSongService() {
        return global.getSongService();
    }

    //----------------------------------------------------------------------------------------//

    public static void exit() {
        PowerManager.WakeLock wakeLock = mActivity.getWakeLock();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        BassSoundProvider sound = resources.getSound("seeya");
        if (sound != null) {
            sound.play();
        }

        Game.mainScene.playExitAnim();
        UI.mainMenu.playExitAnim();

        ScheduledExecutorService taskPool = Executors.newScheduledThreadPool(1);
        taskPool.schedule(new TimerTask() {
            @Override
            public void run() {
                if (global.getSongService() != null) {
                    global.getSongService().hideNotification();
                    mActivity.unbindService(mActivity.connection);
                    mActivity.stopService(new Intent(mActivity, SongService.class));
                }
                mActivity.finish();
            }
        }, 3000, TimeUnit.MILLISECONDS);
    }

    public static void watchReplay(String path) {
        Replay replay = new Replay();

        if (replay.loadInfo(path)) {
            if (replay.replayVersion >= 3) {
                ScoringScene scoring = global.getScoring();
                StatisticV2 stat = replay.getStat();
                TrackInfo track = library.findTrackByFileNameAndMD5(replay.getMapFile(), replay.getMd5());

                if (track != null) {
                    UI.beatmapList.setSelected(track.getBeatmap());
                    resources.loadBackground(track.getBackground());
                    global.getSongService().preLoad(track.getBeatmap().getMusic());
                    global.getSongService().play();
                    scoring.load(stat, null, global.getSongService(), path, null, track);
                    engine.setScene(scoring.getScene());
                }
            }
        }
    }

    public static void runOnUiThread(Runnable task) {
        mActivity.runOnUiThread(task);
    }
}
