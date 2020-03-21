package com.edlplan.framework.support;

import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.support.graphics.BaseCanvas;
import com.edlplan.framework.support.graphics.BlendType;
import com.edlplan.framework.support.graphics.GLWrapped;
import com.edlplan.framework.support.graphics.SupportCanvas;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.opengl.util.GLHelper;

import javax.microedition.khronos.opengles.GL10;

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
    protected void doDraw(GL10 pGL, Camera pCamera) {
        SupportState.setUsingSupportCamera(true);
        BatchEngine.pGL = pGL;

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

        SupportState.setUsingSupportCamera(false);
        GLHelper.blendFunction(pGL, BlendType.Normal.srcTypePreM, BlendType.Normal.dstTypePreM);
    }

    public interface OnSupportDraw {

        void draw(BaseCanvas canvas);

    }
}
