package com.reco1l.view.custom;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.view.RoundLayout;

public class TiltedLayout extends RoundLayout {

    public TiltedLayout(@NonNull Context context) {
        super(context);
    }

    public TiltedLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TiltedLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onPathCreated(Path path, float radius, int width, int height) {

        int tilt = sdp(4);

        Point A = new Point(0, 0);
        Point B = new Point(width, 0);
        Point C = new Point(width - tilt, height);
        Point D = new Point(0, height);

        path.lineTo(B.x, B.y);
        path.lineTo(C.x, C.y);
        path.lineTo(D.x, D.y);
        path.lineTo(A.x, A.y);
    }
}
