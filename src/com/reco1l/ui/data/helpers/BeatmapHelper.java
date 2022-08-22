package com.reco1l.ui.data.helpers;

import android.graphics.drawable.Drawable;

import com.reco1l.utils.interfaces.IMainClasses;

import java.io.IOException;
import java.io.InputStream;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;

// Created by Reco1l on 1/8/22 05:27

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

    // Don't ask me how this works, but if you want you can write a better algorithm.
    // TODO [BeatmapHelper] make a better algorithm to get the difficulty color.
    public static float[] getColor(float stars) {

        float[] hsv = {1f, 0.75f, 0.50f};

        float f = 1 + stars - (int) stars; // Decimal part
        int i = 36; // Hue difference between difficulty

        if (range(stars, 1, 5)) {
            hsv[0] = 180 - (i * stars);
        }
        else if (range(stars, 5, 6)) {
            hsv[0] = 360 - (i * (f - 1));
        }
        else if (range(stars, 6, 7)) {
            hsv[0] = 360 - (i * f);
        }
        else if (range(stars, 7, 8)) {
            hsv[0] = 360 - (i + (i * f));
        }
        else if (range(stars, 8, 9)) {
            hsv[0] = 360 - (2 * i + (i * f));
        }
        else if (range(stars, 9, 10)) {
            hsv[0] = 360 - (3 * i + (i * f));
            hsv[2] = 0.50f - (0.1f * f);
        }

        if (stars < 1) {
            hsv[0] = 180;
        } else if (stars >= 10) {
            hsv[2] = 0;
        }
        return hsv;
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
}
