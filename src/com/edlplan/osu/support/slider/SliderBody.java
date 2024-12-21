package com.edlplan.osu.support.slider;

import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.framework.math.line.LinePath;
import com.reco1l.andengine.DepthInfo;
import com.reco1l.andengine.shape.TriangleMesh;
import com.reco1l.andengine.container.Container;
import com.rian.osu.math.Vector2;


public class SliderBody extends Container {

    private static final BuildCache buildCache = new BuildCache();


    private LinePath path;

    private final TriangleMesh background;

    private final TriangleMesh border;

    private final TriangleMesh hint;

    private float backgroundWidth;

    private float borderWidth;

    private float hintWidth;

    private float startLength = 0;

    private float endLength = 0;

    private boolean shouldRebuildVertices = true;


    public SliderBody(boolean allowHint) {

        if (allowHint) {
            hint = new TriangleMesh();
            hint.setVisible(false);
            hint.setDepthInfo(DepthInfo.Clear);
        } else {
            hint = null;
        }

        border = new TriangleMesh();
        border.setDepthInfo(DepthInfo.Default);
        attachChild(border, 0);

        background = new TriangleMesh();
        background.setDepthInfo(DepthInfo.Default);
        attachChild(background, 0);

        if (hint != null) {
            attachChild(hint, 0);
        }

        setHintVisible(false);
    }


    public void init(LinePath path, boolean beginEmpty, Vector2 position) {

        reset();
        this.path = path;

        startLength = 0;

        if (beginEmpty) {
            endLength = 0;
        } else {
            endLength = path.getMeasurer().maxLength();
        }

        shouldRebuildVertices = true;
        setPosition(position.x, position.y);
    }


    public void setBackgroundWidth(float value) {
        backgroundWidth = value;
    }

    public void setBackgroundColor(float r, float g, float b, float a) {
        background.setColor(r, g, b, a);
    }


    public void setHintVisible(boolean visible) {
        if (hint != null) {
            hint.setVisible(visible);
            background.setDepthInfo(visible ? DepthInfo.Default : DepthInfo.Clear);
        } else {
            background.setDepthInfo(DepthInfo.Clear);
        }
    }

    public void setHintWidth(float value) {
        hintWidth = value;
    }

    public void setHintColor(float r, float g, float b, float a) {
        if (hint != null) {
            hint.setColor(r, g, b, a);
        }
    }


    public void setBorderWidth(float value) {
        borderWidth = value;
    }

    public void setBorderColor(float r, float g, float b) {
        border.setColor(r, g, b);
    }


    private void buildVertices(LinePath subPath) {

        TriangleBuilder builder = buildCache.triangleBuilder;

        if (hint != null && hint.isVisible()) {
            buildCache.drawLinePath
                    .reset(subPath, Math.min(hintWidth, backgroundWidth - borderWidth))
                    .computeTriangles(builder)
                    .applyVertices(hint.getVertices());

            hint.setContentSize(builder.maxX, builder.maxY);
        }

        buildCache.drawLinePath
                .reset(subPath, backgroundWidth - borderWidth)
                .computeTriangles(builder)
                .applyVertices(background.getVertices());

        background.setContentSize(builder.maxX, builder.maxY);

        buildCache.drawLinePath
                .reset(subPath, backgroundWidth)
                .computeTriangles(builder)
                .applyVertices(border.getVertices());

        border.setContentSize(builder.maxX, builder.maxY);
    }


    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {

        if (path != null && shouldRebuildVertices) {
            shouldRebuildVertices = false;

            buildVertices(path.cutPath(startLength, endLength).fitToLinePath(buildCache.path));
        }

        super.onManagedUpdate(pSecondsElapsed);
    }


    public void setStartLength(float length) {
        startLength = length;
        shouldRebuildVertices = true;
    }

    public void setEndLength(float length) {
        endLength = length;
        shouldRebuildVertices = true;
    }


    private static class BuildCache {
        public LinePath path = new LinePath();
        public TriangleBuilder triangleBuilder = new TriangleBuilder();
        public DrawLinePath drawLinePath = new DrawLinePath();
    }

}
