package ru.nsu.ccfit.zuev.osu;

public class RGBAColor {

    private final float cr;

    private final float cg;

    private final float cb;

    private final float ca;

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

}
