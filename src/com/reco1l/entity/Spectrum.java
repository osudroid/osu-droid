package com.reco1l.entity;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.palette.graphics.Palette;

import com.reco1l.utils.ModifierListener;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.util.modifier.IModifier;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class Spectrum {

    private final Drawing spectrum, shadow;
    private static int lines;

    private static final int
            lineWidth = 12,
            lineDistance = 1,
            peakMultiplier = 600;

    //--------------------------------------------------------------------------------------------//

    public Spectrum() {
        lines = Config.getRES_WIDTH() / (lineWidth + lineDistance) + 1;

        spectrum = new Drawing();
        shadow = new Drawing();
    }

    //--------------------------------------------------------------------------------------------//

    public void draw(Scene scene) {
        spectrum.draw(scene);
        shadow.draw(scene);

        spectrum.downRateMultiplier = 20;
        spectrum.alpha = 0.3f;
        shadow.downRateMultiplier = 50;
        shadow.alpha = 0.2f;
    }

    public void clear(boolean force) {
        spectrum.clear(force);
        shadow.clear(force);
    }

    public void update() {
        float[] fft = GlobalManager.getInstance().getSongService().getSpectrum();

        if (fft == null)
            return;

        spectrum.update(fft);
        shadow.update(fft);
    }

    //--------------------------------------------------------------------------------------------//

    private boolean isDark(int color) { // Credits: https://stackoverflow.com/a/24261119
        double d = 1
                - (0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255;
        return !(d < 0.5);
    }

    public void updateColor(String background) {
        if (background == null || Build.VERSION.SDK_INT < 24) {
            spectrum.setColor(1, 1, 1);
            shadow.setColor(1, 1, 1);
            return;
        }

        BitmapDrawable drw = (BitmapDrawable) Drawable.createFromPath(background);
        if (drw == null || drw.getBitmap() == null) {
            spectrum.setColor(1, 1, 1);
            shadow.setColor(1, 1, 1);
            return;
        }

        Palette palette = Palette.from(drw.getBitmap()).generate();

        if (isDark(palette.getDominantColor(0xFFFFFFFF))) {
            spectrum.setColor(1, 1, 1);
            shadow.setColor(1, 1, 1);
        } else {
            spectrum.setColor(0, 0, 0);
            shadow.setColor(0, 0, 0);
        }

        drw.getBitmap().recycle();
    }

    private static class Drawing {

        private final Rectangle[] spectrum;

        public int downRateMultiplier;
        public float alpha;

        private final float[] peakLevel, peakDownRate;
        private boolean isForcedClearInProgress = false;


        //--------------------------------------------------------------------------------------------//

        private Drawing() {
            peakLevel = new float[Spectrum.lines];
            peakDownRate = new float[Spectrum.lines];
            spectrum = new Rectangle[Spectrum.lines];
        }

        private void draw(Scene scene) {
            for (int i = 0; i < Spectrum.lines; i++) {
                spectrum[i] = new Rectangle((Spectrum.lineWidth + Spectrum.lineDistance) * i, 0,
                        Spectrum.lineWidth, 0);
                spectrum[i].setAlpha(0);
                scene.attachChild(spectrum[i], 1);
            }
        }

        private void clear(boolean force) {
            isForcedClearInProgress = force;

            for (int i = 0; i < Spectrum.lines; i++) {
                spectrum[i].clearEntityModifiers();

                AlphaModifier modifier = new AlphaModifier(force ? 0.4f : 0.5f, spectrum[i].getAlpha(), 0);

                if (force && i == 0) {
                    modifier.addModifierListener(new ModifierListener() {
                        @Override
                        public void onModifierFinished(IModifier<IEntity> m, IEntity i) {
                            isForcedClearInProgress = false;
                        }
                    });
                }

                spectrum[i].registerEntityModifier(modifier);
            }
        }

        private void update(float[] fft) {

            for (int i = 0; i < Spectrum.lines; i++) {
                if (i > fft.length - 1)
                    break;

                float peak = fft[i + 1] * Spectrum.peakMultiplier;

                if (peak > peakLevel[i]) {
                    peakLevel[i] = peak;
                    peakDownRate[i] = peakLevel[i] / downRateMultiplier;
                } else {
                    peakLevel[i] = Math.max(peakLevel[i] - peakDownRate[i], 0);
                }
                spectrum[i].setHeight(peakLevel[i]);
                spectrum[i].setPosition((Spectrum.lineWidth + Spectrum.lineDistance) * i,
                        Config.getRES_HEIGHT() - spectrum[i].getHeight());

                if (spectrum[i].getAlpha() != alpha && !isForcedClearInProgress) {
                    spectrum[i].clearEntityModifiers();
                    spectrum[i].setAlpha(alpha);
                }
            }
        }

        private void setColor(int r, int g, int b) {
            for (int i = 0; i < Spectrum.lines; i++) {
                spectrum[i].setColor(r, g, b);
            }
        }
    }
}
