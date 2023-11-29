package ru.nsu.ccfit.zuev.osu;

public class RGBAColor {

    private float cr;

    private float cg;

    private float cb;

    private float ca;

    public RGBAColor() {
        cr = 0;
        cg = 0;
        cb = 0;
        ca = 1;
    }

    public RGBAColor(final float r, final float g, final float b, final float a) {
        cr = r;
        cg = g;
        cb = b;
        ca = a;
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

    public float a() {
        return ca;
    }

    public void set(final float r, final float g, final float b, final float a) {
        cr = r;
        cg = g;
        cb = b;
        ca = a;
    }

}
