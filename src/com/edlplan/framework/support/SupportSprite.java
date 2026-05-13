package com.edlplan.framework.support;

import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.support.batch.object.TextureQuadBatch;
import com.edlplan.framework.support.graphics.BaseCanvas;
import com.edlplan.framework.support.graphics.BlendType;
import com.edlplan.framework.support.graphics.GLWrapped;
import com.edlplan.framework.support.graphics.SupportCanvas;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.Entity;
import org.andengine.opengl.util.GLState;

public class SupportSprite extends Entity {

    private OnSupportDraw draw;

    private SupportCanvas canvas;

    private float width, height;

    public SupportSprite(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public OnSupportDraw getDraw() {
        return draw;
    }

    public void setDraw(OnSupportDraw draw) {
        this.draw = draw;
    }

    protected void onSupportDraw(BaseCanvas canvas) {
        if (draw != null) {
            draw.draw(canvas);
        }
    }

    @Override
    protected void draw(GLState pGLState, Camera pCamera) {
        // Capture AndEngine's current ortho MVP so TextureQuadBatch can use it as
        // the base projection (screen pixels → NDC) when rendering storyboard quads.
        System.arraycopy(pGLState.getModelViewProjectionGLMatrix(), 0,
                TextureQuadBatch.sBaseGLMatrix, 0, 16);

        // Inject GLState so TextureQuadBatch.applyToGL() can keep it in sync
        // (shader binding, buffer binding) instead of calling raw GLES20 methods.
        TextureQuadBatch.sGLState = pGLState;

        GLWrapped.blend.setBlendType(BlendType.Normal);
        GLWrapped.blend.apply();

        com.edlplan.framework.support.graphics.Camera camera = new com.edlplan.framework.support.graphics.Camera();
        BatchEngine.setGlobalCamera(camera);

        if (canvas == null) {
            canvas = new SupportCanvas(new SupportCanvas.SupportInfo() {{
                supportWidth = width;
                supportHeight = height;
            }});
        }
        canvas.prepare();
        int count = canvas.save();
        int count2 = canvas.getBlendSetting().save();

        onSupportDraw(canvas);

        canvas.getBlendSetting().restoreToCount(count2);
        canvas.restoreToCount(count);
        canvas.unprepare();

        // Clear the GLState reference — TextureQuadBatch.applyToGL() has already
        // performed all necessary GLState resync internally (program, array buffer,
        // and attrib arrays), so no further repair is needed here.
        TextureQuadBatch.sGLState = null;
    }

    public interface OnSupportDraw {

        void draw(BaseCanvas canvas);

    }
}
