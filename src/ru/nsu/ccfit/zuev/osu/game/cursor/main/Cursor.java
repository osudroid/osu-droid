package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import android.graphics.PointF;

public class Cursor {

    public final PointF mousePos = new PointF(0, 0);

    public final PointF trackPos = new PointF(0, 0);

    public boolean mouseDown = false;

    public boolean mouseOldDown = false;

    public boolean mousePressed = false;

    public double mouseDownOffsetMS = 0;

}
