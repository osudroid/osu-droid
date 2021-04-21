package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

public enum TrailConst {
    DEFAULT_TRAIL_RATE_MIN(30),
    DEFAULT_TRAIL_RATE_MAX(30),
    DEFAULT_MAX_PARTICLES(30),
    LONG_TRAIL_RATE_MIN(DEFAULT_TRAIL_RATE_MIN.v * 2),
    LONG_TRAIL_RATE_MAX(DEFAULT_TRAIL_RATE_MIN.v * 2),
    LONG_TRAIL_MAX_PARTICLES(DEFAULT_MAX_PARTICLES.v * 4);

    int v;
    TrailConst(int v) {
        this.v = v;
    }
}
