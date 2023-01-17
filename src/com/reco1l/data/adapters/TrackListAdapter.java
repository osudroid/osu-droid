package com.reco1l.data.adapters;

// Created by Reco1l on 18/9/22 00:08

import static com.reco1l.data.adapters.TrackListAdapter.*;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.utils.Animation;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.view.effects.StripsEffect;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osuplus.R;

public class TrackListAdapter extends BaseAdapter<TrackViewHolder, TrackInfo> {

    //--------------------------------------------------------------------------------------------//

    public TrackListAdapter(ArrayList<TrackInfo> items) {
        super(items);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.item_beatmap_child;
    }

    @Override
    protected TrackViewHolder getViewHolder(View pRootView) {
        return new TrackViewHolder(pRootView);
    }

    //--------------------------------------------------------------------------------------------//

    public static class TrackViewHolder extends BaseViewHolder<TrackInfo> {

        private final CardView body;
        private final ImageView mark;
        private final TextView stars, difficulty;

        private final StripsEffect stripsEffect;

        private int color;

        //----------------------------------------------------------------------------------------//

        public TrackViewHolder(@NonNull View root) {
            super(root);

            stripsEffect = new StripsEffect(root.getContext());

            mark = root.findViewById(R.id.bl_mark);
            stars = root.findViewById(R.id.bl_stars);
            body = root.findViewById(R.id.bl_childBody);
            difficulty = root.findViewById(R.id.bl_difficulty);

            UI.beatmapCarrousel.bindTouch(body, () -> {
                if(!select()) {
                    Game.musicManager.stop();
                    Game.resourcesManager.getSound("menuhit").play();
                    Game.gameScene.startGame(item, null);
                } else {
                    Game.selectorScene.onTrackSelect(item);
                }
            });
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(TrackInfo item, int position) {
            difficulty.setText(item.getMode());
            stars.setText("" + GameHelper.Round(item.getDifficulty(), 2));

            float diff = item.getDifficulty();

            int textColor = BeatmapHelper.Palette.getTextColor(diff);
            color = BeatmapHelper.Palette.getColor(diff);

            String markTex = Game.scoreLibrary.getBestMark(item.getFilename());

            if (markTex != null) {
                mark.setImageBitmap(Game.bitmapManager.get("ranking-" + markTex));
                mark.setVisibility(View.VISIBLE);
            }

            stars.setTextColor(textColor);
            stars.getCompoundDrawablesRelative()[0].setTint(textColor);
            stars.getBackground().setTint(color);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public void onSelect() {
            Animation.of(body)
                    .toAlpha(1)
                    .play(200);

            body.addView(stripsEffect, 0);
        }

        @Override
        public void onDeselect() {
            Animation.of(body)
                    .toAlpha(0.8f)
                    .play(200);

            body.removeView(stripsEffect);
        }
    }
}
