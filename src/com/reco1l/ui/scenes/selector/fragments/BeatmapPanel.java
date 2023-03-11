package com.reco1l.ui.scenes.selector.fragments;

import static androidx.recyclerview.widget.RecyclerView.*;

import android.view.View;

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
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.framework.Animation;

import java.util.ArrayList;

import main.osu.TrackInfo;

import com.reco1l.view.AlertBoxView;
import com.reco1l.view.TabSelectorView;
import com.rimu.R;

// Created by Reco1l on 13/9/22 01:22

public final class BeatmapPanel extends BaseFragment implements
        ModAcronyms,
        IMusicObserver,
        ScoreboardManager.Observer {

    public static final BeatmapPanel instance = new BeatmapPanel();

    private View mBody;
    private AlertBoxView mTextBox;
    private RecyclerView mScoresList;
    private TabSelectorView mTabSelector;
    private ScoreboardAdapter mScoresAdapter;

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
        mTrackCard = new TrackCard(find("banner"));

        mBody = find("body");
        mTextBox = find("alert");
        mTabSelector = find("tabs");
        mScoresList = find("scoreboard");

        mBody.setAlpha(0);
        hideBoxView();

        mTabSelector.setListener(tab -> {
            hideBoxView();

            if (tab.equals("Local")) {
                Game.scoreboardManager.setOnline(false);
            }
            else if (tab.equals("Global")) {
                Game.scoreboardManager.setOnline(true);
            }
        });

        mTrackCard.onMusicChange(Game.musicManager.getTrack());
        mScoresList.setLayoutManager(new LinearLayoutManager(getContext(), VERTICAL, true));
    }

    private void hideBoxView() {
        mScoresList.setAlpha(0);
        mScoresList.setScaleX(0.9f);
        mScoresList.setScaleY(0.9f);

        mTextBox.setAlpha(0);
        mTextBox.setScaleX(0.9f);
        mTextBox.setScaleY(0.9f);
    }

    @Override
    protected void onPost() {
        mBody.post(() ->
                Animation.of(mBody)
                         .fromX(-mBody.getWidth())
                         .toX(0)
                         .toAlpha(1)
                         .play(300)
        );

        Game.scoreboardManager.load(Game.musicManager.getTrack());
    }

    @Override
    public void onMusicChange(@Nullable TrackInfo newTrack, boolean isSameAudio) {
        if (!isLoaded()) {
            return;
        }

        mTrackCard.onMusicChange(newTrack);
        Game.activity.runOnUiThread(this::hideBoxView);
        Game.scoreboardManager.load(newTrack);
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

        rootView.post(() -> {
            mScoresList.setAdapter(mScoresAdapter);

            if (list == null || list.isEmpty()) {
                mTextBox.setText(Game.scoreboardManager.getErrorMessage());

                Animation.of(mTextBox)
                         .toAlpha(1)
                         .toScale(1)
                         .runOnStart(() -> mTextBox.setVisibility(VISIBLE))
                         .play(200);

                Animation.of(mScoresList)
                         .toAlpha(0)
                         .toScale(0.9f)
                         .runOnEnd(() -> mScoresList.setVisibility(GONE))
                         .play(200);
            }
            else {
                Animation.of(mScoresList)
                         .toAlpha(1)
                         .toScale(1)
                         .runOnStart(() -> mScoresList.setVisibility(VISIBLE))
                         .play(200);

                Animation.of(mTextBox)
                         .toAlpha(0)
                         .toScale(0.9f)
                         .runOnEnd(() -> mTextBox.setVisibility(GONE))
                         .play(200);
            }
        });
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

        Animation.of(mBody)
                 .toX(-mBody.getWidth())
                 .toAlpha(0)
                 .runOnEnd(super::close)
                 .play(300);
    }
}
