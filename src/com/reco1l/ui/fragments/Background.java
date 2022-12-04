package com.reco1l.ui.fragments;

// Created by Reco1l on 13/11/2022, 21:13

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.AnimationTable;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.BlurEffect;
import com.reco1l.utils.Resources;
import com.reco1l.utils.helpers.BitmapHelper;
import com.reco1l.view.SidesEffectView;
import com.reco1l.view.SpectrumView;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

public class Background extends UIFragment {

    public static Background instance;

    private SidesEffectView kiaiView;
    private SpectrumView spectrum;

    private ImageView image0, image1;

    private AsyncExec backgroundTask;

    private String imagePath;
    private Bitmap bitmap;

    private boolean
            isReload = false,
            isBlurEnabled = false,
            areEffectsEnabled = true;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "b";
    }

    @Override
    protected int getLayout() {
        return R.layout.background;
    }

    @Override
    protected Screens[] getParents() {
        return new Screens[] {Screens.Main, Screens.Selector, Screens.Loader, Screens.Summary};
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        image0 = find("image0");
        image1 = find("image1");

        spectrum = find("spectrum");
        spectrum.setLinesWidth(Resources.sdp(6));
        spectrum.setLineDistance(Resources.sdp(1));

        kiaiView = find("kiai");
        kiaiView.setPaintColor(Color.WHITE);

        if (!areEffectsEnabled) {
            kiaiView.setAlpha(0);
            spectrum.setAlpha(0);
        }

        if (bitmap != null) {
            image0.setImageBitmap(bitmap);
        }
    }

    @Override
    protected void onUpdate(float secondsElapsed) {
        if (areEffectsEnabled) {
            float[] fft = Game.songService.getSpectrum();
            float level = Game.songService.getLevel() * 2;

            if (spectrum != null) {
                spectrum.setFft(fft);
            }

            if (kiaiView != null) {
                kiaiView.setAlpha(level);
            }
        }
    }

    public void onBeatUpdate(float beatLength, int beat) {
        if (!areEffectsEnabled) {
            return;
        }

        if (kiaiView != null) {
            Game.activity.runOnUiThread(() ->
                    kiaiView.onBeatUpdate(beatLength, beat)
            );
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void setKiai(boolean bool) {
        if (kiaiView != null) {
            kiaiView.setKiai(bool);
        }
    }

    public void setBlur(boolean bool) {
        if (bool == isBlurEnabled) {
            return;
        }
        isBlurEnabled = bool;
        isReload = true;
        changeFrom(imagePath);
    }

    public void setEffects(boolean bool) {
        if (bool == areEffectsEnabled) {
            return;
        }
        areEffectsEnabled = bool;

        if (bool) {
            if (kiaiView != null) {
                AnimationTable.fadeIn(kiaiView).play();
            }
            if (spectrum != null) {
                AnimationTable.fadeIn(spectrum).play();
            }
        } else {
            if (kiaiView != null) {
                AnimationTable.fadeOut(kiaiView).play();
            }
            if (spectrum != null) {
                AnimationTable.fadeOut(spectrum).play();
            }
        }
    }

    //--------------------------------------------------------------------------------------------//


    public void changeFrom(String path) {
        if (!isReload) {
            if (path == null || path.equals(imagePath)) {
                return;
            }
        }
        isReload = false;
        imagePath = path;

        if (backgroundTask != null) {
            backgroundTask.cancel(true);
        }

        backgroundTask = new AsyncExec() {
            Bitmap newBitmap;

            public void run() {
                int quality = Math.max(Config.getBackgroundQuality(), 1);

                newBitmap = BitmapFactory.decodeFile(imagePath);
                newBitmap = BitmapHelper.compress(newBitmap, 100 / quality);

                if (isBlurEnabled) {
                    newBitmap = BlurEffect.applyTo(newBitmap, 25);
                }
            }

            public void onComplete() {
                handleChange(newBitmap);
                Game.resources.loadBackground(path);
            }
        };
        backgroundTask.execute();
    }

    private void handleChange(Bitmap newBitmap) {

        boolean cursor = image0.getVisibility() == View.VISIBLE;

        ImageView front = cursor ? image0 : image1;
        ImageView back = cursor ? image1 : image0;

        Game.activity.runOnUiThread(() -> {
            back.setImageBitmap(newBitmap);
            back.setVisibility(View.VISIBLE);
            back.setAlpha(1f);
        });

        AnimationTable.fadeOut(front)
                .runOnEnd(() -> {
                    front.setImageBitmap(null);
                    front.setVisibility(View.GONE);
                    front.setElevation(0f);

                    back.setElevation(1f);

                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    bitmap = newBitmap;
                })
                .play(500);
    }

    public void reload() {
        if (isShowing && imagePath != null) {
            isReload = true;
            changeFrom(imagePath);
        }
    }
}
