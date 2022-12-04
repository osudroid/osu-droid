package com.reco1l.utils.helpers;
// Created by Reco1l on 02/12/2022, 01:28

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.core.math.MathUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class BitmapHelper {

    public static Bitmap cropInCenter(Bitmap raw, int width, int height) {

        int x = raw.getWidth() / 2 - width / 2;
        int y = raw.getHeight() / 2 - height / 2;

        Bitmap cropped = Bitmap.createBitmap(raw, x, y, width, height);
        raw.recycle();
        return cropped;
    }

    public static Bitmap resize(Bitmap raw, float toWidth, float toHeight) {
        int width = raw.getWidth();
        int height = raw.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(toWidth / width, toHeight / height);

        Bitmap resize = Bitmap.createBitmap(raw, 0, 0, width, height, matrix, false);
        raw.recycle();

        return resize;
    }

    public static Bitmap compress(Bitmap raw, int quality) {
        quality = MathUtils.clamp(quality, 1, 100);

        if (quality < 100) {
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            raw.compress(Bitmap.CompressFormat.JPEG, quality, stream);

            Bitmap compressed = BitmapFactory.decodeStream(new ByteArrayInputStream(stream.toByteArray()));
            raw.recycle();
            return compressed;
        }
        return raw;
    }
}
