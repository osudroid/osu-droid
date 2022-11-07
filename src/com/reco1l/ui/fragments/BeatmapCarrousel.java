package com.reco1l.ui.fragments;

import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.data.BeatmapListAdapter;
import com.reco1l.ui.data.TrackListAdapter;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.AsyncExec;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/8/22 00:31

public class BeatmapCarrousel extends UIFragment {

    public static BeatmapCarrousel instance;

    public List<BeatmapInfo> beatmaps = new ArrayList<>();

    public BeatmapListAdapter.VH selectedBeatmapHolder;
    public TrackListAdapter.VH selectedTrackHolder;

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
        return R.layout.beatmap_carrousel;
    }

    @Override
    protected Screens getParent() {
        return Screens.SONG_MENU;
    }

    //--------------------------------------------------------------------------------------------//

    private void navigate(BeatmapInfo beatmap) {
        int i = 0;
        while (i < recyclerView.getChildCount()) {
            View child = recyclerView.getChildAt(i);
            BeatmapListAdapter.VH holder = (BeatmapListAdapter.VH) recyclerView.getChildViewHolder(child);

            if (holder.beatmap.equals(beatmap)) {
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

        if (Game.musicManager.beatmap == null || !Game.musicManager.beatmap.equals(beatmap)) {
            navigate(beatmap);
        }
        Game.musicManager.beatmap = beatmap;
        this.selectedTrack = beatmap.getTrack(0);
        setSelected(this.selectedTrack);
    }

    public void setSelected(TrackInfo track) {
        if (track == null)
            return;

        if (Game.musicManager.beatmap == null || !Game.musicManager.beatmap.equals(track.getBeatmap())) {
            navigate(track.getBeatmap());
        }

        if (selectedBeatmapHolder != null) {
            selectedBeatmapHolder.navigateTrack(track);
        }

        Game.musicManager.beatmap = track.getBeatmap();
        this.selectedTrack = track;
        Game.songMenu.onTrackSelect(track);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onUpdate(float elapsed) {
        if (!isShowing)
            return;

        if (recyclerView != null) {
            for(int i = 0; i < recyclerView.getChildCount(); ++i) {
                View child = recyclerView.getChildAt(i);

                if (child != null) {
                    child.setTranslationX(computeTranslationX(child));
                }
            }
        }

        /*if (selectedBeatmapHolder != null && selectedBeatmapHolder.trackList != null) {
            for (int i = 0; i < selectedBeatmapHolder.trackList.getChildCount(); i++) {
                View trackView = selectedBeatmapHolder.trackList.getChildAt(i);

                if (trackView != null) {
                    trackView.setTranslationX(computeTranslationX(trackView));
                }
            }
        }*/
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        new AsyncExec() {
            @Override
            public void run() {
                loadBeatmaps();

                recyclerView.post(() -> {
                    if (Game.songMenu.lastTrack != null) {
                        setSelected(Game.songMenu.lastTrack);
                    } else {
                        setSelected(Game.musicManager.beatmap);
                    }
                });
            }
        }.execute();

        new Animation(recyclerView).moveX(80, 0).fade(0, 1).interpolator(Easing.OutExpo)
                .play(500);
    }

    @Override
    public void close() {
        selectedBeatmapHolder = null;
        selectedTrackHolder = null;
        super.close();
    }
}
