package ru.nsu.ccfit.zuev.osu.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.reco1l.BuildUtils;
import com.reco1l.framework.Bitmaps;

import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.source.BaseTextureAtlasSource;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class QualityAssetBitmapSource extends BaseTextureAtlasSource implements
        IBitmapTextureAtlasSource {

    private final int mWidth;
    private final int mHeight;

    private final String mAssetPath;
    private final Context mContext;

    private Bitmap bitmap = null;

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
        super(pTexturePositionX, pTexturePositionY);
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
    }

    QualityAssetBitmapSource(final Context pContext, final String pAssetPath,
                             final int pTexturePositionX, final int pTexturePositionY,
                             final int pWidth, final int pHeight) {
        super(pTexturePositionX, pTexturePositionY);
        this.mContext = pContext;
        this.mAssetPath = pAssetPath;
        this.mWidth = pWidth;
        this.mHeight = pHeight;
    }


    public QualityAssetBitmapSource deepCopy() {
        return new QualityAssetBitmapSource(this.mContext, this.mAssetPath,
                this.mTexturePositionX, this.mTexturePositionY, this.mWidth,
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
        InputStream in = null;
        try {
            final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inPreferredConfig = pBitmapConfig;
            decodeOptions.inSampleSize = ru.nsu.ccfit.zuev.osu.Config.getTextureQuality();
            decodeOptions.inMutable = BuildUtils.noTexturesMode;

            in = this.mContext.getAssets().open(this.mAssetPath);
            var bitmap = BitmapFactory.decodeStream(in, null, decodeOptions);

            if (BuildUtils.noTexturesMode) {
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
