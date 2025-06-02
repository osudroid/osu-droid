package com.edlplan.osu.support.slider;

import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.framework.math.line.LinePath;
import com.reco1l.andengine.component.ClearInfo;
import com.reco1l.andengine.component.DepthInfo;
import com.reco1l.andengine.shape.UITriangleMesh;
import com.reco1l.andengine.container.UIContainer;
import com.reco1l.framework.ColorARGB;
import com.rian.osu.math.Vector2;


public class SliderBody extends UIContainer {

    private static final BuildCache buildCache = new BuildCache();


    private LinePath path;

    private final UITriangleMesh background;

    private final UITriangleMesh border;

    private final UITriangleMesh hint;

    private float backgroundWidth;

    private float borderWidth;

    private float hintWidth;

    private float startLength = 0;

    private float endLength = 0;

    private boolean shouldRebuildVertices = true;


    public SliderBody(boolean allowHint) {

        if (allowHint) {
            hint = new UITriangleMesh();
            hint.setVisible(false);
            hint.setClearInfo(ClearInfo.ClearDepthBuffer);
            hint.setDepthInfo(DepthInfo.Less);
        } else {
            hint = null;
        }

        border = new UITriangleMesh();
        border.setDepthInfo(DepthInfo.Default);
        attachChild(border, 0);

        background = new UITriangleMesh();
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

    public void setBackgroundColor(ColorARGB color, float alpha) {
        background.setColor(color);
        background.setAlpha(alpha);
    }


    public void setHintVisible(boolean visible) {
        if (hint != null) {
            hint.setVisible(visible);
            background.setClearInfo(visible ? ClearInfo.None : ClearInfo.ClearDepthBuffer);
            background.setDepthInfo(visible ? DepthInfo.Default : DepthInfo.Less);
        } else {
            background.setClearInfo(ClearInfo.ClearDepthBuffer);
            background.setDepthInfo(DepthInfo.Less);
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

    public void setHintColor(ColorARGB color, float alpha) {
        if (hint != null) {
            hint.setColor(color);
            hint.setAlpha(alpha);
        }
    }


    public void setBorderWidth(float value) {
        borderWidth = value;
    }

    public void setBorderColor(ColorARGB color) {
        border.setColor(color);
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
    protected void onManagedUpdate(float deltaTimeSec) {

        if (path != null && shouldRebuildVertices) {
            shouldRebuildVertices = false;

            buildVertices(path.cutPath(startLength, endLength).fitToLinePath(buildCache.path));
        }

        super.onManagedUpdate(deltaTimeSec);
    }


    public void setStartLength(float length) {
        if (length == startLength) {
            return;
        }

        startLength = length;
        shouldRebuildVertices = true;
    }

    public void setEndLength(float length) {
        if (length == endLength) {
            return;
        }

        endLength = length;
        shouldRebuildVertices = true;
    }


    private static class BuildCache {
        public LinePath path = new LinePath();
        public TriangleBuilder triangleBuilder = new TriangleBuilder();
        public DrawLinePath drawLinePath = new DrawLinePath();
    }

}
