package com.reco1l.tools;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public final class Logging {

    public static final String DIRECTORY = "rimu-logs";

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
        File dir = new File(storage, DIRECTORY);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
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

    //--------------------------------------------------------------------------------------------//

    public static void e(Object o, String m, Exception e) {
        e(o, m);
        e.printStackTrace();
    }

    public static void e(Object o, String m) {
        Log.e(o.getClass().getSimpleName(), m);
    }

    public static void i(Object o, String m) {
        Log.i(o.getClass().getSimpleName(), m);
    }
}
