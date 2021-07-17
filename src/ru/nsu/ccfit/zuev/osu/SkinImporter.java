package ru.nsu.ccfit.zuev.osu;

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
 * A class for decompressing *.osk files and importing
 * them into skin directory.
 */
public class SkinImporter {
    private SkinImporter() {

    }

    /**
     * Imports an *.osk file.
     *
     * @param filePath The path to the *.osk file
     * @return Whether the import was successful.
     */
    public static boolean importSkin(final String filePath) {
        final File osk = new File(filePath);

        // Check if we can use SD card for storage
        if (!OSZParser.canUseSD()) {
            return false;
        }

        ToastLogger.addToLog("Importing " + filePath);

        final String folderName = osk.getName().substring(0, osk.getName().length() - 4);

        final ArrayList<File> list = new ArrayList<>();

        final File folderFile = new File(Config.getSkinTopPath() + folderName);

        if (folderFile.mkdirs()) {
            list.add(folderFile);
        }

        try {
            // Read file as a zip archive
            final ZipInputStream inputStream = new ZipInputStream(new FileInputStream(osk));

            for (ZipEntry entry = inputStream.getNextEntry(); entry != null; entry = inputStream
                    .getNextEntry()) {
                // Opening a file according to entry information
                final File entryFile = new File(folderFile, entry.getName());

                if (entry.isDirectory()) {
                    if (entryFile.mkdirs()) {
                        list.add(entryFile);
                    }
                } else if (entryFile.createNewFile()) {
                    list.add(entryFile);

                    final FileOutputStream entryStream = new FileOutputStream(entryFile);

                    // Writing data from zip stream to output stream
                    final byte[] buff = new byte[4096];
                    int len;
                    while ((len = inputStream.read(buff)) > 0) {
                        entryStream.write(buff, 0, len);
                    }

                    // Closing output stream
                    entryStream.close();
                }
            }

            // Closing .osk file stream
            inputStream.close();

            // Delete .osk file
            osk.delete();
        } catch (final FileNotFoundException e) {
            Debug.e("SkinImporter.importSkin: " + e.getMessage(), e);
            return false;
        } catch (final Exception e) {
            ToastLogger.showText(
                    StringTable.format(R.string.message_error, e.getMessage()),
                    false);
            Debug.e("SkinImporter.importSkin: ", e);
            osk.renameTo(new File(osk.getParentFile(), osk.getName() + ".badosk"));
            for (int i = list.size() - 1; i >= 0; i--) {
                list.get(i).delete();
            }
            return false;
        }

        return true;
    }
}
