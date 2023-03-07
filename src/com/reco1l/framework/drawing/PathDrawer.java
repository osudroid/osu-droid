package com.reco1l.framework.drawing;

import android.graphics.Path;
import android.graphics.Point;

public final class PathDrawer {

    private final Path mPath;

    public PathDrawer(Path path) {
        mPath = path;
    }

    public void line(Point... points) {
        move(points[0]);

        for (Point point : points) {
            mPath.lineTo(point.x, point.y);
        }
    }

    public void move(Point point) {
        mPath.moveTo(point.x, point.y);
    }
}
