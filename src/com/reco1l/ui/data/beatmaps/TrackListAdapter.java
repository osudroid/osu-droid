package com.reco1l.ui.data.beatmaps;

// Created by Reco1l on 18/9/22 00:08

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.ui.TriangleEffectView;
import com.reco1l.ui.data.helpers.BeatmapHelper;
import com.reco1l.ui.platform.UI;
import com.reco1l.utils.ViewTouchHandler;
import com.reco1l.utils.ViewUtils;

import java.util.List;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osuplus.R;

public class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.VH> implements UI {

    private List<TrackInfo> tracks;

    public TrackListAdapter(List<TrackInfo> tracks) {
        this.tracks = tracks;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.beatmap_list_child_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(tracks.get(position));
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public static class VH extends RecyclerView.ViewHolder {

        private final RelativeLayout body;
        private final TextView stars, difficulty;

        public VH(@NonNull View root) {
            super(root);
            stars = root.findViewById(R.id.bl_stars);
            difficulty = root.findViewById(R.id.bl_difficulty);
            body = root.findViewById(R.id.bl_childBody);
        }

        private void bind(TrackInfo track) {

            new ViewTouchHandler(() -> beatmapList.setSelected(track)).apply(body);

            difficulty.setText(track.getMode());
            stars.setText("" + GameHelper.Round(track.getDifficulty(), 2));

            int textColor = BeatmapHelper.getDifficultyTextColor(track.getDifficulty());
            int color = BeatmapHelper.getDifficultyColor(track.getDifficulty());

            /*TriangleEffectView triangles = new TriangleEffectView(beatmapList.getContext());
            triangles.setTriangleColor(color);
            triangles.setAlpha(0.3f);
            body.addView(triangles, 0, ViewUtils.match_parent());*/

            stars.setTextColor(textColor);
            stars.getCompoundDrawablesRelative()[0].setTint(textColor);
            stars.getBackground().setTint(color);
        }
    }
}
