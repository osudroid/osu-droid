package com.edlplan.framework.support.graphics;

import com.edlplan.framework.math.Mat4;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.utils.interfaces.Copyable;
import com.edlplan.framework.utils.interfaces.Recycleable;

public class CanvasData implements Recycleable, Copyable {

    private float width;

    private float height;

    private Camera camera;

    private float pixelDensity = 1;

    private float canvasAlpha = 1;

    private Vec2 theOrigin = new Vec2();

    public CanvasData(CanvasData c) {
        this.camera = c.camera.copy();
        this.width = c.width;
        this.height = c.height;
        this.pixelDensity = c.pixelDensity;
        this.canvasAlpha = c.canvasAlpha;
        this.theOrigin.set(c.theOrigin);
    }

    public CanvasData() {
        camera = new Camera();
    }

    public Vec2 getTheOrigin() {
        return theOrigin;
    }

    public float getCanvasAlpha() {
        return canvasAlpha;
    }

    public void setCanvasAlpha(float canvasAlpha) {
        this.canvasAlpha = canvasAlpha;
    }

    /**
     * 定义了canvas上每单位有多少像素
     */
    public float getPixelDensity() {
        return pixelDensity;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Mat4 getCurrentProjMatrix() {
        return camera.getProjectionMatrix();
    }

    public void setCurrentProjMatrix(Mat4 projMatrix) {
        this.camera.setProjectionMatrix(projMatrix);
        freshMatrix();
    }

    /**
     * 每次直接操作之后要freshMatrix，否则效果不会显示
     */
    public Mat4 getCurrentMaskMatrix() {
        return camera.getMaskMatrix();
    }

    public void setCurrentMaskMatrix(Mat4 matrix) {
        this.camera.setMaskMatrix(matrix);
    }

    public CanvasData translate(float tx, float ty) {
        getCurrentMaskMatrix().translate(tx, ty, 0);
        theOrigin.add(tx, ty);
        freshMatrix();
        return this;
    }

    public CanvasData rotate(float rotation) {
        getCurrentMaskMatrix().rotate2D(0, 0, rotation, true);
        freshMatrix();
        return this;
    }

    public CanvasData rotate(float ox, float oy, float rotation) {
        getCurrentMaskMatrix().rotate2D(ox, oy, rotation, true);
        freshMatrix();
        return this;
    }

    //可能导致部分运算误差（像素密度相关）
    public CanvasData scale(float sx, float sy) {
        getCurrentMaskMatrix().scale(sx, sy, 1);
        theOrigin.x *= sx;
        theOrigin.y *= sy;
        freshMatrix();
        return this;
    }

    /**
     * 对轴进行缩放，而不是对物件缩放，所以处理Matrix时用倒数
     */
    public CanvasData expendAxis(float s) {
        if (s == 0)
            throw new IllegalArgumentException("you can't scale content using a scale rate ==0");
        float rs = 1 / s;
        scale(rs, rs);
        this.pixelDensity *= s;
        return this;
    }

    public void freshMatrix() {
        camera.refresh();
    }

	/*
	public Mat4 getFinalMatrix(){
		return camera.getFinalMatrix();
	}
	*/

    public Camera getCamera() {
        return camera;
    }

    public CanvasData clip(float w, float h) {
        setWidth(w);
        setHeight(h);
        return this;
    }

    @Override
    public void recycle() {
        this.camera = null;
    }

    @Override
    public Copyable copy() {
        return new CanvasData(this);
    }
}
