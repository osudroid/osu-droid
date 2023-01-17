package com.reco1l.data.adapters;

import static com.reco1l.data.adapters.ScoreboardAdapter.*;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.edlplan.replay.OdrDatabase;
import com.edlplan.replay.OsuDroidReplay;
import com.google.android.material.imageview.ShapeableImageView;
import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.data.GameNotification;
import com.reco1l.data.ScoreInfo;
import com.reco1l.interfaces.Endpoint;
import com.reco1l.tables.Res;
import com.reco1l.ui.custom.ContextMenu;
import com.reco1l.ui.custom.ContextMenuBuilder;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;
import com.reco1l.utils.TouchHandler;
import com.reco1l.utils.TouchListener;
import com.reco1l.utils.Views;

import com.reco1l.utils.helpers.OnlineHelper;
import com.reco1l.utils.helpers.ReplayHelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osuplus.R;

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
    protected ScoreHolder getViewHolder(View pRootView) {
        return new ScoreHolder(pRootView);
    }

    //--------------------------------------------------------------------------------------------//

    public static class ScoreHolder extends BaseViewHolder<ScoreInfo> {

        public View body;

        private final LinearLayout modsLayout;
        private final TextView rank, name, score, combo, accuracy, difference;
        private final ShapeableImageView avatar;
        private final ImageView mark;

        private final NumberFormat mFormat = NumberFormat.getNumberInstance(Locale.US);

        //--------------------------------------------------------------------------------------------//

        public ScoreHolder(@NonNull View item) {
            super(item);
            body = item.findViewById(R.id.sb_body);
            rank = item.findViewById(R.id.sb_rank);
            avatar = item.findViewById(R.id.sb_avatar);
            mark = item.findViewById(R.id.sb_mark);
            name = item.findViewById(R.id.sb_name);
            score = item.findViewById(R.id.sb_score);
            combo = item.findViewById(R.id.sb_combo);
            modsLayout = item.findViewById(R.id.sb_mods);
            accuracy = item.findViewById(R.id.sb_accuracy);
            difference = item.findViewById(R.id.sb_difference);
        }

        @Override
        protected void onBind(ScoreInfo item, int position) {
            new TouchHandler(new TouchListener() {
                public void onPressUp() {
                    Game.selectorScene.loadScore(item.getId(), item.getName());
                }

                @Override
                public void onLongPress() {
                    ContextMenuBuilder builder = new ContextMenuBuilder()
                            .addItem(new ContextMenu.Item("Export", () ->
                                    ReplayHelper.export(item.getId()))
                            )
                            .addItem(new ContextMenu.Item("Delete", () -> {


                    }));

                    new ContextMenu(builder).show(body);
                }
            }).apply(body);


            for (GameMod mod : item.getMods()) {
                ImageView image = new ImageView(body.getContext());
                image.setImageBitmap(Game.bitmapManager.get("selection-mod-" + mod.texture));

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

            mark.setImageBitmap(Game.bitmapManager.get("ranking-" + item.getMark()));

            name.setText(item.getName());
            rank.setText(item.getRank());
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

        //--------------------------------------------------------------------------------------------//

        public void assign(ScoreInfo data) {
            if (body == null)
                return;


        }
    }
}
