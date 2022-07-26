package com.reco1l.ui;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.ui.platform.UIManager;
import com.reco1l.utils.Animation;
import com.reco1l.utils.ClickListener;
import com.reco1l.utils.Res;
import com.reco1l.utils.interfaces.IMainClasses;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.MainScene;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 1/7/22 22:45

public class MusicPlayer extends UIFragment implements IMainClasses {

    private final SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
    private static Drawable playDrw, pauseDrw;

    public MainScene.MusicOption currentOption;

    private BeatmapInfo lastBeatmap;
    private Drawable songDrw;
    private int length, position, toPosition;

    private TextView titleTv, artistTv, timeTv, lengthTv;
    private ImageView play, songIv;
    private CardView body;
    private View innerBody;
    private SeekBar seekBar;

    private boolean isTrackingTouch = false;
    private final Runnable closeTask = this::close;

    //--------------------------------------------------------------------------------------------//

    public static void onResourcesLoad() {
        playDrw = Res.drw(R.drawable.v_play_xl_circle);
        pauseDrw = Res.drw(R.drawable.v_pause_xl);
    }

    @Override
    protected int getLayout() {
        return R.layout.music_player;
    }

    @Override
    protected String getPrefix() {
        return "mp";
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(true, true);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));

        body = find("body");
        body.postDelayed(closeTask, 8000);

        new Animation(body)
                .height((int) Res.dimen(R.dimen._30sdp), (int) Res.dimen(R.dimen.musicPlayerHeight))
                .interpolatorMode(Animation.InterpolatorTo.VALUE_ANIMATOR)
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
        songIv = find("songImage");
        innerBody = find("songBody");

        titleTv = find("title");
        artistTv = find("artist");
        lengthTv = find("songLength");
        timeTv = find("songProgress");

        play = find("play");
        ImageView prev = find("prev");
        ImageView next = find("next");

        new ClickListener(play).touchEffect(false).simple(() -> body.removeCallbacks(closeTask), () -> {
            if (global.getSongService().getStatus() == Status.PLAYING) {
                global.getMainScene().musicControl(MainScene.MusicOption.PAUSE);
            } else {
                global.getMainScene().musicControl(MainScene.MusicOption.PLAY);
            }
            body.postDelayed(closeTask, 8000);
        });

        new ClickListener(prev).simple(() -> {
            global.getMainScene().doChange = true;
            body.removeCallbacks(closeTask);
        }, () -> {
            global.getMainScene().lastHit = System.currentTimeMillis();
            global.getMainScene().musicControl(MainScene.MusicOption.PREV);
            body.postDelayed(closeTask, 8000);
        });

        new ClickListener(next).simple(() -> {
            global.getMainScene().doChange = true;
            body.removeCallbacks(closeTask);
        }, () -> {
            global.getMainScene().lastHit = System.currentTimeMillis();
            global.getMainScene().musicControl(MainScene.MusicOption.NEXT);
            body.postDelayed(closeTask, 8000);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTrackingTouch = true;
                //global.getMainScene().musicControl(MainScene.MusicOption.PAUSE);
                body.removeCallbacks(closeTask);
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
                //global.getMainScene().musicControl(MainScene.MusicOption.PLAY);
                isTrackingTouch = false;
                body.postDelayed(closeTask, 8000);
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
                if (play.getDrawable() != pauseDrw) {
                    new Animation(play).rotation(180, 0)
                            .runOnEnd(() -> play.setImageDrawable(pauseDrw))
                            .play(160);
                }
            }
            else if (play.getDrawable() != playDrw) {
                new Animation(play).rotation(180, 0)
                        .runOnEnd(() -> play.setImageDrawable(playDrw))
                        .play(160);
            }
        });
    }

    private void change() {
        TrackInfo track = global.getMainScene().selectedTrack;
        BeatmapInfo beatmap = library.getBeatmap();
        songDrw = null;

        if (beatmap == null)
            return;

        String title = !Config.isForceRomanized() ? beatmap.getTitleUnicode() : beatmap.getTitle();
        if (topBar.isShowing) {
            new Animation(topBar.musicText).fade(1, 0)
                    .play(150);
            new Animation(topBar.musicText).fade(0, 1)
                    .runOnStart(() -> topBar.musicText.setText(title))
                    .delay(150)
                    .play(150);
        }

        if (track != null && track.getBackground() != null)
            songDrw = Drawable.createFromPath(track.getBackground());

        if (!isShowing)
            return;

        if (currentOption == MainScene.MusicOption.NEXT) {
            new Animation(innerBody).moveX(0, -10).fade(1, 0)
                    .play(200);

            new Animation(innerBody).moveX(10, 0).fade(0, 1)
                    .runOnStart(() -> loadSongData(beatmap))
                    .delay(200)
                    .play(200);
        }
        if (currentOption == MainScene.MusicOption.PREV) {
            new Animation(innerBody).moveX(0, 10).fade(1, 0)
                    .play(200);
            new Animation(innerBody).moveX(-10, 0).fade(0, 1)
                    .runOnStart(() -> loadSongData(beatmap))
                    .delay(200)
                    .play(200);
        }
    }

    private void loadSongData(BeatmapInfo beatmap) {

        String title = !Config.isForceRomanized() ? beatmap.getTitleUnicode() : beatmap.getTitle();
        String artist = !Config.isForceRomanized() ? beatmap.getArtistUnicode() : beatmap.getArtist();

        titleTv.setText(title);
        artistTv.setText(artist);
        lengthTv.setText(sdf.format(length));

        if (songDrw == null) {
            songIv.setImageDrawable(null);
            return;
        }
        songIv.setImageDrawable(songDrw);
    }

    //--------------------------------------------------------------------------------------------//


    @Override
    public void show() {
        if (isShowing)
            return;

        platform.closeThis(UIManager.getExtras());
        
        new Animation(topBar.musicBody).moveY(0, -10).fade(1, 0).play(120);
        new Animation(topBar.musicArrow).rotation(180, 180).moveY(10, 0).fade(0, 1).delay(120).play(120);
        super.show();
    }

    @Override
    public void close() {
        if (!isShowing)
            return;

        body.removeCallbacks(closeTask);
        new Animation(topBar.musicArrow).rotation(180, 180).moveY(0, -10).fade(1, 0).play(120);
        new Animation(topBar.musicBody).moveY(10, 0).fade(0, 1).delay(120).play(120);

        new Animation(find("innerBody")).fade(1, 0).play(100);

        new Animation(body)
                .height((int) Res.dimen(R.dimen.musicPlayerHeight), (int) Res.dimen(R.dimen._30sdp))
                .interpolatorMode(Animation.InterpolatorTo.VALUE_ANIMATOR)
                .interpolator(Easing.OutExpo)
                .runOnEnd(super::close)
                .moveY(0, -30)
                .fade(1, 0)
                .play(240);
    }
}
