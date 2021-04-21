package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import ru.nsu.ccfit.zuev.osu.Config;

public enum CursorConst {
    CURSOR_SIZE(Config.getCursorSize() * 2);

    float v;
    CursorConst(float v) {
        this.v = v;
    }
}
