package ru.nsu.ccfit.zuev.osu.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.osudroid.BuildSettings;
import com.reco1l.framework.Bitmaps;

import org.andengine.graphics.texture.Pixels;
import org.andengine.graphics.texture.StraightPixels;
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource;
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource;
import org.andengine.util.debug.Debug;
import org.andengine.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class QualityAssetBitmapSource extends BaseTextureAtlasSource implements
        IPixelTextureAtlasSource {

    private final int mWidth;
    private final int mHeight;

    private final String mAssetPath;
    private final Context mContext;

    private Bitmap bitmap = null;

    /**
     * Pixels decoded by {@link #preload(boolean)}, handed out once by the next {@link #onLoadPixels(boolean)}
     * of the same variant ({@link Pixels#getPremultiplied()}); a load of the other variant drops them.
     */
    private Pixels preloadedPixels = null;

    // ===========================================================
    // Constructors
    // ===========================================================

    public QualityAssetBitmapSource(final Context pContext,
                                    final String pAssetPath) {
        this(pContext, pAssetPath, 0, 0);
    }

    public QualityAssetBitmapSource(final Context pContext,
                                    final String pAssetPath, final int pTexturePositionX,
                                    final int pTexturePositionY) {
        super(pTexturePositionX, pTexturePositionY, 0, 0);
        this.mContext = pContext;
        this.mAssetPath = pAssetPath;

        final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inJustDecodeBounds = true;
        decodeOptions.inSampleSize = ru.nsu.ccfit.zuev.osu.Config
                .getTextureQuality();

        InputStream in = null;
        try {
            in = pContext.getAssets().open(pAssetPath);
            BitmapFactory.decodeStream(in, null, decodeOptions);
        } catch (final IOException e) {
            // Debug.e("Failed loading Bitmap in AssetBitmapTextureAtlasSource. AssetPath: " + pAssetPath, e);
        } finally {
            StreamUtils.close(in);
        }

        this.mWidth = decodeOptions.outWidth;
        this.mHeight = decodeOptions.outHeight;
        this.mTextureWidth = this.mWidth;
        this.mTextureHeight = this.mHeight;
    }

    QualityAssetBitmapSource(final Context pContext, final String pAssetPath,
                             final int pTexturePositionX, final int pTexturePositionY,
                             final int pWidth, final int pHeight) {
        super(pTexturePositionX, pTexturePositionY, pWidth, pHeight);
        this.mContext = pContext;
        this.mAssetPath = pAssetPath;
        this.mWidth = pWidth;
        this.mHeight = pHeight;
    }


    public QualityAssetBitmapSource deepCopy() {
        return new QualityAssetBitmapSource(this.mContext, this.mAssetPath,
                this.mTextureX, this.mTextureY, this.mWidth,
                this.mHeight);
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

    /**
     * Decodes the image ahead of the upload of a straight-alpha atlas, see {@link #preload(boolean)}.
     */
    public boolean preload() {
        return preload(false);
    }

    /**
     * Decodes the image ahead of the upload and reports whether it can be decoded. The next load
     * uses the result: as pixels of the given variant (for an atlas that premultiplies alpha if
     * {@code premultiplied}) when possible, otherwise as a bitmap.
     */
    public boolean preload(final boolean premultiplied) {
        if (!isNoTexturesMode()) {
            final Pixels pixels = decodePixels(premultiplied);
            if (pixels != null) {
                synchronized (this) {
                    preloadedPixels = pixels;
                }
                return true;
            }
        }
        final Bitmap bmp = onLoadBitmap(Bitmap.Config.ARGB_8888);
        synchronized (this) {
            bitmap = bmp;
        }
        return bmp != null;
    }

    /**
     * Whether textures are replaced by placeholders ({@link BuildSettings#NO_TEXTURES_MODE}), which
     * only the bitmap path paints.
     */
    protected boolean isNoTexturesMode() {
        return BuildSettings.NO_TEXTURES_MODE;
    }

    @Override
    public boolean canLoadPixels(final boolean premultiplied) {
        return true;
    }

    @Override
    public Pixels onLoadPixels() {
        return onLoadPixels(false);
    }

    @Override
    public Pixels onLoadPixels(final boolean premultiplied) {
        synchronized (this) {
            if (preloadedPixels != null) {
                final Pixels pixels = preloadedPixels;
                preloadedPixels = null;
                if (pixels.getPremultiplied() == premultiplied) {
                    return pixels;
                }
                // Preloaded for the other variant: dropped, this load decodes anew.
            }
            if (bitmap != null) {
                // Preloaded as a bitmap: the bitmap path uses it.
                return null;
            }
        }
        if (isNoTexturesMode()) {
            return null;
        }
        return decodePixels(premultiplied);
    }

    private Pixels decodePixels(final boolean premultiplied) {
        InputStream in = null;
        try {
            in = this.mContext.getAssets().open(this.mAssetPath);
            return StraightPixels.decode(in, ru.nsu.ccfit.zuev.osu.Config.getTextureQuality(), premultiplied);
        } catch (final IOException e) {
            Debug.e("Failed loading pixels in "
                    + this.getClass().getSimpleName() + ". AssetPath: " + this.mAssetPath, e);
            return null;
        } finally {
            StreamUtils.close(in);
        }
    }


    public Bitmap onLoadBitmap(final Config pBitmapConfig) {
        synchronized (this) {
            // Pixels preloaded for the pixel path are of no use here.
            preloadedPixels = null;
            if (bitmap != null) {
                final Bitmap bmp = bitmap;
                bitmap = null;
                return bmp;
            }
        }
        InputStream in = null;
        try {
            final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inPreferredConfig = pBitmapConfig;
            decodeOptions.inSampleSize = ru.nsu.ccfit.zuev.osu.Config.getTextureQuality();
            decodeOptions.inMutable = isNoTexturesMode();

            in = this.mContext.getAssets().open(this.mAssetPath);
            var bitmap = BitmapFactory.decodeStream(in, null, decodeOptions);

            if (isNoTexturesMode()) {
                bitmap = Bitmaps.paintBitmap(bitmap);
            }

            return bitmap;
        } catch (final IOException e) {
            Debug.e("Failed loading Bitmap in "
                    + this.getClass().getSimpleName() + ". AssetPath: "
                    + this.mAssetPath, e);
            return null;
        } finally {
            StreamUtils.close(in);
        }
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.mAssetPath + ")";
    }

}
