package com.reco1l.framework.drawing;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class CompoundRect {

    public final Rect rect;
    public final Paint paint;

    //--------------------------------------------------------------------------------------------//

    public CompoundRect() {
        this(new Rect(), new Paint());
    }

    public CompoundRect(Rect rect) {
        this(rect, new Paint());
    }

    public CompoundRect(Paint paint) {
        this(new Rect(), paint);
    }

    public CompoundRect(CompoundRect clone) {
        this(new Rect(clone.rect), new Paint(clone.paint));
    }

    public CompoundRect(Rect rect, Paint paint) {
        if (rect == null) {
            rect = new Rect();
        }

        if (paint == null) {
            paint = new Paint();
        }

        this.rect = rect;
        this.paint = paint;
    }

    //--------------------------------------------------------------------------------------------//

    public void drawTo(Canvas canvas) {
        canvas.drawRect(rect, paint);
    }

    public void drawTo(Canvas canvas, float rx, float ry) {
        canvas.drawRoundRect(new RectF(rect), rx, ry, paint);
    }
}
