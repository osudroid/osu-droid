package com.reco1l.scenes;
// Created by Reco1l on 19/11/2022, 23:39

import android.view.View;
import android.widget.LinearLayout;

import com.reco1l.global.Game;
import com.reco1l.global.UI;
import com.reco1l.global.Scenes;
import com.reco1l.view.IconButton;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SummaryScene extends BaseScene {

    public static final SummaryScene instance = new SummaryScene();

    private final IconButton
            mRetryButton,
            mReplayButton;

    public int replayID = -1;

    private StatisticV2 mLastStats;
    private TrackInfo mLastTrack;
    private String mLastReplay;

    private BassSoundProvider mApplauseSound;

    //--------------------------------------------------------------------------------------------//

    public SummaryScene() {
        super();

        mRetryButton = new IconButton(context());
        mReplayButton = new IconButton(context());
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

            ModMenu.getInstance().setMod(mLastStats.getMod());
            ModMenu.getInstance().setChangeSpeed(mLastStats.getChangeSpeed());
            ModMenu.getInstance().setForceAR(mLastStats.getForceAR());
            ModMenu.getInstance().setEnableForceAR(mLastStats.isEnableForceAR());
            ModMenu.getInstance().setFLfollowDelay(mLastStats.getFLFollowDelay());
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

    public StatisticV2 getReplayStats() {
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

            if (Game.onlineManager.isStayOnline() && Game.onlineManager.isReadyToSend()) {
                UI.gameSummary.retrieveOnlineData();
                upload(stats, replay);
            }
        }
    }

    private boolean isUnranked(StatisticV2 stats) {
        if (Config.isRemoveSliderLock() || ModMenu.getInstance().isChangeSpeed() || ModMenu.getInstance().isEnableForceAR()) {
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
        Game.scoreLibrary.sendScoreOnline(stats, replayPath);
    }
}
