package com.reco1l.utils;

// Created by Reco1l on 2/7/22 06:18

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

import com.reco1l.Game;

import ru.nsu.ccfit.zuev.osuplus.R;

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
        if (dp == 0)
            return 0;

        int id;
        if (dp < 0) {
            if (dp < -60) { // This because Scalable DP doesn't support negative values less than -60
                float count = dimen(R.dimen._minus60sdp);

                for (int i = -60; i >= dp; i--) {
                    if (i < -600)
                        break;
                    count -= dimen(R.dimen._1sdp);
                }
                return (int) count;
            }
            id = id("_minus" + dp + "sdp", "dimen");
        } else {
            if (dp > 600) {
                return dimen(R.dimen._600sdp); // This because Scalable DP doesn't support values greater than 600
            }
            id = id("_" + dp + "sdp", "dimen");
        }
        return (int) Game.activity.getResources().getDimension(id);
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
