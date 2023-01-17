package com.reco1l.ui.fragments;

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.management.Scoreboard;
import com.reco1l.ui.BaseFragment;
import com.reco1l.view.CarrouselRecyclerView;
import com.reco1l.data.adapters.BeatmapListAdapter;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/8/22 00:31

public final class BeatmapCarrousel extends BaseFragment implements Scoreboard.Observer<BeatmapInfo> {

    public static BeatmapCarrousel instance;

    private CarrouselRecyclerView mCarrousel;
    private BeatmapListAdapter mAdapter;

    //--------------------------------------------------------------------------------------------//

    public BeatmapCarrousel() {
        super(Screens.Selector);
        Game.beatmapCollection.addListener(this);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.selector_beatmap_carrousel;
    }

    @Override
    protected String getPrefix() {
        return "bl";
    }

    @Override
    protected boolean getConditionToShow() {
        return Game.libraryManager.getSizeOfBeatmaps() != 0;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mCarrousel = find("recycler");

        mCarrousel.setLayoutManager(new LinearLayoutManager(getContext(), VERTICAL, false));
        mCarrousel.setYOffset(-UI.topBar.getHeight());
        mCarrousel.setAdapter(mAdapter);

        restoreSelection();
    }

    @Override
    public void onScoreboardChange(ArrayList<BeatmapInfo> pList) {
        if (mAdapter == null) {
            mAdapter = new BeatmapListAdapter(pList);
            mAdapter.setSelectionListener(pos ->
                    mCarrousel.scrollToPosition(pos)
            );
        }
        mAdapter.setData(pList);
    }

    //--------------------------------------------------------------------------------------------//

    private void restoreSelection() {
        if (mAdapter != null) {
            mAdapter.select(Game.musicManager.getTrack().getBeatmap());
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        super.close();
    }
}
