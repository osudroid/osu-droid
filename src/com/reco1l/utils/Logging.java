package com.reco1l.utils;

import android.content.Context;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public final class Logging {

    //--------------------------------------------------------------------------------------------//

    private Logging() {}

    //--------------------------------------------------------------------------------------------//

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

    //--------------------------------------------------------------------------------------------//

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void logcat() {

        File storage = Environment.getExternalStorageDirectory();
        File dir = new File(storage, "osu!droid/Log");

        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File log = new File(dir, "logcat.txt");
            if (log.exists()) {
                log.delete();
            }
            log.createNewFile();

            Runtime.getRuntime().exec("logcat -f " + log.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
