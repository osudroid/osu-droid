package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

public enum FLConst {
    SLIDER_DIM_ALPHA(0.75f),
    AREA_SHRINK_FADE_DURATION(0.8f),
    TEXTURE_WIDTH(1024),
    TEXTURE_HEIGHT(512),
    BASE_SCALE_SIZE(8f),
    BASE_PX(-TEXTURE_WIDTH.v / 2f),
    BASE_PY(-TEXTURE_HEIGHT.v / 2f);

    float v;
    FLConst(float v) {
        this.v = v;
    }
}
