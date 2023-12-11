package ru.nsu.ccfit.zuev.osu;

import android.os.Build;
import com.reco1l.legacy.engine.VideoTexture;
import org.anddev.andengine.util.Debug;
import org.jetbrains.annotations.Nullable;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public enum LibraryManager {
    INSTANCE;

    private static final String VERSION = "library4.2";

    private static final List<BeatmapInfo> library = Collections.synchronizedList(new ArrayList<>());

    private static boolean isCaching = true;

    private Integer fileCount = 0;

    private int currentIndex = 0;

    public static void deleteDir(final File dir) {
        if (dir.exists() && dir.isDirectory()) {
            final File[] files = FileUtils.listFiles(dir);
            if (files == null) {
                return;
            }
            for (final File f : files) {
                if (f.isDirectory()) {
                    deleteDir(f);
                } else if (f.delete()) {
                    Debug.i(f.getPath() + " deleted");
                }
            }
            if (dir.delete()) {
                Debug.i(dir.getPath() + " deleted");
            }
        }
    }

    private static void fillEmptyFields(BeatmapInfo info) {
        info.setCreator(info.getTrack(0).getCreator());
        if (info.getTitle().isEmpty()) {
            info.setTitle("unknown");
        }
        if (info.getArtist().isEmpty()) {
            info.setArtist("unknown");
        }
        if (info.getCreator().isEmpty()) {
            info.setCreator("unknown");
        }
    }

    private static void scanFolder(final BeatmapInfo info) {
        final File dir = new File(info.getPath());
        info.setDate(dir.lastModified());
        File[] filelist = FileUtils.listFiles(dir, ".osu");

        if (filelist == null) {
            return;
        }
        for (final File file : filelist) {
            final BeatmapParser parser = new BeatmapParser(file);
            if (!parser.openFile()) {
                if (Config.isDeleteUnimportedBeatmaps()) {
                    file.delete();
                }
                continue;
            }

            final TrackInfo track = new TrackInfo(info);
            track.setFilename(file.getPath());
            track.setCreator("unknown");

            final BeatmapData data = parser.parse(true);
            if (data == null || !data.populateMetadata(info) || !data.populateMetadata(track)) {
                if (Config.isDeleteUnimportedBeatmaps()) {
                    file.delete();
                }
                continue;
            }

            if (data.events.videoFilename != null && Config.isDeleteUnsupportedVideos()) {
                try {
                    var videoFile = new File(info.getPath(), data.events.videoFilename);

                    if (!VideoTexture.Companion.isSupportedVideo(videoFile)) {
                        //noinspection ResultOfMethodCallIgnored
                        videoFile.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            info.addTrack(track);
        }

        if (Config.isDeleteUnimportedBeatmaps() && info.getCount() == 0) {
            deleteDir(dir);
        }

        Collections.sort(info.getTracks(), (object1, object2) -> Float.compare(object1.getDifficulty(), object2.getDifficulty()));
    }

    public File getLibraryCacheFile() {
        return new File(GlobalManager.getInstance().getMainActivity().getFilesDir(), String.format("library.%s.dat", VERSION));
    }

    @SuppressWarnings("unchecked")
    public boolean loadLibraryCache(boolean forceUpdate) {
        synchronized (library) {
            library.clear();
        }

        ToastLogger.addToLog("Loading library...");
        if (!FileUtils.canUseSD()) {
            ToastLogger.addToLog("Can't use SD card!");
            return true;
        }

        final File replayDir = new File(Config.getScorePath());
        if (!replayDir.exists()) {
            if (!replayDir.mkdir()) {
                ToastLogger.showText(StringTable.format(R.string.message_error_createdir, replayDir.getPath()), true);
                return false;
            }
            createNoMediaFile(replayDir);
        }

        final File lib = getLibraryCacheFile();
        final File dir = new File(Config.getBeatmapPath());
        if (!dir.exists()) {
            return false;
        }
        try {
            if (lib.createNewFile()) {
                Debug.i("LibraryManager: create library cache file");
            } else {
                Debug.i("LibraryManager: library cache file already exists");
            }
        } catch (final IOException e) {
            Debug.e("LibraryManager: " + e.getMessage(), e);
        }

        try (final ObjectInputStream istream = new ObjectInputStream(new FileInputStream(lib))) {
            Object obj = istream.readObject();
            if (obj instanceof String) {
                if (!obj.equals(VERSION)) {
                    return false;
                }

                obj = istream.readObject();
                if (obj instanceof Integer) {
                    fileCount = (Integer) obj;

                    obj = istream.readObject();
                    if (obj instanceof Collection<?>) {
                        synchronized (library) {
                            library.addAll((Collection<? extends BeatmapInfo>) obj);
                        }

                        ToastLogger.addToLog("Library loaded");
                        if (forceUpdate) {
                            checkLibrary();
                        }
                        return true;
                    }
                }
            }
        } catch (final IOException | ClassNotFoundException | ClassCastException e) {
            Debug.e("LibraryManager: " + e.getMessage(), e);
        }
        ToastLogger.addToLog("Cannot load library!");
        return false;
    }

    private void checkLibrary() {
        final File dir = new File(Config.getBeatmapPath());
        final File[] files = FileUtils.listFiles(dir);
        if (files.length == fileCount) {
            return;
        }

        ToastLogger.showText(StringTable.get(R.string.message_lib_update), true);

        final int fileCount = files.length;
        LibraryCacheManager manager = new LibraryCacheManager(fileCount, files);
        manager.addUncachedBeatmaps();

        while (isCaching) {
            try {
                this.wait();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                Debug.e("LibraryManager: " + e.getMessage(), e);
            }
        }
        isCaching = true;

        this.fileCount = files.length;
        saveToCache();
    }

    public synchronized void scanLibrary() {
        ToastLogger.addToLog("Caching library...");
        library.clear();

        final File dir = new File(Config.getBeatmapPath());
        // Creating Osu directory if it doesn't exist
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                ToastLogger.showText(StringTable.format(R.string.message_error_createdir, dir.getPath()), true);
                return;
            }
            createNoMediaFile(dir);
            return;
        }
        // Getting all files
        final File[] filelist = FileUtils.listFiles(dir);

        // Here we go!
        this.fileCount = filelist.length;

        Debug.i("LibraryManager: Operating in multithreaded mode");
        LibraryCacheManager manager = new LibraryCacheManager(fileCount, filelist);
        manager.start();

        // Wait for all threads to finish
        while (isCaching) {
            try {
                this.wait();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                Debug.e("LibraryManager: " + e.getMessage(), e);
            }
        }
        isCaching = true;

        saveToCache();
        ToastLogger.showText(StringTable.format(R.string.message_lib_complete, manager.getTotalMaps()), true);
    }

    private void createNoMediaFile(File dir) {
        final File nomedia = new File(dir.getParentFile(), ".nomedia");
        try {
            if (nomedia.createNewFile()) {
                Debug.i("LibraryManager: create .nomedia file");
            } else {
                Debug.i("LibraryManager: .nomedia file already exists");
            }
        } catch (final IOException e) {
            Debug.e("LibraryManager: " + e.getMessage(), e);
        }
    }

    public void deleteMap(final BeatmapInfo info) {
        final File dir = new File(info.getPath());
        deleteDir(dir);

        synchronized (library) {
            library.remove(info);
        }
    }

    public void saveToCache() {
        if (library.isEmpty()) {
            return;
        }
        final File lib = getLibraryCacheFile();
        try (final ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(lib))) {
            lib.createNewFile();
            ostream.writeObject(VERSION);
            ostream.writeObject(fileCount);

            synchronized (library) {
                ostream.writeObject(library);
            }
        } catch (final IOException e) {
            ToastLogger.showText(StringTable.format(R.string.message_error, e.getMessage()), false);
            Debug.e("LibraryManager: " + e.getMessage(), e);
        }
        shuffleLibrary();
        currentIndex = 0;
    }

    public void clearCache() {
        final File lib = getLibraryCacheFile();
        if (lib.exists()) {
            lib.delete();
            ToastLogger.showText(StringTable.get(R.string.message_lib_cleared), false);
        }
        currentIndex = 0;
    }

    public List<BeatmapInfo> getLibrary() {
        synchronized (library) {
            return library;
        }
    }

    public void shuffleLibrary() {
        synchronized (library) {
            Collections.shuffle(library);
        }
    }

    public int getSizeOfBeatmaps() {
        synchronized (library) {
            return library.size();
        }
    }

    public BeatmapInfo getBeatmap() {
        return getBeatmapByIndex(currentIndex);
    }

    public BeatmapInfo getNextBeatmap() {
        return getBeatmapByIndex(++currentIndex);
    }

    public BeatmapInfo getPrevBeatmap() {
        return getBeatmapByIndex(--currentIndex);
    }

    public BeatmapInfo getBeatmapByIndex(int index) {
        synchronized (library) {
            Debug.i("Music Changing Info: Require index :" + index + "/" + library.size());
            if (library.isEmpty()) {
                return null;
            }
            if (index < 0 || index >= library.size()) {
                shuffleLibrary();
                currentIndex = 0;
                return library.get(0);
            } else {
                currentIndex = index;
                return library.get(index);
            }
        }
    }

    public int findBeatmap(BeatmapInfo info) {
        synchronized (library) {
            for (int i = 0; i < library.size(); i++) {
                if (library.get(i).equals(info)) {
                    return currentIndex = i;
                }
            }
        }
        return currentIndex = 0;
    }

    @Nullable
    public TrackInfo findTrackByMD5(String md5) {
        if (md5 == null) {
            return null;
        }

        synchronized (library) {
            int i = library.size() - 1;

            while (i >= 0) {
                var tracks = library.get(i).getTracks();

                int j = tracks.size() - 1;
                while (j >= 0) {
                    var track = tracks.get(j);

                    if (md5.equals(track.getMD5())) {
                        return track;
                    }
                    --j;
                }
                --i;
            }
        }
        return null;
    }

    public int findBeatmapById(int mapSetId) {
        synchronized (library) {
            for (int i = 0; i < library.size(); i++) {
                if (library.get(i).getTrack(0).getBeatmapSetID() == mapSetId) {
                    return currentIndex = i;
                }
            }
        }
        return currentIndex = 0;
    }

    public int getCurrentIndex() {
        return this.currentIndex;
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public TrackInfo findTrackByFileNameAndMD5(String fileName, String md5) {
        synchronized (library) {
            for (BeatmapInfo info : library) {
                for (int j = 0; j < info.getCount(); j++) {
                    TrackInfo track = info.getTrack(j);
                    File trackFile = new File(track.getFilename());
                    if (fileName.equals(trackFile.getName())) {
                        String trackMD5 = FileUtils.getMD5Checksum(trackFile);
                        if (md5.equals(trackMD5)) {
                            return track;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void updateLibrary(boolean force) {
        if (!loadLibraryCache(force)) {
            scanLibrary();
        }
        saveToCache();
    }

    private static final class LibraryCacheManager {

        private final int fileCount;

        private final ExecutorService executors;

        private final List<File> files;

        private volatile int fileCached = 0;

        private volatile int totalMaps = 0;

        private LibraryCacheManager(final int fileCount, final File[] files) {
            this.fileCount = fileCount;
            this.files = Arrays.asList(files);
            this.executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }

        public void start() {
            int optimalChunkSize = (int) Math.ceil((double) fileCount / Runtime.getRuntime().availableProcessors());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Split list into chunks of N elements per M sublist, N being number of files and M being number of processors
                List<List<File>> sub_files = new ArrayList<>(files.stream().collect(Collectors.groupingBy(s -> files.indexOf(s) / optimalChunkSize)).values());
                sub_files.parallelStream().forEach(this::submitToExecutor);
            } else {
                // Android versions below N don't support streams, so we have to do it the old-fashioned way
                for (int i = 0; i < files.size(); i += optimalChunkSize) {
                    submitToExecutor(files.subList(i, Math.min(i + optimalChunkSize, files.size())));
                }
            }

            executors.shutdown();
            try {
                if (executors.awaitTermination(1, TimeUnit.HOURS)) {
                    Debug.i("Library Cache: " + totalMaps + " maps loaded");
                    isCaching = false;

                    synchronized (LibraryManager.class) {
                        LibraryManager.class.notify();
                    }
                } else {
                    Debug.e("Library Cache: Timeout");
                }
            } catch (InterruptedException e) {
                Debug.e(e);
            }
        }

        public void addUncachedBeatmaps() {
            int optimalChunkSize = (int) Math.ceil((double) fileCount / Runtime.getRuntime().availableProcessors());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                List<List<File>> sub_files = new ArrayList<>(files.stream().collect(Collectors.groupingBy(s -> files.indexOf(s) / optimalChunkSize)).values());
                sub_files.parallelStream().forEach(this::submitToExecutorCheckCached);
            } else {
                for (int i = 0; i < files.size(); i += optimalChunkSize) {
                    submitToExecutorCheckCached(files.subList(i, Math.min(i + optimalChunkSize, files.size())));
                }
            }

            executors.shutdown();
            try {
                if (executors.awaitTermination(1, TimeUnit.HOURS)) {
                    Debug.i("Library Cache Updated");
                    isCaching = false;

                    synchronized (LibraryManager.class) {
                        LibraryManager.class.notify();
                    }
                } else {
                    Debug.e("Library Cache: Timeout");
                }
            } catch (InterruptedException e) {
                Debug.e(e);
            }

            // Remove all beatmaps that are no longer in the library
            synchronized (library) {
                var iterator = library.iterator();

                while (iterator.hasNext()) {
                    var beatmap = iterator.next();

                    if (!files.contains(new File(beatmap.getPath()))) {
                        iterator.remove();
                    }
                }
            }
        }

        private void submitToExecutorCheckCached(List<File> files) {
            executors.submit(() -> {
                for (final File file : files) {
                    GlobalManager.getInstance().setLoadingProgress(50 + 50 * fileCached / fileCount);
                    ToastLogger.setPercentage(fileCached * 100f / fileCount);

                    synchronized (this) {
                        fileCached++;
                    }

                    if (!file.isDirectory()) {
                        continue;
                    }

                    final BeatmapInfo info = new BeatmapInfo();
                    info.setPath(file.getPath());

                    synchronized (library) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            if (library.stream().anyMatch(i -> i.getPath().equals(info.getPath()))) {
                                continue;
                            }
                        } else {
                            boolean found = false;
                            for (final BeatmapInfo i : library) {
                                if (i.getPath().equals(info.getPath())) {
                                    found = true;
                                    break;
                                }
                            }

                            if (found) {
                                continue;
                            }
                        }
                    }

                    GlobalManager.getInstance().setInfo("Loading " + file.getName() + " ...");

                    scanFolder(info);
                    if (info.getCount() < 1) {
                        continue;
                    }

                    fillEmptyFields(info);

                    synchronized (library) {
                        library.add(info);
                    }
                }
            });
        }

        private void submitToExecutor(List<File> files) {
            executors.submit(() -> {
                for (File file : files) {
                    GlobalManager.getInstance().setLoadingProgress(50 + 50 * fileCached / fileCount);
                    ToastLogger.setPercentage(fileCached * 100f / fileCount);

                    synchronized (this) {
                        fileCached++;
                    }

                    if (!file.isDirectory()) {
                        continue;
                    }

                    GlobalManager.getInstance().setInfo("Loading " + file.getName() + "...");
                    final BeatmapInfo info = new BeatmapInfo();
                    info.setPath(file.getPath());
                    scanFolder(info);
                    if (info.getCount() < 1) {
                        continue;
                    }

                    fillEmptyFields(info);

                    synchronized (library) {
                        library.add(info);
                    }

                    synchronized (this) {
                        totalMaps += info.getCount();
                    }
                }
            });
        }

        public synchronized int getTotalMaps() {
            return totalMaps;
        }

    }
}
