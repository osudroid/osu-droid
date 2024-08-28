package ru.nsu.ccfit.zuev.osu;

public class Constants {
    public static final int MAP_WIDTH = 512;
    public static final int MAP_HEIGHT = 384;
    public static final int MAP_ACTUAL_WIDTH_OLD = 820;
    public static final int MAP_ACTUAL_HEIGHT_OLD = 570;
    public static final int MAP_ACTUAL_HEIGHT = (int) (Config.getRES_HEIGHT() * 0.85f);
    public static final int MAP_ACTUAL_WIDTH = MAP_ACTUAL_HEIGHT / 3 * 4;

    private Constants() {
    }
}
