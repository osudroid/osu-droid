package com.edlplan.andengine;

import static android.graphics.Bitmap.Config.ARGB_8888;

import android.graphics.Bitmap;

import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.EmptyBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.bitmap.source.FileBitmapTextureAtlasSource;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class TextureHelper {

    private static int tmpFileId = 0;

    public static File createFactoryFromBitmap(Bitmap bitmap) {
        tmpFileId++;
        try {
            File tmp = File.createTempFile("bmp_cache" + tmpFileId, ".png");
            tmp.deleteOnExit();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(tmp));
            return tmp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static TextureRegion createRegion(Bitmap bitmap) {
        int tw = 4, th = 4;
        final var source = FileBitmapTextureAtlasSource.create(
                createFactoryFromBitmap(bitmap));
        if (source.getTextureWidth() == 0 || source.getTextureHeight() == 0) {
            return null;
        }
        while (tw < source.getTextureWidth()) {
            tw *= 2;
        }
        while (th < source.getTextureHeight()) {
            th *= 2;
        }

        int errorCount = 0;
        while (source.onLoadBitmap(ARGB_8888) == null && errorCount < 3) {
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

    public static TextureRegion create1xRegion(int color) {
        Bitmap bmp = Bitmap.createBitmap(8, 8, ARGB_8888);
        bmp.eraseColor(color);
        int tw = 4, th = 4;
        final var source = new EmptyBitmapTextureAtlasSource(1, 1);
        if (source.getTextureWidth() == 0 || source.getTextureHeight() == 0) {
            return null;
        }
        while (tw < source.getTextureWidth()) {
            tw *= 2;
        }
        while (th < source.getTextureHeight()) {
            th *= 2;
        }

        int errorCount = 0;
        while (source.onLoadBitmap(ARGB_8888) == null && errorCount < 3) {
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
