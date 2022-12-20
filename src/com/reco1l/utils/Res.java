package com.reco1l.utils;

// Created by Reco1l on 2/7/22 06:18

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.reco1l.Game;

public class Res {

    private static final Resources R = Game.activity.getResources();
    private static final Resources.Theme theme = Game.activity.getTheme();

    //--------------------------------------------------------------------------------------------//

    public static int id(String name, String type) {
        return R.getIdentifier(name, type, Game.activity.getPackageName());
    }

    //--------------------------------------------------------------------------------------------//

    public static Drawable drw(@DrawableRes int id) {
        return R.getDrawable(id, theme);
    }

    public static Bitmap drwAsBitmap(@DrawableRes int id) {
        return BitmapFactory.decodeResource(R, id);
    }

    //--------------------------------------------------------------------------------------------//

    public static int dimen(@DimenRes int id) {
        return (int) R.getDimension(id);
    }

    public static int sdp(int dp) {
        if (dp <= 0) {
            return 0;
        }
        return (int) R.getDimension(id("_" + dp + "sdp", "dimen"));
    }

    //--------------------------------------------------------------------------------------------//

    public static String str(@StringRes int id) {
        return R.getString(id);
    }

    //--------------------------------------------------------------------------------------------//

    public static int color(@ColorRes int id) {
        return R.getColor(id, theme);
    }
}
