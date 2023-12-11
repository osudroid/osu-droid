package ru.nsu.ccfit.zuev.osu.scoring;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

import java.io.Serializable;
import java.util.EnumSet;

public class Statistic implements Serializable {

    private static final long serialVersionUID = 8339570462000129479L;

    int notes = 0;

    int hit300 = 0, hit100 = 0, hit50 = 0;

    int hit300k = 0, hit100k = 0;

    int misses = 0;

    int maxCombo = 0;

    int currentCombo = 0;

    int possibleScore = 0;

    int realScore = 0;

    float hp = 1;

    float diffModifier = 1;

    EnumSet<GameMod> mod = EnumSet.noneOf(GameMod.class);

}
