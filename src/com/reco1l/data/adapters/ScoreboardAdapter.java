package com.reco1l.data.adapters;

import static com.reco1l.data.adapters.ScoreboardAdapter.*;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.caverock.androidsvg.SVGImageView;
import com.google.android.material.imageview.ShapeableImageView;
import com.reco1l.Game;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.management.scoreboard.ScoreInfo;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.custom.ContextMenu;
import com.reco1l.ui.custom.ContextMenuBuilder;
import com.reco1l.framework.input.TouchHandler;
import com.reco1l.framework.input.TouchListener;
import com.reco1l.tools.Views;

import com.reco1l.framework.execution.Async;
import com.reco1l.tools.helpers.ReplayHelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import main.osu.game.GameHelper;
import main.osu.game.mods.GameMod;
import com.rimu.R;

// Created by Reco1l on 18/6/22 01:20

public class ScoreboardAdapter extends BaseAdapter<ScoreHolder, ScoreInfo> {

    //--------------------------------------------------------------------------------------------//

    public ScoreboardAdapter(ArrayList<ScoreInfo> pItems) {
        super(pItems);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.item_score;
    }

    @Override
    protected ScoreHolder getViewHolder(View rootView) {
        return new ScoreHolder(rootView);
    }

    //--------------------------------------------------------------------------------------------//

    public static class ScoreHolder extends BaseViewHolder<ScoreInfo> {

        private final LinearLayout modsLayout;
        private final TextView rank, name, score, combo, accuracy, difference;
        private final ShapeableImageView avatar;
        private final SVGImageView mark;

        private final NumberFormat mFormat = NumberFormat.getNumberInstance(Locale.US);

        //--------------------------------------------------------------------------------------------//

        public ScoreHolder(@NonNull View root) {
            super(root);

            rank = root.findViewById(R.id.sb_rank);
            avatar = root.findViewById(R.id.sb_avatar);
            mark = root.findViewById(R.id.sb_mark);
            name = root.findViewById(R.id.sb_name);
            score = root.findViewById(R.id.sb_score);
            combo = root.findViewById(R.id.sb_combo);
            modsLayout = root.findViewById(R.id.sb_mods);
            accuracy = root.findViewById(R.id.sb_accuracy);
            difference = root.findViewById(R.id.sb_difference);
        }

        @Override
        protected void onBind(ScoreInfo item, int position) {


            TouchHandler.of(root, new TouchListener() {

                public void onPressUp() {
                    Scenes.selector.loadScore(item.getId(), item.getName());
                }

                public void onLongPress() {
                    ContextMenuBuilder builder = new ContextMenuBuilder(getTouchPosition())
                            .addItem("Export", () -> ReplayHelper.export(item.getId()))
                            .addItem("Delete", () -> ReplayHelper.delete(item.getId()));

                    new ContextMenu(builder).show();
                }

            });


            for (GameMod mod : item.getMods()) {
                ImageView image = new ImageView(context);
                //image.setImageBitmap(Game.bitmapManager.get("selection-mod-" + mod.texture));

                modsLayout.addView(image);
                Views.size(image, dimen(R.dimen.scoreboardItemModSize));

                if (mod.ordinal() > 0) {
                    Views.margins(image).left(dimen(R.dimen.XXS));
                }
            }

            if (Game.onlineManager.isStayOnline()) {
                String url = Endpoint.Avatar_URL + item.getAvatar();
                avatar.setImageDrawable(OnlineHelper.getAvatarFromURL(url, item.getName()));
            } else {
                avatar.setImageDrawable(OnlineHelper.getPlayerAvatar());
            }
            mark.setImageAsset("svg/ranking-" + item.getMark() + ".svg");

            name.setText(item.getName());
            rank.setText("" + item.getRank());
            score.setText(mFormat.format(item.getScore()));
            combo.setText(mFormat.format(item.getCombo()) + "x");
            accuracy.setText(GameHelper.Round(item.getAccuracy() * 100, 2) + "%");

            if (item.getDifference() > 0) {
                difference.setVisibility(View.VISIBLE);
                difference.setText("+" + mFormat.format(item.getDifference()));
            } else {
                difference.setVisibility(View.GONE);
            }
        }
    }
}
