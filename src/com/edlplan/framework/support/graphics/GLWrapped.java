package com.edlplan.framework.support.graphics;

import android.opengl.GLES10;
import android.util.Log;

import com.edlplan.framework.math.Color4;
import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.utils.advance.BooleanSetting;

import java.nio.Buffer;
import java.util.Stack;

public class GLWrapped {

    public static final
    BooleanSetting depthTest = new BooleanSetting(t -> {
        BatchEngine.flush();
        if (t) {
            GLES10.glEnable(GLES10.GL_DEPTH_TEST);
        } else {
            GLES10.glDisable(GLES10.GL_DEPTH_TEST);
        }
    },
            false).initial();
    public static int GL_SHORT = GLES10.GL_SHORT;
    public static int GL_UNSIGNED_SHORT = GLES10.GL_UNSIGNED_SHORT;
    public static int GL_TRIANGLES = GLES10.GL_TRIANGLES;
    public static int GL_MAX_TEXTURE_SIZE;
    public static BlendSetting blend = new BlendSetting().setUp();
    private static boolean enable = true;
    private static int drawCalls = 0;
    private static int fboCreate = 0;
    private static int px1, pw, py1, ph;
    private static Stack<BaseCanvas> canvasStack = new Stack<>();

    public static boolean isEnable() {
        return enable;
    }

    public static void setEnable(boolean enable) {
        GLWrapped.enable = enable;
    }

    public static void onFrame() {
        drawCalls = 0;
        fboCreate = 0;
    }

    public static void drawArrays(int mode, int offset, int count) {
        if (enable) GLES10.glDrawArrays(mode, offset, count);
        drawCalls++;
    }

    public static void drawElements(int mode, int count, int type, Buffer b) {
        if (enable) GLES10.glDrawElements(mode, count, type, b);
        drawCalls++;
    }

    public static int frameDrawCalls() {
        return drawCalls;
    }

    public static void setViewport(int x1, int y1, int w, int h) {
        GLES10.glViewport(x1, y1, w, h);
        px1 = x1;
        pw = w;
        py1 = y1;
        ph = h;
    }

    public static void setClearColor(float r, float g, float b, float a) {
        GLES10.glClearColor(r, g, b, a);
    }

    public static void clearColorBuffer() {
        if (enable) GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT);
    }

    public static void clearDepthBuffer() {
        if (enable) GLES10.glClear(GLES10.GL_DEPTH_BUFFER_BIT);
    }

    public static void clearDepthAndColorBuffer() {
        if (enable) GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT | GLES10.GL_DEPTH_BUFFER_BIT);
    }

    public static void setClearColor(Color4 c) {
        setClearColor(c.r, c.g, c.b, c.a);
    }

    public static int getFboCreate() {
        return fboCreate;
    }

    public static int getIntegerValue(int key) {
        int[] b = new int[1];
        GLES10.glGetIntegerv(key, b, 0);
        return b[0];
    }

    public static void checkGlError(String op) {
        int error;
        while ((error = GLES10.glGetError()) != GLES10.GL_NO_ERROR) {
            Log.e("ES20_ERROR", op + ": glError " + error);
            throw new GLException(op + ": glError " + error);
        }
    }

    protected static void prepareCanvas(BaseCanvas canvas) {
        if (!canvasStack.empty()) {
            final BaseCanvas pre = canvasStack.peek();
            if (pre.isPrepared()) {
                //忘记释放，这里就帮忙释放
                pre.onUnprepare();
            }
        }
        BatchEngine.flush();
        canvasStack.push(canvas);
        canvas.onPrepare();
        BatchEngine.setGlobalCamera(canvas.getCamera());
    }

    protected static void unprepareCanvas(BaseCanvas canvas) {
        if (canvasStack.empty() || canvasStack.peek() != canvas) {
            //发生错误，释放的画板不是当前画板
            throw new GLException("错误的canvas释放顺序！");
        }
        BatchEngine.flush();
        canvas.onUnprepare();
        canvasStack.pop();
        if (!canvasStack.empty()) {
            canvasStack.peek().onPrepare();
        }
    }

    public static BaseCanvas getUsingCanvas() {
        return canvasStack.empty() ? null : canvasStack.peek();
    }
}