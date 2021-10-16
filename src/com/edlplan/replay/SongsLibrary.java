package com.edlplan.replay;

import java.io.File;
import java.util.HashMap;

import ru.nsu.ccfit.zuev.osu.helper.FileUtils;

public class SongsLibrary {

    private static SongsLibrary library;
    private HashMap<String, String> osu2set = new HashMap<>();

    public SongsLibrary() {
        File songs = OdrConfig.getSongDir();
        File[] songsList = FileUtils.listFiles(songs, ".osu");
        for (File set : songsList) {
            if (set.isDirectory()) {
                for (String osu : set.list()) {
                    osu2set.put(osu, set.getName() + "/" + osu);
                }
            }
        }
    }

    public static SongsLibrary get() {
        if (library == null) {
            library = new SongsLibrary();
        }
        return library;
    }

    public String toSetLocal(String raw) {
        String osu = raw.substring(raw.indexOf("/") + 1, raw.length());
        if (osu2set.containsKey(osu)) {
            return osu2set.get(osu);
        } else {
            return raw;
        }
    }


}
