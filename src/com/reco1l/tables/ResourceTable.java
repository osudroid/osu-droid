package com.reco1l.tables;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.reco1l.Game;

public interface ResourceTable {

    Resources resources = Game.activity.getResources();
    Resources.Theme theme = Game.activity.getTheme();

    default int id(String pName, String pType) {
        String pkg = Game.activity.getPackageName();

        return resources.getIdentifier(pName, pType, pkg);
    }

    //--------------------------------------------------------------------------------------------//

    // Return DP dimension in scalable type
    default int sdp(int dp) {
        if (dp <= 0) {
            return 0;
        }
        return (int) resources.getDimension(id("_" + dp + "sdp", "dimen"));
    }

    // Return SP dimension in scalable type (Use only in texts)
    default int ssp(int sp) {
        if (sp <= 0) {
            return 0;
        }
        return (int) resources.getDimension(id("_" + sp + "ssp", "dimen"));
    }

    //--------------------------------------------------------------------------------------------//

    default String str(@StringRes int pStringRes) {
        return resources.getString(pStringRes);
    }

    default int dimen(@DimenRes int pDimenRes) {
        return (int) resources.getDimension(pDimenRes);
    }

    default int color(@ColorRes int pColorRes) {
        return resources.getColor(pColorRes, theme);
    }

    default Drawable drw(@DrawableRes int pDrawableRes) {
        return resources.getDrawable(pDrawableRes, theme);
    }
}
