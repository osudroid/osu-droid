package com.reco1l.ui.fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.interfaces.MusicObserver;
import com.reco1l.tables.ResourceTable;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.TouchListener;
import com.reco1l.utils.Views;
import com.reco1l.view.IconButton;
import com.reco1l.ui.fragments.MusicPlayer.PlaylistAdapter.ViewHolder;
import com.reco1l.utils.helpers.BeatmapHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 1/7/22 22:45

public final class MusicPlayer extends BaseFragment implements MusicObserver {

    public static final MusicPlayer instance = new MusicPlayer();

    public IconButton button;

    private View
            mBody,
            mSongBody,
            mListBody;

    private SeekBar mSeekBar;
    private ImageView mPlayButton;
    private RecyclerView mPlaylist;

    private TextView
            mTime,
            mTitle,
            mArtist,
            mLength;

    private boolean
            mIsPlaylistVisible = false,
            mIsTrackingTouch = false,
            mWasPlaying = false;

    private final SimpleDateFormat mTimeFormat;

    //--------------------------------------------------------------------------------------------//

    public MusicPlayer() {
        super();
        Game.musicManager.bindMusicObserver(this);

        mTimeFormat = new SimpleDateFormat("mm:ss");
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.extra_music_player;
    }

    @Override
    protected String getPrefix() {
        return "mp";
    }

    @Override
    protected long getCloseTime() {
        return 10000;
    }

