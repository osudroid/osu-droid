package com.reco1l.framework.drawing;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class CompoundPath {

    public final Path path;
    public final Paint paint;

    //--------------------------------------------------------------------------------------------//

    public CompoundPath() {
        this(new Path(), new Paint());
    }

    public CompoundPath(Path path) {
        this(path, new Paint());
    }

    public CompoundPath(Paint paint) {
        this(new Path(), paint);
    }

    public CompoundPath(CompoundPath clone) {
        this(new Path(clone.path), new Paint(clone.paint));
    }

    public CompoundPath(Path path, Paint paint) {
        if (path == null) {
            path = new Path();
        }

        if (paint == null) {
            paint = new Paint();
        }

        this.path = path;
        this.paint = paint;
    }

    //--------------------------------------------------------------------------------------------//

    public void drawTo(Canvas canvas) {
        canvas.drawPath(path, paint);
    }
}
