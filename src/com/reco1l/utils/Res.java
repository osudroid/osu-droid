package com.reco1l.utils;

// Created by Reco1l on 2/7/22 06:18

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.reco1l.utils.interfaces.IMainClasses;

public class Res implements IMainClasses {

    public static Drawable drw(@DrawableRes int id) {
        return mActivity.getDrawable(id);
    }

    public static float dimen(@DimenRes int id) {
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
