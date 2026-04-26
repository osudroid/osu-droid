package com.edlplan.framework.support;

import android.opengl.GLES20;

import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.support.batch.object.TextureQuadBatch;
import com.edlplan.framework.support.graphics.BaseCanvas;
import com.edlplan.framework.support.graphics.BlendType;
import com.edlplan.framework.support.graphics.GLWrapped;
import com.edlplan.framework.support.graphics.SupportCanvas;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.Entity;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;
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

        // The storyboard rendering pipeline (TextureQuadBatch) calls raw GLES20 methods
        // that bypass AndEngine's GLState cache. Restore the state that AndEngine expects:
        //
        // 1. glUseProgram(0) was called directly → GLState still thinks old shader is bound.
        //    Tell GLState the current program is 0 so next useProgram(X) will re-bind properly.
        pGLState.useProgram(0);
        //
        // 2. glBindBuffer(GL_ARRAY_BUFFER, 0) was called directly → GLState still thinks the
        //    old buffer is bound, so the next bindArrayBuffer(X) call is skipped.
        //    Tell GLState the buffer is now 0 so it re-binds on the next draw.
        pGLState.bindArrayBuffer(0);
        //
        // 3. glDisableVertexAttribArray(0) and (1) were called. AndEngine expects
        //    ATTRIBUTE_POSITION (0) and ATTRIBUTE_COLOR (1) to always be enabled.
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION);
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);
    }

    public interface OnSupportDraw {

        void draw(BaseCanvas canvas);

    }
}
