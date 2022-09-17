package com.reco1l.ui.fragments.extras;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.ui.data.helpers.BeatmapHelper;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.ui.platform.UIManager;
import com.reco1l.utils.Animation;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.Resources;
import com.reco1l.interfaces.IMainClasses;
import com.reco1l.utils.listeners.TouchListener;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.MainScene;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 1/7/22 22:45

public class MusicPlayer extends UIFragment implements IMainClasses {

    public MainScene.MusicOption currentOption;

    private CardView body;
    private View songBody;
    private SeekBar seekBar;
    private ImageView play, songImage;
    private TextView titleTv, artistTv, timeTv, lengthTv;

    private Bitmap songBitmap;
    private SimpleDateFormat sdf;
    private BeatmapInfo lastBeatmap;
    private Drawable playDrawable, pauseDrawable;

    private AsyncExec songBitmapTask;

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
                    global.getMainScene().musicControl(MainScene.MusicOption.PAUSE);
                } else {
                    global.getMainScene().musicControl(MainScene.MusicOption.PLAY);
                }
            }
        });

        bindTouchListener(find("prev"), new TouchListener() {
            public void onPressDown() {
                global.getMainScene().doChange = true;
            }
            public void onPressUp() {
                global.getMainScene().lastHit = System.currentTimeMillis();
                global.getMainScene().musicControl(MainScene.MusicOption.PREV);
            }
        });

        bindTouchListener(find("next"), new TouchListener() {
            public void onPressDown() {
                global.getMainScene().doChange = true;
            }
            public void onPressUp() {
                global.getMainScene().lastHit = System.currentTimeMillis();
                global.getMainScene().musicControl(MainScene.MusicOption.NEXT);
            }
        });

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
                isTrackingTouch = false;
                onTouchEventNotified(MotionEvent.ACTION_UP);
            }
        });
        loadSongData(library.getBeatmap());
    }

    public void update() {
        length = global.getSongService().getLength();
        position = global.getSongService().getPosition();

        mActivity.runOnUiThread(() -> {

            if (library.getBeatmap() != lastBeatmap) {
                lastBeatmap = library.getBeatmap();
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
        BeatmapInfo beatmap = library.getBeatmap();
        songBitmap = null;

        if (topBar.isShowing) {
            topBar.musicButton.update(beatmap);
        }
        if (!isShowing)
            return;

        songBitmap = BeatmapHelper.getCompressedBackground(beatmap.getTrack(0));

        if (currentOption == MainScene.MusicOption.NEXT) {
            new Animation(songBody).moveX(0, -10).fade(1, 0)
                    .play(200);

            new Animation(songBody).moveX(10, 0).fade(0, 1)
                    .runOnStart(() -> loadSongData(beatmap))
                    .delay(200)
                    .play(200);
        }
        if (currentOption == MainScene.MusicOption.PREV) {
            new Animation(songBody).moveX(0, 10).fade(1, 0)
                    .play(200);
            new Animation(songBody).moveX(-10, 0).fade(0, 1)
                    .runOnStart(() -> loadSongData(beatmap))
                    .delay(200)
                    .play(200);
        }
    }

    private void loadSongData(BeatmapInfo beatmap) {
        titleTv.setText(BeatmapHelper.getTitle(beatmap));
        artistTv.setText(BeatmapHelper.getArtist(beatmap));
        lengthTv.setText(sdf.format(length));

        TrackInfo track = beatmap.getTrack(0);

        songImage.setImageDrawable(null);
        songImage.animate().cancel();
        ((View) songImage).setAlpha(0);

        if (track.getBackground() == null) {
            return;
        }

        Animation fade = new Animation(songImage).fade(0, 1)
                .runOnStart(() -> {
                    Bitmap bitmap = BeatmapHelper.getCompressedBackground(track);
                    songImage.setImageBitmap(bitmap);
                });

        if (songBitmap == null) {
            if (songBitmapTask != null) {
                songBitmapTask.cancel(true);
            }
            songBitmapTask = new AsyncExec() {
                public void run() {
                    BeatmapHelper.loadCompressedBackground(track);
                }
                public void onComplete() {
                    fade.play(200);
                }
            };
        } else {
            fade.play(200);
        }
    }

    //--------------------------------------------------------------------------------------------//


    @Override
    public void show() {
        if (isShowing)
            return;
        platform.closeThis(UIManager.getExtras());
        topBar.musicButton.playAnimation(true);
        super.show();
    }

    @Override
    public void close() {
        if (!isShowing)
            return;

        topBar.musicButton.playAnimation(false);

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
