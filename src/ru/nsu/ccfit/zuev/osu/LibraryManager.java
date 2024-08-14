package ru.nsu.ccfit.zuev.osu;

import android.util.Log;
import com.reco1l.osu.BeatmapSetInfo;
import com.reco1l.osu.DatabaseManager;
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

import com.reco1l.osu.BeatmapInfo;


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
        library = DatabaseManager.getBeatmapTable().getBeatmapSetList();
    }

    public static void scanDirectory() {

        if (!checkDirectory(Config.getBeatmapPath())) {
            return;
        }

        var directory = new File(Config.getBeatmapPath());
        var files = FileUtils.listFiles(directory);

        new LibraryDatabaseManager(files.length, files).start(library);

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
        DatabaseManager.getBeatmapTable().deleteAll();
        loadLibrary();
        currentIndex = 0;
    }

    public static void deleteBeatmapSet(BeatmapSetInfo beatmapSet) {
        FilesKt.deleteRecursively(new File(beatmapSet.getPath()));
        DatabaseManager.getBeatmapTable().deleteBeatmapSet(beatmapSet.getPath());
        loadLibrary();
    }

    private static void scanBeatmapSetFolder(String path) {

        var directory = new File(path);
        var fileList = FileUtils.listFiles(directory, ".osu");

        if (fileList == null) {
            return;
        }

        // var parentKey = FileUtils.getMD5Checksum(directory);
        var beatmapsFound = 0;

        for (int i = library.size() - 1; i >= 0; --i) {
            if (library.get(i).getPath().equals(path /*parentKey*/)) {
                return;
            }
        }

        for (var file : fileList) {

            try (var parser = new BeatmapParser(file)) {

                var beatmap = parser.parse(true);

                if (beatmap == null) {
                    if (Config.isDeleteUnimportedBeatmaps()) {
                        file.delete();
                    }
                    continue;
                }

                // We can associate beatmap to beatmap sets by their beatmap set ID, but this will require a storage
                // system that ensures beatmap files are where they should be.
                // if (beatmap.metadata.beatmapSetId > 0) {
                //     parentKey = String.valueOf(beatmap.metadata.beatmapSetId);
                // }

                var beatmapInfo = BeatmapInfo.from(beatmap, path /*parentKey*/, directory.lastModified(), file.getPath());

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
                    DatabaseManager.getBeatmapTable().insert(beatmapInfo);
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
        GlobalManager.getInstance().setSelectedBeatmap(library.get(0).get(0));
    }

    public static int getSizeOfBeatmaps() {
        return library.size();
    }


    public static BeatmapSetInfo getCurrentBeatmapSet() {
        return library.get(currentIndex);
    }

    public static BeatmapSetInfo selectNextBeatmapSet() {

        if (library.isEmpty()) {
            GlobalManager.getInstance().setSelectedBeatmap(null);
            return null;
        }

        currentIndex = ++currentIndex % library.size();
        return library.get(currentIndex);
    }

    public static BeatmapSetInfo selectPreviousBeatmapSet() {

        if (library.isEmpty()) {
            return null;
        }

        currentIndex = currentIndex == 0 ? library.size() - 1 : --currentIndex;
        return library.get(currentIndex);
    }


    public static int findBeatmapSet(BeatmapSetInfo info) {

        for (int i = 0; i < library.size(); i++) {

            if (library.get(i).getPath().equals(info.getPath())) {
                return currentIndex = i;
            }
        }

        return currentIndex = 0;
    }


    @Nullable
    public static BeatmapInfo findBeatmapByMD5(String md5) {
        if (md5 == null)
            return null;

        int i = library.size() - 1;

        while (i >= 0) {
            var tracks = library.get(i).getBeatmaps();

            int j = tracks.size() - 1;
            while (j >= 0) {
                var track = tracks.get(j);

                if (md5.equals(track.getMD5()))
                    return track;
                --j;
            }
            --i;
        }
        return null;
    }


    private static final class LibraryDatabaseManager {

        private final ExecutorService executors;

        private final List<File> files;

        private final int fileCount;

        private volatile int fileCached = 0;


        private LibraryDatabaseManager(int fileCount, File[] files) {

            this.fileCount = fileCount;
            this.files = Arrays.asList(files);
            this.executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }


        public void start(List<BeatmapSetInfo> previousLibrary) {

            int optimalChunkSize = (int) Math.ceil((double) fileCount / Runtime.getRuntime().availableProcessors());

            for (int i = 0; i < files.size(); i += optimalChunkSize) {
                submitToExecutor(files.subList(i, Math.min(i + optimalChunkSize, files.size())));
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
            for (int i = previousLibrary.size() - 1; i >= 0; i--) {

                var beatmapSet = previousLibrary.get(i);
                var found = false;

                for (int j = files.size() - 1; j >= 0; j--) {

                    if (files.get(j).getPath().equals(beatmapSet.getPath())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    DatabaseManager.getBeatmapTable().deleteBeatmapSet(beatmapSet.getPath());
                }
            }
        }

        private void submitToExecutor(List<File> files) {

            executors.submit(() -> {

                for (var file : files) {

                    GlobalManager.getInstance().setLoadingProgress(50 + 50 * fileCached / fileCount);
                    GlobalManager.getInstance().setInfo("Loading " + file.getName() + "...");

                    ToastLogger.setPercentage(fileCached * 100f / fileCount);

                    synchronized (this) {
                        fileCached++;
                    }

                    if (!file.isDirectory()) {
                        continue;
                    }

                    scanBeatmapSetFolder(file.getPath());
                }
            });
        }

    }
}
