package com.reco1l.tables;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.reco1l.global.Game;

public interface ResourceTable {

    //--------------------------------------------------------------------------------------------//

    default Context context() {
        return Game.activity;
    }

    default Resources resources() {
        return context().getResources();
    }

    default Theme theme() {
        return context().getTheme();
    }

    default DisplayMetrics metrics() {
        return resources().getDisplayMetrics();
    }

    default int id(String name, String type) {
        return resources().getIdentifier(name, type, context().getPackageName());
    }

    //--------------------------------------------------------------------------------------------//

    // Return DP dimension in scalable type
    default int sdp(int dp) {
        if (dp <= 0) {
            // Invert the positive value instead.
            return 0;
        }
        return (int) resources().getDimension(id("_" + dp + "sdp", "dimen"));
    }

    // Return SP dimension in scalable type (Use only in texts)
    default int ssp(int sp) {
        if (sp <= 0) {
            // Invert the positive value instead.
            return 0;
        }
        return (int) resources().getDimension(id("_" + sp + "ssp", "dimen"));
    }

    //--------------------------------------------------------------------------------------------//

    default String str(@StringRes int res) {
        return resources().getString(res);
    }

    default int dimen(@DimenRes int res) {
        return (int) resources().getDimension(res);
    }

    default int color(@ColorRes int res) {
        return resources().getColor(res, theme());
    }

    default Drawable drw(@DrawableRes int res) {
        return resources().getDrawable(res, theme());
    }
}
