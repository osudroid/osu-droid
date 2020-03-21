package com.edlplan.framework.support.batch;

import com.edlplan.framework.support.SupportState;

import org.anddev.andengine.opengl.util.GLHelper;

import javax.microedition.khronos.opengles.GL10;

/**
 * 一个Batch表示缓存了一类的操作
 *
 * @param <T>
 */
public abstract class AbstractBatch<T> {

    protected abstract void onBind();

    protected abstract void onUnbind();

    public abstract void add(T t);

    protected abstract void clearData();

    protected abstract boolean applyToGL();

    public void addAll(T... ts) {
        for (T t : ts) {
            add(t);
        }
    }

    protected void checkForBind() {
        if (!isBind()) {
            bind();
        }
    }

    public final void bind() {
        BatchEngine.bind(this);
        onBind();
    }

    public final void unbind() {
        BatchEngine.unbind(this);
        onUnbind();
    }

    public final boolean isBind() {
        return BatchEngine.currentBatch() == this;
    }

    public final void flush() {
        GL10 pGL = BatchEngine.pGL;
        int type = GLHelper.getCurrentMatrixType();
        if (SupportState.isUsingSupportCamera()) {
            pGL.glMatrixMode(GL10.GL_PROJECTION);
            pGL.glPushMatrix();
            pGL.glMultMatrixf(BatchEngine.shaderGlobals.camera.getProjectionMatrix().data, 0);

            pGL.glMatrixMode(GL10.GL_MODELVIEW);
            pGL.glPushMatrix();
            pGL.glMultMatrixf(BatchEngine.shaderGlobals.camera.getMaskMatrix().data, 0);
        }
        if (applyToGL()) {
            clearData();
        }
        if (SupportState.isUsingSupportCamera()) {
            pGL.glMatrixMode(GL10.GL_PROJECTION);
            pGL.glPopMatrix();
            pGL.glMatrixMode(GL10.GL_MODELVIEW);
            pGL.glPopMatrix();
            pGL.glMatrixMode(type);
        }
    }
}
