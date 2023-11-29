package ru.nsu.ccfit.zuev.osu.scoring;

import java.io.Serializable;
import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public class Statistic implements Serializable {

    private static final long serialVersionUID = 8339570462000129479L;

    final int notes = 0;

    final int hit300 = 0;

    final int hit100 = 0;

    final int hit50 = 0;

    final int hit300k = 0;

    final int hit100k = 0;

    final int misses = 0;

    final int maxCombo = 0;

    final int currentCombo = 0;

    final int possibleScore = 0;

    final int realScore = 0;

    final float hp = 1;

    final float diffModifier = 1;

    final EnumSet<GameMod> mod = EnumSet.noneOf(GameMod.class);

}
