package com.edlplan.ui;

import android.view.animation.Interpolator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.easing.EasingManager;

import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import java.util.HashMap;
import java.util.Map;

public class EasingHelper {


    private static final Map<Easing, Interpolator> interpolatorsMap = new HashMap<>();

    private static final Map<Easing, IEaseFunction> easeFunctionsMap = new HashMap<>();


    public static Interpolator asInterpolator(Easing easing) {
        var interpolator = interpolatorsMap.get(easing);
        if (interpolator != null) {
            return interpolator;
        }
        interpolator = f -> (float) EasingManager.apply(easing, f);
        interpolatorsMap.put(easing, interpolator);
        return interpolator;
    }

    public static IEaseFunction asEaseFunction(Easing easing) {
        var easeFunction = easeFunctionsMap.get(easing);
        if (easeFunction != null) {
            return easeFunction;
        }
        easeFunction = (pSecondsElapsed, pDuration) -> (float) EasingManager.apply(easing, pSecondsElapsed / pDuration);
        easeFunctionsMap.put(easing, easeFunction);
        return easeFunction;
    }


}
