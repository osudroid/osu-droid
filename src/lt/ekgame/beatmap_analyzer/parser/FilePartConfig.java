package lt.ekgame.beatmap_analyzer.parser;

import java.util.HashMap;
import java.util.Map;

import lt.ekgame.beatmap_analyzer.Gamemode;

public class FilePartConfig {

    private FilePart part;
    private Map<String, String> values = new HashMap<>();

    public FilePartConfig(FilePart part) {
        this.part = part;
        for (String line : part.getLines()) {
            int seperator = line.indexOf(":");
            // I found one map file that has a stack trace in it (https://osu.ppy.sh/osu/326)
            // That's why I need this check
            if (seperator != -1) {
                String key = line.substring(0, seperator).trim();
                String value = line.substring(seperator + 1).trim();
                values.put(key, value);
            }
        }
    }

    public String getString(String name) throws BeatmapException {
        if (values.containsKey(name))
            return values.get(name);
        throw new BeatmapException("Can't find property \"" + name + "\" under [" + part.getTag() + "].");
    }

    public String getString(String name, String defaultValue) throws BeatmapException {
        if (hasProperty(name))
            return getString(name);
        return defaultValue;
    }

    public double getDouble(String name) throws BeatmapException {
        return Double.parseDouble(getString(name));
    }

    public int getInt(String name) throws BeatmapException {
        return Integer.parseInt(getString(name));
    }

    public boolean getBoolean(String name) throws BeatmapException {
        return Integer.parseInt(getString(name)) == 1;
    }

    public Gamemode getGamemode(String name) throws BeatmapException {
        return Gamemode.fromInt(getInt(name));
    }

    public double getDouble(String name, double defaultValue) throws BeatmapException {
        if (hasProperty(name))
            return getDouble(name);
        return defaultValue;
    }

    public int getInt(String name, int defaultValue) throws BeatmapException {
        if (hasProperty(name))
            return getInt(name);
        return defaultValue;
    }

    public boolean getBoolean(String name, boolean defaultValue) throws BeatmapException {
        if (hasProperty(name))
            return getBoolean(name);
        return defaultValue;
    }

    public Gamemode getGamemode(String name, Gamemode defaultValue) throws BeatmapException {
        if (hasProperty(name))
            return getGamemode(name);
        return defaultValue;
    }

    public boolean hasProperty(String name) throws BeatmapException {
        return values.containsKey(name);
    }
}
