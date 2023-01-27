package com.reco1l.data.adapters;

// Created by Reco1l on 18/9/22 00:01

import static com.reco1l.data.adapters.BeatmapListAdapter.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.tables.DialogTable;
import com.reco1l.ui.custom.ContextMenu;
import com.reco1l.ui.custom.ContextMenuBuilder;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;
import com.reco1l.utils.Animation;
import com.reco1l.utils.TouchListener;
import com.reco1l.view.CarrouselRecyclerView;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.utils.helpers.BitmapHelper;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.BeatmapProperties;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

public class BeatmapListAdapter extends BaseAdapter<BeatmapViewHolder, BeatmapInfo> {

    public BeatmapListAdapter(ArrayList<BeatmapInfo> list) {
        super(list);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected BeatmapViewHolder getViewHolder(View pRootView) {
        return new BeatmapViewHolder(pRootView);
    }

    @Override
    protected int getItemLayout() {
        return R.layout.item_beatmap;
    }

    //--------------------------------------------------------------------------------------------//

    public static class BeatmapViewHolder extends BaseViewHolder<BeatmapInfo> {

        private final ImageView mBackground;
        private final CarrouselRecyclerView mTrackList;

        private final TextView
                mTitle,
                mArtist,
                mMapper;

        private final TrackListAdapter mTrackAdapter;

        private final Runnable mBackgroundCallback;

        private BeatmapProperties mProperties;
        private AsyncTask mBackgroundAsync;

        //----------------------------------------------------------------------------------------//

        private BeatmapViewHolder(@NonNull View root) {
            super(root);

            mTitle = root.findViewById(R.id.bl_title);
            mArtist = root.findViewById(R.id.bl_artist);
            mMapper = root.findViewById(R.id.bl_mapper);
            mTrackList = root.findViewById(R.id.bl_trackList);
            mBackground = root.findViewById(R.id.bl_songBackground);

            CardView body = root.findViewById(R.id.bl_item);

            mBackgroundCallback = () -> {
                if (mBackgroundAsync != null && !mBackgroundAsync.isShutdown()) {
                    mBackgroundAsync.execute();
                }
            };
            mTrackAdapter = new TrackListAdapter(null);

            mTrackList.setAdapter(mTrackAdapter);
            mTrackList.setYOffset(-UI.topBar.getHeight());
            mTrackList.setParentWindow(Game.platform.screenContainer);
            mTrackList.setLayoutManager(new LinearLayoutManager(Game.activity));

            UI.beatmapCarrousel.bindTouch(body, new TouchListener() {

                public void onPressUp() {
                    if (select()) {
                        Game.selectorScene.onTrackSelect(item.getTrack(0));
                    }
                }

                public void onLongPress() {
                    createContextMenu(getTouchPosition());
                }
            });
        }

        private void createContextMenu(PointF pPosition) {
            ContextMenuBuilder builder = new ContextMenuBuilder(pPosition)
                    .addItem(new ContextMenu.Item() {

                        public String getText() {
                            if (mProperties.isFavorite()) {
                                return "Remove from favorites";
                            }
                            return "Add to favorites";
                        }

                        public void onClick(TextView view) {
                            mProperties.setFavorite(!mProperties.isFavorite());
                            saveProperties();

                            if (mProperties.isFavorite()) {
                                view.setText("Remove from favorites");
                            } else {
                                view.setText("Add to favorites");
                            }
                        }
                    })
                    .addItem("Offset", () -> new Dialog(DialogTable.offset(item)).show())
                    .addItem("Delete...", () -> {

                        DialogBuilder dialog = new DialogBuilder("Delete Beatmap")
                                .setMessage("Are you sure you want to delete this beatmap?")
                                .addButton("Yes", d -> Game.libraryManager.deleteMap(item))
                                .addCloseButton();

                        new Dialog(dialog).show();
                    });

            new ContextMenu(builder).show();
        }

        private void saveProperties() {
            Game.propertiesLibrary.setProperties(item.getPath(), mProperties);
            Game.propertiesLibrary.save();
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(BeatmapInfo item, int position) {
            loadBackground(false);

            mProperties = Game.propertiesLibrary.getProperties(item.getPath());

            mTitle.setText(BeatmapHelper.getTitle(item));
            mArtist.setText("by " + BeatmapHelper.getArtist(item));
            mMapper.setText(item.getCreator());
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
        public void onSelect() {
            mTrackAdapter.setData(item.getTracks());
            mTrackList.post(() ->
                    mTrackAdapter.select(Game.musicManager.getTrackIndex())
            );
        }

        @Override
        public void onDeselect() {
            mTrackAdapter.setData(null);
        }

        //----------------------------------------------------------------------------------------//

        private void loadBackground(boolean animated) {
            if (!isAttached()) {
                return;
            }
            cancelBackgroundLoad();

            if (mBackgroundAsync == null || mBackgroundAsync.isShutdown()) {
                mBackgroundAsync = new AsyncTask() {
                    Bitmap bitmap;

                    public void run() {
                        bitmap = getBackgroundBitmap(item.getTrack(0));
                    }

                    public void onComplete() {
                        Game.activity.runOnUiThread(() -> {
                            mBackground.setImageBitmap(bitmap);

                            if (animated) {
                                Animation.of(mBackground)
                                        .fromAlpha(0)
                                        .toAlpha(1)
                                        .play(100);
                            }
                        });
                    }
                };
            }
            mBackground.postDelayed(mBackgroundCallback, 50);
        }

        private Bitmap getBackgroundBitmap(TrackInfo track) {
            if (track.getBackground() == null) {
                return null;
            }

            String key = "itemBackground@" + track.getBeatmapID() + "/" + track.getPublicName();

            if (!Game.bitmapManager.contains(key)) {
                Bitmap bm = BitmapFactory.decodeFile(track.getBackground());

                if (bm == null) {
                    // For some reason in some devices decodeFile() returns null so i've to put this little workaround.
                    return null;
                }
                bm = bm.copy(Bitmap.Config.ARGB_8888, true);

                float scale = (float) mBackground.getWidth() / bm.getWidth();

                // Resize to improve performance
                bm = BitmapHelper.resize(bm, bm.getWidth() * scale, bm.getHeight() * scale);
                bm = BitmapHelper.cropInCenter(bm, mBackground.getWidth(), mBackground.getHeight());

                Game.bitmapManager.put(key, bm);
            }
            return Game.bitmapManager.get(key);
        }

        private void cancelBackgroundLoad() {
            mBackground.removeCallbacks(mBackgroundCallback);

            if (mBackgroundAsync != null) {
                mBackgroundAsync.cancel(true);
            }

            mBackground.animate().cancel();
            mBackground.setImageBitmap(null);
        }
    }
}
