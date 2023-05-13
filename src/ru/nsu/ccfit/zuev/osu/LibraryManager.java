package ru.nsu.ccfit.zuev.osu;

import android.os.Build;
import org.anddev.andengine.util.Debug;
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
    private static final String VERSION = "library4.0";
    private static final List<BeatmapInfo> library = Collections.synchronizedList(new ArrayList<>());
    private Integer fileCount = 0;
    private int currentIndex = 0;

    private static boolean isCaching = true;

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
                ToastLogger.showText(StringTable.format(
                        R.string.message_error_createdir, replayDir.getPath()), true);
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
//                        library = (ArrayList<BeatmapInfo>) obj;
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
        int fileCached = 0;
        LinkedList<String> cachedFiles = new LinkedList<>();
        for (final File f : files) {
            GlobalManager.getInstance().setLoadingProgress(50 + 50 * fileCached / fileCount);
            ToastLogger.setPercentage(fileCached * 100f / fileCount);
            fileCached++;
            addBeatmap(f, cachedFiles);
        }

        final LinkedList<BeatmapInfo> uncached = new LinkedList<>();

        synchronized (library) {
            for (final BeatmapInfo i : library) {
                if (!cachedFiles.contains(i.getPath())) {
                    uncached.add(i);
                }
            }
            for (final BeatmapInfo i : uncached) {
                library.remove(i);
            }
        }

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
                ToastLogger.showText(StringTable.format(
                        R.string.message_error_createdir, dir.getPath()), true);
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
        ToastLogger.showText(
                StringTable.format(R.string.message_lib_complete, manager.getTotalMaps()),
                true);
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
        try (final ObjectOutputStream ostream = new ObjectOutputStream(
                new FileOutputStream(lib))) {
            lib.createNewFile();
            ostream.writeObject(VERSION);
            ostream.writeObject(fileCount);

            synchronized (library) {
                ostream.writeObject(library);
            }
        } catch (final IOException e) {
            ToastLogger.showText(
                    StringTable.format(R.string.message_error, e.getMessage()),
                    false);
            Debug.e("LibraryManager: " + e.getMessage(), e);
        }
        shuffleLibrary();
        currentIndex = 0;
    }

    public void clearCache() {
        final File lib = getLibraryCacheFile();
        if (lib.exists()) {
            lib.delete();
            ToastLogger.showText(StringTable.get(R.string.message_lib_cleared),
                    false);
        }
        currentIndex = 0;
    }

    public void addBeatmap(final File file, LinkedList<String> cachedFiles) {
        if (!file.isDirectory()) {
            return;
        }
        GlobalManager.getInstance().setInfo("Loading " + file.getName() + " ...");
        final BeatmapInfo info = new BeatmapInfo();
        info.setPath(file.getPath());
        synchronized (library) {
            for (final BeatmapInfo i : library) {
                if (i.getPath().substring(i.getPath().lastIndexOf('/'))
                        .equals(info.getPath().substring(info.getPath().lastIndexOf('/')))) {
                    //Log.i("ed-d", "found " + i.getPath());
                    if (cachedFiles != null) {
                        cachedFiles.add(i.getPath());
                    }
                    return;
                }
            }
        }
        //Log.i("ed-d", "not found " + info.getPath());
        if (cachedFiles != null) {
            cachedFiles.add(info.getPath());
        }

        scanFolder(info);
        if (info.getCount() == 0) {
            return;
        }

        fillEmptyFields(info);

        synchronized (library) {
            library.add(info);
        }
    }

    private static void fillEmptyFields(BeatmapInfo info) {
        info.setCreator(info.getTrack(0).getCreator());
        if (info.getTitle().equals("")) {
            info.setTitle("unknown");
        }
        if (info.getArtist().equals("")) {
            info.setArtist("unknown");
        }
        if (info.getCreator().equals("")) {
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
            if (data == null || !data.populateMetadata(info, track)) {
                if (Config.isDeleteUnimportedBeatmaps()) {
                    file.delete();
                }
                continue;
            }
            if (track.getBackground() != null) {
                track.setBackground(info.getPath() + "/"
                        + track.getBackground());
            }
            info.addTrack(track);
        }

        if (Config.isDeleteUnimportedBeatmaps() && info.getCount() == 0) {
            deleteDir(dir);
        }

        Collections.sort(info.getTracks(), (object1, object2) -> Float.compare(object1.getDifficulty(), object2.getDifficulty()));
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
            if (library.size() == 0) return null;
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
            if (library.size() > 0) {
                for (int i = 0; i < library.size(); i++) {
                    if (library.get(i).equals(info)) {
                        return currentIndex = i;
                    }
                }
            }
        }
        return currentIndex = 0;
    }

    public int findBeatmapById(int mapSetId) {
        synchronized (library) {
            if (library.size() > 0) {
                for (int i = 0; i < library.size(); i++) {
                    if (library.get(i).getTrack(0).getBeatmapSetID() == mapSetId) {
                        return currentIndex = i;
                    }
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
            if (library.size() > 0) {
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
        }
        return null;
    }

    public void updateLibrary(boolean force)
    {
        saveToCache();

        if (!loadLibraryCache(force))
        {
            scanLibrary();
        }
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
            List<List<File>> sub_files;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Split list into chunks of N elements per M sublist, N being number of files and M being number of processors
                sub_files = new ArrayList<>(files.stream()
                        .collect(Collectors.groupingBy(s -> files.indexOf(s) / optimalChunkSize))
                        .values());
                sub_files.parallelStream().forEach(this::submitToExecutor);
            } else {
                // Android versions below N don't support streams, so we have to do it the old-fashioned way
                sub_files = new ArrayList<>();
                for (int i = 0; i < files.size(); i += optimalChunkSize) {
                    sub_files.add(files.subList(i, Math.min(i + optimalChunkSize, files.size())));
                }

                for (List<File> files : sub_files) {
                    submitToExecutor(files);
                }
            }

            executors.shutdown();
            try {
                if (executors.awaitTermination(10, TimeUnit.MINUTES)) {
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
