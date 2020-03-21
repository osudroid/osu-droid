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
    MOD_PRECISE("pr");

    public final String shortName;

    GameMod(String shortName) {
        this.shortName = shortName;
    }
}
