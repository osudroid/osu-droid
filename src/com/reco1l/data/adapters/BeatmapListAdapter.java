package com.reco1l.data.adapters;

// Created by Reco1l on 18/9/22 00:01

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static com.reco1l.data.adapters.BeatmapListAdapter.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.Game;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.utils.Animation;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.UI;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.utils.helpers.BitmapHelper;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

public class BeatmapListAdapter extends BaseAdapter<BeatmapViewHolder, BeatmapInfo> {

    public BeatmapListAdapter(ArrayList<BeatmapInfo> list) {
        super(list);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected BeatmapViewHolder getViewHolder(View root) {
        return new BeatmapViewHolder(root);
    }

    @Override
    protected int getLayout() {
        return R.layout.beatmap_list_item;
    }

    //--------------------------------------------------------------------------------------------//


    @Override
    protected void onItemSelect(BeatmapViewHolder holder, BeatmapInfo item) {
        if (recyclerView != null) {
            recyclerView.scrollToPosition(holder.getAdapterPosition());
            holder.tracksAdapter.select(Game.musicManager.getTrack());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class BeatmapViewHolder extends BaseViewHolder<BeatmapInfo> {

        private final ImageView background;
        private final TextView title, artist, mapper;

        private final TrackListAdapter tracksAdapter;

        private final Runnable backgroundCallback;

        private AsyncTask backgroundTask;

        //----------------------------------------------------------------------------------------//

        private BeatmapViewHolder(@NonNull View root) {
            super(root);

            title = root.findViewById(R.id.bl_title);
            artist = root.findViewById(R.id.bl_artist);
            mapper = root.findViewById(R.id.bl_mapper);
            background = root.findViewById(R.id.bl_songBackground);

            CardView body = root.findViewById(R.id.bl_item);
            RecyclerView trackList = root.findViewById(R.id.bl_trackList);

            backgroundCallback = () -> {
                if (backgroundTask != null && !backgroundTask.isShutdown()) {
                    backgroundTask.execute();
                }
            };
            tracksAdapter = new TrackListAdapter(null);

            trackList.setLayoutManager(new LinearLayoutManager(Game.activity, VERTICAL, false));
            trackList.setAdapter(tracksAdapter);

            UI.beatmapCarrousel.bindTouchListener(body, () -> {
                if (select()) {
                    Game.musicManager.change(item.getTrack(0));
                }
            });
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(BeatmapInfo item, int position) {
            loadBackground(false);
            title.setText(BeatmapHelper.getTitle(item));
            artist.setText("by " + BeatmapHelper.getArtist(item));
            mapper.setText(item.getCreator());
        }

        @Override
        protected void onAttachmentChange(boolean isAttached) {
            if (!Config.isItemBackground()) {
                return;
            }

            if (isAttached) {
                loadBackground(true);
            } else {
                cancelBackgroundLoad();
            }
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public void onSelectVisually() {
            tracksAdapter.setData(item.getTracks());
        }

        @Override
        public void onDeselectVisually() {
            tracksAdapter.setData(null);
        }

        //----------------------------------------------------------------------------------------//

        private void loadBackground(boolean animated) {
            if (!isAttached()) {
                return;
            }
            cancelBackgroundLoad();

            if (backgroundTask == null || backgroundTask.isShutdown()) {
                backgroundTask = new AsyncTask() {
                    Bitmap bitmap;

                    public void run() {
                        bitmap = getBackgroundBitmap(item.getTrack(0));
                    }

                    public void onComplete() {
                        Game.activity.runOnUiThread(() -> {
                            background.setImageBitmap(bitmap);

                            if (animated) {
                                Animation.of(background)
                                        .fromAlpha(0)
                                        .toAlpha(1)
                                        .play(100);
                            }
                        });
                    }
                };
            }
            background.postDelayed(backgroundCallback, 50);
        }

        private Bitmap getBackgroundBitmap(TrackInfo track) {
            if (track.getBackground() == null) {
                return null;
            }

            String key = "itemBackground@" + track.getBeatmapID() + "/" + track.getPublicName();

            if (!Game.bitmapManager.contains(key)) {
                Bitmap bm = BitmapFactory.decodeFile(track.getBackground());

                float scale = (float) background.getWidth() / bm.getWidth();

                // Resize to improve performance
                bm = BitmapHelper.resize(bm, bm.getWidth() * scale, bm.getHeight() * scale);
                bm = BitmapHelper.cropInCenter(bm, background.getWidth(), background.getHeight());

                Game.bitmapManager.put(key, bm);
            }
            return Game.bitmapManager.get(key);
        }

        private void cancelBackgroundLoad() {
            background.removeCallbacks(backgroundCallback);

            if (backgroundTask != null) {
                backgroundTask.cancel(true);
            }

            background.animate().cancel();
            background.setImageBitmap(null);
        }
    }
}
