package ru.nsu.ccfit.zuev.osu.game.mods;

public enum GameMod {
    /*
     * Temporarily unranked mods (until adjusted):
     * sd, pf, sc, re, fl
     */
    MOD_NOFAIL(0.5f), MOD_AUTO(1, true), MOD_EASY(0.5f), MOD_HARDROCK(1.06f), MOD_HIDDEN(1.06f), MOD_RELAX(0.001f, true), MOD_AUTOPILOT(0.001f, true), MOD_DOUBLETIME(1.12f), MOD_NIGHTCORE(1.12f), MOD_HALFTIME(0.3f), MOD_SUDDENDEATH(1), MOD_PERFECT(1), MOD_FLASHLIGHT(1.12f), MOD_PRECISE(1.06f), MOD_REALLYEASY(0.5f, true), MOD_SCOREV2(1, true);

    public final float scoreMultiplier;

    public final boolean unranked;

    GameMod(float scoreMultiplier) {
        this(scoreMultiplier, false);
    }

    GameMod(float scoreMultiplier, boolean unranked) {
        this.scoreMultiplier = scoreMultiplier;
        this.unranked = unranked;
    }
}
