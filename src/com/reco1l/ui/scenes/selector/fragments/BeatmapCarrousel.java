package com.reco1l.ui.scenes.selector.fragments;

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.reco1l.Game;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.UI;
import com.reco1l.management.music.IMusicObserver;
import com.reco1l.management.BeatmapCollection;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.ui.scenes.selector.views.CarrouselRecyclerView;
import com.reco1l.data.adapters.BeatmapListAdapter;

import java.util.ArrayList;

import main.osu.BeatmapInfo;
import main.osu.TrackInfo;

import com.rimu.R;

// Created by Reco1l on 22/8/22 00:31

public final class BeatmapCarrousel extends BaseFragment implements BeatmapCollection.Listener, IMusicObserver {

    public static final BeatmapCarrousel instance = new BeatmapCarrousel();

    private CarrouselRecyclerView mCarrousel;
    private BeatmapListAdapter mAdapter;

    //--------------------------------------------------------------------------------------------//

    public BeatmapCarrousel() {
        super(Scenes.selector);
        Game.beatmapCollection.addListener(this);
        Game.musicManager.bindMusicObserver(this);
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
            mAdapter.setSelectionListener(pos -> {
                if (mCarrousel != null) {
                    mCarrousel.scrollToPosition(pos);
                }
            });
            mAdapter.setMarginAtBounds(sdp(32));
        }
        Game.activity.runOnUiThread(() -> mAdapter.setData(pList));
    }

    @Override
    public void onMusicChange(@Nullable TrackInfo newTrack, boolean isSameAudio) {
        if (!isLoaded()) {
            return;
        }

        restoreSelection();
    }

    //--------------------------------------------------------------------------------------------//

    private void restoreSelection() {
        mCarrousel.post(() -> {
            if (mAdapter != null) {
                if (mAdapter.getSelectedPosition() != Game.musicManager.getBeatmapIndex()) {
                    mAdapter.select(Game.musicManager.getBeatmapIndex());
                }
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        super.close();
    }
}
