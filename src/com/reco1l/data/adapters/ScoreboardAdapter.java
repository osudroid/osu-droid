package com.reco1l.data.adapters;

import android.content.Intent;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.replay.OdrDatabase;
import com.edlplan.replay.OsuDroidReplay;
import com.edlplan.replay.OsuDroidReplayPack;
import com.google.android.material.imageview.ShapeableImageView;
import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.data.GameNotification;
import com.reco1l.data.Scoreboard;
import com.reco1l.ui.custom.ContextMenuBuilder;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;
import com.reco1l.ui.custom.ContextMenu;
import com.reco1l.tables.Res;
import com.reco1l.utils.TouchHandler;
import com.reco1l.utils.Views;
import com.reco1l.utils.helpers.OnlineHelper;
import com.reco1l.utils.TouchListener;

import java.io.File;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 18/6/22 01:20

public class ScoreboardAdapter extends RecyclerView.Adapter <ScoreboardAdapter.VH> {

    private static final String AVATAR_URL = "https://" + OnlineManager.hostname + "/user/avatar/?s=100&id=";
    private final List<Scoreboard.Item> data;

    //--------------------------------------------------------------------------------------------//

    public ScoreboardAdapter(List<Scoreboard.Item> data) {
        this.data = data;
    }

    //--------------------------------------------------------------------------------------------//

    @Override @NonNull
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.scoreboard_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.assign(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    //--------------------------------------------------------------------------------------------//

    public static class VH extends RecyclerView.ViewHolder {

        public View body;

        private final LinearLayout modsLayout;
        private final TextView rank, name, score, combo, accuracy, difference;
        private final ShapeableImageView avatar;
        private final ImageView mark;

        //--------------------------------------------------------------------------------------------//

        public VH(@NonNull View item) {
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

        //--------------------------------------------------------------------------------------------//

        public void assign(Scoreboard.Item data) {
            if (body == null)
                return;

            new TouchHandler(new TouchListener() {
                public void onPressUp() {
                    Game.selectorScene.loadScore(data.id, data.name);
                }

                @Override
                public void onLongPress() {
                    ContextMenuBuilder builder = new ContextMenuBuilder();


                    builder.addItem(new ContextMenu.Item("Export", () -> {
                        List<OsuDroidReplay> replays = OdrDatabase.get().getReplayById(data.id);

                        if (replays.size() > 0) {
                            GameNotification notification = new GameNotification("replay export");

                            try {
                                OsuDroidReplay r = replays.get(0);
                                File parent = new File(Environment.getExternalStorageDirectory(), "osu!droid/export");

                                CharSequence name = r.getFileName().subSequence(
                                        r.getFileName().indexOf('/') + 1,
                                        r.getFileName().lastIndexOf('.')
                                );

                                File file = new File(parent, String.format( "%s [%s]-%d.edr", name, r.getPlayerName(), r.getTime()));

                                if (file.getParentFile() != null) {
                                    file.getParentFile().mkdirs();
                                }
                                OsuDroidReplayPack.packTo(file, r);


                                notification.runOnClick = () -> {
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);
                                    intent.setDataAndType(FileProvider.getUriForFile(Game.activity, BuildConfig.APPLICATION_ID + ".fileProvider", file), "*/*");
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    Game.activity.startActivityForResult(intent, 0);
                                };

                                notification.message = String.format(Res.str(R.string.frg_score_menu_export_succeed), file.getAbsolutePath());
                            } catch (Exception exception) {
                                exception.printStackTrace();
                                notification.message = Res.str(R.string.frg_score_menu_export_failed) + "\n" + exception.getMessage();
                            }

                            UI.notificationCenter.add(notification);
                        }
                    }));

                    builder.addItem(new ContextMenu.Item("Delete", () -> {

                        DialogBuilder dialog = new DialogBuilder();

                        dialog.title = "Delete replay";
                        dialog.message = "Are you sure?";
                        dialog.addButton("Yes", d -> {
                            List<OsuDroidReplay> replays = OdrDatabase.get().getReplayById(data.id);
                            if (replays.size() > 0) {

                                GameNotification notification = new GameNotification("replay delete");
                                notification.message = "Failed to delete replay!";

                                try {
                                    if (OdrDatabase.get().deleteReplay(data.id) != 0) {
                                        notification.message = Res.str(R.string.menu_deletescore_delete_success);
                                    }
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                    notification.message += "\n" + exception.getMessage();
                                }
                                UI.notificationCenter.add(notification);
                            }
                            d.close();
                        });

                        new Dialog(dialog).show();
                    }));

                    new ContextMenu(builder).show(body);
                }
            }).apply(body);

            rank.setText(data.rank);
            if (data.avatar != null && Game.onlineManager.isStayOnline()) {
                avatar.setImageDrawable(OnlineHelper.getAvatarFromURL(AVATAR_URL + data.avatar, data.name));
            }

            mark.setImageBitmap(Game.bitmapManager.get("ranking-" + data.mark));

            // Loading mods icons
            for (GameMod mod : data.getMods()) {
                ImageView image = new ImageView(body.getContext());
                modsLayout.addView(image, Views.wrap_content);

                image.setImageBitmap(Game.bitmapManager.get("selection-mod-" + mod.texture));

                Views.size(image, (int) Res.dimen(R.dimen.scoreboardItemModSize));
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);

                if (data.getMods().indexOf(mod) > 0) {
                    Views.margins(image).left((int) Res.dimen(R.dimen.XXS));
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
