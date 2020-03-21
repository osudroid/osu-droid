package com.edlplan.osu.support.slider;

import com.edlplan.andengine.Triangle3DPack;
import com.edlplan.framework.math.line.LinePath;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.RGBColor;

public class SliderBody3D extends AbstractSliderBody {

    private static float zOff = 0.001f;

    private static float zStart = -1 + zOff;

    private static float zEnd = 1;

    private Triangle3DPack body = null, border = null, bodyMask = null, borderMask = null;

    private RGBColor bodyColor = new RGBColor(), borderColor = new RGBColor();

    private float bodyWidth, borderWidth;

    private float startLength = 0, endLength = 0;

    public SliderBody3D(LinePath path) {
        super(path);
    }

    @Override
    public void onUpdate() {
        LinePath sub = path.cutPath(startLength, endLength).fitToLinePath();

        float zBody = -bodyWidth / borderWidth + zOff;

        float alpha = endLength / path.getMeasurer().maxLength();

        /*bodyMask.setVertices(
                (new Draw3DLinePath(sub, bodyWidth, zEnd - zOff, zBody - zOff))
                        .getTriangles()
                        .getVertex());*/

        body.setVertices(
                (new Draw3DLinePath(sub, bodyWidth, 1, 1))
                        .getTriangles()
                        .getVertex());

        body.setAlpha(0.7f * alpha);

        /*borderMask.setVertices(
                (new Draw3DLinePath(sub, borderWidth, zEnd - zOff, zStart - zOff))
                        .getTriangles()
                        .getVertex());*/

        border.setVertices(
                (new Draw3DLinePath(sub, borderWidth, -1, -1))
                        .getTriangles()
                        .getVertex());

        border.setAlpha(alpha);
    }

    @Override
    public void setBodyWidth(float width) {
        bodyWidth = width;
    }

    @Override
    public void setBorderWidth(float width) {
        borderWidth = width;
    }

    @Override
    public void setBodyColor(float r, float g, float b) {
        bodyColor.set(r, g, b);
        if (body != null) {
            body.setColor(r, g, b);
        }
    }

    @Override
    public void setBorderColor(float r, float g, float b) {
        borderColor.set(r, g, b);
        if (border != null) {
            border.setColor(r, g, b);
        }
    }

    @Override
    public void setStartLength(float length) {
        startLength = length;
    }

    @Override
    public void setEndLength(float length) {
        endLength = length;
    }

    @Override
    public void applyToScene(Scene scene, boolean emptyOnStart) {

        if (!emptyOnStart) {
            startLength = 0;
            endLength = path.getMeasurer().maxLength();
        }

        float zBody = -bodyWidth / borderWidth + zOff;

        /*bodyMask = new Triangle3DPack(0, 0,
                emptyOnStart ?
                        new float[0] :
                        (new Draw3DLinePath(path, bodyWidth, zEnd - zOff, zBody - zOff))
                                .getTriangles()
                                .getVertex());
        bodyMask.setClearDepthOnStart(true);*/

        body = new Triangle3DPack(0, 0,
                emptyOnStart ?
                        new float[0] :
                        (new Draw3DLinePath(path, bodyWidth, zEnd, zBody))
                                .getTriangles()
                                .getVertex()
        );

        body.setClearDepthOnStart(true);

        /*borderMask = new Triangle3DPack(0, 0,
                emptyOnStart ?
                        new float[0] :
                        (new Draw3DLinePath(path, borderWidth, zEnd - zOff, zStart - zOff))
                                .getTriangles()
                                .getVertex()
        );*/

        border = new Triangle3DPack(0, 0,
                emptyOnStart ?
                        new float[0] :
                        (new Draw3DLinePath(path, borderWidth, zEnd, zStart))
                                .getTriangles()
                                .getVertex()
        );

        //bodyMask.setAlpha(0);
        //borderMask.setAlpha(0);
        body.setColor(bodyColor.r(), bodyColor.g(), bodyColor.b());
        border.setColor(borderColor.r(), borderColor.g(), borderColor.b());


        scene.attachChild(border, 0);
        //scene.attachChild(borderMask, 0);
        scene.attachChild(body, 0);
        //scene.attachChild(bodyMask, 0);

    }

    @Override
    public void removeFromScene(Scene scene) {
        if (body != null) {
            body.detachSelf();
        }
        if (border != null) {
            border.detachSelf();
        }
        if (bodyMask != null) {
            bodyMask.detachSelf();
        }
        if (borderMask != null) {
            borderMask.detachSelf();
        }
    }
}
