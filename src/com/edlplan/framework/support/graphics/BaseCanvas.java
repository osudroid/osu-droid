package com.edlplan.framework.support.graphics;

import com.edlplan.framework.math.Color4;
import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.utils.AbstractSRable;


/**
 * 事实证明，既然自己是辣鸡就别写Canvas这种重量级东西了x
 */
public abstract class BaseCanvas extends AbstractSRable<CanvasData> {

    public BaseCanvas() {

    }

    public BaseCanvas translate(float tx, float ty) {
        getData().translate(tx, ty);
        BatchEngine.setGlobalCamera(getData().getCamera());
        return this;
    }

    public BaseCanvas rotate(float r) {
        getData().rotate(r);
        BatchEngine.setGlobalCamera(getData().getCamera());
        return this;
    }

    public BaseCanvas rotate(float ox, float oy, float r) {
        getData().rotate(ox, oy, r);
        BatchEngine.setGlobalCamera(getData().getCamera());
        return this;
    }

    /**
     * 对画布对应区域缩放<br>
     */
    public BaseCanvas scale(float x, float y) {
        getData().scale(x, y);
        BatchEngine.setGlobalCamera(getData().getCamera());
        return this;
    }

    /**
     * 拉伸轴，对应的实际像素数不会改变
     * 比如当前的Canvas是100x100大小对应100x100的像素，<br>
     * expendAxis(2)后就变成了200x200的大小对应100x100的像素
     */
    public BaseCanvas expendAxis(float s) {
        getData().expendAxis(s);
        BatchEngine.setGlobalCamera(getData().getCamera());
        return this;
    }

    /**
     * 限制Canvas范围，但是并没有限制外部像素绘制
     */
    public BaseCanvas clip(float w, float h) {
        getData().clip(w, h);
        BatchEngine.setGlobalCamera(getData().getCamera());
        return this;
    }

    public float getPixelDensity() {
        return getData().getPixelDensity();
    }

    public float getWidth() {
        return getData().getWidth();
    }

    public float getHeight() {
        return getData().getHeight();
    }

    public Camera getCamera() {
        return getData().getCamera();
    }

    public float getCanvasAlpha() {
        return getData().getCanvasAlpha();
    }

    public void setCanvasAlpha(float a) {
        if (Math.abs(a - getData().getCanvasAlpha()) > 0.0001) {
            BatchEngine.flush();
        }
        getData().setCanvasAlpha(a);
        BatchEngine.getShaderGlobals().alpha = a;
    }

    /**
     * @param p:当前应该是的状态
     */
    public void checkPrepared(String msg, boolean p) {
        if (p != isPrepared()) {
            throw new GLException("prepare err [n,c]=[" + p + "," + isPrepared() + "] msg: " + msg);
        }
    }

    public boolean isPrepared() {
        return this == GLWrapped.getUsingCanvas();
    }

    public final void prepare() {
        GLWrapped.prepareCanvas(this);
    }

    protected abstract void onPrepare();

    public void unprepare() {
        flush();
        GLWrapped.unprepareCanvas(this);
    }

    protected abstract void onUnprepare();

    @Override
    public void onSave(CanvasData t) {
        BatchEngine.flush();
    }

    @Override
    public void onRestore(CanvasData now, CanvasData pre) {
        BatchEngine.setGlobalCamera(now.getCamera());
        BatchEngine.getShaderGlobals().alpha = now.getCanvasAlpha();
        pre.recycle();
    }


    /**
     * @return 返回是否支持裁剪画板的一部分
     */
    public boolean supportClip() {
        return false;
    }

    /**
     * 返回一个被裁剪的画板，
     *
     * @param x      裁剪区域起始x
     * @param y      裁剪区域起始y
     * @param width  裁剪区域宽度
     * @param height 裁剪区域高度
     * @return 返回一个新画板，画板的新原点为裁剪起点（会产生新对象）
     */
    protected BaseCanvas clipCanvas(float x, float y, float width, float height) {
        return null;
    }


    public final BaseCanvas requestClipCanvas(float x, float y, float width, float height) {
        checkPrepared("you can only clip canvas when it is not working", false);
        if (supportClip()) {
            return clipCanvas(x, y, width, height);
        } else {
            return null;
        }
    }


    public abstract BlendSetting getBlendSetting();

    protected abstract void checkCanDraw();

    public abstract CanvasData getDefData();

    public abstract void clearBuffer();

    public abstract void clearColor(Color4 c);

    public void flush() {
        BatchEngine.flush();
    }

    @Override
    public void recycle() {

    }

    @Override
    protected void finalize() throws Throwable {

        super.finalize();
    }
}

