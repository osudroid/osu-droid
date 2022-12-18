package com.reco1l.ui.fragments;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.data.BeatmapListAdapter;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.AnimationOld;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.Resources;
import com.reco1l.utils.ViewUtils;
import com.reco1l.view.LogoView;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/8/22 00:31

public class BeatmapCarrousel extends UIFragment {

    public static BeatmapCarrousel instance;

    public TrackInfo selectedTrack;

    private final ArrayList<BeatmapInfo> beatmaps;
    private final BeatmapListAdapter adapter;

    private BaseViewHolder currentHolder;
    private RecyclerView recyclerView;

    private LogoView logo;

    //--------------------------------------------------------------------------------------------//

    public enum SortOrder {
        TITLE, ARTIST, CREATOR, DATE, BPM, STARS, LENGTH
    }

    //--------------------------------------------------------------------------------------------//

    public BeatmapCarrousel() {
        beatmaps = new ArrayList<>();
        adapter = new BeatmapListAdapter(beatmaps);
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
        return Screens.Selector;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        recyclerView = find("recycler");
        logo = find("logo");

        ViewUtils.margins(logo)
                .bottom(-Resources.sdp(42))
                .right(-Resources.sdp(22));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        recyclerView.post(() -> new AsyncExec() {
            public void run() {
                loadBeatmaps();

                if (selectedTrack != null) {
                    setSelected(null, selectedTrack);
                } else {
                    setSelected(Game.library.getBeatmap(), null);
                }
            }
        }.execute());

        new AnimationOld(recyclerView).moveX(80, 0).fade(0, 1).interpolator(Easing.OutExpo)
                .play(500);
    }

    public void loadBeatmaps() {
        beatmaps.clear();
        beatmaps.addAll(Game.library.getLibrary());
        Game.activity.runOnUiThread(adapter::notifyDataSetChanged);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onUpdate(float elapsed) {
        if (!isShowing)
            return;

        if (recyclerView != null) {
            int i = 0;
            while (i < recyclerView.getChildCount()) {
                View child = recyclerView.getChildAt(i);

                if (child != null) {
                    child.setTranslationX(computeTranslationX(child));
                }
                ++i;
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public float computeTranslationX(View view) {
        int oy = (int) ((screenHeight - view.getHeight()) * 1f / 2);

        float fx = 1 - Math.abs(view.getY() - oy) / Math.abs(oy + view.getHeight() / 0.025f);
        float val = view.getWidth() - view.getWidth() * FMath.clamp(fx, 0f, 1f);

        return FMath.clamp(val, 0, view.getWidth());
    }

    //--------------------------------------------------------------------------------------------//

    public void setSelected(BeatmapInfo beatmap, TrackInfo track) {
        if (track == null) {
            track = beatmap.getTrack(0);
        }

        if (beatmap != null) {
            if (!beatmap.equals(Game.library.getBeatmap())) {
                Game.selectorScene.playMusic(track.getBeatmap());
            }
            navigate(beatmap);
        }

        if (currentHolder != null) {
            ((BeatmapListAdapter.ViewHolder) currentHolder).navigate(track);
        }

        Game.selectorScene.onTrackSelect(track, track == selectedTrack);
        selectedTrack = track;
    }

    private void navigate(BeatmapInfo beatmap) {
        recyclerView.post(() -> {
            int i = 0;
            while (i < recyclerView.getChildCount()) {

                View child = recyclerView.getChildAt(i);
                BaseViewHolder holder = (BaseViewHolder) recyclerView.getChildViewHolder(child);

                if (holder.beatmap.equals(beatmap)) {
                    holder.select();
                    currentHolder = holder;
                    recyclerView.scrollToPosition(recyclerView.getChildAdapterPosition(child));
                } else {
                    holder.deselect();
                }
                ++i;
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    public void sort(SortOrder order) {

    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        super.close();
    }

    //--------------------------------------------------------------------------------------------//

    public static abstract class BaseViewHolder extends RecyclerView.ViewHolder {

        public BeatmapInfo beatmap;
        public TrackInfo track;

        protected boolean isSelected = false;

        //----------------------------------------------------------------------------------------//

        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        //----------------------------------------------------------------------------------------//

        public abstract void select();

        public abstract void deselect();

    }
}
