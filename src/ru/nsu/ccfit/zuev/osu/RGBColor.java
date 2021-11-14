package ru.nsu.ccfit.zuev.osu;

import androidx.annotation.NonNull;

import org.anddev.andengine.entity.Entity;

public class RGBColor {
    private float cr;
    private float cg;
    private float cb;

    public RGBColor(RGBColor copy) {
        this(copy.cr, copy.cg, copy.cb);
    }

    public RGBColor() {
        cr = 0;
        cg = 0;
        cb = 0;
    }

    public RGBColor(final float r, final float g, final float b) {
        cr = r;
        cg = g;
        cb = b;
    }

    /**
     * @param colorStr e.g. "#FFFFFF"
     * @return
     */
    public static RGBColor hex2Rgb(String colorStr) {
        return new RGBColor(
                Integer.valueOf(colorStr.substring(1, 3), 16) / 255.0f,
                Integer.valueOf(colorStr.substring(3, 5), 16) / 255.0f,
                Integer.valueOf(colorStr.substring(5, 7), 16) / 255.0f);
    }

    public float r() {
        return cr;
    }

    public float g() {
        return cg;
    }

    public float b() {
        return cb;
    }

    public void set(final float r, final float g, final float b) {
        cr = r;
        cg = g;
        cb = b;
    }

    public void apply(@NonNull Entity entity) {
        entity.setColor(cr, cg, cb);
    }

    public void applyAll(@NonNull Entity... entities) {
        for (Entity entity : entities) {
            entity.setColor(cr, cg, cb);
        }
    }
}
