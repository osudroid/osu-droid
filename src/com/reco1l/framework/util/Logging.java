package com.reco1l.framework.util;

import android.util.Log;

import java.io.File;
import java.io.IOException;

public final class Logging
{

    public static final String CRASH_LOG_DIRECTORY = "rimu-logs";

    //----------------------------------------------------------------------------------------------------------------//

    private Logging()
    {
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Notify new instance creation of a class.
     */
    public static void initOf(Class<?> c)
    {
        if (c == null)
        {
            return;
        }
        Log.i("JVM", "Class new instance created: " + c.getSimpleName() + "@" + c.hashCode());
    }

    /**
     * Notify VM load of a class.
     */
    public static void loadOf(Class<?> c)
    {
        if (c == null)
        {
            return;
        }
        Log.i("JVM", "Class static loaded: " + c.getSimpleName() + "@" + c.hashCode());
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Save logcat as a TXT file.
     *
     * @param folder The desired directory to save the file.
     */
    public static void logcat(String folder) throws IOException
    {
        var directory = new File(folder);

        if (!directory.exists() && !directory.mkdirs())
        {
            throw new IOException("Unable to create parent directory.");
        }

        var file = new File(directory, "logcat.txt");

        if (file.exists() && !file.delete())
        {
            throw new IOException("Unable to delete existing logcat file.");
        }

        if (!file.createNewFile())
        {
            throw new IOException("Unable to create logcat file.");
        }

        Runtime.getRuntime().exec("logcat -f " + file.getAbsolutePath());
    }

    //----------------------------------------------------------------------------------------------------------------//

    public static void e(Object o, String m, Exception e)
    {
        e(o, m);
        e.printStackTrace();
    }

    public static void e(Object o, String m)
    {
        Log.e(o.getClass().getSimpleName(), m);
    }

    public static void i(Object o, String m)
    {
        Log.i(o.getClass().getSimpleName(), m);
    }
}
