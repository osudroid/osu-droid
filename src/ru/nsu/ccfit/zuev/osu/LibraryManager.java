package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;

import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
// import java.util.HashSet;
// import java.util.Set;

import ru.nsu.ccfit.zuev.osu.helper.MD5Calcuator;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

public class LibraryManager {
    private static final String VERSION = "library3.4";
    private static LibraryManager mgr = new LibraryManager();
    private ArrayList<BeatmapInfo> library;
    private Integer fileCount = 0;
    private int currentIndex = 0;

    private LibraryManager() {
    }

    public static LibraryManager getInstance() {
        return mgr;
    }

    public File getLibraryCacheFile() {
        return new File(GlobalManager.getInstance().getMainActivity().getFilesDir(), String.format("library.%s.dat", VERSION));
    }

    @SuppressWarnings("unchecked")
    public boolean loadLibraryCache(final Activity activity, boolean forceUpdate) {
        library = new ArrayList<BeatmapInfo>();
        ToastLogger.addToLog("Loading library...");
        if (!FileUtils.canUseSD()) {
            ToastLogger.addToLog("Can't use SD card!");
            return true;
        }

        final File replayDir = new File(Config.getScorePath());
        if (replayDir.exists() == false) {
            if (replayDir.mkdir() == false) {
                ToastLogger.showText(StringTable.format(
                        R.string.message_error_createdir, replayDir.getPath()), true);
                return false;
            }
            final File nomedia = new File(replayDir.getParentFile(), ".nomedia");
            try {
                nomedia.createNewFile();
            } catch (final IOException e) {
                Debug.e("LibraryManager: " + e.getMessage(), e);
            }
        }

        final File lib = getLibraryCacheFile();
        //Log.i("ed-d", "load cache from " + lib.getAbsolutePath());
        final File dir = new File(Config.getBeatmapPath());
        if (!dir.exists()) {
            return false;
        }
        try {
            if (!lib.exists()) {
                lib.createNewFile();
            }
            final ObjectInputStream istream = new ObjectInputStream(
                    new FileInputStream(lib));
            Object obj = istream.readObject();
            //Log.i("ed-d", "load cache step 1");
            if (obj instanceof String) {
                if (((String) obj).equals(VERSION) == false) {
                    istream.close();
                    return false;
                }
            } else {
                istream.close();
                return false;
            }
            //Log.i("ed-d", "load cache step 2");
            obj = istream.readObject();
            if (obj instanceof Integer) {
                fileCount = (Integer) obj;
            } else {
                istream.close();
                return false;
            }
            //Log.i("ed-d", "load cache step 3");
            obj = istream.readObject();
            if (obj instanceof ArrayList<?>) {
                //Log.i("ed-d", "load cache step 4");
                library = (ArrayList<BeatmapInfo>) obj;
                istream.close();
                ToastLogger.addToLog("Library loaded");
                //Log.i("ed-d", "load cache step 5");
                //for (BeatmapInfo info : library) {
                //	Log.i("ed-d", "cache : " + info.getPath());
                //}
                if (forceUpdate) {
                    //Log.i("ed-d", "load cache step 6");
                    checkLibrary(activity);
                    //Log.i("ed-d", "load cache step 7");
                }
                return true;
            }
            istream.close();
            //Log.i("ed-d", "load cache");
            return false;
        } catch (final FileNotFoundException e) {
            Debug.e("LibraryManager: " + e.getMessage(), e);
        } catch (final IOException e) {
            Debug.e("LibraryManager: " + e.getMessage(), e);
        } catch (final ClassNotFoundException e) {
            Debug.e("LibraryManager: " + e.getMessage(), e);
        } catch (final ClassCastException e) {
            Debug.e("LibraryManager: " + e.getMessage(), e);
        }
        ToastLogger.addToLog("Cannot load library!");
        return false;
    }

    private void checkLibrary(final Activity activity) {
        final File dir = new File(Config.getBeatmapPath());
        final File[] files = FileUtils.listFiles(dir);
        if (files.length == fileCount) {
            return;
        }
        ToastLogger
                .showText(StringTable.get(R.string.message_lib_update), true);
        final int fileCount = files.length;
        int fileCached = 0;
        LinkedList<String> cachedFiles = new LinkedList<String>();
        for (final File f : files) {
            GlobalManager.getInstance().setLoadingProgress(50 + 50 * fileCached / fileCount);
            ToastLogger.setPercentage(fileCached * 100f / fileCount);
            fileCached++;
            addBeatmap(f, cachedFiles);
        }
        final LinkedList<BeatmapInfo> uncached = new LinkedList<BeatmapInfo>();
        for (final BeatmapInfo i : library) {
            if (!cachedFiles.contains(i.getPath())) {
                uncached.add(i);
            }
        }
        for (final BeatmapInfo i : uncached) {
            library.remove(i);
        }
        this.fileCount = files.length;
        savetoCache(activity);
    }

