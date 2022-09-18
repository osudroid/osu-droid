package com.reco1l.ui.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.edlplan.framework.math.FMath;
import com.edlplan.ui.TriangleEffectView;
import com.reco1l.Scenes;
import com.reco1l.ui.data.helpers.BeatmapHelper;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.Resources;
import com.reco1l.utils.ViewTouchHandler;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.listeners.TouchListener;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/8/22 00:31

public class BeatmapList extends UIFragment {

    public List<Item> beatmaps = new ArrayList<>();
    public TrackInfo selectedTrack;

    protected RecyclerView recyclerView;

    private List<Item> temp;
    private Item selectedItem;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "bl";
    }

    @Override
    protected int getLayout() {
        return R.layout.beatmap_list;
    }

    @Override
    protected Scenes getParent() {
        return Scenes.SONG_MENU;
    }

    //--------------------------------------------------------------------------------------------//

    public void setSelected(TrackInfo selectedTrack) {
        this.selectedTrack = selectedTrack;

        if (isShowing) {
            mActivity.runOnUiThread(this::reload);
        }
    }

    public void update() {
        if (!isShowing || recyclerView == null)
            return;

        for(int i = 0; i < recyclerView.getChildCount(); ++i) {
            final View child = recyclerView.getChildAt(i);

            if (child == null)
                continue;

            mActivity.runOnUiThread(() -> child.setTranslationX(computeTranslationX(child)));
        }
    }

    private float computeTranslationX(View view) {
        int oy = (int) ((recyclerView.getHeight() - view.getHeight()) * 1f / 2);

        float fx = 1 - Math.abs(view.getY() - oy) / Math.abs(oy + view.getHeight() / 0.025f);
        float val = view.getWidth() - view.getWidth() * FMath.clamp(fx, 0f, 1f);

        return FMath.clamp(val, 0, view.getWidth());
    }

    public void loadBeatmaps() {
        this.beatmaps = new ArrayList<>();
        for (BeatmapInfo beatmap : library.getLibrary()) {
            this.beatmaps.add(new Item(beatmap));
        }
    }

    @Override
    protected void onLoad() {
        recyclerView = find("recycler");
        loadBeatmaps();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        this.selectedTrack = global.getMainScene().selectedTrack;
        reload();

        recyclerView.post(() -> {
            if (selectedTrack == null)
                return;

            for (Item item : beatmaps) {
                if (this.selectedTrack.getBeatmap().getPath().equals(item.beatmap.getPath())) {
                    setSelected(item.beatmap.getTrack(0));
                    break;
                }
            }
        });
    }

    protected void selectItem(Item item) {
        if (item == selectedItem)
            return;

        this.selectedItem = item;

        if (!item.isTrack) {
            selectedTrack = item.beatmap.getTrack(0); // Always selecting first song when changing beatmap

            new AsyncExec() {
                public void run() {
                    reload();
                }

                public void onComplete() {
                    recyclerView.smoothScrollToPosition(beatmaps.indexOf(item));
                }
            }.execute();
            return;
        }
        recyclerView.smoothScrollToPosition(temp.indexOf(item));
    }

    protected void reload() {
        temp = new ArrayList<>();
        ListAdapter adapter = new ListAdapter(temp);

        for (Item song : beatmaps) {
            temp.add(song);

            if (selectedTrack != null && selectedTrack.getBeatmap() == song.beatmap) {
                for (TrackInfo track : song.beatmap.getTracks()) {
                    temp.add(new Item(track));
                }
            }
        }
        mActivity.runOnUiThread(() -> {
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void close() {
        super.close();
    }

    //--------------------------------------------------------------------------------------------//

    public static class ListAdapter extends Adapter<ListVH> {

        private final List<Item> items;

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

        @Override @NonNull
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
        private ImageView background;
        private AsyncExec task;

        private final CardView body;
        private final TextView title, artist, mapper, stars, difficulty;
        private final RelativeLayout beatmapLayout, trackLayout;

        //----------------------------------------------------------------------------------------//

        ListVH(@NonNull View root) {
            super(root);
            body = root.findViewById(R.id.bl_itemBody);
            beatmapLayout = root.findViewById(R.id.bl_beatmapLayout);
            trackLayout = root.findViewById(R.id.bl_trackLayout);

            title = root.findViewById(R.id.bl_title);
            artist = root.findViewById(R.id.bl_artist);
            mapper = root.findViewById(R.id.bl_mapper);
            background = root.findViewById(R.id.bl_songBackground);
            stars = root.findViewById(R.id.bl_stars);
            difficulty = root.findViewById(R.id.bl_difficulty);
        }

        //----------------------------------------------------------------------------------------//

        private final Runnable callback = () -> {
            TrackInfo track = song.beatmap.getTrack(0);
            background.animate().cancel();
            ((View) background).setAlpha(0);

            if (track.getBackground() == null) {
                return;
            }

            if (BeatmapHelper.getCompressedBackground(track) != null) {
                background.setImageBitmap(BeatmapHelper.getCompressedBackground(track));
                background.animate()
                        .alpha(1f)
                        .withEndAction(() -> task = null)
                        .setDuration(100)
                        .start();
                return;
            }

            task = new AsyncExec() {
                @Override
                public void run() {
                    BeatmapHelper.loadCompressedBackground(track);
                }

                @Override
                public void onComplete() {
                    background.setImageBitmap(BeatmapHelper.getCompressedBackground(track));
                    background.animate()
                            .alpha(1f)
                            .withEndAction(() -> task = null)
                            .setDuration(100)
                            .start();
                }
            };
            task.execute();
        };

        private void cancelBackgroundLoad() {
            if (song.isTrack)
                return;

            if (task != null) {
                task.cancel(true);
            }
            background.animate().cancel();
            background.removeCallbacks(callback);
            background.setImageDrawable(null);
            ((View) background).setAlpha(0);
            task = null;
        }

        private void loadBackground() {
            if (!song.isTrack) {
                background.postDelayed(callback, 50); // Delay to avoid loading the background on fast scrolling
            }
        }

        //----------------------------------------------------------------------------------------//

        void bind(Item song) {
            this.song = song;

            new ViewTouchHandler(new TouchListener() {
                public void onPressUp() {
                    beatmapList.selectItem(song);
                }
            }).apply(body);

            if (song.isTrack) {
                trackLayout.setVisibility(View.VISIBLE);
                beatmapLayout.setVisibility(View.GONE);

                TriangleEffectView triangles = new TriangleEffectView(beatmapList.getContext());
                triangles.setAlpha(0.3f);
                trackLayout.addView(triangles, 0, ViewUtils.match_parent());

                final TrackInfo track = song.track;

                difficulty.setText(track.getMode());
                stars.setText("" + GameHelper.Round(track.getDifficulty(), 2));

                final int textColor = BeatmapHelper.getDifficultyTextColor(track.getDifficulty());

                stars.setTextColor(textColor);
                stars.getCompoundDrawablesRelative()[0].setTint(textColor);
                stars.getBackground().setTint(BeatmapHelper.getDifficultyColor(track.getDifficulty()));

                body.setCardElevation(0);
                body.setCardBackgroundColor(Resources.color(R.color.backgroundDimmed));

                ViewUtils.margins(body).left((int) Resources.dimen(R.dimen.beatmapListTrackLeftMargin));
                return;
            }
            trackLayout.setVisibility(View.GONE);
            beatmapLayout.setVisibility(View.VISIBLE);

            title.setText(BeatmapHelper.getTitle(song.beatmap));
            artist.setText("by " + BeatmapHelper.getArtist(song.beatmap));
            mapper.setText(song.beatmap.getCreator());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Item {

        protected final boolean isTrack;

        protected BeatmapInfo beatmap;
        protected TrackInfo track;

        //----------------------------------------------------------------------------------------//

        Item(BeatmapInfo beatmap) {
            this.beatmap = beatmap;
            this.isTrack = false;
        }

        Item(TrackInfo track) {
            this.track = track;
            this.isTrack = true;
            this.beatmap = track.getBeatmap();
        }
    }
}
