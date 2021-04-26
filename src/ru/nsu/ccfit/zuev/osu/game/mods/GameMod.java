package ru.nsu.ccfit.zuev.osu.game.mods;

public enum GameMod {
    MOD_NOFAIL("nf", 0.5f, true),
    MOD_AUTO("auto", 0, false, true),
    MOD_EASY("es", 0.5f, true),
    MOD_HARDROCK("hr", 1.06f, true),
    MOD_HIDDEN("hd", 1.06f, true),
    MOD_RELAX("relax", 0.001f, false, true),
    MOD_AUTOPILOT("ap", 0.001f, false, true),
    MOD_DOUBLETIME("dt", 1.12f, true),
    MOD_NIGHTCORE("nc", 1.12f, true),
    MOD_HALFTIME("ht", 0.3f, true),
    MOD_SUDDENDEATH("sd", 1, false),
    MOD_PERFECT("pf", 1, false),
    MOD_FLASHLIGHT("fl", 1.12f, false),
    MOD_PRECISE("pr", 1.06f, true),
    MOD_SMALLCIRCLE("sc", 1.06f, false),
    MOD_REALLYEASY("re", 0.5f, false),
    MOD_SCOREV2("v2", 1, false),
    MOD_SPEEDUP("su", 1.06f, false);
    
    public final String shortName;
    public final float scoreMultiplier;
    public final boolean isRanked;
    public final boolean typeAuto;

    GameMod(String shortName, float scoreMultiplier, boolean isRanked) {
        this.shortName = shortName;
        this.scoreMultiplier = scoreMultiplier;
        this.isRanked = isRanked;
        this.typeAuto = false;
    }

    GameMod(String shortName, float scoreMultiplier, boolean isRanked, boolean typeAuto) {
        this.shortName = shortName;
        this.scoreMultiplier = scoreMultiplier;
        // Generally, autoplaying mods should not be ranked
        this.isRanked = !typeAuto && isRanked;
        this.typeAuto = typeAuto;
    }
}
