package ru.nsu.ccfit.zuev.osu.helper;

import android.content.Context;

import androidx.annotation.StringRes;

import java.util.Formatter;

public class StringTable {

    static Context context;

    private static StringBuilder sb;

    private static Formatter f;

    public static void setContext(final Context context) {
        StringTable.context = context;
    }

    public static String get(@StringRes final int resid) {
        String str;
        try {
            str = context.getString(resid);
        } catch (final NullPointerException e) {
            str = "<error>";
        }
        return str;
    }

    private static void allocateFormatter() {
        if (sb == null) {
            sb = new StringBuilder();
        }
        sb.setLength(0);
        if (f == null) {
            f = new Formatter(sb);
        }
    }

    public static String format(final int resid, final Object... objects) {
        allocateFormatter();
        f.format(get(resid), objects);
        return sb.toString();
    }

}