    public void scanLibrary(final Activity activity) {
        //ToastLogger.showText(StringTable.get(R.string.message_lib_caching),
        //		false);
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
            final File nomedia = new File(dir.getParentFile(), ".nomedia");
            try {
                nomedia.createNewFile();
            } catch (final IOException e) {
                Debug.e("LibraryManager: " + e.getMessage(), e);
            }
            return;
        }
        // Getting all files
        int totalMaps = 0;
        final File[] filelist = FileUtils.listFiles(dir);
        // Here we go!
        final int fileCount = filelist.length;
        this.fileCount = fileCount;
        int fileCached = 0;
        for (final File file : filelist) {
            GlobalManager.getInstance().setLoadingProgress(50 + 50 * fileCached / fileCount);
            ToastLogger.setPercentage(fileCached * 100f / fileCount);
            fileCached++;
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
            library.add(info);
            totalMaps += info.getCount();
            // totalMaps += loadFolder(file);
        }

        // sort();

        savetoCache(activity);
        ToastLogger.showText(
                StringTable.format(R.string.message_lib_complete, totalMaps),
                true);
    }

    /* 
    public int loadFolder(File folder) {
        final BeatmapInfo info = new BeatmapInfo();
        info.setPath(folder.getPath());
        scanFolder(info);
        if (info.getCount() == 0) {
            return 0;
        }

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
        library.add(info);
        return info.getCount();
    }*/ 

    // NOTE: IS THIS EVEN USED?????
    public void updateMapSet(File folder, BeatmapInfo beatmapInfo) {
        library.remove(beatmapInfo);
        // loadFolder(folder);
        savetoCache(GlobalManager.getInstance().getMainActivity());
    }

    /* public void sort() {
        *
         * Collections.sort(library, new Comparator<BeatmapInfo>() {
         *
         *  public int compare(final BeatmapInfo object1, final
         * BeatmapInfo object2) { return object1.getTitle().compareToIgnoreCase(
         * object2.getTitle()); } });
         *
    }*/

    public void deleteDir(final File dir) {
        if (dir.exists() == false || dir.isDirectory() == false) {
            return;
        }
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

    public void deleteMap(final BeatmapInfo info) {
        final File dir = new File(info.getPath());
        deleteDir(dir);
        if (library != null) {
            library.remove(info);
        }
    }

    public void savetoCache(final Activity activity) {        
        if (library.isEmpty()) {
            return;
        }
        final File lib = getLibraryCacheFile();
        try {
            if (!lib.exists()) {
                lib.createNewFile();
            }
            final ObjectOutputStream ostream = new ObjectOutputStream(
                    new FileOutputStream(lib));
            ostream.writeObject(VERSION);
            ostream.writeObject(fileCount);
            ostream.writeObject(library);
            ostream.close();
        } catch (final FileNotFoundException e) {
            ToastLogger.showText(
                    StringTable.format(R.string.message_error, e.getMessage()),
                    false);
            Debug.e("LibraryManager: " + e.getMessage(), e);
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
        if (file.isDirectory() == false) {
            return;
        }
        GlobalManager.getInstance().setInfo("Loading " + file.getName() + " ...");
        final BeatmapInfo info = new BeatmapInfo();
        info.setPath(file.getPath());
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
        //Log.i("ed-d", "not found " + info.getPath());
        if (cachedFiles != null) {
            cachedFiles.add(info.getPath());
        }

        scanFolder(info);
        if (info.getCount() == 0) {
            return;
        }

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

        library.add(info);
    }

    private void scanFolder(final BeatmapInfo info) {        
        final File dir = new File(info.getPath());
        info.setDate(dir.lastModified());
        File[] filelist = FileUtils.listFiles(dir, ".osu");

        if (filelist == null) {
            return;
        }
        for (final File file : filelist) {
            final OSUParser parser = new OSUParser(file);
            if (!parser.openFile()) {
                continue;
            }

            final TrackInfo track = new TrackInfo(info);
            track.setFilename(file.getPath());
            track.setCreator("unknown");
            if (!parser.readMetaData(track, info)) {
                continue;
            }
            if (track.getBackground() != null) {
                track.setBackground(info.getPath() + "/"
                        + track.getBackground());
            }
            info.addTrack(track);
        }

        Collections.sort(info.getTracks(), new Comparator<TrackInfo>() {
            public int compare(final TrackInfo object1, final TrackInfo object2) {
                return Float.valueOf(object1.getDifficulty()).compareTo(object2.getDifficulty());
            }
        });
    }

    public ArrayList<BeatmapInfo> getLibrary() {
        return library;
    }

    public void shuffleLibrary() {
        if (library != null) {
            Collections.shuffle(library);
        }
    }

    public int getSizeOfBeatmaps() {
        if (library != null) {
            return library.size();
        } else {
            return 0;
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
        Debug.i("Music Changing Info: Require index :" + index + "/" + library.size());
        if (library == null || library.size() <= 0) return null;
        if (index < 0 || index >= library.size()) {
            shuffleLibrary();
            currentIndex = 0;
            return library.get(0);
        } else {
            currentIndex = index;
            return library.get(index);
        }
    }

    public int findBeatmap(BeatmapInfo info) {
        if (library != null && library.size() > 0) {
            for (int i = 0; i < library.size(); i++) {
                if (library.get(i).equals(info)) {
                    return currentIndex = i;
                }
            }
        }
        return currentIndex = 0;
    }

    public int findBeatmapById(int mapSetId) {
        if (library != null && library.size() > 0) {
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
        if (library != null && library.size() > 0) {
            for (int i = 0; i < library.size(); i++) {
                BeatmapInfo info = library.get(i);
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
}
