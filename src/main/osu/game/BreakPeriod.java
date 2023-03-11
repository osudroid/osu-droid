package main.osu.game;

public class BreakPeriod {
    private final float length;
    private final float start;
    private final float endTime;

    public BreakPeriod(final float starttime, final float endtime) {
        start = starttime;
        length = endtime - starttime;
        endTime = endtime;
    }

    public float getLength() {
        return length;
    }

    public float getStart() {
        return start;
    }

    public float getEndTime() {
        return endTime;
    }
}
