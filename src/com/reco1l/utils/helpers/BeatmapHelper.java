package com.reco1l.utils.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.edlplan.framework.math.FMath;
import com.reco1l.interfaces.IReferences;

import java.io.IOException;
import java.io.InputStream;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;

// Created by Reco1l on 1/8/22 05:27

public class BeatmapHelper implements IReferences {

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

    public static class Palette {

        private static final float[] domain = {
                0.1f, 1.25f, 2f, 2.5f, 3.3f,
                4.2f, 4.9f, 5.8f, 6.7f, 7.7f, 9f, Float.MAX_VALUE
        };

        private static final int[] range = {
                0xFF4290FB, 0xFF4FC0FF, 0xFF4FFFD5, 0xFF7CFF4F, 0xFFF6F05C,
                0xFFFF8068, 0xFFFF4E6F, 0xFFC645B8, 0xFF6563DE, 0xFF18158E, 0xFF000000
        };

        // TODO make it as a spectrum like osu!web does.
        public static int getColor(float stars) {
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

        public static int getTextColor(float stars) {
            return stars < domain[5] ? 0xCF000000 : Color.WHITE;
        }

        public static int getDarkerColor(float stars) {
            float[] hsv = new float[3];

            Color.colorToHSV(getColor(stars), hsv);
            hsv[1] = stars >= domain[10] ? 0 : 0.70f;
            hsv[2] = stars >= domain[10] ? 0.08f : 0.20f;

            return Color.HSVToColor(hsv);
        }
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
            is = activity.getAssets().open("gfx/menu-background.png");
        } catch (IOException e) {
            return null;
        }
        if (is != null) {
            return Drawable.createFromStream(is, null);
        }
        return null;
    }

    //--------------------------------------------------------------------------------------------//
    private static String getBackgroundKey(TrackInfo track) {
        if (track == null) {
            return null;
        }
        return "compressed@" + track.getBeatmapID() + "/" + track.getPublicName();
    }

    public static Bitmap getCompressedBackground(TrackInfo track) {
        return bitmapManager.get(getBackgroundKey(track));
    }

    public static void loadCompressedBackground(TrackInfo track) {
        if (!bitmapManager.contains(getBackgroundKey(track))) {
            Bitmap bitmap = BitmapHelper.compress(BitmapFactory.decodeFile(track.getBackground()), 10);
            bitmapManager.put(getBackgroundKey(track), bitmap);
        }
    }
}
