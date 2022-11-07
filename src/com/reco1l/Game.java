package com.reco1l;

import android.content.Intent;
import android.os.PowerManager;

import com.reco1l.andengine.scenes.PlayerLoader;
import com.reco1l.andengine.scenes.SongMenu;
import com.reco1l.andengine.scenes.MainScene;
import com.reco1l.interfaces.IReferences;

import java.util.Timer;
import java.util.TimerTask;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

// Created by Reco1l on 26/9/22 19:10

public final class Game implements IReferences {

    public static MainScene mainScene = global.getMainScene();
    public static SongMenu songMenu = global.getSongMenu();
    public static GameScene gameScene = global.getGameScene();
    public static ScoringScene scoringScene = global.getScoring();
    public static PlayerLoader playerLoader = global.getPlayerLoader();

    public static SongService songService = getSongService();

    //----------------------------------------------------------------------------------------//

    private static SongService getSongService() {
        return global.getSongService();
    }

    //----------------------------------------------------------------------------------------//

    public static void exit() {
        engine.setScene(mainScene);

        PowerManager.WakeLock wakeLock = mActivity.getWakeLock();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        mainScene.onExit();

        new Timer().schedule(new TimerTask() {
            public void run() {
                if (global.getSongService() != null) {
                    mActivity.unbindService(mActivity.connection);
                    mActivity.stopService(new Intent(mActivity, SongService.class));
                }
                mActivity.finish();
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
                    UI.beatmapCarrousel.setSelected(track.getBeatmap());
                    resources.loadBackground(track.getBackground());
                    global.getSongService().preLoad(track.getBeatmap().getMusic());
                    global.getSongService().play();
                    scoringScene.load(stat, null, global.getSongService(), path, null, track);
                    engine.setScene(scoringScene.getScene());
                }
            }
        }
    }

    public static void runOnUiThread(Runnable task) {
        mActivity.runOnUiThread(task);
    }

    public static void runOnUpdateThread(Runnable task) {
        mActivity.runOnUpdateThread(task);
    }
}
