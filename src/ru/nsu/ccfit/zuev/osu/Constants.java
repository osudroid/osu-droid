package ru.nsu.ccfit.zuev.osu;

public class Constants {

    public static final int MAP_WIDTH = 512;

    public static final int MAP_HEIGHT = 384;

    public static final int MAP_ACTUAL_WIDTH_OLD = 820;

    public static final int MAP_ACTUAL_HEIGHT_OLD = 570;

    public static final int MAP_ACTUAL_HEIGHT = (int) (Config.getRES_HEIGHT() * 0.85f);

    public static final int MAP_ACTUAL_WIDTH = MAP_ACTUAL_HEIGHT / 3 * 4;

    public static final int SLIDER_STEP = 10;

    public static final int HIGH_SLIDER_STEP = 14;

    public static final String DDL_URL_HTTPS = "https://osu.yas-online.net";

    public static final String DDL_URL = "http://osu.yas-online.net";

    public static final String[] SAMPLE_PREFIX = {"", "normal", "soft", "drum"};

    public static final String SERVICE_ENDPOINT = "http://ops.dgsrz.com/api/";

    public static final String SERVICE_IDL_VERSION = "29";

    private Constants() {
    }

}
