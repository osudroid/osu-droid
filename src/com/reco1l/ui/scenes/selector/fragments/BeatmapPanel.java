package com.reco1l.ui.scenes.selector.fragments;

import static android.view.View.LAYER_TYPE_HARDWARE;

import static androidx.recyclerview.widget.RecyclerView.*;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.data.adapters.ScoreboardAdapter;
import com.reco1l.Game;
import com.reco1l.ui.scenes.selector.elements.TrackCard;
import com.reco1l.management.scoreboard.ScoreInfo;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.management.scoreboard.ScoreboardManager;
import com.reco1l.management.modding.ModAcronyms;
import com.reco1l.management.music.IMusicObserver;
import com.reco1l.tables.Res;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.utils.Animation;

import java.util.ArrayList;

import main.osu.TrackInfo;
import com.rimu.R;

// Created by Reco1l on 13/9/22 01:22

public final class BeatmapPanel extends BaseFragment implements
        ModAcronyms,
        IMusicObserver,
        ScoreboardManager.Observer {

    public static final BeatmapPanel instance = new BeatmapPanel();

    private int bodyWidth;

    private View body, message, pageContainer, tabIndicator;
    private TextView localTab, globalTab, messageText;

    private RecyclerView mScoresList;
    private ScoreboardAdapter mScoresAdapter;

    private boolean isTabAnimInProgress = false;

    private TrackCard mTrackCard;

    //--------------------------------------------------------------------------------------------//

    public BeatmapPanel() {
        super(Scenes.selector);
        Game.musicManager.bindMusicObserver(this);
        Game.scoreboardManager.bindBoardObserver(this);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.selector_beatmap_panel;
    }

    @Override
    protected String getPrefix() {
        return "bp";
    }

    @Override
    protected boolean getConditionToShow() {
        return Game.libraryManager.getSizeOfBeatmaps() != 0;
    }

    //--------------------------------------------------------------------------------------------//
    @Override
    protected void onLoad() {
        rootBackground.setLayerType(LAYER_TYPE_HARDWARE, null);

        mScoresList = find("scoreboard");
        mScoresList.setLayoutManager(new LinearLayoutManager(getContext(), VERTICAL, true));

        bodyWidth = Res.dimen(R.dimen.beatmapPanelContentWidth);
        body = find("body");

        Animation.of(body)
                .fromX(-bodyWidth)
                .toX(0)
                .fromAlpha(0)
                .toAlpha(1)
                .play(300);

        message = find("messageLayout");
        messageText = find("messageTv");
        pageContainer = find("pageContainer");

        localTab = find("localTab");
        globalTab = find("globalTab");
        tabIndicator = find("tabIndicator");

        message.setVisibility(View.GONE);

        bindTouch(globalTab, () -> switchTab(globalTab));
        bindTouch(localTab, () -> switchTab(localTab));

        mTrackCard = new TrackCard(find("banner"));
        mTrackCard.onMusicChange(Game.musicManager.getTrack());

        switchTab(localTab);
    }

    @Override
    protected void onPost() {
        Game.scoreboardManager.load(Game.musicManager.getTrack());
    }

    @Override
    public void onMusicChange(@Nullable TrackInfo newTrack, boolean isSameAudio) {
        if (isLoaded()) {
            mTrackCard.onMusicChange(newTrack);
            Game.scoreboardManager.load(newTrack);
        }
    }

    @Override
    public void onBoardChange(ArrayList<ScoreInfo> list) {
        if (!isLoaded()) {
            return;
        }
        if (mScoresAdapter == null) {
            mScoresAdapter = new ScoreboardAdapter(list);
        }
        mScoresAdapter.setData(list);

        if (isLoaded()) {
            mScoresList.setAdapter(mScoresAdapter);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void updateAttributes() {
        mTrackCard.handleModifications();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded()) {
            return;
        }

        Animation.of(body)
                .toX(-bodyWidth)
                .toTopMargin(Res.dimen(R.dimen.topBarHeight))
                .toAlpha(0)
                .runOnEnd(super::close)
                .play(300);
    }

    //--------------------------------------------------------------------------------------------//
    // Temporal workaround until DuringGameScoreBoard gets replaced (old UI)

    public ScoreInfo[] getBoard() {
        return Game.scoreboardManager.getListAsArray();
    }

    //--------------------------------------------------------------------------------------------//
}
