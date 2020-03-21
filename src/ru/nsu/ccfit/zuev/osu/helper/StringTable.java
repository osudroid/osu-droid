package ru.nsu.ccfit.zuev.osu.helper;

import android.content.Context;

public class StringTable {
    static Context context;

    public static void setContext(final Context context) {
        StringTable.context = context;
    }

    public static String get(final int resid) {
        String str;
        try {
            str = context.getString(resid);
        } catch (final NullPointerException e) {
            str = "<error>";
        }
        return str;
    }

    public static String format(final int resid, final Object... objects) {
        return String.format(get(resid), objects);
    }
}
