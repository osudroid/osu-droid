package com.reco1l.ui.scenes.summary.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.view.RoundLayout;

public class PlayerArrowView extends RoundLayout {

    private int mArrowWidth;

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
        mArrowWidth = sdp(a.getAttributeIntValue(appNS, "arrowWidth", 60));
    }

    @Override
    protected void onPathCreated(Path path, float radius, int width, int height) {


        /*
        * A        B
        *
        *               C
        *
        * E        D
        */

        Point B = new Point(width - mArrowWidth, 0);
        Point C = new Point(width, height / 2);
        Point D = new Point(width - mArrowWidth, height);
        Point E = new Point(0, height);

        path.lineTo(B.x, B.y);
        path.lineTo(C.x, C.y);
        path.lineTo(D.x, D.y);
        path.lineTo(E.x, E.y);
        path.lineTo(0, 0);
        path.close();
    }
}
