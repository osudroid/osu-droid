package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.os.Environment;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        ZipInputStream istream;

        // Checking if we can use SD card for storage
        if (canUseSD() == false) {
            return false;
        }

        ToastLogger.addToLog("Importing " + filename);
        // Getting file name without extention ^_^
        String folderName = osz.getName();
        folderName = folderName.substring(0, folderName.length() - 4);
        ArrayList<File> list = new ArrayList<>();
        final File folderFile = new File(Config.getBeatmapPath() + folderName);
        if (folderFile.mkdirs()) {
            list.add(folderFile);
        }


        try {
            // Trying read file as zip-archive
            istream = new ZipInputStream(new FileInputStream(osz));
            // For each entry in archive
            for (ZipEntry entry = istream.getNextEntry(); entry != null; entry = istream
                    .getNextEntry()) {
                //if (entry.getName().matches(".*[.]avi")
                //		|| entry.getName().matches(".*[.]flv")) {
                //	continue;
                //}
                // Opening a file according to entry information
                final File entryFile = new File(folderFile, entry.getName());
                if (entryFile.getParent() != null) {
                    final File entryFolder = new File(entryFile.getParent());
                    if (entryFolder.exists() == false) {
                        if (entryFolder.mkdirs()) {
                            list.add(entryFile);
                        }
                    }
                }
                if (entryFile.createNewFile()) {
                    list.add(entryFile);
                }
                final FileOutputStream entryStream = new FileOutputStream(
                        entryFile);

                // Writing data from zip stream to output stream
                final byte[] buff = new byte[4096];
                int len = 0;
                while ((len = istream.read(buff)) > 0) {
                    entryStream.write(buff, 0, len);
                }
                // Closing output stream
                entryStream.close();
            }
            // Closing .osz file stream
            istream.close();

            // Adding to library
            LibraryManager.getInstance().addBeatmap(
                    new File(Config.getBeatmapPath() + folderName), null);

        } catch (final FileNotFoundException e) {
            Debug.e("OSZParser.ParseOSZ: " + e.getMessage(), e);
            return false;
        } catch (final Exception e) {
            ToastLogger.showText(
                    StringTable.format(R.string.message_error, e.getMessage()),
                    false);
            Debug.e("OSZParser.ParseOSZ: ", e);
            osz.renameTo(new File(osz.getParentFile(), osz.getName() + ".badosz"));
            for (int i = list.size() - 1; i >= 0; i--) {
                list.get(i).delete();
            }
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
