package ru.nsu.ccfit.zuev.osu.game.mods;

public enum GameMod {
    /*
     * Temporarily unranked mods (until adjusted):
     * sd, pf, sc, re, fl
     */
    MOD_NOFAIL("nf", 0.5f, "nofail"),
    MOD_AUTO("auto", 0, true, "autoplay"),
    MOD_EASY("es", 0.5f, "easy"),
    MOD_HARDROCK("hr", 1.06f, "hardrock"),
    MOD_HIDDEN("hd", 1.06f, "hidden"),
    MOD_RELAX("relax", 0.001f, true, "relax"),
    MOD_AUTOPILOT("ap", 0.001f, true, "relax2"),
    MOD_DOUBLETIME("dt", 1.12f, "doubletime"),
    MOD_NIGHTCORE("nc", 1.12f, "nightcore"),
    MOD_HALFTIME("ht", 0.3f, "halftime"),
    MOD_SUDDENDEATH("sd", 1, true, "suddendeath"),
    MOD_PERFECT("pf", 1, true, "perfect"),
    MOD_FLASHLIGHT("fl", 1.12f, true, "flashlight"),
    MOD_PRECISE("pr", 1.06f, "precise"),
    MOD_SMALLCIRCLE("sc", 1.06f, true, "smallcircle"),
    MOD_REALLYEASY("re", 0.5f, true, "reallyeasy"),
    MOD_SCOREV2("v2", 1, true, "scorev2"),
    MOD_SPEEDUP("su", 1.06f, true, "speedup");

    public final String shortName;
    public final float scoreMultiplier;
    public final boolean unranked;
    public final String texture;

    GameMod(String shortName, float scoreMultiplier, String texture) {
        this(shortName, scoreMultiplier, false, texture);
    }

    GameMod(String shortName, float scoreMultiplier, boolean unranked, String texture) {
        this.shortName = shortName;
        this.scoreMultiplier = scoreMultiplier;
        this.unranked = unranked;
        this.texture = texture;
    }
}
