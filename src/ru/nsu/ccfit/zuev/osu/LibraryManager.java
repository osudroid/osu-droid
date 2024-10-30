package ru.nsu.ccfit.zuev.osu;

import static com.reco1l.osu.data.BeatmapsKt.BeatmapInfo;

import android.util.Log;

import com.reco1l.osu.DifficultyCalculationManager;
import com.reco1l.osu.data.BeatmapSetInfo;
import com.reco1l.osu.data.DatabaseManager;
import com.reco1l.andengine.texture.VideoTexture;
import com.rian.osu.beatmap.parser.BeatmapParser;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.Nullable;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.reco1l.osu.data.BeatmapInfo;


public class LibraryManager {


    private static List<BeatmapSetInfo> library = new ArrayList<>();

    private static int currentIndex = 0;

    private static boolean isCaching = true;


    private static final ArrayList<BeatmapInfo> pendingBeatmaps = new ArrayList<>();

    private static final Object mutex = new Object();


    private LibraryManager() {
    }


    private static boolean checkDirectory(String path) {

        var directory = new File(path);

        if (!directory.exists()) {

            if (!directory.mkdir()) {
                ToastLogger.showText(StringTable.format(com.osudroid.resources.R.string.message_error_createdir, directory.getPath()), true);
                return false;
            }

            try {
                //noinspection ResultOfMethodCallIgnored
                new File(directory, ".nomedia").createNewFile();

            } catch (IOException e) {
                Log.e("LibraryManager", "Failed to create .nomedia file", e);
            }
        }

        return true;
    }


    public static void loadLibrary() {

        library = new ArrayList<>();

        if (!FileUtils.canUseSD() || !checkDirectory(Config.getScorePath()) && !checkDirectory(Config.getBeatmapPath())) {
            return;
        }

        currentIndex = 0;
        library = DatabaseManager.getBeatmapInfoTable().getBeatmapSetList();

        DifficultyCalculationManager.calculateDifficulties();
    }

    /**
     * Scan the beatmap directory to find differences between the database and the file system.
     */
    public static void scanDirectory() {

        if (!checkDirectory(Config.getBeatmapPath())) {
            return;
        }

        var directories = new File(Config.getBeatmapPath()).listFiles(File::isDirectory);

        if (directories == null) {
            return;
        }

        new LibraryDatabaseManager(directories).start();

        // Wait for all threads to finish
        while (isCaching) {
            synchronized (mutex) {
                try {
                    mutex.wait();
                } catch (InterruptedException e) {
                    Log.e("LibraryManager", "Failed to wait for thread termination", e);
                }
            }
        }

        isCaching = true;
    }


    public static void clearDatabase() {
        DatabaseManager.getBeatmapInfoTable().deleteAll();
        loadLibrary();
        currentIndex = 0;
    }

    public static void deleteBeatmapSet(BeatmapSetInfo beatmapSet) {
        FilesKt.deleteRecursively(new File(beatmapSet.getPath()));
        DatabaseManager.getBeatmapInfoTable().deleteBeatmapSet(beatmapSet.getDirectory());
        loadLibrary();
    }

    private static void scanBeatmapSetFolder(File directory) {

        var osuFiles = directory.listFiles((dir, name) -> name.endsWith(".osu"));

        if (osuFiles == null) {
            if (Config.isDeleteUnimportedBeatmaps()) {
                FilesKt.deleteRecursively(directory);
            }
            return;
        }

        var beatmapsFound = 0;

        for (var osuFile : osuFiles) {

            try (var parser = new BeatmapParser(osuFile)) {

                var data = parser.parse(false);

                if (data == null) {
                    if (Config.isDeleteUnimportedBeatmaps()) {
                        //noinspection ResultOfMethodCallIgnored
                        osuFile.delete();
                    }
                    continue;
                }

                var beatmapInfo = BeatmapInfo(data, directory.lastModified(), false);

                if (data.getEvents().videoFilename != null && Config.isDeleteUnsupportedVideos()) {
                    try {
                        var videoFile = new File(beatmapInfo.getSetDirectory(), data.getEvents().videoFilename);

                        if (!VideoTexture.Companion.isSupportedVideo(videoFile)) {
                            //noinspection ResultOfMethodCallIgnored
                            videoFile.delete();
                        }
                    } catch (Exception e) {
                        Log.e("LibraryManager", "Failed to delete video file", e);
                    }
                }

                pendingBeatmaps.add(beatmapInfo);
                beatmapsFound++;
            }
        }

        if (Config.isDeleteUnimportedBeatmaps() && beatmapsFound == 0) {
            FilesKt.deleteRecursively(directory);
        }
    }


