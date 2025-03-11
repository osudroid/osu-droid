package ru.nsu.ccfit.zuev.osu.helper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Formatter;

public class StringTable {

    private static Context context;

    private static final StringBuilder stringBuilder = new StringBuilder();

    private static final Formatter formatter = new Formatter(stringBuilder);


    public static void setContext(final Context context) {
        StringTable.context = context;
    }

    public static String get(@StringRes final int resid) {
        return context.getString(resid);
    }


    @NonNull
    synchronized public static String format(final int resid, final Object... objects) {
        stringBuilder.setLength(0);
        formatter.format(get(resid), objects);
        return stringBuilder.toString();
    }

    @NonNull
    synchronized public static String format(final String format, final Object... objects) {
        stringBuilder.setLength(0);
        formatter.format(format, objects);
        return stringBuilder.toString();
    }
}
