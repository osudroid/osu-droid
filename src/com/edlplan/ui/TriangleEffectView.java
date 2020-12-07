package com.edlplan.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class TriangleEffectView extends View {
    private static final String NAMESPACE = "http://ui.edlplan.com/customview";

    private TriangleDrawable triangleDrawable;

    public TriangleEffectView(Context context) {
        super(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(triangleDrawable = new TriangleDrawable());
        }
    }

    public TriangleEffectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(triangleDrawable = createDrawable(attrs));
        }
    }

    public TriangleEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(triangleDrawable = createDrawable(attrs));
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TriangleEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setBackground(triangleDrawable = createDrawable(attrs));
    }

    protected TriangleDrawable createDrawable(@Nullable AttributeSet attrs) {
        boolean preSpawnTriangles = true;
        if (attrs != null) {
            preSpawnTriangles = attrs.getAttributeBooleanValue(NAMESPACE, "preSpawnTriangles", true);
        }
        TriangleDrawable triangle = new TriangleDrawable(preSpawnTriangles);
        if (attrs != null) {
            if (attrs.getAttributeBooleanValue(NAMESPACE, "freeze", false)) {
                triangle.setFreeze(true);
            }
            triangle.setEdgeClampRate(attrs.getAttributeFloatValue(NAMESPACE, "edgeClampRate", triangle.getEdgeClampRate()));
        }
        return triangle;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        invalidate();
    }

    public void setXDistribution(TriangleDrawable.PosXDistribution xDistribution) {
        triangleDrawable.setXDistribution(xDistribution);
    }
}
