package com.reco1l.ui.fragments.extras;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.enums.MusicOption;
import com.reco1l.UI;
import com.reco1l.management.MusicManager;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.Resources;
import com.reco1l.interfaces.IReferences;
import com.reco1l.utils.listeners.TouchListener;

import java.text.SimpleDateFormat;
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
    private BeatmapInfo lastBeatmap;
    private Drawable playDrawable, pauseDrawable;

    private AsyncExec bitmapTask;

    private int length, position, toPosition;
    private boolean isTrackingTouch = false;

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
        
        seekBar = find("seekBar");
        songImage = find("songImage");
        songBody = find("songBody");

        titleTv = find("title");
        artistTv = find("artist");
        lengthTv = find("songLength");
        timeTv = find("songProgress");
        play = find("play");

        bindTouchListener(play, new TouchListener() {
            public boolean hasTouchEffect() { return false; }

            public void onPressUp() {
                if (global.getSongService().getStatus() == Status.PLAYING) {
                    Game.musicManager.pause();
                } else {
                    Game.musicManager.play();
                }
            }
        });

        bindTouchListener(find("prev"), Game.musicManager::previous);
        bindTouchListener(find("next"), Game.musicManager::next);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTrackingTouch = true;
                onTouchEventNotified(MotionEvent.ACTION_DOWN);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (global.getSongService() != null && fromTouch) {
                    toPosition = progress;
                    timeTv.setText(sdf.format(progress));
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                global.getSongService().setPosition(toPosition);
                Log.i("MusicPlayer", "Skipped song position to " + sdf.format(toPosition));
                isTrackingTouch = false;
                onTouchEventNotified(MotionEvent.ACTION_UP);
            }
        });

        loadMetadata(library.getBeatmap());
        loadSeekbarImage();
    }

    public void update() {
        length = global.getSongService().getLength();
        position = global.getSongService().getPosition();

        mActivity.runOnUiThread(() -> {

            if (MusicManager.beatmap != lastBeatmap) {
                lastBeatmap = MusicManager.beatmap;
                change();
                return;
            }

            if (!isShowing || !isVisible())
                return;

            if (!isTrackingTouch) {
                seekBar.setMax(length);
                seekBar.setProgress(position);
                timeTv.setText(sdf.format(position));
            }

            if (global.getSongService().getStatus() == Status.PLAYING) {
                if (play.getDrawable() != pauseDrawable) {
                    new Animation(play).rotation(180, 0)
                            .runOnEnd(() -> play.setImageDrawable(pauseDrawable))
                            .play(160);
                }
            }
            else if (play.getDrawable() != playDrawable) {
                new Animation(play).rotation(180, 0)
                        .runOnEnd(() -> play.setImageDrawable(playDrawable))
                        .play(160);
            }
        });
    }

    private void change() {
        BeatmapInfo beatmap = MusicManager.beatmap;

        if (UI.topBar.isShowing) {
            UI.topBar.musicButton.update(beatmap);
        }
        changeBitmap(beatmap.getTrack(0));

        if (isShowing) {
            if (currentOption == MusicOption.NEXT) {
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
        if (!isShowing)
            return;
        ((View) songImage).setAlpha(0);

        if (songBitmap != null) {
            songImage.setImageBitmap(songBitmap);
            new Animation(songImage).fade(0, 1).play(200);
        } else {
            songImage.setImageDrawable(null);
        }
    }

    //--------------------------------------------------------------------------------------------//


    @Override
    public void show() {
        if (isShowing)
            return;
        platform.close(UI.getExtras());
        UI.topBar.musicButton.playAnimation(true);
        super.show();
    }

    @Override
    public void close() {
        if (!isShowing)
            return;

        UI.topBar.musicButton.playAnimation(false);

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
}
