package org.anddev.andengine.opengl.texture.buffer;

import org.anddev.andengine.opengl.buffer.BufferObject;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.font.Letter;
import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.vertex.TextVertexBuffer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 11:05:56 - 03.04.2010
 */
// osu!droid modified: Track which atlas page each character's glyph came from, since a Font may now span multiple pages.
// This is exposed as compressed PageRuns for Text to issue one draw call per contiguous run of same-page characters.
public class TextTextureBuffer extends BufferObject {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final List<PageRun> mPageRuns = new ArrayList<>();

	// ===========================================================
	// Constructors
	// ===========================================================

	public TextTextureBuffer(final int pCapacity, final int pDrawType, final boolean pManaged) {
		super(pCapacity, pDrawType, pManaged);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public synchronized List<PageRun> getPageRuns() {
		return this.mPageRuns;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public synchronized void update(final Font pFont, final String[] pLines) {
		final FloatBuffer textureFloatBuffer = this.mFloatBuffer;
		textureFloatBuffer.position(0);

        this.mPageRuns.clear();

		final Font font = pFont;
		final String[] lines = pLines;

		final int lineCount = lines.length;
		for (int i = 0; i < lineCount; i++) {
			final String line = lines[i];

			final int lineLength = line.length();
			for (int j = 0; j < lineLength; j++) {
				final Letter letter = font.getLetter(line.charAt(j));

				final float letterTextureX = letter.mTextureX;
				final float letterTextureY = letter.mTextureY;
				final float letterTextureX2 = letterTextureX + letter.mTextureWidth;
				final float letterTextureY2 = letterTextureY + letter.mTextureHeight;

				textureFloatBuffer.put(letterTextureX);
				textureFloatBuffer.put(letterTextureY);

				textureFloatBuffer.put(letterTextureX);
				textureFloatBuffer.put(letterTextureY2);

				textureFloatBuffer.put(letterTextureX2);
				textureFloatBuffer.put(letterTextureY2);

				textureFloatBuffer.put(letterTextureX2);
				textureFloatBuffer.put(letterTextureY2);

				textureFloatBuffer.put(letterTextureX2);
				textureFloatBuffer.put(letterTextureY);

				textureFloatBuffer.put(letterTextureX);
				textureFloatBuffer.put(letterTextureY);

				this.appendToPageRuns(letter.getTexture());
			}
		}
		textureFloatBuffer.position(0);

		this.setHardwareBufferNeedsUpdate();
	}

	private void appendToPageRuns(final ITexture pTexture) {
		final List<PageRun> pPageRuns = this.mPageRuns;
		final int vertexCount = TextVertexBuffer.VERTICES_PER_CHARACTER;

		if (!pPageRuns.isEmpty()) {
			final PageRun lastRun = pPageRuns.get(pPageRuns.size() - 1);

			if (lastRun.texture == pTexture) {
				lastRun.vertexCount += vertexCount;
				return;
			}
		}

		final int startVertex = pPageRuns.isEmpty() ? 0 : pPageRuns.get(pPageRuns.size() - 1).startVertex + pPageRuns.get(pPageRuns.size() - 1).vertexCount;
		pPageRuns.add(new PageRun(pTexture, startVertex, vertexCount));
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * A contiguous run of vertices (in {@link TextVertexBuffer}/{@link TextTextureBuffer} order) that all sample the
	 * same atlas page.
	 */
	public static final class PageRun {
		public final ITexture texture;
		public final int startVertex;
		public int vertexCount;

		PageRun(final ITexture pTexture, final int pStartVertex, final int pVertexCount) {
			this.texture = pTexture;
			this.startVertex = pStartVertex;
			this.vertexCount = pVertexCount;
		}
	}
}
