package org.anddev.andengine.opengl.font;

import java.util.function.Supplier;

import org.anddev.andengine.opengl.texture.ITexture;

import android.content.Context;
import android.graphics.Typeface;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 17:17:28 - 16.06.2010
 */
// osu!droid modified: Factory methods now take a Supplier<ITexture> pPageFactory in place of a
// single texture. Font uses it to create the first atlas page as well as any additional ones
// allocated on overflow.
public class FontFactory {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private static String sAssetBasePath = "";

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	/**
	 * @param pAssetBasePath must end with '<code>/</code>' or have <code>.length() == 0</code>.
	 */
	public static void setAssetBasePath(final String pAssetBasePath) {
		if(pAssetBasePath.endsWith("/") || pAssetBasePath.length() == 0) {
			FontFactory.sAssetBasePath = pAssetBasePath;
		} else {
			throw new IllegalStateException("pAssetBasePath must end with '/' or be lenght zero.");
		}
	}

	public static void reset() {
		FontFactory.setAssetBasePath("");
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public static Font create(final Supplier<ITexture> pPageFactory, final Typeface pTypeface, final float pSize, final boolean pAntiAlias, final int pColor) {
		return new Font(pPageFactory, pTypeface, pSize, pAntiAlias, pColor);
	}

	public static StrokeFont createStroke(final Supplier<ITexture> pPageFactory, final Typeface pTypeface, final float pSize, final boolean pAntiAlias, final int pColor, final float pStrokeWidth, final int pStrokeColor) {
		return new StrokeFont(pPageFactory, pTypeface, pSize, pAntiAlias, pColor, pStrokeWidth, pStrokeColor);
	}

	public static StrokeFont createStroke(final Supplier<ITexture> pPageFactory, final Typeface pTypeface, final float pSize, final boolean pAntiAlias, final int pColor, final float pStrokeWidth, final int pStrokeColor, final boolean pStrokeOnly) {
		return new StrokeFont(pPageFactory, pTypeface, pSize, pAntiAlias, pColor, pStrokeWidth, pStrokeColor, pStrokeOnly);
	}

	public static Font createFromAsset(final Supplier<ITexture> pPageFactory, final Context pContext, final String pAssetPath, final float pSize, final boolean pAntiAlias, final int pColor) {
		return new Font(pPageFactory, Typeface.createFromAsset(pContext.getAssets(), FontFactory.sAssetBasePath + pAssetPath), pSize, pAntiAlias, pColor);
	}

	public static StrokeFont createStrokeFromAsset(final Supplier<ITexture> pPageFactory, final Context pContext, final String pAssetPath, final float pSize, final boolean pAntiAlias, final int pColor, final float pStrokeWidth, final int pStrokeColor) {
		return new StrokeFont(pPageFactory, Typeface.createFromAsset(pContext.getAssets(), FontFactory.sAssetBasePath + pAssetPath), pSize, pAntiAlias, pColor, pStrokeWidth, pStrokeColor);
	}

	public static StrokeFont createStrokeFromAsset(final Supplier<ITexture> pPageFactory, final Context pContext, final String pAssetPath, final float pSize, final boolean pAntiAlias, final int pColor, final float pStrokeWidth, final int pStrokeColor, final boolean pStrokeOnly) {
		return new StrokeFont(pPageFactory, Typeface.createFromAsset(pContext.getAssets(), FontFactory.sAssetBasePath + pAssetPath), pSize, pAntiAlias, pColor, pStrokeWidth, pStrokeColor, pStrokeOnly);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
