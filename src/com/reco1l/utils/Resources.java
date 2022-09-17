package com.reco1l.utils;

// Created by Reco1l on 2/7/22 06:18

import static com.reco1l.interfaces.IMainClasses.mActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import ru.nsu.ccfit.zuev.osuplus.R;

public class Resources {

    //--------------------------------------------------------------------------------------------//

    public static int id(String name, String type) {
        return mActivity.getResources().getIdentifier(name, type, mActivity.getPackageName());
    }

    //--------------------------------------------------------------------------------------------//

    public static Drawable drw(@DrawableRes int id) {
        return mActivity.getDrawable(id);
    }

    public static Bitmap drwAsBitmap(@DrawableRes int id) {
        return BitmapFactory.decodeResource(mActivity.getResources(), id);
    }

    //--------------------------------------------------------------------------------------------//

    public static float dimen(@DimenRes int id) {
        return mActivity.getResources().getDimension(id);
    }

    public static float sdp(int dp) {
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
                return count;
            }
            id = id("_minus" + dp + "sdp", "dimen");
        } else {
            if (dp > 600) {
                return dimen(R.dimen._600sdp); // This because Scalable DP doesn't support values greater than 600
            }
            id = id("_" + dp + "sdp", "dimen");
        }
        return mActivity.getResources().getDimension(id);
    }

    //--------------------------------------------------------------------------------------------//

    public static String str(@StringRes int id) {
        return mActivity.getString(id);
    }

    //--------------------------------------------------------------------------------------------//

    public static int color(@ColorRes int id) {
        return mActivity.getResources().getColor(id);
    }

    public static ColorDrawable colorAsDrw(@ColorRes int id) {
        return new ColorDrawable(color(id));
    }
}
