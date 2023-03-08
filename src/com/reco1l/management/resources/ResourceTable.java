package com.reco1l.management.resources;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

import com.reco1l.Game;

import main.osu.MainActivity;

public interface ResourceTable {

    default Context context() {
        return MainActivity.instance;
    }

    default Resources resources() {
        return context().getResources();
    }

    default Theme theme() {
        return context().getTheme();
    }

    default int id(String name, String type) {
        return resources().getIdentifier(name, type, context().getPackageName());
    }

    //--------------------------------------------------------------------------------------------//

    // Return DP dimension in scalable type
    default int sdp(int dp) {
        if (dp == 0) {
            return 0;
        }
        if (dp < 0) {
            return -sdp(Math.abs(dp));
        }
        if (dp > 600) {
            return sdp(1) * dp;
        }
        return (int) resources().getDimension(id("_" + dp + "sdp", "dimen"));
    }

    // Return SP dimension in scalable type (Use only in texts)
    @Deprecated
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

    default Typeface font(String name) {
        return ResourcesCompat.getFont(context(), id(name, "font"));
    }

    default TypedValue attr(@AttrRes int id, boolean resolveRefs) {
        TypedValue outValue = new TypedValue();

        theme().resolveAttribute(id, outValue, resolveRefs);
        return outValue;
    }
}
