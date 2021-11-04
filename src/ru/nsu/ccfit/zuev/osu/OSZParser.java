package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.os.Environment;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileNotFoundException;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

/**
 * Class for decompressing *.osz files and copying files into application
 * directory;
 **/
public class OSZParser {
    private OSZParser() {
    }

    /**
     * Parse an *.osz file. Location of decompressed files depends of settings
     * and storage availability.
     */
    public static boolean parseOSZ(final Activity activity,
                                   final String filename) {
        final File osz = new File(filename);

        // Checking if we can use SD card for storage
        if (!canUseSD()) {
            return false;
        }

        ToastLogger.addToLog("Importing " + filename);
        // Getting file name without extention ^_^
        String folderName = osz.getName();
        folderName = folderName.substring(0, folderName.length() - 4);
        final File folderFile = new File(Config.getBeatmapPath() + folderName);
        if(!folderFile.exists()) {
            folderFile.mkdirs();
        }

        try {
            new ZipFile(osz).extractAll(folderFile.getAbsolutePath());
            // Adding to library
            LibraryManager.getInstance().addBeatmap(
                    new File(Config.getBeatmapPath() + folderName), null);
        } catch (final ZipException e) {
            Debug.e("OSZParser.ParseOSZ: " + e.getMessage(), e);
            return false;
        } catch (final Exception e) {
            ToastLogger.showText(
                    StringTable.format(R.string.message_error, e.getMessage()),
                    false);
            Debug.e("OSZParser.ParseOSZ: ", e);
            osz.renameTo(new File(osz.getParentFile(), osz.getName() + ".badosz"));
            LibraryManager.getInstance().deleteDir(folderFile);
            return false;
        }

        // And trying to delete .osz
        if (Config.isDELETE_OSZ()) {
            osz.delete();
        }

        return true;
    }

    public static boolean canUseSD() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED_READ_ONLY)) {
                ToastLogger.showText(
                        StringTable.get(R.string.message_error_sdcardread),
                        false);
            } else {
                ToastLogger.showText(
                        StringTable.get(R.string.message_error_sdcard), false);
            }
        }

        return false;
    }
}
