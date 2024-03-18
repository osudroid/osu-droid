package org.andengine.entity.sprite.vbo;

import org.andengine.entity.sprite.ISprite;
import org.andengine.opengl.vbo.IVertexBufferObject;

/**
 * (c) Zynga 2012
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 18:40:47 - 28.03.2012
 */
// osu!droid modified - Changed Sprite references to ISprite, following changes specified in ISprite.
public interface ISpriteVertexBufferObject extends IVertexBufferObject {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void onUpdateColor(final ISprite pSprite);
	public void onUpdateVertices(final ISprite pSprite);
	public void onUpdateTextureCoordinates(final ISprite pSprite);
}