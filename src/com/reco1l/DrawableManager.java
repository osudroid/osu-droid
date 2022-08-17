package com.reco1l;

import android.graphics.drawable.Drawable;

import com.reco1l.utils.interfaces.IMainClasses;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Created by Reco1l on 8/8/22 16:49

public class DrawableManager implements ITextures, IMainClasses {
    private static DrawableManager instance;

    private final Map<String, Drawable> drawables = new HashMap<>();
    private String[] assets;

    //--------------------------------------------------------------------------------------------//

    public static DrawableManager getInstance() {
        if (instance == null) {
            instance = new DrawableManager();
        }
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    public void loadAssets(String folder) {
        List<String> usedTextures = Arrays.asList(fileNames);

        // Loading default textures
        if (assets == null) {
            try {
                assets = mActivity.getAssets().list("gfx");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (String asset : assets) {
            final String name = asset.substring(0, asset.length() - 4);

            if (usedTextures.contains(name)) {
                try {
                    InputStream stream = mActivity.getAssets().open("gfx/" + asset);
                    drawables.put(name, Drawable.createFromStream(stream, null));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public Drawable get(String name) {
        Drawable drawable = drawables.get(name);
        if (drawable != null) {
            return drawable.mutate();
        }
        return null;
    }
}
