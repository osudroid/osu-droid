package com.edlplan.framework.support.batch;

import com.edlplan.framework.support.graphics.Camera;
import com.edlplan.framework.support.graphics.ShaderGlobals;

import org.andengine.opengl.util.GLState;
import javax.microedition.khronos.opengles.GL10;

public class BatchEngine {

    public static GLState pGL;
    public static ShaderGlobals shaderGlobals = new ShaderGlobals();
    private static AbstractBatch<?> savedbatch = null;
    private static boolean flushing = false;

    public static ShaderGlobals getShaderGlobals() {
        return shaderGlobals;
    }

    /**
     * 设置全局透明度，
     *
     * @param alpha
     */
    public static void setGlobalAlpha(float alpha) {
        if (Math.abs(shaderGlobals.alpha - alpha) > 0.002f) {
            flush();
            shaderGlobals.alpha = alpha;
        }
    }

    /**
     * 更换全局相机，调用时必定触发flush
     *
     * @param camera
     */
    public static void setGlobalCamera(Camera camera) {
        flush();
        shaderGlobals.camera.set(camera);
    }

    static <T> void bind(AbstractBatch<T> batch) {
        flush();
        savedbatch = batch;
    }

    static <T> void unbind(AbstractBatch<T> batch) {
        if (savedbatch == batch) {
            savedbatch = null;
        }
    }

    public static void flush() {
        if (flushing) return;
        if (savedbatch != null) {
            flushing = true;
            savedbatch.flush();
            flushing = false;
        }
    }

    public static AbstractBatch<?> currentBatch() {
        return savedbatch;
    }
}
