package com.reco1l.ui.scenes.summary.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.annotation.Direction;
import com.reco1l.framework.drawing.PathDrawer;
import com.reco1l.view.RoundLayout;

public class PlayerArrowView extends RoundLayout {

    private int mArrowSize;

    private @Direction int mDirection;

    //--------------------------------------------------------------------------------------------//

    public PlayerArrowView(@NonNull Context context) {
        super(context);
    }

    public PlayerArrowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerArrowView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        mArrowSize = sdp(a.getAttributeIntValue(appNS, "arrowSize", 60));
        mDirection = a.getAttributeIntValue(appNS, "direction", 2);
    }

    @Override
    protected void onPathCreated(Path path, float radius, int w, int h) {

        int halfW = w / 2;
        int halfH = h / 2;

        PathDrawer drawer = new PathDrawer(path);

        Point arrowPeak;

        Point topL = new Point(0, 0);
        Point topR = new Point(w, 0);

        Point bottomR = new Point(w, h);
        Point bottomL = new Point(0, h);

        switch (mDirection) {
            case Direction.BOTTOM_TO_TOP:
                arrowPeak = new Point(halfW, 0);

                topL.y += mArrowSize;
                topR.y += mArrowSize;

                drawer.line(topL, arrowPeak, topR, bottomR, bottomL);
                break;
            case Direction.TOP_TO_BOTTOM:
                arrowPeak = new Point(halfW, h);

                bottomL.y -= mArrowSize;
                bottomR.y -= mArrowSize;

                drawer.line(topL, topR, bottomR, arrowPeak, bottomL);
                break;
            case Direction.LEFT_TO_RIGHT:
                arrowPeak = new Point(w, halfH);

                topR.x -= mArrowSize;
                bottomR.x -= mArrowSize;

                drawer.line(topL, topR, arrowPeak, bottomR, bottomL);
                break;
            case Direction.RIGHT_TO_LEFT:
                arrowPeak = new Point(0, halfH);

                topL.x += mArrowSize;
                bottomL.x += mArrowSize;

                drawer.line(topL, topR, bottomR, bottomL, arrowPeak);
                break;
        }
        path.close();
    }

    public @Direction int getDirection() {
        return mDirection;
    }
}
