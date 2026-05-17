package com.edlplan.framework.support.batch.object;

import org.andengine.opengl.texture.region.TextureRegion;

public abstract class ATextureQuad {

    public TextureRegion texture;

    public abstract void write(float[] ary, int offset);

}