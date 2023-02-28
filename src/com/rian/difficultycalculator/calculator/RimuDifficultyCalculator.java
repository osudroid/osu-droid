package com.rian.difficultycalculator.calculator;

import com.rian.difficultycalculator.attributes.RimuDifficultyAttributes;
import com.rian.difficultycalculator.beatmap.BeatmapDifficultyManager;
import com.rian.difficultycalculator.beatmap.DifficultyBeatmap;
import com.rian.difficultycalculator.skills.Skill;
import com.rian.difficultycalculator.skills.rimu.RimuAim;
import com.rian.difficultycalculator.skills.rimu.RimuFlashlight;
import com.rian.difficultycalculator.skills.rimu.RimuRhythm;
import com.rian.difficultycalculator.skills.rimu.RimuTap;
import com.rian.difficultycalculator.skills.rimu.RimuVisual;
import com.rian.difficultycalculator.utils.RimuHitWindowConverter;
import com.rian.difficultycalculator.utils.StandardHitWindowConverter;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameObjectSize;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * A difficulty calculator for rimu!.
 */
public class RimuDifficultyCalculator extends DifficultyCalculator {
    private static final double difficultyMultiplier = 0.18;

    /**
     * @param beatmap The beatmap to calculate. Must have at least 1 hit object.
     */
    public RimuDifficultyCalculator(DifficultyBeatmap beatmap) {
        super(beatmap);
    }

    @Override
    protected RimuDifficultyAttributes createDifficultyAttributes(DifficultyBeatmap beatmap, Skill[] skills,
                                                                  DifficultyCalculationParameters parameters) {
        RimuDifficultyAttributes attributes = new RimuDifficultyAttributes();

        attributes.aimDifficulty = calculateRating(skills[0]);
        attributes.rhythmDifficulty = calculateRating(skills[2]);
        attributes.tapDifficulty = calculateRating(skills[3]);
        attributes.speedNoteCount = ((RimuTap) skills[3]).relevantNoteCount();
        attributes.flashlightDifficulty = calculateRating(skills[4]);
        attributes.visualDifficulty = calculateRating(skills[5]);

        double aimRatingNoSliders = calculateRating(skills[1]);
        attributes.sliderFactor = attributes.aimDifficulty > 0 ? aimRatingNoSliders / attributes.aimDifficulty : 1;

        if (parameters.mods.contains(GameMod.MOD_RELAX)) {
            attributes.aimDifficulty *= 0.9;
            attributes.tapDifficulty = 0;
            attributes.rhythmDifficulty = 0;
            attributes.flashlightDifficulty *= 0.7;
            attributes.visualDifficulty = 0;
        }

        double baseAimPerformance = Math.pow(5 * Math.max(1, Math.pow(attributes.aimDifficulty, 0.8) / 0.0675) - 4, 3) / 100000;
        double baseTapPerformance = Math.pow(5 * Math.max(1, attributes.tapDifficulty / 0.0675) - 4, 3) / 100000;
        double baseFlashlightPerformance = 0;
        double baseVisualPerformance = Math.pow(attributes.visualDifficulty, 1.6) * 22.5;

        if (parameters.mods.contains(GameMod.MOD_FLASHLIGHT)) {
            baseFlashlightPerformance = Math.pow(attributes.flashlightDifficulty, 1.6) * 25.0;
        }

        double basePerformance = Math.pow(
                Math.pow(baseAimPerformance, 1.1) +
                Math.pow(baseTapPerformance, 1.1) +
                Math.pow(baseFlashlightPerformance, 1.1) +
                Math.pow(baseVisualPerformance, 1.1),
                1.0 / 1.1
        );

        // Document for formula derivation:
        // https://docs.google.com/document/d/10DZGYYSsT_yjz2Mtp6yIJld0Rqx4E-vVHupCqiM4TNI/edit
        attributes.starRating = basePerformance > 1e-5
                ? 0.027 * (Math.cbrt(100000 / Math.pow(2, 1 / 1.1) * basePerformance) + 4)
                : 0;

        double ar = beatmap.getDifficultyManager().getAR();
        double preempt = (ar <= 5) ? (1800 - 120 * ar) : (1950 - 150 * ar);

        if (!parameters.isForceAR()) {
            preempt /= parameters.getTotalSpeedMultiplier();
        }

        attributes.approachRate = preempt > 1200 ? (1800 - preempt) / 120 : (1200 - preempt) / 150 + 5;

        double od = beatmap.getDifficultyManager().getOD();
        double odMS = StandardHitWindowConverter.odToHitWindow300(od) / parameters.getTotalSpeedMultiplier();

        attributes.overallDifficulty = StandardHitWindowConverter.hitWindow300ToOD(odMS);

        attributes.maxCombo = beatmap.getMaxCombo();
        attributes.hitCircleCount = beatmap.getHitObjectsManager().getCircleCount();
        attributes.sliderCount = beatmap.getHitObjectsManager().getSliderCount();
        attributes.spinnerCount = beatmap.getHitObjectsManager().getSpinnerCount();

        return attributes;
    }

