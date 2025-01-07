package ru.nsu.ccfit.zuev.osu.game.mods;

public enum GameMod {
    /*
     * Temporarily unranked mods (until adjusted):
     * sd, pf, sc, re, fl
     */
    MOD_NOFAIL("nf", 0.5f),
    MOD_AUTO("auto", 1, true),
    MOD_EASY("es", 0.5f),
    MOD_HARDROCK("hr", 1.03f),
    MOD_HIDDEN("hd", 1.06f),
    MOD_RELAX("relax", 0.001f, true),
    MOD_AUTOPILOT("ap", 0.001f, true),
    MOD_DOUBLETIME("dt", 1.12f),
    MOD_NIGHTCORE("nc", 1.12f),
    MOD_HALFTIME("ht", 0.3f),
    MOD_SUDDENDEATH("sd", 1),
    MOD_PERFECT("pf", 1),
    MOD_FLASHLIGHT("fl", 1.12f),
    MOD_PRECISE("pr", 1.06f),
    MOD_REALLYEASY("re", 0.5f, true),
    MOD_SCOREV2("v2", 1, true);

    public final String shortName;
    public final float scoreMultiplier;
    public final boolean unranked;

    GameMod(String shortName, float scoreMultiplier) {
        this(shortName, scoreMultiplier, false);
    }

    GameMod(String shortName, float scoreMultiplier, boolean unranked) {
        this.shortName = shortName;
        this.scoreMultiplier = scoreMultiplier;
        this.unranked = unranked;
    }


    /**
     * Provides the texture name corresponding to the mod entry.
     */
    public static String getTextureName(GameMod mod) {
        return "selection-mod-" + switch (mod) {
            case MOD_NOFAIL -> "nofail";
            case MOD_AUTO -> "autoplay";
            case MOD_EASY -> "easy";
            case MOD_HARDROCK -> "hardrock";
            case MOD_HIDDEN -> "hidden";
            case MOD_RELAX -> "relax";
            case MOD_AUTOPILOT -> "relax2";
            case MOD_DOUBLETIME -> "doubletime";
            case MOD_NIGHTCORE -> "nightcore";
            case MOD_HALFTIME -> "halftime";
            case MOD_SUDDENDEATH -> "suddendeath";
            case MOD_PERFECT -> "perfect";
            case MOD_FLASHLIGHT -> "flashlight";
            case MOD_PRECISE -> "precise";
            case MOD_REALLYEASY -> "reallyeasy";
            case MOD_SCOREV2 -> "scorev2";
        };
    }
}
