package com.reco1l.andengine.entity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import androidx.palette.graphics.Palette;

import com.reco1l.andengine.IAttachableEntity;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.listeners.ModifierListener;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.util.modifier.IModifier;

import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class Spectrum implements IAttachableEntity {

    private static int lines;

    private static final int
            lineWidth = 12,
            lineDistance = 1,
            peakMultiplier = 600;

    private final Drawing spectrum, shadow;

    private Bitmap bitmap;
    private AsyncExec task;

    //--------------------------------------------------------------------------------------------//

    public Spectrum() {
        lines = screenWidth / (lineWidth + lineDistance) + 1;

        spectrum = new Drawing();
        shadow = new Drawing();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
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

    public void setColor(int r, int g, int b) {
        spectrum.setColor(r, g, b);
        shadow.setColor(r, g, b);
    }

    @Override
    public void update() {
        float[] fft = GlobalManager.getInstance().getSongService().getSpectrum();

        if (fft != null) {
            spectrum.fft = fft;
            shadow.fft = fft;

            spectrum.update();
            shadow.update();
        }
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
            setColor(1, 1, 1);
            return;
        }

        if (task != null) {
            task.cancel(true);
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
            task = null;
        }

        task = new AsyncExec() {
            @Override
            public void run() {
                bitmap = BitmapFactory.decodeFile(background);
                if (bitmap == null) {
                    setColor(1, 1, 1);
                    return;
                }

                Palette palette = Palette.from(bitmap).generate();

                if (isDark(palette.getDominantColor(0))) {
                    setColor(1, 1, 1);
                } else {
                    setColor(0, 0, 0);
                }
            }

            @Override
            public void onComplete() {
                if (bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }
                task = null;
            }
        };
        task.execute();
    }

    private static class Drawing implements IAttachableEntity {

        private final Rectangle[] spectrum;

        private final float[] peakLevel;
        private final float[] peakDownRate;

        private float[] fft;

        private float alpha;
        private int downRateMultiplier;
        private boolean isForcedClearInProgress = false;

        //--------------------------------------------------------------------------------------------//

        private Drawing() {
            peakLevel = new float[Spectrum.lines];
            peakDownRate = new float[Spectrum.lines];
            spectrum = new Rectangle[Spectrum.lines];
        }

        @Override
        public void draw(Scene scene) {
            for (int i = 0; i < Spectrum.lines; i++) {
                spectrum[i] = new Rectangle(
                        (Spectrum.lineWidth + Spectrum.lineDistance) * i, 0, Spectrum.lineWidth, 0
                );
                spectrum[i].setAlpha(0);
                scene.attachChild(spectrum[i], 1);
            }
        }

        public void clear(boolean force) {
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

        @Override
        public void update() {
            for (int i = 0; i < Spectrum.lines; i++) {

                if (i > fft.length - 1) {
                    break;
                }

                float peak = fft[i + 1] * Spectrum.peakMultiplier;

                if (peak > peakLevel[i]) {
                    peakLevel[i] = peak;
                    peakDownRate[i] = peakLevel[i] / downRateMultiplier;
                } else {
                    peakLevel[i] = Math.max(peakLevel[i] - peakDownRate[i], 0);
                }
                spectrum[i].setHeight(peakLevel[i]);
                spectrum[i].setPosition(
                        (Spectrum.lineWidth + Spectrum.lineDistance) * i, screenHeight - spectrum[i].getHeight()
                );

                if (spectrum[i].getAlpha() != alpha && !isForcedClearInProgress) {
                    spectrum[i].clearEntityModifiers();
                    spectrum[i].setAlpha(alpha);
                }
            }
        }

        public void setColor(int r, int g, int b) {
            for (int i = 0; i < Spectrum.lines; i++) {
                spectrum[i].setColor(r, g, b);
            }
        }
    }
}
