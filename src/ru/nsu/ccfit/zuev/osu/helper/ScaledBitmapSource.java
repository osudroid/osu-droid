package ru.nsu.ccfit.zuev.osu.helper;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.source.BaseTextureAtlasSource;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ScaledBitmapSource extends BaseTextureAtlasSource implements
        IBitmapTextureAtlasSource {

    private final File mFile;
    private int mWidth;
    private int mHeight;
    private Bitmap bitmap = null;

    // ===========================================================
    // Constructors
    // ===========================================================

    public ScaledBitmapSource(final File pFile) {
        this(pFile, 0, 0);
    }

    public ScaledBitmapSource(final File pFile, final int pTexturePositionX,
                              final int pTexturePositionY) {
        super(pTexturePositionX, pTexturePositionY);
        this.mFile = pFile;

        final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inJustDecodeBounds = true;
        decodeOptions.inSampleSize = ru.nsu.ccfit.zuev.osu.Config
                .getBackgroundQuality();

        try (FileInputStream in = new FileInputStream(pFile)) {
            BitmapFactory.decodeStream(in, null, decodeOptions);
            this.mWidth = decodeOptions.outWidth;
            this.mHeight = decodeOptions.outHeight;
        } catch (final IOException e) {
            Debug.e("Failed loading Bitmap in FileBitmapTextureAtlasSource. File: "
                    + pFile, e);
            this.mWidth = 0;
            this.mHeight = 0;
        }
    }

    @NonNull
    @Override
    public ScaledBitmapSource clone() {
        try {
            return (ScaledBitmapSource) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================


    public int getWidth() {
        return this.mWidth;
    }


    public int getHeight() {
        return this.mHeight;
    }

    public boolean preload() {
        bitmap = onLoadBitmap(Bitmap.Config.ARGB_8888);
        return bitmap != null;
    }


    public Bitmap onLoadBitmap(final Config pBitmapConfig) {
        if (bitmap != null) {
            final Bitmap bmp = bitmap;
            bitmap = null;
            return bmp;
        }
        final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inPreferredConfig = pBitmapConfig;
        decodeOptions.inSampleSize = ru.nsu.ccfit.zuev.osu.Config
                .getBackgroundQuality();

        try (FileInputStream in = new FileInputStream(this.mFile)) {
            return BitmapFactory.decodeStream(in, null, decodeOptions);
        } catch (final IOException e) {
            Debug.e("Failed loading Bitmap in "
                            + this.getClass().getSimpleName() + ". File: " + this.mFile,
                    e);
            return null;
        }
    }


    @NonNull
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.mFile + ")";
    }


    public ScaledBitmapSource deepCopy() {
        return new ScaledBitmapSource(mFile, mTexturePositionX,
                mTexturePositionY);
    }

}
