package com.edlplan.andengine;

import android.graphics.Bitmap;

import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource;

public class TextureHelper {

    public static QualityFileBitmapSource.InputFactory createMemoryFactoryFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return () -> new ByteArrayInputStream(bytes);
    }

    public static TextureRegion create1xRegion(int color) {
        Bitmap bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(color);
        int tw = 4, th = 4;
        final QualityFileBitmapSource source = new QualityFileBitmapSource(
                createMemoryFactoryFromBitmap(bmp));
        if (source.getWidth() == 0 || source.getHeight() == 0) {
            return null;
        }
        while (tw < source.getWidth()) {
            tw *= 2;
        }
        while (th < source.getHeight()) {
            th *= 2;
        }

        int errorCount = 0;
        while (source.preload() == false && errorCount < 3) {
            errorCount++;
        }
        if (errorCount >= 3) {
            return null;
        }
        final BitmapTextureAtlas tex = new BitmapTextureAtlas(GlobalManager.getInstance().getEngine().getTextureManager(), tw, th, TextureOptions.BILINEAR);

        TextureRegion region = TextureRegionFactory.createFromSource(tex, source, 0, 0,
                false);
        GlobalManager.getInstance().getEngine().getTextureManager().loadTexture(tex);
        return region;
    }

}
