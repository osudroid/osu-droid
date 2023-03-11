package com.reco1l.ui.scenes.summary;
// Created by Reco1l on 19/11/2022, 23:39

import android.view.View;
import android.widget.LinearLayout;

import com.reco1l.Game;
import com.reco1l.ui.UI;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.framework.execution.Async;
import com.reco1l.view.IconButtonView;

import org.anddev.andengine.entity.scene.Scene;

import main.audio.BassSoundProvider;
import main.osu.Config;
import main.osu.TrackInfo;
import main.osu.game.mods.GameMod;
import main.osu.scoring.StatisticV2;

import com.rimu.R;

import java.io.File;

public class SummaryScene extends BaseScene {

    public static final SummaryScene instance = new SummaryScene();

    private final IconButtonView
            mRetryButton,
            mReplayButton;

    // TODO [SummaryScene] Fix replay ID
    private int mReplayID = -1;

    private StatisticV2 mLastStats;
    private TrackInfo mLastTrack;
    private String mLastReplay;

    private BassSoundProvider mApplauseSound;

    //--------------------------------------------------------------------------------------------//

    public SummaryScene() {
        super();

        mRetryButton = new IconButtonView(context());
        mReplayButton = new IconButtonView(context());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onButtonContainerChange(LinearLayout layout) {
        mRetryButton.setIcon(drw(R.drawable.v18_retry));
        mRetryButton.setTouchListener(() ->
                Scenes.player.startGame(null, null)
        );

        mReplayButton.setIcon(drw(R.drawable.v18_replay));
        mReplayButton.setTouchListener(() -> {
            Scenes.player.startGame(mLastTrack, mLastReplay);
            Game.modManager.setFromStats(mLastStats);
        });

        layout.addView(mRetryButton);
        layout.addView(mReplayButton);
    }

    @Override
    public boolean onBackPress() {
        Game.engine.setScene(Scenes.selector);
        return true;
    }

    @Override
    public void onSceneChange(Scene lastScene, Scene newScene) {
        super.onSceneChange(lastScene, newScene);

        if (lastScene == this) {
            if (mApplauseSound != null) {
                mApplauseSound.stop();
            }
        }
    }

    public StatisticV2 getReplayStat() {
        return mLastStats;
    }

    //--------------------------------------------------------------------------------------------//

    public void load(StatisticV2 stats, TrackInfo track, String replay, boolean isReplaying) {
        mLastTrack = track;
        mLastStats = stats;
        mLastReplay = replay;

        Game.activity.runOnUiThread(() -> {
            if (replay == null) {
                mReplayButton.setVisibility(View.GONE);
            } else {
                mReplayButton.setVisibility(View.VISIBLE);
            }
        });

        UI.gameSummary.setData(track, stats);
        show();

        if (!isReplaying) {
            if (mApplauseSound == null) {
                mApplauseSound = Game.resourcesManager.getSound("applause");
            }
            mApplauseSound.play();

            Game.scoreLibrary.addScore(track.getFilename(), stats, replay);
            //UI.gameSummary.retrieveOnlineData();
            upload(stats, replay);
        }
    }

    private boolean isUnranked(StatisticV2 stats) {
        if (Config.isRemoveSliderLock() || Game.modManager.isCustomSpeed() || Game.modManager.isCustomAR()) {
            return true;
        }

        for (GameMod mod : stats.getMod()) {
            if (mod.unranked) {
                return true;
            }
        }
        return false;
    }

    private void upload(StatisticV2 stats, String replayPath) {
        if (stats.getModifiedTotalScore() <= 0 || isUnranked(stats)) {
            return;
        }
        File replay = new File(replayPath);

        Async.run(() -> {
            try {
                Game.onlineManager2.submitReplay(0, stats, replay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    public int getReplayID() {
        return mReplayID;
    }

    public void setReplayID(int replayID) {
        mReplayID = replayID;
    }
}
