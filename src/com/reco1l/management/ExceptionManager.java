package com.reco1l.management;

import static android.content.pm.PackageManager.GET_ACTIVITIES;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;

import com.reco1l.tables.NotificationTable;
import com.reco1l.utils.Logging;
import com.reco1l.utils.execution.ScheduledTask;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.util.Date;

import ru.nsu.ccfit.zuev.osu.MainActivity;

public final class ExceptionManager extends Exception implements UncaughtExceptionHandler {

    public static final ExceptionManager instance = new ExceptionManager();

    private final UncaughtExceptionHandler mDefaultHandler;

    //--------------------------------------------------------------------------------------------//

    public ExceptionManager() {
        super();
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        Context context = MainActivity.instance;

        if (context == null) {
            if (mDefaultHandler != null) {
                mDefaultHandler.uncaughtException(t, e);
            }
            return;
        }

        if (t.getName().startsWith("async::")) {
            NotificationTable.exception(e);
            return;
        }

        String log = buildLogText(t, e);
        String response = buildLogFile(log);

        new Thread(() -> {
            Looper.prepare();

            String text = "An unexpected error has occurred!\n\n" + response;

            new Builder(context)
                    .setTitle("Error")
                    .setMessage(text)
                    .setPositiveButton("exit", ((dialog, which) -> exit()))
                    .show();

            Looper.loop();
        }).start();

        ScheduledTask.of(this::exit, 5000);
    }

    private void exit() {
        Process.killProcess(Process.myPid());
        System.exit(1);
    }

    //--------------------------------------------------------------------------------------------//

    private String buildLogText(Thread thread, Throwable e) {
        return
                "An unexpected error occurred." +
                        "\n\n" +
                        "Date: " + DateFormat.format("yyyy/MM/dd hh:mm:ss", new Date()) + "\n" +
                        "Build: " + getPackageInformation() + "\n" +
                        "Android: " + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")" +
                        "\n\n" +
                        "System Information: " + "\n" + getSystemInformation() +
                        "\n" +
                        "Exception in thread \"" + thread.getName() + "\": " + e.getClass().getSimpleName() +
                        "\n\n" +
                        Log.getStackTraceString(e);
    }

    private String getPackageInformation() {
        Context context = MainActivity.instance;

        PackageManager manager = context.getPackageManager();
        String name = context.getPackageName();

        try {
            PackageInfo pkg = manager.getPackageInfo(name, GET_ACTIVITIES);

            return pkg.versionName + " (" + pkg.versionCode + ")";
        } catch (NameNotFoundException e) {
            return e.getLocalizedMessage();
        }
    }

    private String getSystemInformation() {
        StringBuilder b = new StringBuilder();

        Field[] fields = Build.class.getDeclaredFields();

        for (Field field : fields) {
            b.append("\t");
            b.append(field.getName());
            b.append(": ");

            try {
                field.setAccessible(true);
                b.append(field.get(null));
            } catch (Exception e) {
                b.append("Cannot access field!");
            }
            b.append("\n");
        }

        return b.toString();
    }

    public String buildLogFile(String log) {

        String name = "crash_" + DateFormat.format("yyyy-MM-dd_hh-mm-ss", new Date()) + ".txt";

        File storage = Environment.getExternalStorageDirectory();
        File dir = new File(storage, Logging.DIRECTORY);

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return "Unable to create log directory, please check app permissions.";
            }
        }

        File file = new File(dir, name);
        try {
            FileWriter fw = new FileWriter(file, true);
            PrintWriter pw = new PrintWriter(fw);

            if (!file.createNewFile()) {
                pw.print("\n\n");
            }

            pw.println(log);
            pw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to create log file, please check app permissions.";
        }

        return "For more information read the log files.";
    }
}
