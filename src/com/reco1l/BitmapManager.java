package com.reco1l;

import static com.reco1l.interfaces.ITextures.fileNames;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.math.MathUtils;

import com.reco1l.interfaces.IMainClasses;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Created by Reco1l on 22/8/22 21:38

public class BitmapManager implements IMainClasses {
    public static BitmapManager instance;

    private final Map<String, Bitmap> bitmaps = new HashMap<>();

    //--------------------------------------------------------------------------------------------//

    public static BitmapManager getInstance() {
        if (instance == null) {
            instance = new BitmapManager();
        }
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    public void loadAssets(String folder) {
        List<String> valid = Arrays.asList(fileNames);

        try {
            for (String asset : mActivity.getAssets().list("gfx")) {
                final String name = asset.substring(0, asset.length() - 4);

                if (valid.contains(name)) {
                    try {
                        InputStream stream = mActivity.getAssets().open("gfx/" + asset);
                        bitmaps.put(name, BitmapFactory.decodeStream(stream));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        global.setInfo("Loading song backgrounds...");
        global.setLoadingProgress(15);
    }

    public Bitmap loadBitmap(String key, Bitmap bitmap) {
        bitmaps.put(key, bitmap);
        return bitmap;
    }

    //--------------------------------------------------------------------------------------------//

    public static Bitmap compress(Bitmap raw, int quality) {
        if (raw == null)
            return null;

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

    //--------------------------------------------------------------------------------------------//

    public Bitmap get(String name) {
        return bitmaps.get(name);
    }

    public Drawable getAsDrawable(String name) {
        return new BitmapDrawable(mActivity.getResources(), bitmaps.get(name));
    }

    public boolean contains(String name) {
        return bitmaps.containsKey(name);
    }

}
