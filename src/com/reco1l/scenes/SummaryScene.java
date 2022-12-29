package com.reco1l.scenes;
// Created by Reco1l on 19/11/2022, 23:39

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.utils.ResUtils;
import com.reco1l.view.IconButton;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SummaryScene extends BaseScene {

    public static SummaryScene instance;

    public StatisticV2 replayStats;

    public int replayID = -1;

    //--------------------------------------------------------------------------------------------//

    public static SummaryScene getInstance() {
        if (instance == null) {
            instance = new SummaryScene();
        }
        return instance;
    }

    @Override
    public Screens getIdentifier() {
        return Screens.Summary;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setContinuousPlay(false);
        createTopBarButtons();
    }

    private void createTopBarButtons() {
        IconButton retry = new IconButton(Game.activity);

        retry.setIcon(ResUtils.drw(R.drawable.v_tune));
        retry.runOnTouch(() -> {
            Game.resourcesManager.getSound("applause").stop();
            Game.gameScene.startGame(null, null);
        });

        IconButton watchReplay = new IconButton(Game.activity);

        watchReplay.setIcon(ResUtils.drw(R.drawable.v_search));

        UI.topBar.addButton(getIdentifier(), retry);
        UI.topBar.addButton(getIdentifier(), watchReplay);
    }

    //--------------------------------------------------------------------------------------------//

    public void loadFromBoard(TrackInfo track, int id, boolean isOnline, final String player) {

        if (!isOnline) {
            StatisticV2 stats = Game.scoreLibrary.getScore(id);
            load(track, stats, stats.getReplayName(), true);
            return;
        }

        Game.loaderScene.show();

        new AsyncTask() {
            StatisticV2 stats;
            String replay;

            public void run() {
                try {
                    String pack = Game.onlineManager.getScorePack(id);
                    String[] params = pack.split("\\s+");

                    if (params.length >= 11) {
                        stats = new StatisticV2(params);
                        stats.setPlayerName(player);

                        replay = OnlineManager.getReplayURL(id);
                    }
                } catch (OnlineManagerException ignored) {}
            }

            public void onComplete() {
                Game.loaderScene.runOnComplete(() -> {
                    if (replay != null && stats != null) {
                        load(track, stats, replay, true);
                    } else {
                        Game.selectorScene.show();
                    }
                });
                Game.loaderScene.notifyComplete();
            }
        }.execute();
    }


    public void load(TrackInfo track, StatisticV2 stats, String replayPath, boolean isReplaying) {
        UI.gameSummary.setData(track, stats);
        show();

        if (replayPath != null && isReplaying) {
            replayStats = stats;
        }

        if (!isReplaying) {
            Game.resourcesManager.getSound("applause").play();
            Game.scoreLibrary.addScore(track.getFilename(), stats, replayPath);

            if (Game.onlineManager.isStayOnline() && Game.onlineManager.isReadyToSend()) {
                UI.gameSummary.retrieveOnlineData();
                upload(stats, replayPath);
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

    @Override
    protected void onSceneUpdate(float secondsElapsed) {

    }

    @Override
    public boolean onBackPress() {
        Game.selectorScene.show();
        return true;
    }
}
