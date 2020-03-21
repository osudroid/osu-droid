package lt.ekgame.beatmap_analyzer.utils;

public enum Mod {

    NO_FAIL(0, "No Fail", "nf"),
    EASY(1, "Easy", "ez"),
    HIDDEN(3, "Hidden", "hd"),
    HARDROCK(4, "Hardrock", "hr"),
    SUDDEN_DEATH(5, "Sudden Death", "sd"),
    DOUBLE_TIME(6, "Double Time", "dt"),
    RELAX(7, "Relax", "rx", false),
    HALF_TIME(8, "Half Time", "ht"),
    NIGHTCORE(9, "Nightcore", "nc"),
    FLASHLIGHT(10, "Flashlight", "fl"),
    AUTOPLAY(11, "Autoplay", "ap", false),
    SPUN_OUT(12, "Spun Out", "so"),
    AUTOPILOT(13, "Autopilot", "ap", false);

    private int offset;
    private String name, shortName;
    private boolean isRanked = true;

    Mod(int offset, String name, String shortName, boolean isRanked) {
        this(offset, name, shortName);
        this.isRanked = isRanked;
    }

    Mod(int offset, String name, String shortName) {
        this.offset = offset;
        this.name = name;
        this.shortName = shortName;
    }

    public static Mod parse(String shortName) {
        if (shortName == null)
            return null;

        shortName = shortName.toLowerCase();
        for (Mod mod : Mod.values())
            if (mod.getShortName().equals(shortName))
                return mod;

        return null;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public boolean isRanked() {
        return isRanked;
    }

    public int getBitOffset() {
        return offset;
    }

    public int getBit() {
        return 1 << offset;
    }
}
