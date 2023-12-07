package ru.nsu.ccfit.zuev.osu.game;

public class BreakPeriod {

    private final float length;

    private final float start;

    public BreakPeriod(final float starttime, final float endtime) {
        start = starttime;
        length = endtime - starttime;
    }

    public float getLength() {
        return length;
    }

    public float getStart() {
        return start;
    }

}
