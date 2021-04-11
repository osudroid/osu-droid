package com.edlplan.framework.support;

import com.edlplan.framework.support.graphics.BaseCanvas;

public class ProxySprite extends SupportSprite {
    DrawProxy drawProxy;

    public ProxySprite(float width, float height) {
        super(width, height);
    }

    public void setDrawProxy(DrawProxy drawProxy) {
        this.drawProxy = drawProxy;
    }

    public DrawProxy getDrawProxy() {
        return drawProxy;
    }

    @Override
    protected void onSupportDraw(BaseCanvas canvas) {
        super.onSupportDraw(canvas);
        if (drawProxy != null) {
            drawProxy.onSupportDraw(canvas);
        }
    }

    public interface DrawProxy {
        void onSupportDraw(BaseCanvas canvas);
    }

}
