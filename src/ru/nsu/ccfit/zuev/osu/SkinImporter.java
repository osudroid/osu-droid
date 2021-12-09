package ru.nsu.ccfit.zuev.osu;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileNotFoundException;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

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

        final File folderFile = new File(Config.getSkinTopPath() + folderName);
        if(!folderFile.exists()) {
            folderFile.mkdirs();
        }

        try {
            new ZipFile(osk).extractAll(folderFile.getAbsolutePath());
            // Delete .osk file
            osk.delete();
        } catch (final ZipException e) {
            Debug.e("SkinImporter.importSkin: " + e.getMessage(), e);
            return false;
        } catch (final Exception e) {
            ToastLogger.showText(
                    StringTable.format(R.string.message_error, e.getMessage()),
                    false);
            Debug.e("SkinImporter.importSkin: ", e);
            osk.renameTo(new File(osk.getParentFile(), osk.getName() + ".badosk"));
            LibraryManager.getInstance().deleteDir(folderFile);
            return false;
        }

        return true;
    }
}
