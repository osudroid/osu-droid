package com.edlplan.replay;

import android.os.Environment;

import java.io.File;

public class OdrConfig {

    public static File getSongDir() {
        return new File(Environment.getExternalStorageDirectory(), "osu!droid/Songs");
    }

    public static File getDatabaseDir() {
        return new File(Environment.getExternalStorageDirectory(), "osu!droid/databases");
    }

    public static File getScoreDir() {
        return new File(Environment.getExternalStorageDirectory(), "osu!droid/Scores");
    }

    public static File getMainDatabase() {
        return new File(getDatabaseDir(), "osudroid_test.db");
    }

}
