package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.widget.Toast;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.helper.StringTable;

public class ToastLogger implements Runnable {
    private static ToastLogger instance = null;
    Activity activty;
    String message = "";
    boolean showlong = false;
    ArrayList<String> debugLog = new ArrayList<String>();
    float percentage;

    private ToastLogger(final Activity activty) {
        this.activty = activty;
    }

    public static void init(final Activity activty) {
        instance = new ToastLogger(activty);
    }

    public static void showText(final String message, final boolean showlong) {
        if (instance == null) {
            return;
        }
        instance.message = message;
        instance.showlong = showlong;
        instance.activty.runOnUiThread(instance);

    }

    public static void showTextId(final int resID, final boolean showlong) {
        showText(StringTable.get(resID), showlong);
    }

    public static void addToLog(final String str) {
        if (instance == null) {
            return;
            /*
             * if (instance.debugLog.size() >= 20) instance.debugLog.remove(0);
             * instance.debugLog.add(str);
             */
        }
    }

    public static ArrayList<String> getLog() {
        if (instance == null) {
            return null;
        }
        return instance.debugLog;
    }

    public static float getPercentage() {
        if (instance == null) {
            return -1;
        }
        return instance.percentage;
    }

    public static void setPercentage(final float perc) {
        if (instance == null) {
            return;
        }
        instance.percentage = perc;
    }

    public void run() {
        Toast.makeText(instance.activty, message,
                showlong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}
