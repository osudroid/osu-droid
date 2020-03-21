package com.edlplan.framework.support;

import com.edlplan.framework.support.batch.BatchEngine;

public class SupportState {

    private static boolean usingSupportCamera = false;

    public static boolean isUsingSupportCamera() {
        return usingSupportCamera;
    }

    public static void setUsingSupportCamera(boolean usingSupportCamera) {
        BatchEngine.flush();
        SupportState.usingSupportCamera = usingSupportCamera;
    }

}
