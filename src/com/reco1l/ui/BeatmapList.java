package com.reco1l.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.reco1l.BitmapManager;
import com.reco1l.ui.data.helpers.BeatmapHelper;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.ClickListener;
import com.reco1l.view.AnimatedRecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/8/22 00:31

public class BeatmapList extends UIFragment {

    public List<Item> items = new ArrayList<>();
    public Item selected;

    protected AnimatedRecyclerView recyclerView;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "bl";
    }

    @Override
    protected int getLayout() {
        return R.layout.beatmap_list;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        items = new ArrayList<>();
        for (BeatmapInfo beatmap : library.getLibrary()) {
            items.add(new Item(beatmap));
        }

        for (Item item : items) {
            if (global.getMainScene().beatmapInfo == item.beatmap) {
                selected = item;
                break;
            }
        }

        recyclerView = find("recycler");
        reload();
    }

    protected void reload() {
        items = new ArrayList<>();
        ListAdapter adapter = new ListAdapter(items);
        int selectedPosition = -1;

        for (BeatmapInfo beatmap : library.getLibrary()) {
            Item item = new Item(beatmap);
            item.isSelected = selected != null && selected.beatmap == beatmap;
            items.add(item);

            if (item.isSelected) {
                if (!selected.isTrack) {
                    selectedPosition = items.indexOf(item);
                }

                for (TrackInfo track : beatmap.getTracks()) {
                    Item child = new Item(track);
                    items.add(child);

                    if (selected.isTrack && track == selected.track) {
                        selectedPosition = items.indexOf(child);
                    }
                }
            }

        }
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        if (selectedPosition >= 0) {
            recyclerView.smoothScrollToPosition(selectedPosition);
        }
    }

    @Override
    public void close() {
        super.close();
    }

    //--------------------------------------------------------------------------------------------//

    public static class ListAdapter extends Adapter<ListVH> {

        final List<Item> items;

        //----------------------------------------------------------------------------------------//

        ListAdapter(List<Item> items) {
            this.items = items;
            setHasStableIds(true);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public void onViewAttachedToWindow(@NonNull ListVH holder) {
            holder.loadBackground();
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ListVH holder) {
            holder.cancelBackgroundLoad();
        }

        //----------------------------------------------------------------------------------------//

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

        //----------------------------------------------------------------------------------------//

        @NonNull
        @Override
        public ListVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ListVH(LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.beatmap_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ListVH holder, int position) {
            holder.bind(items.get(position));
        }
    }

    public static class ListVH extends ViewHolder {

        private Item song;
        private ImageView songBackground;
        private AsyncTask<OsuAsyncCallback, Integer, Boolean> task;

        final CardView body;
        final TextView title, artist, mapper, stars, difficulty;
        final RelativeLayout beatmapLayout, trackLayout;

        //----------------------------------------------------------------------------------------//

        ListVH(@NonNull View item) {
            super(item);
            body = item.findViewById(R.id.bl_itemBody);
            beatmapLayout = item.findViewById(R.id.bl_beatmapLayout);
            trackLayout = item.findViewById(R.id.bl_trackLayout);

            title = item.findViewById(R.id.bl_title);
            artist = item.findViewById(R.id.bl_artist);
            mapper = item.findViewById(R.id.bl_mapper);
            songBackground = item.findViewById(R.id.bl_songBackground);
            stars = item.findViewById(R.id.bl_difficulty);
            difficulty = item.findViewById(R.id.bl_difficulty);
        }

        //----------------------------------------------------------------------------------------//

        final Runnable callback = () -> {
            TrackInfo track = song.beatmap.getTrack(0);

            if (track.getBackground() == null) {
                songBackground.setImageAlpha(0);
                return;
            }

            if (bitmapManager.contains("bg@" + track.getFilename())) {
                songBackground.setImageBitmap(bitmapManager.get("bg@" + track.getFilename()));
                return;
            }

            task = new AsyncTaskLoader().execute(new OsuAsyncCallback() {
                @Override
                public void run() {
                    Bitmap bitmap = BitmapManager.compress(BitmapFactory.decodeFile(track.getBackground()), 10);
                    bitmapManager.loadBitmap("bg@" + track.getFilename(), bitmap);
                    mActivity.runOnUiThread(() -> songBackground.setImageBitmap(bitmap));
                }

                @Override
                public void onComplete() {
                    task = null;
                }
            });
        };

        void cancelBackgroundLoad() {
            if (!song.isTrack) {
                if (task != null) {
                    task.cancel(true);
                }
                songBackground.removeCallbacks(callback);
                songBackground.setImageDrawable(null);
            }
        }

        void loadBackground() {
            if (!song.isTrack) {
                songBackground.postDelayed(callback, 50);
            }
        }

        //----------------------------------------------------------------------------------------//

        void bind(Item song) {
            this.song = song;

            if (song.isTrack) {
                trackLayout.setVisibility(View.VISIBLE);
                beatmapLayout.setVisibility(View.GONE);

                stars.setText("" + GameHelper.Round(song.track.getDifficulty(), 2));
                difficulty.setText(song.track.getMode());
                return;
            }
            trackLayout.setVisibility(View.GONE);
            beatmapLayout.setVisibility(View.VISIBLE);

            title.setText(BeatmapHelper.getTitle(song.beatmap));
            artist.setText("by " + BeatmapHelper.getArtist(song.beatmap));
            mapper.setText(song.beatmap.getCreator());

            new ClickListener(body).simple(this::setSelected);
        }

        void setSelected() {
            if (this.song.isSelected)
                return;

            this.song.isSelected = true;
            beatmapList.selected = this.song;
            beatmapList.reload();
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Item {

        final boolean isTrack;

        protected BeatmapInfo beatmap;
        protected TrackInfo track;
        protected ArrayList<TrackInfo> tracks;

        boolean isFavorite;
        boolean isSelected = false;

        //----------------------------------------------------------------------------------------//

        Item(BeatmapInfo beatmap) {
            this.beatmap = beatmap;
            this.isTrack = false;
            tracks = beatmap.getTracks();
        }

        Item(TrackInfo track) {
            this.track = track;
            this.beatmap = track.getBeatmap();
            this.isTrack = true;
        }
    }
}
