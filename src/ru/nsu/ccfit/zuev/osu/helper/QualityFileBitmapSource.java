package ru.nsu.ccfit.zuev.osu.helper;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.osudroid.resources.StreamBitmapSource;

import org.andengine.util.StreamUtils;
import org.andengine.util.debug.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class QualityFileBitmapSource extends StreamBitmapSource {

    private int mWidth;
    private int mHeight;

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

    @Override
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

    @Override
    protected int sampleSize() {
        return inSampleSize;
    }

    @Override
    protected String sourceLabel() {
        return "File: " + this.fileBitmapInput;
    }

    // Always decoded as ARGB_8888, whatever the atlas asks for.
    @Override
    protected Bitmap.Config bitmapConfig(final Config requested) {
        return Config.ARGB_8888;
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.fileBitmapInput + ")";
    }


    public interface InputFactory {
        InputStream openInput() throws IOException;
    }

}
