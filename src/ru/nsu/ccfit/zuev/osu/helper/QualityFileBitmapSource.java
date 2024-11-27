package ru.nsu.ccfit.zuev.osu.helper;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.reco1l.BuildUtils;
import com.reco1l.framework.Bitmaps;

import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.source.BaseTextureAtlasSource;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class QualityFileBitmapSource extends BaseTextureAtlasSource implements
        IBitmapTextureAtlasSource {

    private int mWidth;
    private int mHeight;
    private Bitmap bitmap = null;

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
        super(pTexturePositionX, pTexturePositionY);

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

    }

    QualityFileBitmapSource(final InputFactory pFile, final int pTexturePositionX,
                            final int pTexturePositionY, final int pWidth, final int pHeight) {
        super(pTexturePositionX, pTexturePositionY);
        fileBitmapInput = pFile;
        this.mWidth = pWidth;
        this.mHeight = pHeight;
    }

    public InputStream openInputStream() throws IOException {
        return fileBitmapInput.openInput();
    }


    public QualityFileBitmapSource deepCopy() {
        QualityFileBitmapSource source = new QualityFileBitmapSource(this.fileBitmapInput, this.mTexturePositionX,
                this.mTexturePositionY, this.mWidth, this.mHeight);
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
        decodeOptions.inPreferredConfig = Config.ARGB_8888;//pBitmapConfig;
        decodeOptions.inSampleSize = inSampleSize;
        decodeOptions.inMutable = BuildUtils.noTexturesMode;

        InputStream in = null;
        try {
            in = openInputStream();
            var bitmap = BitmapFactory.decodeStream(in, null, decodeOptions);

            if (BuildUtils.noTexturesMode) {
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
