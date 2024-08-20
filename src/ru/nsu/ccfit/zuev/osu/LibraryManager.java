package ru.nsu.ccfit.zuev.osu;

import static com.reco1l.osu.data.Beatmaps.BeatmapInfo;

import android.util.Log;
import com.reco1l.osu.data.BeatmapSetInfo;
import com.reco1l.osu.data.DatabaseManager;
import com.reco1l.osu.graphics.VideoTexture;
import com.rian.osu.beatmap.parser.BeatmapParser;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.Nullable;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

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


    private LibraryManager() {
    }


    private static boolean checkDirectory(String path) {

        var directory = new File(path);

        if (!directory.exists()) {

            if (!directory.mkdir()) {
                ToastLogger.showText(StringTable.format(R.string.message_error_createdir, directory.getPath()), true);
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
    }

    /**
     * Scan the beatmap directory to find differences between the database and the file system.
     */
    public static void scanDirectory() {

        if (!checkDirectory(Config.getBeatmapPath())) {
            return;
        }

        var directory = new File(Config.getBeatmapPath());
        var files = directory.listFiles();

        if (files == null) {
            return;
        }

        new LibraryDatabaseManager(files.length, files).start();

        // Wait for all threads to finish
        while (isCaching) {
            try {
                Thread.currentThread().wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e("LibraryManager", "Failed to wait for thread termination", e);
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
        DatabaseManager.getBeatmapInfoTable().deleteBeatmapSet(beatmapSet.getPath());
        loadLibrary();
    }

    private static void scanBeatmapSetFolder(File directory) {

        var files = directory.listFiles((dir, name) -> name.endsWith(".osu"));

        if (files == null) {
            return;
        }

        var beatmapsFound = 0;

        for (var file : files) {

            try (var parser = new BeatmapParser(file)) {

                var beatmap = parser.parse(true);

                if (beatmap == null) {
                    if (Config.isDeleteUnimportedBeatmaps()) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                    continue;
                }

                var beatmapInfo = BeatmapInfo(beatmap, directory.getPath(), directory.lastModified(), file.getPath());

                if (beatmap.events.videoFilename != null && Config.isDeleteUnsupportedVideos()) {
                    try {
                        var videoFile = new File(beatmapInfo.getPath(), beatmap.events.videoFilename);

                        if (!VideoTexture.Companion.isSupportedVideo(videoFile)) {
                            //noinspection ResultOfMethodCallIgnored
                            videoFile.delete();
                        }
                    } catch (Exception e) {
                        Log.e("LibraryManager", "Failed to delete video file", e);
                    }
                }

                try {
                    // Conflict strategy is set to replace when the primary key is already in the
                    // database. But that should never happen because the path is the primary key.
                    DatabaseManager.getBeatmapInfoTable().insert(beatmapInfo);
                } catch (Exception e) {
                    Log.e("LibraryManager", "Failed to insert beatmap into database", e);
                }

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

            if (library.get(i).getPath().equals(info.getParentPath())) {
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


        private final File[] files;

        private final List<String> savedPaths;

        private final ExecutorService executors;


        private int fileCount;

        private int fileCached = 0;


        private LibraryDatabaseManager(int fileCount, File[] files) {

            this.fileCount = fileCount;
            this.files = files;
            this.executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            this.savedPaths = DatabaseManager.getBeatmapInfoTable().getBeatmapSetPaths();
        }


        public void start() {

            int optimalChunkSize = (int) Math.ceil((double) fileCount / Runtime.getRuntime().availableProcessors());

            for (int i = 0; i < files.length; i += optimalChunkSize) {
                submitToExecutor(Arrays.copyOfRange(files, i, Math.min(i + optimalChunkSize, files.length)));
            }

            executors.shutdown();

            try {

                if (executors.awaitTermination(1, TimeUnit.HOURS)) {

                    isCaching = false;

                    synchronized (LibraryManager.class) {
                        LibraryManager.class.notify();
                    }

                } else {
                    Log.e("LibraryManager", "Timeout");
                }

            } catch (InterruptedException e) {
                Log.e("LibraryManager", "Failed to wait for executor termination", e);
            }

            // Removing beatmap sets from the database that are not in the library anymore.
            for (int i = savedPaths.size() - 1; i >= 0; i--) {

                var path = savedPaths.get(i);
                var found = false;

                for (int j = files.length - 1; j >= 0; j--) {

                    if (path.equals(files[j].getPath())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    DatabaseManager.getBeatmapInfoTable().deleteBeatmapSet(path);
                }
            }
        }

        private void submitToExecutor(File[] files) {

            executors.submit(() -> {

                for (int i = files.length - 1; i >= 0; i--) {
                    var file = files[i];

                    if (!file.isDirectory()) {
                        continue;
                    }

                    var found = false;
                    for (int j = savedPaths.size() - 1; j >= 0; j--) {
                        if (savedPaths.get(j).equals(file.getPath())) {
                            fileCount--;
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        continue;
                    }

                    GlobalManager.getInstance().setLoadingProgress(50 + 50 * fileCached / fileCount);
                    GlobalManager.getInstance().setInfo("Loading " + file.getName() + "...");

                    ToastLogger.setPercentage(fileCached * 100f / fileCount);
                    fileCached++;

                    scanBeatmapSetFolder(file);
                }
            });
        }

    }
}
