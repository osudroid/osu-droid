package ru.nsu.ccfit.zuev.osu.game.mods;

public enum GameMod {
    MOD_NOFAIL("nf"),
    MOD_AUTO("auto"),
    MOD_EASY("es"),
    MOD_HARDROCK("hr"),
    MOD_HIDDEN("hd"),
    MOD_RELAX("relax"),
    MOD_AUTOPILOT("ap"),
    MOD_DOUBLETIME("dt"),
    MOD_NIGHTCORE("nc"),
    MOD_HALFTIME("ht"),
    MOD_SUDDENDEATH("sd"),
    MOD_PERFECT("pf"),
    MOD_FLASHLIGHT("fl"),
    MOD_PRECISE("pr"),
    MOD_SMALLCIRCLE("sc"),
    MOD_REALLYEASY("re"),
    MOD_SPEEDUP("su");
    
    public final String shortName;

    GameMod(String shortName) {
        this.shortName = shortName;
    }
}
