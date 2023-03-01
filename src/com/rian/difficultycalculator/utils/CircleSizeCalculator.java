package com.rian.difficultycalculator.utils;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameObjectSize;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * A utility for calculating circle sizes across all modes (rimu! and osu!standard).
 */
public final class CircleSizeCalculator {
    /**
     * Converts rimu! CS to rimu! scale.
     *
     * @param cs The CS to convert.
     * @param mods The mods to apply.
     * @return The calculated rimu! scale.
     */
    public static double rimuCSToRimuScale(double cs, EnumSet<GameMod> mods) {
        double scale = (Config.getRES_HEIGHT() / 480d) *
                (54.42 - cs * 4.48) *
                2 / GameObjectSize.BASE_OBJECT_SIZE +
                0.5 * Config.getScaleMultiplier();

        if (mods.contains(GameMod.MOD_HARDROCK)) {
            scale -= 0.125;
        }
        if (mods.contains(GameMod.MOD_EASY)) {
            scale += 0.125;
        }
        if (mods.contains(GameMod.MOD_REALLYEASY)) {
            scale += 0.125;
        }
        if (mods.contains(GameMod.MOD_SMALLCIRCLE)) {
            scale -= Config.getRES_HEIGHT() / 480d * 4 * 4.48 * 2 / GameObjectSize.BASE_OBJECT_SIZE;
        }

        return scale;
    }

    /**
     * Converts rimu! scale to osu!standard radius.
     *
     * @param scale The rimu! scale to convert.
     * @return The osu!standard radius of the given rimu! scale.
     */
    public static double rimuScaleToStandardRadius(double scale) {
        return 64 * Math.max(1e-3, scale) / (Config.getRES_HEIGHT() * 0.85 / 384);
    }

    /**
     * Converts osu!standard radius to osu!standard circle size.
     *
     * @param radius The osu!standard radius to convert.
     * @return The osu!standard circle size of the given radius.
     */
    public static double standardRadiusToStandardCS(double radius) {
        return 5 + (1 - radius / 32) * 5 / 0.7;
    }
}
