package com.reco1l.utils;

import android.util.Log;

public final class Logging {

    private Logging() {}

    // Notify new instance creation of a class
    public static void initOf(Class<?> c) {
        if (c == null) {
            return;
        }
        Log.i("JVM", "Class new instance created: " + c.getSimpleName() + "@" + c.hashCode());
    }

    // Notify static load of a class
    public static void loadOf(Class<?> c) {
        if (c == null) {
            return;
        }
        Log.i("JVM", "Class static loaded: " + c.getSimpleName() + "@" + c.hashCode());
    }

    public static void e(Class<?> c, String msg, Throwable e) {
        Log.e(c.getSimpleName(), msg, e);
    }
}
