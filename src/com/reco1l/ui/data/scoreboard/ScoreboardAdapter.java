package com.reco1l.ui.data.scoreboard;

import static android.view.ViewGroup.*;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;
import com.reco1l.utils.ViewTouchHandler;
import com.reco1l.interfaces.IMainClasses;
import com.reco1l.utils.listeners.TouchListener;

import java.util.List;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 18/6/22 01:20

public class ScoreboardAdapter extends RecyclerView.Adapter <ScoreboardAdapter.BoardViewHolder>
        implements IMainClasses {

    private static final String AVATAR_URL = "https://" + OnlineManager.hostname + "/user/avatar/?s=100&id=";
    private final List<ScoreboardItem> data;

    //--------------------------------------------------------------------------------------------//

    public ScoreboardAdapter(List<ScoreboardItem> data) {
        this.data = data;
    }

    //--------------------------------------------------------------------------------------------//

    @Override @NonNull
    public ScoreboardAdapter.BoardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new ScoreboardAdapter.BoardViewHolder(LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.scoreboard_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreboardAdapter.BoardViewHolder holder, int position) {
        holder.assign(data.get(position));
        holder.body.setAlpha(0);
        holder.body.postDelayed(() -> new Animation(holder.body).fade(0f, 1f).play(300), 20L * position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    //--------------------------------------------------------------------------------------------//

    public static class BoardViewHolder extends RecyclerView.ViewHolder {

        public View body;

        private final LinearLayout mods;
        private final TextView rank, name, score, combo, accuracy, difference;
        private final ShapeableImageView avatar;
        private final ImageView mark;

        //--------------------------------------------------------------------------------------------//

        public BoardViewHolder(@NonNull View item) {
            super(item);
            body = item.findViewById(R.id.sb_body);
            rank = item.findViewById(R.id.sb_rank);
            avatar = item.findViewById(R.id.sb_avatar);
            mark = item.findViewById(R.id.sb_mark);
            name = item.findViewById(R.id.sb_name);
            score = item.findViewById(R.id.sb_score);
            combo = item.findViewById(R.id.sb_combo);
            mods = item.findViewById(R.id.sb_mods);
            accuracy = item.findViewById(R.id.sb_accuracy);
            difference = item.findViewById(R.id.sb_difference);
        }

        //--------------------------------------------------------------------------------------------//

        public void assign(ScoreboardItem data) {
            if (body == null)
                return;

            new ViewTouchHandler(new TouchListener() {

                public void onPressUp() {
                    if (data.onClick != null) {
                        data.onClick.run();
                    }
                }
                public void onLongPress() {
                    if (data.onLongClick != null) {
                        data.onLongClick.run();
                    }
                }
            }).apply(body);

            rank.setText(data.rank);
            avatar.setImageDrawable(onlineHelper.getAvatarFromURL(AVATAR_URL + data.avatar, data.name));

            mark.setImageBitmap(bitmapManager.get("ranking-" + data.mark + "-small"));

            // Loading mods icons
            for (GameMod mod : data.getMods()) {
                ImageView image = new ImageView(body.getContext());
                mods.addView(image);

                image.setImageBitmap(bitmapManager.get("mod-selection-" + mod.texture));

                image.getLayoutParams().width = (int) Resources.dimen(R.dimen.scoreboardItemModSize);
                image.getLayoutParams().height = (int) Resources.dimen(R.dimen.scoreboardItemModSize);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);

                if (data.getMods().indexOf(mod) > 0) {
                    ((MarginLayoutParams) image.getLayoutParams()).leftMargin = (int) Resources.dimen(R.dimen.XXS);
                }
            }

            name.setText(data.name);
            score.setText(data.getScore());
            combo.setText(data.getCombo() + "x");
            accuracy.setText(data.getAccuracy() + "%");

            if (data.getDifference() != null && !data.getDifference().equals("0")) {
                difference.setVisibility(View.VISIBLE);
                difference.setText("+" + data.getDifference());
            } else {
                difference.setVisibility(View.GONE);
            }
        }
    }
}
