package ru.nsu.ccfit.zuev.osu.helper;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.osudroid.resources.StreamBitmapSource;

import org.andengine.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class QualityAssetBitmapSource extends StreamBitmapSource {

    private final int mWidth;
    private final int mHeight;

    private final String mAssetPath;
    private final Context mContext;

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

    @Override
    public InputStream openInputStream() throws IOException {
        return this.mContext.getAssets().open(this.mAssetPath);
    }

    @Override
    protected int sampleSize() {
        return ru.nsu.ccfit.zuev.osu.Config.getTextureQuality();
    }

    @Override
    protected String sourceLabel() {
        return "AssetPath: " + this.mAssetPath;
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.mAssetPath + ")";
    }

}
