package com.reco1l.utils;

// Created by Reco1l on 2/7/22 06:18

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.math.MathUtils;

import com.reco1l.Game;

public class Resources {

    //--------------------------------------------------------------------------------------------//

    public static int id(String name, String type) {
        return Game.activity.getResources().getIdentifier(name, type, Game.activity.getPackageName());
    }

    //--------------------------------------------------------------------------------------------//

    public static Drawable drw(@DrawableRes int id) {
        return Game.activity.getDrawable(id);
    }

    public static Bitmap drwAsBitmap(@DrawableRes int id) {
        return BitmapFactory.decodeResource(Game.activity.getResources(), id);
    }

    //--------------------------------------------------------------------------------------------//

    public static int dimen(@DimenRes int id) {
        return (int) Game.activity.getResources().getDimension(id);
    }

    public static int sdp(int dp) {
        if (dp <= 0) {
            return 0;
        }
        String id = "_" + dp + "sdp";

        return (int) Game.activity.getResources().getDimension(id(id, "dimen"));
    }

    //--------------------------------------------------------------------------------------------//

    public static String str(@StringRes int id) {
        return Game.activity.getString(id);
    }

    //--------------------------------------------------------------------------------------------//

    public static int color(@ColorRes int id) {
        return Game.activity.getResources().getColor(id);
    }

    public static ColorDrawable colorAsDrw(@ColorRes int id) {
        return new ColorDrawable(color(id));
    }
}
