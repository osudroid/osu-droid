package org.andengine.opengl.vbo.attribute;

/**
 * osu!droid: This class was a workaround for Android issue 8931 on Froyo (API ≤ 8), where
 * glEnableVertexAttribArray crashed inside glVertexAttribPointer. minSdk is 24, so
 * WORAROUND_GLES2_GLVERTEXATTRIBPOINTER_MISSING in VertexBufferObjectAttributesBuilder was always
 * false and this subclass was never instantiated. Tombstoned — do not use.
 *
 * @deprecated Dead code — Android issue 8931 / Froyo workaround. Use {@link VertexBufferObjectAttribute}.
 */
@Deprecated
public class VertexBufferObjectAttributeFix extends VertexBufferObjectAttribute {

	public VertexBufferObjectAttributeFix(final int pLocation, final String pName, final int pSize, final int pType, final boolean pNormalized, final int pOffset) {
		super(pLocation, pName, pSize, pType, pNormalized, pOffset);
	}
}