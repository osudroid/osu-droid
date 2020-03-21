package com.edlplan.framework.support.graphics;

import android.graphics.BitmapFactory;

import com.edlplan.framework.math.Vec2Int;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class BitmapUtil {

    public static Vec2Int parseBitmapSize(File file) throws FileNotFoundException {
        final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inJustDecodeBounds = true;
        Vec2Int v = new Vec2Int();
        BitmapFactory.decodeStream(new FileInputStream(file), null, decodeOptions);
        v.x = decodeOptions.outWidth;
        v.y = decodeOptions.outHeight;
        return v;
    }


}
