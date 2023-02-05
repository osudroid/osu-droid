package com.reco1l.ui.fragments;

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.reco1l.global.Game;
import com.reco1l.global.Scenes;
import com.reco1l.global.UI;
import com.reco1l.management.BeatmapCollection;
import com.reco1l.ui.BaseFragment;
import com.reco1l.view.CarrouselRecyclerView;
import com.reco1l.data.adapters.BeatmapListAdapter;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/8/22 00:31

public final class BeatmapCarrousel extends BaseFragment implements BeatmapCollection.Listener {

    public static final BeatmapCarrousel instance = new BeatmapCarrousel();

    private CarrouselRecyclerView mCarrousel;
    private BeatmapListAdapter mAdapter;

    //--------------------------------------------------------------------------------------------//

    public BeatmapCarrousel() {
        super(Scenes.selector);
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
    public void onLibraryChange(ArrayList<BeatmapInfo> pList) {
        if (mAdapter == null) {
            mAdapter = new BeatmapListAdapter(pList);
            mAdapter.setSelectionListener(pos ->
                    mCarrousel.scrollToPosition(pos)
            );
            mAdapter.setMarginAtBounds(sdp(32));
        }
        Game.activity.runOnUiThread(() ->
                mAdapter.setData(pList)
        );
    }

    //--------------------------------------------------------------------------------------------//

    private void restoreSelection() {
        mCarrousel.post(() -> {
            if (mAdapter != null) {
                mAdapter.select(Game.musicManager.getBeatmap());
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        super.close();
    }
}