    public static List<BeatmapSetInfo> getLibrary() {
        return library;
    }

    public static void shuffleLibrary() {
        Collections.shuffle(library);
        currentIndex = 0;
    }

    public static int getSizeOfBeatmaps() {
        return library.size();
    }


    public static BeatmapSetInfo getCurrentBeatmapSet() {
        return library.get(currentIndex);
    }

    public static BeatmapSetInfo selectNextBeatmapSet() {

        if (library.isEmpty()) {
            currentIndex = 0;
            return null;
        }

        currentIndex = ++currentIndex % library.size();
        return library.get(currentIndex);
    }

    public static BeatmapSetInfo selectPreviousBeatmapSet() {

        if (library.isEmpty()) {
            currentIndex = 0;
            return null;
        }

        currentIndex = currentIndex == 0 ? library.size() - 1 : --currentIndex;
        return library.get(currentIndex);
    }


    public static void findBeatmapSetIndex(BeatmapInfo info) {

        for (int i = 0; i < library.size(); i++) {

            if (library.get(i).getDirectory().equals(info.getSetDirectory())) {
                currentIndex = i;
                return;
            }
        }

        currentIndex = 0;
    }


    @Nullable
    public static BeatmapInfo findBeatmapByMD5(String md5) {

        if (md5 == null) {
            return null;
        }

        for (int i = library.size() - 1; i >= 0; i--) {
            var beatmaps = library.get(i).getBeatmaps();

            for (int j = beatmaps.size() - 1; j >= 0; j--) {
                var beatmap = beatmaps.get(j);

                if (md5.equals(beatmap.getMD5())) {
                    return beatmap;
                }
            }
        }

        return null;
    }


    private static final class LibraryDatabaseManager {


        private final File[] directories;

        private final ExecutorService executors;


        private int directoryCount;

        private int fileCached = 0;


        private LibraryDatabaseManager(File[] directories) {

            this.directories = directories;
            this.directoryCount = directories.length;
            this.executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }


        public void start() {

            int optimalChunkSize = (int) Math.ceil((double) directoryCount / Runtime.getRuntime().availableProcessors());

            for (int i = 0; i < directories.length; i += optimalChunkSize) {

                var directoriesSlice = Arrays.copyOfRange(directories, i, Math.min(i + optimalChunkSize, directories.length));

                executors.submit(() -> {

                    for (int i1 = directoriesSlice.length - 1; i1 >= 0; i1--) {

                        var directory = directoriesSlice[i1];

                        if (DatabaseManager.getBeatmapInfoTable().isBeatmapSetImported(directory.getName())) {
                            directoryCount--;
                            continue;
                        }

                        GlobalManager.getInstance().setLoadingProgress(50 + 50 * fileCached / directoryCount);
                        GlobalManager.getInstance().setInfo("Loading " + directory.getName() + "...");

                        ToastLogger.setPercentage(fileCached * 100f / directoryCount);
                        fileCached++;

                        scanBeatmapSetFolder(directory);
                    }
                });
            }

            executors.shutdown();

            try {
                if (executors.awaitTermination(1, TimeUnit.HOURS)) {

                    isCaching = false;

                    synchronized (mutex) {
                        mutex.notify();
                    }

                } else {
                    Log.e("LibraryManager", "Timeout while waiting for executor termination.");
                }

            } catch (InterruptedException e) {
                Log.e("LibraryManager", "Failed while waiting for executor termination.", e);
            }

            try {
                DatabaseManager.getBeatmapInfoTable().insertAll(pendingBeatmaps);
                pendingBeatmaps.clear();
            } catch (Exception e) {
                Log.e("LibraryManager", "Failed to insert beatmaps into database.", e);
                return;
            }

            var missingDirectories = new ArrayList<>(DatabaseManager.getBeatmapInfoTable().getBeatmapSetPaths());

            for (var i = missingDirectories.size() - 1; i >= 0; i--) {

                if (new File(Config.getBeatmapPath(), missingDirectories.get(i)).exists()) {
                    missingDirectories.remove(i);
                }
            }

            if (!missingDirectories.isEmpty()) {
                DatabaseManager.getBeatmapInfoTable().deleteAllBeatmapSets(missingDirectories);
            }
        }

    }
}
