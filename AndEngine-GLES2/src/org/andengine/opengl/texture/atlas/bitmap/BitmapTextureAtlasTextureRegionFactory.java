package org.andengine.opengl.texture.atlas.bitmap;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.andengine.graphics.atlas.TextureAtlasPages;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.source.AssetBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.bitmap.source.ResourceBitmapTextureAtlasSource;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.util.exception.AndEngineRuntimeException;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;


/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 18:15:14 - 09.03.2010
 */
public class BitmapTextureAtlasTextureRegionFactory {
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
			BitmapTextureAtlasTextureRegionFactory.sAssetBasePath = pAssetBasePath;
		} else {
			throw new IllegalArgumentException("pAssetBasePath must end with '/' or be lenght zero.");
		}
	}

	public static String getAssetBasePath() {
		return BitmapTextureAtlasTextureRegionFactory.sAssetBasePath;
	}

	public static void reset() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("");
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Methods using BitmapTexture
	// ===========================================================

	public static TextureRegion createFromAsset(final BitmapTextureAtlas pBitmapTextureAtlas, final Context pContext, final String pAssetPath, final int pTextureX, final int pTextureY) {
		return BitmapTextureAtlasTextureRegionFactory.createFromAsset(pBitmapTextureAtlas, pContext.getAssets(), pAssetPath, pTextureX, pTextureY);
	}

	public static TextureRegion createFromAsset(final BitmapTextureAtlas pBitmapTextureAtlas, final AssetManager pAssetManager, final String pAssetPath, final int pTextureX, final int pTextureY) {
		final IBitmapTextureAtlasSource bitmapTextureAtlasSource = AssetBitmapTextureAtlasSource.create(pAssetManager, BitmapTextureAtlasTextureRegionFactory.sAssetBasePath + pAssetPath);
		return BitmapTextureAtlasTextureRegionFactory.createFromSource(pBitmapTextureAtlas, bitmapTextureAtlasSource, pTextureX, pTextureY);
	}

	public static TiledTextureRegion createTiledFromAsset(final BitmapTextureAtlas pBitmapTextureAtlas, final Context pContext, final String pAssetPath, final int pTextureX, final int pTextureY, final int pTileColumns, final int pTileRows) {
		return BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(pBitmapTextureAtlas, pContext.getAssets(), pAssetPath, pTextureX, pTextureY, pTileColumns, pTileRows);
	}

	public static TiledTextureRegion createTiledFromAsset(final BitmapTextureAtlas pBitmapTextureAtlas, final AssetManager pAssetManager, final String pAssetPath, final int pTextureX, final int pTextureY, final int pTileColumns, final int pTileRows) {
		final IBitmapTextureAtlasSource bitmapTextureAtlasSource = AssetBitmapTextureAtlasSource.create(pAssetManager, BitmapTextureAtlasTextureRegionFactory.sAssetBasePath + pAssetPath);
		return BitmapTextureAtlasTextureRegionFactory.createTiledFromSource(pBitmapTextureAtlas, bitmapTextureAtlasSource, pTextureX, pTextureY, pTileColumns, pTileRows);
	}


	public static TextureRegion createFromResource(final BitmapTextureAtlas pBitmapTextureAtlas, final Context pContext, final int pDrawableResourceID, final int pTextureX, final int pTextureY) {
		return BitmapTextureAtlasTextureRegionFactory.createFromResource(pBitmapTextureAtlas, pContext.getResources(), pDrawableResourceID, pTextureX, pTextureY);
	}

	public static TextureRegion createFromResource(final BitmapTextureAtlas pBitmapTextureAtlas, final Resources pResources, final int pDrawableResourceID, final int pTextureX, final int pTextureY) {
		final IBitmapTextureAtlasSource bitmapTextureAtlasSource = ResourceBitmapTextureAtlasSource.create(pResources, pDrawableResourceID);
		return BitmapTextureAtlasTextureRegionFactory.createFromSource(pBitmapTextureAtlas, bitmapTextureAtlasSource, pTextureX, pTextureY);
	}

	public static TiledTextureRegion createTiledFromResource(final BitmapTextureAtlas pBitmapTextureAtlas, final Context pContext, final int pDrawableResourceID, final int pTextureX, final int pTextureY, final int pTileColumns, final int pTileRows) {
		return BitmapTextureAtlasTextureRegionFactory.createTiledFromResource(pBitmapTextureAtlas, pContext.getResources(), pDrawableResourceID, pTextureX, pTextureY, pTileColumns, pTileRows);
	}

	public static TiledTextureRegion createTiledFromResource(final BitmapTextureAtlas pBitmapTextureAtlas, final Resources pResources, final int pDrawableResourceID, final int pTextureX, final int pTextureY, final int pTileColumns, final int pTileRows) {
		final IBitmapTextureAtlasSource bitmapTextureAtlasSource = ResourceBitmapTextureAtlasSource.create(pResources, pDrawableResourceID);
		return BitmapTextureAtlasTextureRegionFactory.createTiledFromSource(pBitmapTextureAtlas, bitmapTextureAtlasSource, pTextureX, pTextureY, pTileColumns, pTileRows);
	}


	public static TextureRegion createFromSource(final BitmapTextureAtlas pBitmapTextureAtlas, final IBitmapTextureAtlasSource pBitmapTextureAtlasSource, final int pTextureX, final int pTextureY) {
		return TextureRegionFactory.createFromSource(pBitmapTextureAtlas, pBitmapTextureAtlasSource, pTextureX, pTextureY);
	}

	public static TiledTextureRegion createTiledFromSource(final BitmapTextureAtlas pBitmapTextureAtlas, final IBitmapTextureAtlasSource pBitmapTextureAtlasSource, final int pTextureX, final int pTextureY, final int pTileColumns, final int pTileRows) {
		return TextureRegionFactory.createTiledFromSource(pBitmapTextureAtlas, pBitmapTextureAtlasSource, pTextureX, pTextureY, pTileColumns, pTileRows);
	}

	// ===========================================================
	// Methods using TextureAtlasPages
	// ===========================================================

	/**
	 * Loads all files of an assets directory, in alphabetical order, as the consecutive tiles of one
	 * {@link TiledTextureRegion}, all on the same page of {@code pPages} (see {@link TextureAtlasPages#addTiled}).
	 * Must be called inside a session of {@code pPages}.
	 *
	 * @param pAssetSubdirectory to load all files from "gfx/flowers" put "flowers" here (assuming {@link #setAssetBasePath(String)} was set to "gfx/").
	 * @return the tiles, or <code>null</code> if the directory is empty or the tiles cannot be placed.
	 */
	public static TiledTextureRegion createTiledFromAssetDirectory(final TextureAtlasPages pPages, final AssetManager pAssetManager, final String pAssetSubdirectory, final TextureOptions pTextureOptions) {
		final String directory = BitmapTextureAtlasTextureRegionFactory.sAssetBasePath + pAssetSubdirectory;
		final String[] files;
		try {
			files = pAssetManager.list(directory);
		} catch (final IOException e) {
			throw new AndEngineRuntimeException("Listing assets subdirectory: '" + directory + "' failed. Does it exist?", e);
		}
		if (files == null || files.length == 0) {
			return null;
		}
		Arrays.sort(files);

		final List<IBitmapTextureAtlasSource> sources = new ArrayList<>(files.length);
		for (final String file : files) {
			sources.add(AssetBitmapTextureAtlasSource.create(pAssetManager, directory + "/" + file));
		}
		return pPages.addTiled(sources, pTextureOptions);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