    @Override
    protected void applyParameters(DifficultyBeatmap beatmap, DifficultyCalculationParameters parameters) {
        final BeatmapDifficultyManager manager = beatmap.getDifficultyManager();

        processCS(manager, parameters);
        processAR(manager, parameters);
        processOD(manager, parameters);
        processHP(manager, parameters);
    }

    @Override
    protected Skill[] createSkills(DifficultyBeatmap beatmap, DifficultyCalculationParameters parameters) {
        EnumSet<GameMod> mods = parameters != null ? parameters.mods : EnumSet.noneOf(GameMod.class);
        double greatWindow = StandardHitWindowConverter.odToHitWindow300(beatmap.getDifficultyManager().getOD());

        return new Skill[]{
                new RimuAim(mods, true),
                new RimuAim(mods, false),
                // Tap skill depends on rhythm skill, so we put it first
                new RimuRhythm(mods, greatWindow),
                new RimuTap(mods, greatWindow),
                new RimuFlashlight(mods, true),
                new RimuVisual(mods, true)
        };
    }

    private double calculateRating(Skill skill) {
        return Math.sqrt(skill.difficultyValue()) * difficultyMultiplier;
    }

    private void processCS(BeatmapDifficultyManager manager, DifficultyCalculationParameters parameters) {
        double scale = (Config.getRES_HEIGHT() / 480d) *
                (54.42 - manager.getCS() * 4.48) *
                2 / GameObjectSize.BASE_OBJECT_SIZE +
                0.5 * Config.getScaleMultiplier();

        if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
            scale -= 0.125;
        }
        if (parameters.mods.contains(GameMod.MOD_EASY)) {
            scale += 0.125;
        }
        if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
            scale += 0.125;
        }
        if (parameters.mods.contains(GameMod.MOD_SMALLCIRCLE)) {
            scale -= Config.getRES_HEIGHT() / 480d * 4 * 4.48 * 2 / GameObjectSize.BASE_OBJECT_SIZE;
        }

        double radius = 64 * Math.max(1e-3, scale) / (Config.getRES_HEIGHT() * 0.85 / 384);
        manager.setCS(5 + (1 - radius / 32) * 5 / 0.7);
    }

    private void processAR(BeatmapDifficultyManager manager, DifficultyCalculationParameters parameters) {
        boolean isForceAR = !Double.isNaN(parameters.forcedAR);
        if (isForceAR) {
            manager.setAR(parameters.forcedAR);
        } else {
            double ar = manager.getAR();

            if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
                ar *= 1.4;
            }
            if (parameters.mods.contains(GameMod.MOD_EASY)) {
                ar /= 2;
            }
            if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
                if (parameters.mods.contains(GameMod.MOD_EASY)) {
                    ar *= 2;
                    ar -= 0.5;
                }

                ar -= 0.5;
                ar -= parameters.customSpeedMultiplier - 1;
            }

            manager.setAR(Math.min(ar, 10));
        }
    }

    private void processOD(BeatmapDifficultyManager manager, DifficultyCalculationParameters parameters) {
        double od = manager.getOD();
        if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
            od *= 1.4;
        }
        if (parameters.mods.contains(GameMod.MOD_EASY)) {
            od /= 2;
        }
        if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
            od /= 2;
        }

        od = Math.min(od, 10);

        // Convert standard OD to rimu! hit window to take rimu! hit window and the Precise mod in mind.
        double odMS = RimuHitWindowConverter.odToHitWindow300(od, parameters.mods.contains(GameMod.MOD_PRECISE));

        // Convert rimu! hit window back to standard OD.
        od = StandardHitWindowConverter.hitWindow300ToOD(odMS);

        manager.setOD(od);
    }

    private void processHP(BeatmapDifficultyManager manager, DifficultyCalculationParameters parameters) {
        double hp = manager.getHP();

        if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
            hp *= 1.4;
        }
        if (parameters.mods.contains(GameMod.MOD_EASY)) {
            hp /= 2;
        }
        if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
            hp /= 2;
        }

        manager.setHP(Math.min(hp, 10));
    }
}
