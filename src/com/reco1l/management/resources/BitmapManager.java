package com.reco1l.management.resources;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.reco1l.Game;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import main.osu.Config;

// Created by Reco1l on 22/8/22 21:38

public class BitmapManager {

    public static final BitmapManager instance = new BitmapManager();

    private final Map<String, Bitmap> mBitmaps;

    // This will be set to true if the skin directory isn't valid, check validateFolder()
    private boolean mIsDefaultSkin = false;

    //--------------------------------------------------------------------------------------------//

    public BitmapManager() {
        mBitmaps = new HashMap<>();
    }

    //--------------------------------------------------------------------------------------------//

    private String validateFolder(String folder) {
        if (folder != null) {

            // Since is directory should have a separator as last char
            if (!folder.endsWith(File.separator)) {
                folder += File.separator;
            }

            // Check if it's default skin
            mIsDefaultSkin = folder.equals(Config.getSkinTopPath());

            if (!mIsDefaultSkin) {
                File directory = new File(folder);

                // If directory doesn't exist automatically set default skin
                mIsDefaultSkin = !directory.exists();
            }
        } else {
            mIsDefaultSkin = true;
        }
        return folder;
    }

    private Bitmap getDefaultAsset(String name) {
        AssetManager manager = Game.activity.getAssets();

        try (InputStream stream = manager.open("gfx/" + name)) {
            return BitmapFactory.decodeStream(stream);
        }
        catch (Exception e) {
            return null;
        }
    }

    public void loadAssetsFrom(String folder) {
        folder = validateFolder(folder);

        for (String texture : Textures.all) {
            String name = texture + ".png";
            Bitmap bitmap;

            if (mIsDefaultSkin) {
                bitmap = getDefaultAsset(name);
            } else {
                File file = new File(folder + name);

                if (!file.exists()) {
                    bitmap = getDefaultAsset(name);
                } else {
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                }
            }
            mBitmaps.put(texture, bitmap);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void put(String key, Bitmap bitmap) {
        this.mBitmaps.put(key, bitmap);
    }

    public Bitmap get(String name) {
        return mBitmaps.get(name);
    }

    public boolean contains(String name) {
        return this.mBitmaps.containsKey(name);
    }
}