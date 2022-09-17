package com.reco1l.ui.data.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.edlplan.framework.math.FMath;
import com.reco1l.BitmapManager;
import com.reco1l.interfaces.IMainClasses;

import java.io.IOException;
import java.io.InputStream;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;

// Created by Reco1l on 1/8/22 05:27

public class BeatmapHelper implements IMainClasses {

    private static final String COMPRESSED_BG_SUFFIX = "bg@";

    // Title
    //--------------------------------------------------------------------------------------------//
    public static String getTitle(TrackInfo track) {
        if (track == null) {
            return "null";
        }
        return getTitle(track.getBeatmap());
    }

    public static String getTitle(BeatmapInfo beatmap) {
        if (beatmap == null)
            return "null";

        if (beatmap.getTitleUnicode() != null && !Config.isForceRomanized()) {
            return beatmap.getTitleUnicode();
        }
        else if (beatmap.getTitle() != null) {
            return beatmap.getTitle();
        }

        return "null";
    }

    // Artist
    //--------------------------------------------------------------------------------------------//
    public static String getArtist(TrackInfo track) {
        if (track == null) {
            return "null";
        }
        return getArtist(track.getBeatmap());
    }

    public static String getArtist(BeatmapInfo beatmap) {
        if (beatmap == null)
            return "null";

        if (beatmap.getArtistUnicode() != null && !Config.isForceRomanized()) {
            return beatmap.getArtistUnicode();
        }
        else if (beatmap.getArtist() != null) {
            return beatmap.getArtist();
        }

        return "null";
    }

    // Difficulty color in HEX
    //--------------------------------------------------------------------------------------------//

    private static class DifficultyPalette {

        static final float[] domain = {
                0.1f, 1.25f, 2f, 2.5f, 3.3f,
                4.2f, 4.9f, 5.8f, 6.7f, 7.7f, 9f, Float.MAX_VALUE
        };

        static final int[] range = {
                0xFF4290FB, 0xFF4FC0FF, 0xFF4FFFD5, 0xFF7CFF4F, 0xFFF6F05C,
                0xFFFF8068, 0xFFFF4E6F, 0xFFC645B8, 0xFF6563DE, 0xFF18158E, 0xFF000000
        };

        // TODO make it as a spectrum like osu!web does.
        static int getColor(float stars) {
            stars = GameHelper.Round(stars, 2);

            if (stars < domain[0]) {
                return 0xFF999999;
            } else if (stars >= domain[10]) {
                return 0xFF000000;
            }

            int color = 0;
            for (int i = 0; i < range.length; ++i) {
                if (FMath.inInterval(domain[i] - 0.01f, domain[i + 1], stars)) {
                    color = range[i];
                    break;
                }
            }
            return color;
        }

        static int getTextColor(float stars) {
            return stars < domain[5] ? 0xCF000000 : Color.WHITE;
        }

        static int getBackgroundColor(float stars) {
            float[] hsv = new float[3];

            Color.colorToHSV(getDifficultyColor(stars), hsv);
            hsv[1] = stars >= domain[10] ? 0 : 0.70f;
            hsv[2] = stars >= domain[10] ? 0.08f : 0.20f;

            return Color.HSVToColor(hsv);
        }
    }

    public static int getDifficultyColor(float stars) {
        return DifficultyPalette.getColor(stars);
    }

    public static int getDifficultyTextColor(float stars) {
        return DifficultyPalette.getTextColor(stars);
    }

    public static int getDifficultyBackgroundColor(float stars) {
        return DifficultyPalette.getBackgroundColor(stars);
    }

    // Background
    //--------------------------------------------------------------------------------------------//

    public static Drawable getBackground(TrackInfo track) {
        if (track == null)
            return null;

        if (track.getBackground() != null) {
            return Drawable.createFromPath(track.getBackground());
        }
        InputStream is;

        try {
            is = mActivity.getAssets().open("gfx/menu-background.png");
        } catch (IOException e) {
            return null;
        }
        if (is != null) {
            return Drawable.createFromStream(is, null);
        }
        return null;
    }

    public static Bitmap getCompressedBackground(TrackInfo track) {
        return bitmapManager.get(COMPRESSED_BG_SUFFIX + track.getFilename());
    }

    public static void loadCompressedBackground(TrackInfo track) {
        if (!bitmapManager.contains(COMPRESSED_BG_SUFFIX + track.getFilename())) {
            Bitmap bitmap = BitmapManager.compress(BitmapFactory.decodeFile(track.getBackground()), 8);
            bitmapManager.loadBitmap(COMPRESSED_BG_SUFFIX + track.getFilename(), bitmap);
        }
    }
}
