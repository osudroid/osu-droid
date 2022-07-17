package com.reco1l.utils;

// Created by Reco1l on 2/7/22 06:18

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.reco1l.utils.interfaces.IMainClasses;

import ru.nsu.ccfit.zuev.osuplus.R;

public class Res implements IMainClasses {

    public static Drawable drw(@DrawableRes int id) {
        return mActivity.getDrawable(id);
    }

    public static float dimen(@DimenRes int id) {
        return mActivity.getResources().getDimension(id);
    }

    public static float sdp(int dimen) {
        if (dimen == 0)
            return 0;
        int id;
        if (dimen < 0) {
            if (dimen < -60) { // This because Scalable DP doesn't support negative values less than -60
                int count = 0;
                for (int i = 0; i >= dimen; i--) {
                    count -= dimen(R.dimen._1sdp);
                }
                return count;
            }
            id = mActivity.getResources().getIdentifier("_minus" + dimen + "sdp", "dimen", mActivity.getPackageName());
        } else {
            if (dimen > 600)
                return 0; // This because Scalable DP doesn't support values greater than 600 (And why would you want to use such a big value? lol)
            id = mActivity.getResources().getIdentifier("_" + dimen + "sdp", "dimen", mActivity.getPackageName());
        }
        return mActivity.getResources().getDimension(id);
    }

    public static String str(@StringRes int id) {
        return mActivity.getString(id);
    }

    public static int color(@ColorRes int id) {
        return mActivity.getResources().getColor(id);
    }

    public static ColorDrawable colorAsDrw(@ColorRes int id) {
        return new ColorDrawable(color(id));
    }
}
