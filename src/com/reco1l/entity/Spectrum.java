package com.reco1l.entity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.palette.graphics.Palette;

import com.reco1l.utils.ModifierListener;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.ColorModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.util.modifier.IModifier;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;

@SuppressWarnings("FieldCanBeLocal")
public class Spectrum {

    private int lines;

    private final int
            lineWidth = 14,
            lineDistance = 2,
            peakMultiplier = 600;

    private final float alpha = 0.5f;

    private float[] peakLevel, peakDownRate;

    private AlphaModifier[] alphaModifiers;
    private ColorModifier[] colorModifiers;
    private Rectangle[] spectrum;

    private boolean isForcedClearInProgress = false;

    //--------------------------------------------------------------------------------------------//

    public void draw(Scene scene) {
        lines = Config.getRES_WIDTH() / (lineWidth + lineDistance) + 1;

        peakLevel = new float[lines];
        peakDownRate = new float[lines];
        alphaModifiers = new AlphaModifier[lines];
        colorModifiers = new ColorModifier[lines];
        spectrum = new Rectangle[lines];

        for (int i = 0; i < lines; i++) {
            spectrum[i] = new Rectangle((lineWidth + lineDistance) * i, 0, lineWidth, 0);
            spectrum[i].setAlpha(0);
            scene.attachChild(spectrum[i], 1);
        }
    }

    public void clear(boolean force) {
        isForcedClearInProgress = force;

        for (int i = 0; i < lines; i++) {
            spectrum[i].clearEntityModifiers();
            alphaModifiers[i] = new AlphaModifier(force ? 0.3f : 0.5f, spectrum[i].getAlpha(), 0);

            if (force && i == 0) {
                alphaModifiers[i].addModifierListener(new ModifierListener() {
                    @Override
                    public void onModifierFinished(IModifier<IEntity> m, IEntity i) {
                        isForcedClearInProgress = false;
                    }
                });
            }
            spectrum[i].registerEntityModifier(alphaModifiers[i]);
        }
    }

    public void update() {

        float[] fft = GlobalManager.getInstance().getSongService().getSpectrum();
        if (fft == null) return;

        for (int i = 0; i < lines; i++) {
            float peak = 0;

            if (peak < fft[1 + i])
                peak = fft[1 + i];

            float currentPeakLevel = peak * peakMultiplier;

            if (currentPeakLevel > peakLevel[i]) {
                peakLevel[i] = currentPeakLevel;
                peakDownRate[i] = peakLevel[i] / 20;
            } else {
                peakLevel[i] = Math.max(peakLevel[i] - peakDownRate[i], 0f);
            }
            spectrum[i].setHeight(peakLevel[i]);
            spectrum[i].setPosition((lineWidth + lineDistance) * i,
                    Config.getRES_HEIGHT() - spectrum[i].getHeight());

            if (spectrum[i].getAlpha() != alpha && !isForcedClearInProgress) {
                spectrum[i].unregisterEntityModifier(alphaModifiers[i]);
                spectrum[i].setAlpha(alpha);
            }
        }
    }

    //This method change the spectrum color according to the background luminance, using androidx.palette library.
    public void updateColor(TrackInfo currentTrack) {
        if (Build.VERSION.SDK_INT < 24)
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

        //Color.luminance is not compatible with API < 24.
        //This can be replaced with some algorithm to get color luminance.
        boolean isDarker = Color.luminance(color) < 0.5f;

        for (int i = 0; i < lines; i++) {

            spectrum[i].unregisterEntityModifier(colorModifiers[i]);
            final float[] cRGB = {spectrum[i].getRed(), spectrum[i].getGreen(), spectrum[i].getBlue()};

            if (isDarker) {
                colorModifiers[i] = new ColorModifier(0.2f, cRGB[0], 1, cRGB[1], 1, cRGB[2], 1);
            } else {
                colorModifiers[i] = new ColorModifier(0.2f, cRGB[0], 0, cRGB[1], 0, cRGB[2], 0);
            }
            spectrum[i].registerEntityModifier(colorModifiers[i]);
        }

        if (bitmap != null)
            bitmap.recycle(); // Recycling the bitmap to avoid memory leaks.
    }
}
