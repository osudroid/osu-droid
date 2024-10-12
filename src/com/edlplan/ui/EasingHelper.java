package com.edlplan.ui;

import android.view.animation.Interpolator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.easing.EasingManager;

import java.util.HashMap;
import java.util.Map;

public class EasingHelper {

    private static final Map<Easing, Interpolator> interpolatorsMap = new HashMap<>();

    public static Interpolator asInterpolator(Easing easing) {
        var interpolator = interpolatorsMap.get(easing);
        if (interpolator != null) {
            return interpolator;
        }
        interpolator = f -> (float) EasingManager.apply(easing, f);
        interpolatorsMap.put(easing, interpolator);
        return interpolator;
    }

}
