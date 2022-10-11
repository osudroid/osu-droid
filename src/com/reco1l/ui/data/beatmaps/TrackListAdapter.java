package com.reco1l.ui.data.beatmaps;

// Created by Reco1l on 18/9/22 00:08

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.ui.TriangleEffectView;
import com.reco1l.utils.Animation;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.UI;
import com.reco1l.utils.ViewTouchHandler;

import java.util.List;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osuplus.R;

public class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.VH> {

    private final List<TrackInfo> tracks;

    //--------------------------------------------------------------------------------------------//

    public TrackListAdapter(List<TrackInfo> tracks) {
        this.tracks = tracks;
        setHasStableIds(true);
    }

    //--------------------------------------------------------------------------------------------//

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.beatmap_list_child_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(tracks.get(position));
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //--------------------------------------------------------------------------------------------//


    public static class VH extends RecyclerView.ViewHolder {

        public TrackInfo track;

        private final CardView body;
        private final TextView stars, difficulty;

        private TriangleEffectView triangles;

        private int color;

        //----------------------------------------------------------------------------------------//

        public VH(@NonNull View root) {
            super(root);
            stars = root.findViewById(R.id.bl_stars);
            difficulty = root.findViewById(R.id.bl_difficulty);
            body = root.findViewById(R.id.bl_childBody);
        }

        //----------------------------------------------------------------------------------------//

        public void select() {
            if (UI.beatmapList.selectedTrackHolder == this)
                return;

            if (UI.beatmapList.selectedTrackHolder != null) {
                UI.beatmapList.selectedTrackHolder.deselect();
            }

            triangles = new TriangleEffectView(UI.beatmapList.getContext());
            triangles.setTriangleColor(color);
            triangles.setAlpha(0);

            body.addView(triangles, 0, ViewUtils.match_parent());

            new Animation(body).fade(0.85f, 1)
                    .play(200);
            new Animation(triangles).fade(0, 0.3f)
                    .play(200);

            UI.beatmapList.selectedTrackHolder = this;
        }

        public void deselect() {
            new Animation(body).fade(1, 0.85f)
                    .play(200);
            new Animation(triangles).fade(0.3f, 0)
                    .runOnEnd(() -> {
                        body.removeView(triangles);
                        triangles = null;
                    })
                    .play(200);
        }

        //----------------------------------------------------------------------------------------//

        private void bind(TrackInfo track) {
            this.track = track;

            new ViewTouchHandler(() -> UI.beatmapList.setSelected(track)).apply(body);

            difficulty.setText(track.getMode());
            stars.setText("" + GameHelper.Round(track.getDifficulty(), 2));

            float diff = track.getDifficulty();

            int textColor = BeatmapHelper.Palette.getTextColor(diff);
            color = BeatmapHelper.Palette.getColor(diff);

            stars.setTextColor(textColor);
            stars.getCompoundDrawablesRelative()[0].setTint(textColor);
            stars.getBackground().setTint(color);
        }
    }
}
