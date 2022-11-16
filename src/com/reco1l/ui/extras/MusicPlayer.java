package com.reco1l.ui.extras;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.enums.MusicOption;
import com.reco1l.UI;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.Resources;
import com.reco1l.utils.listeners.TouchListener;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 1/7/22 22:45

public class MusicPlayer extends UIFragment {

    public static MusicPlayer instance;

    public MusicOption currentOption;

    private CardView body;
    private View songBody;
    private SeekBar seekBar;
    private ImageView play, songImage;
    private TextView titleTv, artistTv, timeTv, lengthTv;

    private Bitmap songBitmap;
    private SimpleDateFormat sdf;
    private Drawable playDrawable, pauseDrawable;

    private AsyncExec bitmapTask;

    private int length, position, toPosition;

    private boolean
            isListVisible = false,
            isTrackingTouch = false,
            wasChangedFromList = false;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.music_player;
    }

    @Override
    protected String getPrefix() {
        return "mp";
    }

    @Override
    protected long getDismissTime() {
        return 8000;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(true, true);
        sdf = new SimpleDateFormat("mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));

        body = find("body");

        playDrawable = Resources.drw(R.drawable.v_play_xl_circle);
        pauseDrawable = Resources.drw(R.drawable.v_pause_xl);

        new Animation(body)
                .height((int) Resources.dimen(R.dimen._30sdp), (int) Resources.dimen(R.dimen.musicPlayerHeight))
                .interpolatorMode(Animation.Interpolate.VALUE_ANIMATOR)
                .interpolator(Easing.OutExpo)
                .moveY(-30, 0)
                .fade(0, 1)
                .play(240);

        new Animation(find("songBody"))
                .forChildView(child -> new Animation(child).fade(0, child.getAlpha()))
                .runOnStart(() ->
                        find("buttons").setAlpha(0))
                .runOnEnd(() ->
                        new Animation(find("buttons")).fade(0, 1).play(100))
                .play(100);

        RecyclerView list = find("listRv");
        songImage = find("songImage");
        songBody = find("songBody");
        seekBar = find("seekBar");

        lengthTv = find("songLength");
        timeTv = find("songProgress");
        artistTv = find("artist");
        titleTv = find("title");
        play = find("play");

        bindTouchListener(play, new TouchListener() {
            public boolean hasTouchEffect() {
                return false;
            }

            public void onPressUp() {
                if (global.getSongService().getStatus() == Status.PLAYING) {
                    Game.musicManager.pause();
                } else {
                    Game.musicManager.play();
                }
            }
        });

        bindTouchListener(find("list"), this::switchListVisibility);
        bindTouchListener(find("prev"), Game.musicManager::previous);
        bindTouchListener(find("next"), Game.musicManager::next);

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTrackingTouch = true;
                onTouchEventNotified(MotionEvent.ACTION_DOWN);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (Game.songService != null && fromTouch) {
                    toPosition = progress;
                    timeTv.setText(sdf.format(progress));
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Game.songService.setPosition(toPosition);
                isTrackingTouch = false;
                Log.i("MusicPlayer", "Skipped song position to " + sdf.format(toPosition));
                onTouchEventNotified(MotionEvent.ACTION_UP);
            }
        });

        list.setLayoutManager(new LinearLayoutManager(Game.mActivity, VERTICAL, false));
        list.setAdapter(new PlaylistAdapter(Game.library.getLibrary()));

        loadMetadata(Game.musicManager.beatmap);
        loadSeekbarImage();
    }

    private void switchListVisibility() {
        if (!isListVisible) {
            isListVisible = true;
            new Animation(find("listBody")).width(0, Resources.dimen(R.dimen.panelWidth))
                    .play(300);
        } else {
            isListVisible = false;
            new Animation(find("listBody")).width(Resources.dimen(R.dimen.panelWidth), 0)
                    .play(300);
        }
    }

    @Override
    protected void onUpdate(float elapsed) {
        if (Game.songService != null) {
            length = Game.songService.getLength();
            position = Game.songService.getPosition();
        }

        if (!isShowing || Game.songService == null)
            return;

        if (!isTrackingTouch) {
            seekBar.setMax(length);
            seekBar.setProgress(position);
            timeTv.setText(sdf.format(position));
        }

        if (Game.songService.getStatus() == Status.PLAYING) {
            if (play.getDrawable() != pauseDrawable) {
                new Animation(play).rotation(180, 0)
                        .runOnEnd(() -> play.setImageDrawable(pauseDrawable))
                        .play(160);
            }
        } else if (play.getDrawable() != playDrawable) {
            new Animation(play).rotation(180, 0)
                    .runOnEnd(() -> play.setImageDrawable(playDrawable))
                    .play(160);
        }
    }

    public void changeMusic(BeatmapInfo beatmap) {
        changeBitmap(beatmap.getTrack(0));

        if (isShowing) {
            loadMetadata(beatmap);

            if (currentOption == MusicOption.NEXT || wasChangedFromList) {
                wasChangedFromList = false;

                new Animation(songBody).moveX(0, -10).fade(1, 0)
                        .play(200);

                new Animation(songBody).moveX(10, 0).fade(0, 1)
                        .runOnStart(() -> loadMetadata(beatmap))
                        .delay(200)
                        .play(200);
            }
            if (currentOption == MusicOption.PREVIOUS) {
                new Animation(songBody).moveX(0, 10).fade(1, 0)
                        .play(200);
                new Animation(songBody).moveX(-10, 0).fade(0, 1)
                        .runOnStart(() -> loadMetadata(beatmap))
                        .delay(200)
                        .play(200);
            }
        }
    }

    private void changeBitmap(TrackInfo track) {
        if (bitmapTask != null) {
            bitmapTask.cancel(true);
            bitmapTask = null;
        }

        if (track.getBackground() == null) {
            songBitmap = null;
            loadSeekbarImage();
            return;
        }

        bitmapTask = new AsyncExec() {
            public void run() {
                BeatmapHelper.loadCompressedBackground(track);
            }

            public void onComplete() {
                songBitmap = BeatmapHelper.getCompressedBackground(track);
                loadSeekbarImage();
            }
        };
        bitmapTask.execute();
    }

    private void loadMetadata(BeatmapInfo beatmap) {
        titleTv.setText(BeatmapHelper.getTitle(beatmap));
        artistTv.setText(BeatmapHelper.getArtist(beatmap));
        lengthTv.setText(sdf.format(length));
    }

    private void loadSeekbarImage() {
        if (isShowing) {
            ((View) songImage).setAlpha(0);

            if (songBitmap != null) {
                songImage.setImageBitmap(songBitmap);
                new Animation(songImage).fade(0, 1).play(200);
            } else {
                songImage.setImageDrawable(null);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void show() {
        if (isShowing)
            return;

        Game.platform.close(UI.getExtras());
        UI.topBar.musicButton.animateButton(true);
        super.show();
    }

    @Override
    public void close() {
        if (!isShowing)
            return;

        if (isListVisible) {
            switchListVisibility();
        }

        UI.topBar.musicButton.animateButton(false);

        new Animation(find("innerBody")).fade(1, 0).play(100);

        new Animation(body)
                .height((int) Resources.dimen(R.dimen.musicPlayerHeight), (int) Resources.dimen(R.dimen._30sdp))
                .interpolatorMode(Animation.Interpolate.VALUE_ANIMATOR)
                .interpolator(Easing.OutExpo)
                .runOnEnd(super::close)
                .moveY(0, -30)
                .fade(1, 0)
                .play(240);
    }

    //--------------------------------------------------------------------------------------------//

    public static class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {

        private final List<BeatmapInfo> songs;

        //----------------------------------------------------------------------------------------//

        public PlaylistAdapter(List<BeatmapInfo> songs) {
            this.songs = songs;
            setHasStableIds(true);
        }

        //----------------------------------------------------------------------------------------//

        @NonNull
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView view = new TextView(Game.mActivity);

            view.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            view.setBackground(Resources.drw(R.drawable.round_shape));
            view.setEllipsize(TextUtils.TruncateAt.END);
            view.setSingleLine(true);

            int m = (int) Resources.dimen(R.dimen.M);
            int s = (int) Resources.dimen(R.dimen.S);
            view.setPadding(m, s, m, s);

            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.assign(songs.get(position));
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public int getItemCount() {
            return songs.size();
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

        public static class VH extends RecyclerView.ViewHolder {

            private final TextView text;

            //------------------------------------------------------------------------------------//

            public VH(@NonNull View itemView) {
                super(itemView);
                text = (TextView) itemView;
            }

            //------------------------------------------------------------------------------------//

            public void assign(BeatmapInfo song) {
                text.setText(BeatmapHelper.getArtist(song) + " - " + BeatmapHelper.getTitle(song));

                UI.musicPlayer.bindTouchListener(text, () -> {
                    UI.musicPlayer.wasChangedFromList = true;
                    Game.musicManager.change(song);
                });
            }
        }
    }
}
