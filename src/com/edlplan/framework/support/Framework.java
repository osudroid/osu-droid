package com.edlplan.framework.support;

import android.os.Environment;
import android.os.SystemClock;

import com.edlplan.framework.utils.FileUtils;

import java.io.File;

public class Framework {

    public static final int PLATFORM_WIN_PC = 1;

    public static final int PLATFORM_ANDROID = 2;

    private static final int frameworkVersion = 1;

    private static final int platform = PLATFORM_ANDROID;

    public static int getFrameworkVersion() {
        return frameworkVersion;
    }

    public static int getPlatform() {
        return platform;
    }

    public static File getFrameworkDir() {
        final File dir = new File(Environment.getExternalStorageDirectory(), "EdFramework");
        FileUtils.checkExistDir(dir);
        return dir;
    }

    /**
     * 获取相对的精确时间
     */
    public static double relativePreciseTimeMillion() {
        return System.nanoTime() / 1000000d;
    }

    public static int msToNm(double ms) {
        return (int) (ms * 1000000);
    }

    public static long absoluteTimeMillion() {
        return System.currentTimeMillis();
    }

    /**
     * @return 框架的标准时间
     */
    public static double frameworkTime() {
        return SystemClock.uptimeMillis();
    }
}