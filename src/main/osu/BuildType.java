package main.osu;

import com.rimu.BuildConfig;

public class BuildType {
    public static boolean hasOnlineAccess() {
        return BuildConfig.BUILD_TYPE.matches("(release|pre_release)");
    }
}
