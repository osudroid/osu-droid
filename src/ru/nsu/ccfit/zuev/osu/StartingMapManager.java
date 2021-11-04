package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import androidx.preference.PreferenceManager;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

public class StartingMapManager {
    private final Activity activity;

    public StartingMapManager(final Activity activity) {
        this.activity = activity;
    }

    public boolean checkStartingMaps() {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(activity);
        return prefs.getBoolean("initialized", false);
    }

    public void copyStartingMaps() {
        if (checkStartingMaps()) {
            return;
        }
        ToastLogger.showText("Preparing for the first launch", false);
        String dirList[];
        try {
            dirList = activity.getAssets().list("Songs");
        } catch (final IOException e) {
            Debug.e("StartingMapManager: " + e.getMessage(), e);
            return;
        }

        for (final String dir : dirList) {
            copyAllFiles(dir);
        }

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(activity);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("initialized", true);
        editor.commit();
    }

    private void copyAllFiles(final String dirname) {
        final File dir = new File(Config.getBeatmapPath() + "/" + dirname);
        if (dir.exists() == false && dir.mkdirs() == false) {
            ToastLogger.showText("Cannot create " + dir.getPath(), false);
            return;
        }
        String fileList[];
        try {
            fileList = activity.getAssets().list("Songs/" + dirname);
        } catch (final IOException e) {
            Debug.e("StartingMapManager: " + e.getMessage(), e);
            return;
        }

        final AssetManager mgr = activity.getAssets();
        for (final String file : fileList) {
            final String fullname = "Songs/" + dirname + "/" + file;
            try {
                final InputStream istream = mgr.open(fullname);
                copyFile(dirname + "/" + file, istream);
            } catch (final IOException e) {
                Debug.e("StartingMapManager: " + e.getMessage(), e);
            }
        }
    }

    private void copyFile(final String filename, final InputStream istream) {
        OutputStream out;

        try {
            out = new FileOutputStream(Config.getBeatmapPath() + "/" + filename);
        } catch (final FileNotFoundException e) {
            ToastLogger.showText(
                    StringTable.format(R.string.message_error, e.getMessage()),
                    false);
            Debug.e("StartingMapManager: " + e.getMessage(), e);
            return;
        }

        try {
            final byte[] buffer = new byte[4096];
            int read;
            while ((read = istream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            istream.close();
            out.flush();
            out.close();
        } catch (final IOException e) {
            ToastLogger.showText(e.getMessage(), false);
            Debug.e("StartingMapManager: " + e.getMessage(), e);
            return;
        }
    }
}
