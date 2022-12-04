package com.reco1l.ui.data;

// Created by Reco1l on 18/9/22 00:08

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.ui.TriangleEffectView;
import com.reco1l.Game;
import com.reco1l.ui.fragments.BeatmapCarrousel;
import com.reco1l.utils.Animation;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.UI;

import java.util.List;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

public class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.ViewHolder> {

    private final List<TrackInfo> tracks;

    //--------------------------------------------------------------------------------------------//

    public TrackListAdapter(List<TrackInfo> tracks) {
        this.tracks = tracks;
        setHasStableIds(true);
    }

    //--------------------------------------------------------------------------------------------//

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.beatmap_list_child_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

    public static class ViewHolder extends BeatmapCarrousel.BaseViewHolder {

        private final CardView body;
        private final ImageView mark;
        private final TextView stars, difficulty;

        private TriangleEffectView triangles;

        private int color;

        //----------------------------------------------------------------------------------------//

        public ViewHolder(@NonNull View root) {
            super(root);
            mark = root.findViewById(R.id.bl_mark);
            stars = root.findViewById(R.id.bl_stars);
            body = root.findViewById(R.id.bl_childBody);
            difficulty = root.findViewById(R.id.bl_difficulty);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public void select() {
            if (!isSelected) {
                isSelected = true;

                if (triangles == null) {
                    triangles = new TriangleEffectView(UI.beatmapCarrousel.getContext());
                    triangles.setTriangleColor(color);
                    triangles.setAlpha(0);
                }

                body.addView(triangles, 0, ViewUtils.match_parent());

                Animation.of(body)
                        .toAlpha(1)
                        .play(200);

                Animation.of(triangles)
                        .toAlpha(0.3f)
                        .play(200);
            }
        }

        @Override
        public void deselect() {
            if (isSelected) {
                isSelected = false;

                Animation.of(body)
                        .toAlpha(0.8f)
                        .play(200);

                Animation.of(triangles)
                        .toAlpha(0)
                        .runOnEnd(() -> body.removeView(triangles))
                        .play(200);
            }
        }

        //----------------------------------------------------------------------------------------//

        private void bind(TrackInfo track) {
            this.track = track;

            UI.beatmapCarrousel.bindTouchListener(body, () -> {
                UI.beatmapCarrousel.setSelected(null, track);
            });

            difficulty.setText(track.getMode());
            stars.setText("" + GameHelper.Round(track.getDifficulty(), 2));

            float diff = track.getDifficulty();

            int textColor = BeatmapHelper.Palette.getTextColor(diff);
            color = BeatmapHelper.Palette.getColor(diff);

            String markTex = Game.scoreLibrary.getBestMark(track.getFilename());

            if (markTex != null) {
                mark.setImageBitmap(Game.bitmapManager.get("ranking-" + markTex + "-small"));
                mark.setVisibility(View.VISIBLE);
            }

            stars.setTextColor(textColor);
            stars.getCompoundDrawablesRelative()[0].setTint(textColor);
            stars.getBackground().setTint(color);
        }
    }
}
