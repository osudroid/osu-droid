package com.reco1l.ui.data;// Created by Reco1l on 1/8/22 05:27

import android.graphics.drawable.Drawable;

import com.reco1l.utils.interfaces.IMainClasses;

import java.io.IOException;
import java.io.InputStream;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;

public class BeatmapHelper implements IMainClasses {

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

    // Difficulty color in HSV
    //--------------------------------------------------------------------------------------------//
    private static boolean range(float value, float min, float max) {
        return value >= min && value < max;
    }

    public static float[] getDifficultyColor(float stars) {
        return getDifficultyColor(stars, false);
    }

    public static float[] getDifficultyColor(float stars, boolean darker) {

        float[] HSV = {1f, 0.75f, 0.50f};

        float f = 1 + stars - (int) stars; // Decimal part
        int i = 36; // Hue difference between difficulty

        if (range(stars, 1, 5)) {
            HSV[0] = 180 - (i * stars);
        }
        if (range(stars, 5, 6)) {
            HSV[0] = 360 - (i * (f - 1));
        }
        if (range(stars, 6, 7)) {
            HSV[0] = 360 - (i * f);
        }
        if (range(stars, 7, 8)) {
            HSV[0] = 360 - (i + (i * f));
        }
        if (range(stars, 8, 9)) {
            HSV[0] = 360 - (2 * i + (i * f));
        }
        if (range(stars, 9, 10)) {
            HSV[0] = 360 - (3 * i + (i * f));
            HSV[2] = 0.50f - (0.1f * f);
        }

        if (stars < 1) {
            HSV[0] = 180;
        } else if (stars >= 10) {
            HSV[2] = 0;
        }

        if(darker) {
            HSV[1] = 0.70f;
            HSV[2] = 0.20f;

            if(stars >= 10) {
                HSV[2] = 0.08f;
                HSV[1] = 0;
            }
        }

        return HSV;
    }

    // Background
    //--------------------------------------------------------------------------------------------//

    public static Drawable getBackground(TrackInfo track) {
        if (track == null)
            return null;

        if (track.getBackground() != null) {
            return Drawable.createFromPath(track.getBackground());
        } else {
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
    }
}
