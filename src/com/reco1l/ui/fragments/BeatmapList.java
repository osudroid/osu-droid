package com.reco1l.ui.fragments;

import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.framework.math.FMath;
import com.reco1l.Game;
import com.reco1l.enums.Scenes;
import com.reco1l.management.MusicManager;
import com.reco1l.ui.data.beatmaps.BeatmapListAdapter;
import com.reco1l.ui.data.beatmaps.TrackListAdapter;
import com.reco1l.ui.platform.UIFragment;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/8/22 00:31

public class BeatmapList extends UIFragment {

    public static BeatmapList instance;

    public List<BeatmapInfo> beatmaps = new ArrayList<>();

    public BeatmapListAdapter.VH selectedBeatmapHolder;
    public TrackListAdapter.VH selectedTrackHolder;

    public BeatmapInfo selectedBeatmap;
    public TrackInfo selectedTrack;
    public RecyclerView trackList;

    private RecyclerView recyclerView;

    //--------------------------------------------------------------------------------------------//

    public enum SortOrder {
        TITLE, ARTIST, CREATOR, DATE, BPM, STARS, LENGTH
    }

    //--------------------------------------------------------------------------------------------//


    @Override
    protected String getPrefix() {
        return "bl";
    }

    @Override
    protected int getLayout() {
        return R.layout.beatmap_list;
    }

    @Override
    protected Scenes getParent() {
        return Scenes.LIST;
    }

    //--------------------------------------------------------------------------------------------//

    private void navigate(BeatmapInfo beatmap) {
        int i = 0;
        while (i < recyclerView.getChildCount()) {
            View child = recyclerView.getChildAt(i);
            BeatmapListAdapter.VH holder = (BeatmapListAdapter.VH) recyclerView.getChildViewHolder(child);

            if (holder.beatmap.getPath().equals(beatmap.getPath())) {
                holder.select();
                recyclerView.scrollToPosition(recyclerView.getChildAdapterPosition(child));
                break;
            }
            i++;
        }
    }

    public void setSelected(BeatmapInfo beatmap) {
        if (beatmap == null)
            return;

        if (selectedBeatmap == null || !selectedBeatmap.getPath().equals(beatmap.getPath())) {
            navigate(beatmap);
        }
        this.selectedBeatmap = beatmap;
        this.selectedTrack = beatmap.getTrack(0);
        setSelected(this.selectedTrack);
    }

    public void setSelected(TrackInfo track) {
        if (track == null)
            return;

        if (selectedBeatmap == null || !selectedBeatmap.getPath().equals(track.getBeatmap().getPath())) {
            navigate(track.getBeatmap());
        }

        if (selectedBeatmapHolder != null) {
            selectedBeatmapHolder.navigateTrack(track);
        }

        this.selectedBeatmap = track.getBeatmap();
        this.selectedTrack = track;
        Game.listScene.onTrackSelect(track);
    }

    //--------------------------------------------------------------------------------------------//

    public void update() {
        if (!isShowing)
            return;

        if (recyclerView != null) {
            for(int i = 0; i < recyclerView.getChildCount(); ++i) {
                View child = recyclerView.getChildAt(i);

                if (child != null) {
                    mActivity.runOnUiThread(() -> child.setTranslationX(computeTranslationX(child)));
                }
            }
        }
    }

    public float computeTranslationX(View view) {
        int oy = (int) ((screenHeight - view.getHeight()) * 1f / 2);

        float fx = 1 - Math.abs(view.getY() - oy) / Math.abs(oy + view.getHeight() / 0.025f);
        float val = view.getWidth() - view.getWidth() * FMath.clamp(fx, 0f, 1f);

        return FMath.clamp(val, 0, view.getWidth());
    }

    //--------------------------------------------------------------------------------------------//

    public void sort(SortOrder order) {

    }

    public void loadBeatmaps() {
        this.beatmaps = new ArrayList<>();
        this.beatmaps.addAll(library.getLibrary());
        BeatmapListAdapter adapter = new BeatmapListAdapter(this.beatmaps);

        recyclerView.setAdapter(adapter);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        recyclerView = find("recycler");
        loadBeatmaps();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.post(() -> setSelected(MusicManager.beatmap));
    }

    @Override
    public void close() {
        selectedBeatmapHolder = null;
        selectedTrackHolder = null;
        super.close();
    }
}