    @Override
    public int getHeight() {
        return dimen(R.dimen.mp_height);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        closeOnBackgroundClick(true);
        mIsPlaylistVisible = false;

        mBody = find("body");
        mListBody = find("listBody");
        mSongBody = find("songBody");

        mLength = find("songLength");
        mTime = find("songProgress");
        mSeekBar = find("seekBar");
        mPlayButton = find("play");
        mPlaylist = find("listRv");
        mArtist = find("artist");
        mTitle = find("title");

        Views.width(mListBody, 0);

        Animation.of(mBody)
                .fromHeight(sdp(30))
                .toHeight(getHeight())
                .fromY(-30)
                .toY(0)
                .fromAlpha(0)
                .toAlpha(1)
                .play(200);

        Animation.of(find("innerBody"))
                .fromAlpha(0)
                .toAlpha(1)
                .play(200);

        mPlaylist.setLayoutManager(new LinearLayoutManager(getContext()));
        mPlaylist.setAdapter(new PlaylistAdapter(Game.libraryManager.getLibrary()));

        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            private int newPosition;

            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsTrackingTouch = true;
                onTouchEventNotified(MotionEvent.ACTION_DOWN);
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (fromTouch) {
                    newPosition = progress;
                    mTime.setText(mTimeFormat.format(progress));
                }
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsTrackingTouch = false;
                Game.songService.setPosition(newPosition);
                onTouchEventNotified(MotionEvent.ACTION_UP);
            }
        });

        bindTouch(find("list"), new TouchListener() {
            public boolean useBorderlessEffect() { return true; }

            public void onPressUp() {
                switchListVisibility();
            }
        });

        bindTouch(mPlayButton, new TouchListener() {
            public boolean useTouchEffect() {
                return false;
            }

            public void onPressUp() {
                if (Game.musicManager.isPlaying()) {
                    Game.musicManager.pause();
                } else {
                    Game.musicManager.play();
                }
            }
        });

        bindTouch(find("prev"), new TouchListener() {
            public boolean useBorderlessEffect() { return true; }

            public void onPressUp() {
                Game.musicManager.previous();
            }
        });

        bindTouch(find("next"), new TouchListener() {
            public boolean useBorderlessEffect() { return true; }

            public void onPressUp() {
                Game.musicManager.next();
            }
        });

        rootView.post(() -> {
            BeatmapInfo beatmap = Game.libraryManager.getBeatmap();

            if (beatmap != null) {
                loadMetadata(beatmap);
                mPlaylist.post(() -> selectAtList(beatmap));
            }
        });
    }

    @Override
    protected void onUpdate(float pSecElapsed) {
        int length = Game.songService.getLength();
        int position = Game.songService.getPosition();

        if (!mIsTrackingTouch) {
            mSeekBar.setMax(length);
            mSeekBar.setProgress(position);
            mTime.setText(mTimeFormat.format(position));
        }

        if (Game.musicManager.isPlaying() && !mWasPlaying) {
            mWasPlaying = true;
            mPlayButton.setImageDrawable(drw(R.drawable.v32_pause_circle));
        }
        else if (!Game.musicManager.isPlaying() && mWasPlaying) {
            mWasPlaying = false;
            mPlayButton.setImageDrawable(drw(R.drawable.v32_play_circle));
        }
    }

    @Override
    public void onMusicChange(TrackInfo pNewTrack, boolean pWasAudioChanged) {
        if (isAdded()) {
            Animation.of(mSongBody)
                    .toAlpha(0)
                    .runOnEnd(() -> {
                        loadMetadata(pNewTrack.getBeatmap());
                        selectAtList(pNewTrack.getBeatmap());

                        Animation.of(mSongBody)
                                .toX(0)
                                .toAlpha(1)
                                .play(200);
                    })
                    .play(200);
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void loadMetadata(BeatmapInfo pBeatmap) {
        mTitle.setText(BeatmapHelper.getTitle(pBeatmap));
        mArtist.setText(BeatmapHelper.getArtist(pBeatmap));
        mLength.setText(mTimeFormat.format(Game.songService.getLength()));
    }

    private void switchListVisibility() {
        if (!mIsPlaylistVisible) {
            mIsPlaylistVisible = true;

            Animation.of(mListBody)
                    .toWidth(dimen(R.dimen.musicPlayerListWidth))
                    .play(300);
        } else {
            mIsPlaylistVisible = false;

            Animation.of(mListBody)
                    .toWidth(0)
                    .play(300);
        }
    }

    private void selectAtList(BeatmapInfo pBeatmap) {
        int i = 0;
        while (i < mPlaylist.getChildCount()) {
            View child = mPlaylist.getChildAt(i);
            ViewHolder holder = (ViewHolder) mPlaylist.getChildViewHolder(child);

            if (pBeatmap.equals(holder.beatmap)) {
                holder.onSelect();
            }
            ++i;
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public boolean show() {
        if (super.show()) {
            button.setActivated(true);
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        if (isAdded()) {
            button.setActivated(false);

            if (mIsPlaylistVisible) {
                switchListVisibility();
            }

            Animation.of(find("innerBody"))
                    .toAlpha(0)
                    .play(100);

            Animation.of(mBody)
                    .toHeight(sdp(30))
                    .toAlpha(0)
                    .toY(-30)
                    .runOnEnd(super::close)
                    .play(240);
        }
    }

    //--------------------------------------------------------------------------------------------//

    // TODO [MusicPlayer] Rework it extending BaseAdapter & BaseViewHolder
    @Deprecated
    static class PlaylistAdapter extends RecyclerView.Adapter<ViewHolder> implements ResourceTable {

        private final ArrayList<BeatmapInfo> beatmaps;

        private ViewHolder selected;

        //----------------------------------------------------------------------------------------//

        public PlaylistAdapter(ArrayList<BeatmapInfo> beatmaps) {
            this.beatmaps = beatmaps;

            setHasStableIds(true);
        }

        //----------------------------------------------------------------------------------------//

        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView view = new TextView(Game.activity);

            view.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

            Drawable drawable = drw(R.drawable.shape_rounded).mutate();
            drawable.setTint(color(R.color.accent));
            drawable.setAlpha(0);

            view.setBackground(drawable);
            view.setEllipsize(TextUtils.TruncateAt.END);
            view.setSingleLine(true);

            int m = dimen(R.dimen.M);
            int s = dimen(R.dimen.S);
            view.setPadding(m, s, m, s);

            return new ViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(beatmaps.get(position));
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public int getItemCount() {
            return beatmaps.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        //----------------------------------------------------------------------------------------//

        static class ViewHolder extends RecyclerView.ViewHolder implements ResourceTable {

            private final PlaylistAdapter parent;
            private final TextView text;

            private BeatmapInfo beatmap;

            //------------------------------------------------------------------------------------//

            public ViewHolder(@NonNull View itemView, PlaylistAdapter parent) {
                super(itemView);
                this.parent = parent;
                this.text = (TextView) itemView;
            }

            //------------------------------------------------------------------------------------//

            public void bind(BeatmapInfo beatmap) {
                this.beatmap = beatmap;

                text.setText(BeatmapHelper.getArtist(beatmap) + " - " + BeatmapHelper.getTitle(beatmap));

                UI.musicPlayer.bindTouch(text, () -> {
                    if (Game.libraryManager.getBeatmap() != beatmap) {
                        Game.musicManager.change(beatmap.getTrack(0));
                        onSelect();
                    }
                });
            }

            //------------------------------------------------------------------------------------//

            public void onSelect() {
                if (parent.selected != null && parent.selected != this) {
                    parent.selected.onDeselect();
                }
                if (parent.selected == this) {
                    return;
                }
                parent.selected = this;

                Drawable background = text.getBackground();

                Animation.ofInt(background.getAlpha(), 60)
                        .runOnUpdate(value -> {
                            background.setAlpha((int) value);
                            text.setBackground(background);
                        })
                        .play(200);

                Animation.ofColor(Color.WHITE, color(R.color.accent))
                        .runOnUpdate(value -> text.setTextColor((int) value))
                        .play(200);
            }

            public void onDeselect() {
                if (parent.selected != this) {
                    return;
                }
                parent.selected = null;

                Drawable background = text.getBackground();

                Animation.ofInt(background.getAlpha(), 0)
                        .runOnUpdate(value -> {
                            background.setAlpha((int) value);
                            text.setBackground(background);
                        })
                        .play(200);

                Animation.ofColor(color(R.color.accent), Color.WHITE)
                        .runOnUpdate(value -> text.setTextColor((int) value))
                        .play(200);
            }
        }
    }
}
