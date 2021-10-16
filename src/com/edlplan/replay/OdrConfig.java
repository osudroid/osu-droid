package com.edlplan.replay;

import android.os.Environment;

import ru.nsu.ccfit.zuev.osu.Config;

import java.io.File;

public class OdrConfig {

    public static File getSongDir() {
        return new File(Config.getBeatmapPath());
    }

    public static File getDatabaseDir() {
        return new File(Config.getCorePath() + "/databases");
    }

    public static File getScoreDir() {
        return new File(Config.getScorePath());
    }

    public static File getMainDatabase() {
        return new File(getDatabaseDir(), "osudroid_test.db");
    }

}
