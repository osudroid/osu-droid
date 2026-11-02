package org.andengine.opengl.view;

import org.andengine.graphics.DrawContext;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 11:57:29 - 08.03.2010
 */
public interface IRendererListener {
	// ===========================================================
	// Constants
	// ===========================================================

	public void onSurfaceCreated(final DrawContext pDrawContext);
	public void onSurfaceChanged(final DrawContext pDrawContext, final int pWidth, final int pHeight);

	// ===========================================================
	// Methods
	// ===========================================================
}