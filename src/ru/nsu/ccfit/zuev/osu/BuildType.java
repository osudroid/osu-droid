package ru.nsu.ccfit.zuev.osu;

import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

public class BuildType {

    public static boolean hasOnlineAccess() {
        return BuildConfig.BUILD_TYPE.matches("(release|pre_release)");
    }

}
