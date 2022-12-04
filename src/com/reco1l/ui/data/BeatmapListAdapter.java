package com.reco1l.ui.data;

// Created by Reco1l on 18/9/22 00:01

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.Game;
import com.reco1l.ui.fragments.BeatmapCarrousel.BaseViewHolder;
import com.reco1l.utils.Animation;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.UI;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.helpers.BitmapHelper;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

public class BeatmapListAdapter extends RecyclerView.Adapter<BeatmapListAdapter.ViewHolder> {

    private final List<BeatmapInfo> items;

    //--------------------------------------------------------------------------------------------//

    public BeatmapListAdapter(List<BeatmapInfo> items) {
        this.items = items;
        setHasStableIds(true);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        if (Config.isItemBackground()) {
            holder.loadBackground();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        if (Config.isItemBackground()) {
            holder.cancelBackgroundLoad();
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.beatmap_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    //--------------------------------------------------------------------------------------------//

    public static class ViewHolder extends BaseViewHolder {

        private final CardView body;
        private final ImageView background;
        private final RecyclerView trackList;
        private final TextView title, artist, mapper;

        private final TrackListAdapter tracksAdapter;
        private final ArrayList<TrackInfo> tracks;

        private final Runnable backgroundCallback;

        private AsyncExec backgroundTask;

        //----------------------------------------------------------------------------------------//

        private ViewHolder(@NonNull View root) {
            super(root);
            body = root.findViewById(R.id.bl_item);

            title = root.findViewById(R.id.bl_title);
            artist = root.findViewById(R.id.bl_artist);
            mapper = root.findViewById(R.id.bl_mapper);
            trackList = root.findViewById(R.id.bl_trackList);
            background = root.findViewById(R.id.bl_songBackground);

            tracks = new ArrayList<>();
            tracksAdapter = new TrackListAdapter(tracks);

            backgroundCallback = () -> {
                if (backgroundTask != null) {
                    backgroundTask.execute();
                }
            };
        }

        //----------------------------------------------------------------------------------------//

        private Bitmap getBackgroundBitmap(TrackInfo track) {
            if (track.getBackground() == null) {
                return null;
            }

            String key = "itemBackground@" + track.getBeatmapID() + "/" + track.getPublicName();

            if (!Game.bitmapManager.contains(key)) {
                Bitmap bitmap = BitmapFactory.decodeFile(track.getBackground());

                float scale = (float) background.getWidth() / bitmap.getWidth();

                // Resize to improve performance
                bitmap = BitmapHelper.resize(bitmap, bitmap.getWidth() * scale, bitmap.getHeight() * scale);
                bitmap = BitmapHelper.cropInCenter(bitmap, background.getWidth(), background.getHeight());

                Game.bitmapManager.put(key, bitmap);
            }
            return Game.bitmapManager.get(key);
        }

        private void loadBackground() {
            background.removeCallbacks(backgroundCallback);

            if (backgroundTask == null || backgroundTask.isCanceled()) {
                backgroundTask = new AsyncExec() {
                    Bitmap bitmap;

                    public void run() {
                        bitmap = getBackgroundBitmap(beatmap.getTrack(0));
                    }

                    public void onComplete() {
                        Game.activity.runOnUiThread(() -> {
                            background.setImageBitmap(bitmap);

                            Animation.of(background)
                                    .fromAlpha(0)
                                    .toAlpha(1)
                                    .play(200);
                        });
                    }
                };
            }

            background.postDelayed(backgroundCallback, 50);
        }

        private void cancelBackgroundLoad() {
            background.removeCallbacks(backgroundCallback);

            if (backgroundTask != null) {
                backgroundTask.cancel(true);
            }

            background.animate().cancel();
            background.setImageBitmap(null);

            ((View) background).setAlpha(0);
        }

        //----------------------------------------------------------------------------------------//

        public void navigate(TrackInfo track) {
            trackList.post(() -> {
                int i = 0;
                while (i < trackList.getChildCount()) {
                    View child = trackList.getChildAt(i);
                    BaseViewHolder holder = (BaseViewHolder) trackList.getChildViewHolder(child);

                    if (holder.track.equals(track)) {
                        holder.select();
                    } else {
                        holder.deselect();
                    }
                    i++;
                }
            });
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public void select() {
            if (!isSelected) {
                trackList.setVisibility(View.VISIBLE);
                tracks.addAll(beatmap.getTracks());
                tracksAdapter.notifyDataSetChanged();
                isSelected = true;
            }
        }

        @Override
        public void deselect() {
            tracks.clear();
            tracksAdapter.notifyDataSetChanged();
            trackList.setVisibility(View.GONE);
            isSelected = false;
        }

        //----------------------------------------------------------------------------------------//

        private void bind(BeatmapInfo beatmap) {
            this.beatmap = beatmap;

            UI.beatmapCarrousel.bindTouchListener(body, () -> {
                if (!Game.musicManager.beatmap.equals(beatmap)) {
                    UI.beatmapCarrousel.setSelected(beatmap, null);
                }
            });

            trackList.setLayoutManager(new LinearLayoutManager(UI.beatmapCarrousel.getContext(), VERTICAL, false));
            trackList.setAdapter(tracksAdapter);

            title.setText(BeatmapHelper.getTitle(beatmap));
            artist.setText("by " + BeatmapHelper.getArtist(beatmap));
            mapper.setText(beatmap.getCreator());
        }
    }
}
