package com.reco1l.ui.data.beatmaps;

// Created by Reco1l on 18/9/22 00:01

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.ui.data.helpers.BeatmapHelper;
import com.reco1l.ui.platform.UI;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.ViewTouchHandler;

import java.util.List;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

public class BeatmapListAdapter extends RecyclerView.Adapter<BeatmapListAdapter.VH> implements UI {

    private final List<BeatmapInfo> items;

    //--------------------------------------------------------------------------------------------//

    public BeatmapListAdapter(List<BeatmapInfo> items) {
        this.items = items;
        setHasStableIds(true);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onViewAttachedToWindow(@NonNull VH holder) {
        holder.loadBackground();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VH holder) {
        holder.cancelBackgroundLoad();
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
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.beatmap_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    //--------------------------------------------------------------------------------------------//

    public static class VH extends RecyclerView.ViewHolder {

        public BeatmapInfo beatmap;

        private final CardView body;
        private final TextView title, artist, mapper;
        private final RecyclerView trackList;

        private ImageView background;
        private AsyncExec imageTask;

        //----------------------------------------------------------------------------------------//

        private final Runnable callback = () -> {
            TrackInfo track = beatmap.getTrack(0);
            background.animate().cancel();
            ((View) background).setAlpha(0);

            if (track.getBackground() == null) {
                return;
            }

            if (BeatmapHelper.getCompressedBackground(track) != null) {
                background.setImageBitmap(BeatmapHelper.getCompressedBackground(track));
                background.animate()
                        .alpha(1f)
                        .withEndAction(() -> imageTask = null)
                        .setDuration(100)
                        .start();
                return;
            }

            imageTask = new AsyncExec() {
                @Override
                public void run() {
                    BeatmapHelper.loadCompressedBackground(track);
                }

                @Override
                public void onComplete() {
                    background.setImageBitmap(BeatmapHelper.getCompressedBackground(track));
                    background.animate()
                            .alpha(1f)
                            .withEndAction(() -> imageTask = null)
                            .setDuration(100)
                            .start();
                }
            };
            imageTask.execute();
        };

        //----------------------------------------------------------------------------------------//

        private VH(@NonNull View root) {
            super(root);
            body = root.findViewById(R.id.bl_item);

            title = root.findViewById(R.id.bl_title);
            artist = root.findViewById(R.id.bl_artist);
            mapper = root.findViewById(R.id.bl_mapper);
            trackList = root.findViewById(R.id.bl_trackList);
            background = root.findViewById(R.id.bl_songBackground);
        }

        //----------------------------------------------------------------------------------------//

        private void cancelBackgroundLoad() {
            if (imageTask != null) {
                imageTask.cancel(true);
            }
            background.animate().cancel();
            background.removeCallbacks(callback);
            background.setImageDrawable(null);
            ((View) background).setAlpha(0);
            imageTask = null;
        }

        private void loadBackground() {
            background.postDelayed(callback, 50); // Delay to avoid loading the background on fast scrolling
        }

        //----------------------------------------------------------------------------------------//

        public void select() {
            if (beatmapList.selectedHolder == this)
                return;

            if (beatmapList.selectedHolder != null) {
                beatmapList.selectedHolder.deselect();
            }
            beatmapList.selectedHolder = this;
            beatmapList.selectedTrackList = this.trackList;
            trackList.setVisibility(View.VISIBLE);
            trackList.setAdapter(new TrackListAdapter(beatmap.getTracks()));
        }

        public void deselect() {
            trackList.setAdapter(null);
            trackList.setVisibility(View.GONE);
        }

        private void bind(BeatmapInfo beatmap) {
            this.beatmap = beatmap;
            new ViewTouchHandler(() -> {
                if (beatmapList.selectedBeatmap != beatmap) {
                    beatmapList.setSelected(beatmap);
                }
            }).apply(body);

            trackList.setLayoutManager(new LinearLayoutManager(beatmapList.getContext(), LinearLayoutManager.VERTICAL, false));

            title.setText(BeatmapHelper.getTitle(beatmap));
            artist.setText("by " + BeatmapHelper.getArtist(beatmap));
            mapper.setText(beatmap.getCreator());
        }
    }
}
