package com.reco1l.data.adapters;

// Created by Reco1l on 18/9/22 00:01

import static com.reco1l.framework.bitmaps.BitmapQueue.*;
import static com.reco1l.data.adapters.BeatmapListAdapter.*;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.reco1l.management.Settings;
import com.reco1l.framework.bitmaps.BitmapQueue;
import com.reco1l.Game;
import com.reco1l.ui.UI;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.tables.DialogTable;
import com.reco1l.ui.custom.ContextMenu;
import com.reco1l.ui.custom.ContextMenuBuilder;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;
import com.reco1l.framework.Animation;
import com.reco1l.framework.drawing.Dimension;
import com.reco1l.framework.input.TouchListener;
import com.reco1l.ui.scenes.selector.views.CarrouselRecyclerView;
import com.reco1l.utils.helpers.BeatmapHelper;

import java.util.ArrayList;

import main.osu.BeatmapInfo;
import main.osu.BeatmapProperties;
import com.rimu.R;

public class BeatmapListAdapter extends BaseAdapter<BeatmapViewHolder, BeatmapInfo> {

    private final BitmapQueue mBackgroundPool;

    //--------------------------------------------------------------------------------------------//

    public BeatmapListAdapter(ArrayList<BeatmapInfo> list) {
        super(list);
        mBackgroundPool = new BitmapQueue();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected BeatmapViewHolder getViewHolder(View rootView) {
        return new BeatmapViewHolder(rootView, mBackgroundPool);
    }

    @Override
    protected int getItemLayout() {
        return R.layout.item_beatmap;
    }

    //--------------------------------------------------------------------------------------------//

    public static class BeatmapViewHolder extends BaseViewHolder<BeatmapInfo> implements BitmapCallback {


        private final BitmapQueue mBackgroundPool;
        private final TrackListAdapter mTrackAdapter;

        private final View mBody;
        private final ImageView mBackground;
        private final CarrouselRecyclerView mTrackList;

        private final TextView
                mTitle,
                mArtist,
                mMapper;

        private Bitmap mBitmap;
        private QueueItem mQueueItem;
        private BeatmapProperties mProperties;

        //----------------------------------------------------------------------------------------//

        private BeatmapViewHolder(@NonNull View root, BitmapQueue backgroundPool) {
            super(root);
            mBackgroundPool = backgroundPool;

            mTitle = root.findViewById(R.id.bl_title);
            mArtist = root.findViewById(R.id.bl_artist);
            mMapper = root.findViewById(R.id.bl_mapper);
            mTrackList = root.findViewById(R.id.bl_trackList);
            mBackground = root.findViewById(R.id.bl_songBackground);

            mBody = root.findViewById(R.id.bl_itemBody);

            mTrackAdapter = new TrackListAdapter(null);

            mTrackList.setAdapter(mTrackAdapter);
            mTrackList.setYOffset(-UI.topBar.getHeight());
            mTrackList.setParentWindow(Game.platform.getScreenContainer());
            mTrackList.setLayoutManager(new LinearLayoutManager(Game.activity));

            UI.beatmapCarrousel.bindTouch(mBody, new TouchListener() {

                public void onPressUp() {
                    if (select()) {
                        Scenes.selector.onTrackSelect(item.getPreviewTrack());
                    }
                }

                public void onLongPress() {
                    createContextMenu(getTouchPosition());
                }
            });
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(BeatmapInfo item, int position) {
            mProperties = Game.propertiesLibrary.getProperties(item.getPath());

            mBitmap = null;
            onAttachmentChange(isAttached());

            mTitle.setText(BeatmapHelper.getTitle(item));
            mArtist.setText("by " + BeatmapHelper.getArtist(item));
            mMapper.setText(item.getCreator());
        }

        @Override
        protected void onAttachmentChange(boolean isAttached) {
            if (mBitmap != null || !Settings.<Boolean>get("itemBackground", true)) {
                return;
            }

            mBackground.post(() -> {
                mBackgroundPool.remove(mQueueItem);

                if (isAttached) {
                    Dimension dimen = new Dimension(mBackground.getWidth(), mBackground.getHeight());
                    String path = item.getPreviewTrack().getBackground();

                    mQueueItem = mBackgroundPool.queue(this, dimen, path);
                }
            });
        }

        @Override
        public void onQueuePoll(Bitmap bitmap) {
            mBitmap = bitmap;

            Game.activity.runOnUiThread(() -> {
                mBackground.setImageBitmap(bitmap);

                int color = bitmap == null ? 0xFF819DD4 : Color.WHITE;

                mTitle.setTextColor(color);
                mArtist.setTextColor(color);
            });
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public void onSelect() {
            Animation.of(mBody)
                    .toLeftMargin(0)
                    .play(300);

            mTrackAdapter.setData(item.getTracks());
            mTrackList.post(() ->
                    mTrackAdapter.select(Game.musicManager.getTrackIndex())
            );
        }

        @Override
        public void onDeselect() {
            Animation.of(mBody)
                    .toLeftMargin(sdp(18))
                    .play(300);

            mTrackAdapter.setData(null);
        }

        //----------------------------------------------------------------------------------------//

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
    }
}
