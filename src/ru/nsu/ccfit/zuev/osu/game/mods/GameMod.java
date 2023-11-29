package ru.nsu.ccfit.zuev.osu.game.mods;

public enum GameMod {
    /*
     * Temporarily unranked mods (until adjusted):
     * sd, pf, sc, re, fl
     */
    MOD_NOFAIL("nf", 0.5f), MOD_AUTO("auto", 1, true), MOD_EASY("es", 0.5f), MOD_HARDROCK("hr", 1.06f), MOD_HIDDEN("hd", 1.06f), MOD_RELAX("relax", 0.001f, true), MOD_AUTOPILOT(
        "ap",
        0.001f,
        true), MOD_DOUBLETIME("dt", 1.12f), MOD_NIGHTCORE("nc", 1.12f), MOD_HALFTIME("ht", 0.3f), MOD_SUDDENDEATH("sd", 1), MOD_PERFECT("pf", 1), MOD_FLASHLIGHT("fl",
        1.12f), MOD_PRECISE("pr", 1.06f), MOD_REALLYEASY("re", 0.5f, true), MOD_SCOREV2("v2", 1, true);

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
}
