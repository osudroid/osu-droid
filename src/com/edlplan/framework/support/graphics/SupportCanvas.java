package com.edlplan.framework.support.graphics;

import com.edlplan.framework.math.Color4;
import com.edlplan.framework.math.Mat4;

public class SupportCanvas extends BaseCanvas {

    private SupportInfo supportInfo;

    public SupportCanvas(SupportInfo info) {
        this.supportInfo = info;
        initial();
    }

    @Override
    protected void onPrepare() {

    }

    @Override
    protected void onUnprepare() {

    }

    @Override
    public BlendSetting getBlendSetting() {
        return GLWrapped.blend;
    }

    @Override
    protected void checkCanDraw() {

    }

    @Override
    public CanvasData getDefData() {
        CanvasData d = new CanvasData();
        d.setCurrentProjMatrix(Mat4.createIdentity());
        d.setCurrentMaskMatrix(Mat4.createIdentity());
        d.setHeight(supportInfo.supportHeight);
        d.setWidth(supportInfo.supportWidth);
        return d;
    }

    @Override
    public void clearBuffer() {
        GLWrapped.clearDepthAndColorBuffer();
    }

    @Override
    public void clearColor(Color4 c) {
        GLWrapped.setClearColor(c);
        GLWrapped.clearColorBuffer();
    }


    public static class SupportInfo {

        public float supportWidth;

        public float supportHeight;

    }

}
