package org.andengine.entity.sprite;

import org.andengine.entity.shape.IAreaShape;
import org.andengine.opengl.texture.region.ITextureRegion;

// osu!droid modified - Extracted methods used in HighPerformanceSpriteVertexBufferObject to allow
// other types of sprites to be used with the same VBO code.
public interface ISprite extends IAreaShape {

    ITextureRegion getTextureRegion();

    boolean isFlippedHorizontal();

    boolean isFlippedVertical();

}
