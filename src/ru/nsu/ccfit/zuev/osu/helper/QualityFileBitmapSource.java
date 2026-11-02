package ru.nsu.ccfit.zuev.osu.helper;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.osudroid.BuildSettings;
import com.reco1l.framework.Bitmaps;

import org.andengine.graphics.texture.Pixels;
import org.andengine.graphics.texture.StraightPixels;
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource;
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource;

import org.andengine.util.StreamUtils;
import org.andengine.util.debug.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class QualityFileBitmapSource extends BaseTextureAtlasSource implements
        IPixelTextureAtlasSource {

    private int mWidth;
    private int mHeight;
    private Bitmap bitmap = null;

    /**
     * Pixels decoded by {@link #preload(boolean)}, handed out once by the next {@link #onLoadPixels(boolean)}
     * of the same variant ({@link Pixels#getPremultiplied()}); a load of the other variant drops them.
     */
    private Pixels preloadedPixels = null;

    private InputFactory fileBitmapInput;

    private int inSampleSize = 1;

    public QualityFileBitmapSource(final File pFile) {
        this(pFile, 0, 0);
    }

    public QualityFileBitmapSource(final InputFactory pFile) {
        this(pFile, 0, 0, 1);
    }

    public QualityFileBitmapSource(final File pFile, int inSampleSize) {
        this(() -> new FileInputStream(pFile), 0, 0, inSampleSize);
    }

    public QualityFileBitmapSource(final File pFile,
                                   final int pTexturePositionX, final int pTexturePositionY) {
        this(() -> new FileInputStream(pFile), pTexturePositionX, pTexturePositionY, 1);
    }

    public QualityFileBitmapSource(final InputFactory pFile,
                                   final int pTexturePositionX, final int pTexturePositionY, int inSampleSize) {
        super(pTexturePositionX, pTexturePositionY, 0, 0);

        fileBitmapInput = pFile;
        this.inSampleSize = inSampleSize;

        final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inJustDecodeBounds = true;
        decodeOptions.inSampleSize = inSampleSize;

        InputStream in = null;
        try {
            in = openInputStream();
            BitmapFactory.decodeStream(in, null, decodeOptions);

            this.mWidth = decodeOptions.outWidth;
            this.mHeight = decodeOptions.outHeight;
        } catch (final IOException e) {
            Debug.e("Failed loading Bitmap in FileBitmapTextureAtlasSource. File: "
                    + pFile, e);
            this.mWidth = 0;
            this.mHeight = 0;
        } finally {
            StreamUtils.close(in);
        }
        this.mTextureWidth = this.mWidth;
        this.mTextureHeight = this.mHeight;

    }

    QualityFileBitmapSource(final InputFactory pFile, final int pTexturePositionX,
                            final int pTexturePositionY, final int pWidth, final int pHeight) {
        super(pTexturePositionX, pTexturePositionY, pWidth, pHeight);
        fileBitmapInput = pFile;
        this.mWidth = pWidth;
        this.mHeight = pHeight;
    }

    public InputStream openInputStream() throws IOException {
        return fileBitmapInput.openInput();
    }


    public QualityFileBitmapSource deepCopy() {
        QualityFileBitmapSource source = new QualityFileBitmapSource(this.fileBitmapInput, this.mTextureX,
                this.mTextureY, this.mWidth, this.mHeight);
        source.inSampleSize = inSampleSize;
        return source;
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
            in = openInputStream();
            return StraightPixels.decode(in, inSampleSize, premultiplied);
        } catch (final IOException e) {
            Debug.e("Failed loading pixels in "
                    + this.getClass().getSimpleName() + ". File: " + this.fileBitmapInput, e);
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
        final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inPreferredConfig = Config.ARGB_8888;//pBitmapConfig;
        decodeOptions.inSampleSize = inSampleSize;
        decodeOptions.inMutable = isNoTexturesMode();

        InputStream in = null;
        try {
            in = openInputStream();
            var bitmap = BitmapFactory.decodeStream(in, null, decodeOptions);

            if (isNoTexturesMode()) {
                bitmap = Bitmaps.paintBitmap(bitmap);
            }

            return bitmap;
        } catch (final IOException e) {
            Debug.e("Failed loading Bitmap in "
                            + this.getClass().getSimpleName() + ". File: " + this.fileBitmapInput,
                    e);
            return null;
        } finally {
            StreamUtils.close(in);
        }
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.fileBitmapInput + ")";
    }


    public interface InputFactory {
        InputStream openInput() throws IOException;
    }

}
