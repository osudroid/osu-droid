package com.reco1l.game;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

import android.os.Environment;
import android.util.Log;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.ui.data.GameNotification;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;

public class LibraryImport {

    public static boolean isConcurrentImport = false;
    
    //--------------------------------------------------------------------------------------------//

    public synchronized static void scan(boolean updateLibrary) {
        isConcurrentImport = true;

        ArrayList<File> foundFiles = new ArrayList<>();
        File mainDir = new File(Config.getCorePath());

        if (!mainDir.exists() || !mainDir.isDirectory()) {
            Log.e("BeatmapImport", "Main directory doesn't exist!");
            return;
        }

        File songsDir = new File(Config.getBeatmapPath());
        scanFolder(songsDir, foundFiles);

        if (Config.isSCAN_DOWNLOAD()) {
            File dlDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
            scanFolder(dlDir, foundFiles);
        }

        Log.i("BeatmapImport", "Scanning complete, found " + foundFiles.size() + " beatmaps");

        if (foundFiles.size() > 0) {
            for (int i = 0; i < foundFiles.size(); i++) {
                File beatmap = foundFiles.get(i);
                Log.i("BeatmapImport", "Importing " + beatmap.getName());
                Import(beatmap, false);
            }
        }
        if (updateLibrary) {
            updateLibrary();
        }
        isConcurrentImport = false;
    }

    public static void Import(File file, boolean saveCache) {
        if (!file.getName().toLowerCase().endsWith(".osz"))
            return;

        GameNotification notification = new GameNotification("Beatmap Import");

        notification.message = "Importing beatmap " + file.getName() + "...";
        notification.showCloseButton = false;

        UI.notificationCenter.add(notification);

        if (isValid(file)) {
            if (FileUtils.extractZip(file.getPath(), Config.getBeatmapPath())) {
                if (saveCache) {
                    Game.libraryManager.savetoCache(Game.activity);
                }
                notification.message = "Beatmap " + file.getName() + " successfully imported!";
                notification.showCloseButton = true;
                notification.update();
                return;
            }
        }
        notification.message = "Unable to import beatmap " + file.getName();
        notification.showCloseButton = true;
        notification.update();
    }

    public static void scanFolder(File directory, ArrayList<File> list) {
        if (!directory.exists() || !directory.isDirectory()) {
            Log.e("BeatmapImport", "Invalid directory: " + directory.getPath());
            return;
        }

        File[] files = FileUtils.listFiles(directory, ".osz");

        for (File file : files) {
            if (isValid(file)) {
                list.add(file);
                Log.i("BeatmapImport", "Found beatmap: " + file.getName());
            } else {
                Log.e("BeatmapImport", "Invalid beatmap file: " + file.getName());
            }
        }
    }

    private static boolean isValid(File file) {
        try (ZipFile zip = new ZipFile(file)) {
            if (zip.isValidZipFile()) {
                return true;
            } else {
                Log.e("BeatmapImport", file.getName() + " is not a valid ZIP file!");
                return false;
            }
        } catch (IOException e) {
            Log.e("BeatmapImport", "Error trying to check file " + file.getName() + "\n" + e.getMessage());
            return false;
        }
    }

    public static void updateLibrary() {
        Game.libraryManager.savetoCache(Game.activity);
        if (!Game.libraryManager.loadLibraryCache(Game.activity, true)) {
            Game.libraryManager.scanLibrary(Game.activity);
            System.gc();
        }
    }
}
