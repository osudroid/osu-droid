package com.reco1l.framework.support;

import android.graphics.Bitmap;

import com.reco1l.tools.Logging;

import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.source.BaseTextureAtlasSource;

public class BitmapSourceAtlas extends BaseTextureAtlasSource implements IBitmapTextureAtlasSource {

    private Bitmap
            mRawBitmap,
            mBitmap;

    private int
            mWidth,
            mHeight;

    public BitmapSourceAtlas(Bitmap bitmap) {
        super(0, 0);

        mRawBitmap = bitmap;

        if (mRawBitmap != null) {
            mBitmap = Bitmap.createBitmap(mRawBitmap);

            mWidth = mRawBitmap.getWidth();
            mHeight = mRawBitmap.getHeight();
        }
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public IBitmapTextureAtlasSource deepCopy() {
        return new BitmapSourceAtlas(mBitmap);
    }

    @Override
    public Bitmap onLoadBitmap(Bitmap.Config pBitmapConfig) {
        if (mRawBitmap.isRecycled()) {
            Logging.i(this, "This source atlas is no more valid since raw bitmap was recycled!");
            return null;
        }
        return Bitmap.createBitmap(mRawBitmap).copy(pBitmapConfig, false);
    }

    public boolean isLoaded() {
        return mBitmap != null;
    }
}
