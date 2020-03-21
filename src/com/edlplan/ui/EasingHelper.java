package com.edlplan.ui;

import android.view.animation.Interpolator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.easing.EasingManager;

public class EasingHelper {

    public static Interpolator asInterpolator(Easing easing) {
        return f -> (float) EasingManager.apply(easing, f);
    }


}
