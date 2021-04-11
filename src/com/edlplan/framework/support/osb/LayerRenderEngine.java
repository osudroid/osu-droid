package com.edlplan.framework.support.osb;

import com.edlplan.edlosbsupport.elements.StoryboardSprite;

public class LayerRenderEngine extends DepthOrderRenderEngine {

    private com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer layer;

    public LayerRenderEngine(com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer layer) {
        this.layer = layer;
    }

    public StoryboardSprite.Layer getLayer() {
        return layer;
    }

    public void setLayer(StoryboardSprite.Layer layer) {
        this.layer = layer;
    }
}
