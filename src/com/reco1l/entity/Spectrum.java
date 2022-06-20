package com.reco1l.entity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.palette.graphics.Palette;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;

public class Spectrum {
    //Spectrum moved to a separate class as an object.

    @SuppressWarnings("FieldCanBeLocal")
    private final int //Leaving these variables as global to make it easy customizable.
            lines = 80,
            lineWidth = 14,
            lineDistance = 2,
            peakMultiplier = 600;
    @SuppressWarnings("FieldCanBeLocal")
    private final float alpha = 0.5f;

    private final float[]
            peakLevel = new float[lines],
            peakDownRate = new float[lines];

    private final Rectangle[] spectrum = new Rectangle[lines];

    public void draw(Scene scene) {
        for (int i = 0; i < lines; i++) {
            spectrum[i] = new Rectangle((lineWidth + lineDistance) * i, 0, lineWidth, 0);
            scene.attachChild(spectrum[i]);
        }
    }

    public void clear() {
        for (Rectangle line: spectrum) {
            line.setHeight(0);
            line.setAlpha(0);
        }
    }

    public void update() {

        float[] fft = GlobalManager.getInstance().getSongService().getSpectrum();
        if (fft == null)
            return;

        for (int i = 0; i < lines; i++) {
            float peak = 0;

            if (peak < fft[1 + i])
                peak = fft[1 + i];

            float CurrentPeakLevel = peak * peakMultiplier;

            if (CurrentPeakLevel > peakLevel[i]) {
                peakLevel[i] = CurrentPeakLevel;
                peakDownRate[i] = peakLevel[i] / 20;
            } else {
                peakLevel[i] = Math.max(peakLevel[i] - peakDownRate[i], 0f);
            }
            spectrum[i].setHeight(peakLevel[i]);
            spectrum[i].setPosition((lineWidth + lineDistance) * i,
                    Config.getRES_HEIGHT() - spectrum[i].getHeight());
            spectrum[i].setAlpha(alpha);
        }
    }

    //This method change the spectrum color according to the background luminance, using androidx.palette library.
    public void updateColor(TrackInfo currentTrack) {
        if (Build.VERSION.SDK_INT < 24) //todo Replace Color.luminance with some algorithm.
            return;

        Drawable drawable = Drawable.createFromPath(currentTrack.getBackground());
        if (drawable == null)
            return;

        int color = 1;

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        if (bitmap != null) {
            Palette palette = Palette.from(bitmap).generate();
            color = palette.getDominantColor(0x000000);
        }

        //Color.luminance isn't compatible with API < 24.
        //This can be replaced with some algorithm to get color luminance.
        boolean isDarker = Color.luminance(color) < 0.5f;

        for (Rectangle lines : spectrum){
            if (isDarker)
                lines.setColor(1, 1, 1);
            else
                lines.setColor(0, 0, 0);
        }

        if (bitmap != null)
            bitmap.recycle(); //This recycles the bitmap to avoid memory leaks.
    }
}
