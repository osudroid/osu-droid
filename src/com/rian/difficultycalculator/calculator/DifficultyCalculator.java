package com.rian.difficultycalculator.calculator;

import com.rian.difficultycalculator.attributes.DifficultyAttributes;
import com.rian.difficultycalculator.attributes.TimedDifficultyAttributes;
import com.rian.difficultycalculator.beatmap.BeatmapDifficultyManager;
import com.rian.difficultycalculator.beatmap.DifficultyBeatmap;
import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.skills.Aim;
import com.rian.difficultycalculator.skills.Flashlight;
import com.rian.difficultycalculator.skills.Skill;
import com.rian.difficultycalculator.skills.Speed;
import com.rian.difficultycalculator.utils.HitObjectStackEvaluator;
import com.rian.difficultycalculator.utils.HitWindowConverter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * A difficulty calculator for calculating star rating.
 */
public class DifficultyCalculator {
    /**
     * Mods that can alter the star rating when they are used in calculation with one or more mods.
     */
    public final EnumSet<GameMod> difficultyAdjustmentMods = EnumSet.of(
            GameMod.MOD_DOUBLETIME, GameMod.MOD_HALFTIME, GameMod.MOD_NIGHTCORE,
            GameMod.MOD_SMALLCIRCLE, GameMod.MOD_RELAX, GameMod.MOD_EASY,
            GameMod.MOD_REALLYEASY, GameMod.MOD_HARDROCK, GameMod.MOD_HIDDEN,
            GameMod.MOD_FLASHLIGHT, GameMod.MOD_SPEEDUP
    );

    private static final double difficultyMultiplier = 0.0675;

    public DifficultyCalculator() {}

    /**
     * Calculates the difficulty of a beatmap without specific parameters.
     *
     * @param beatmap The beatmap whose difficulty is to be calculated.
     * @return A structure describing the difficulty of the beatmap.
     */
    public DifficultyAttributes calculate(final DifficultyBeatmap beatmap) {
        return calculate(beatmap, null);
    }

    /**
     * Calculates the difficulty of the beatmap with specific parameters.
     *
     * @param beatmap The beatmap whose difficulty is to be calculated.
     * @param parameters The calculation parameters that should be applied to the beatmap.
     * @return A structure describing the difficulty of the beatmap.
     */
    public DifficultyAttributes calculate(final DifficultyBeatmap beatmap, final DifficultyCalculationParameters parameters) {
        DifficultyBeatmap beatmapToCalculate = beatmap;

        if (parameters != null) {
            // Always operate on a clone of the original beatmap, to not modify it game-wide
            beatmapToCalculate = beatmap.deepClone();
            applyParameters(beatmapToCalculate, parameters);
        }

        Skill[] skills = createSkills(beatmapToCalculate, parameters);

        for (DifficultyHitObject object : createDifficultyHitObjects(beatmapToCalculate, parameters)) {
            for (Skill skill : skills) {
                skill.process(object);
            }
        }

        return createDifficultyAttributes(beatmapToCalculate, skills, parameters);
    }

    /**
     * Calculates the difficulty of a beatmap without specific parameters and returns a set of
     * <code>TimedDifficultyAttributes</code> representing the difficulty at every relevant time
     * value in the beatmap.
     *
     * @param beatmap The beatmap whose difficulty is to be calculated.
     * @return The set of <code>TimedDifficultyAttributes</code>.
     */
    public List<TimedDifficultyAttributes> calculateTimed(final DifficultyBeatmap beatmap) {
        return calculateTimed(beatmap, null);
    }

    /**
     * Calculates the difficulty of a beatmap with specific parameters and returns a set of
     * <code>TimedDifficultyAttributes</code> representing the difficulty at every relevant time
     * value in the beatmap.
     *
     * @param beatmap The beatmap whose difficulty is to be calculated.
     * @param parameters The calculation parameters that should be applied to the beatmap.
     * @return The set of <code>TimedDifficultyAttributes</code>.
     */
    public List<TimedDifficultyAttributes> calculateTimed(final DifficultyBeatmap beatmap,
                                                          final DifficultyCalculationParameters parameters) {
        DifficultyBeatmap beatmapToCalculate = beatmap;

        if (parameters != null) {
            // Always operate on a clone of the original beatmap, to not modify it game-wide
            beatmapToCalculate = beatmap.deepClone();
            applyParameters(beatmapToCalculate, parameters);
        }

        Skill[] skills = createSkills(beatmapToCalculate, parameters);
        ArrayList<TimedDifficultyAttributes> attributes = new ArrayList<>();

        if (beatmapToCalculate.getHitObjectsManager().getObjects().isEmpty()) {
            return attributes;
        }

        DifficultyBeatmap progressiveBeatmap = new DifficultyBeatmap(beatmapToCalculate.getDifficultyManager());

        // Add the first object in the beatmap, otherwise it will be ignored.
        progressiveBeatmap.getHitObjectsManager().add(beatmapToCalculate.getHitObjectsManager().getObjects().get(0));

        for (DifficultyHitObject object : createDifficultyHitObjects(beatmapToCalculate, parameters)) {
            progressiveBeatmap.getHitObjectsManager().add(object.object);

            for (Skill skill : skills) {
                skill.process(object);
            }

            attributes.add(new TimedDifficultyAttributes(object.endTime * (parameters != null ? parameters.getTotalSpeedMultiplier() : 1), createDifficultyAttributes(progressiveBeatmap, skills, parameters)));
        }

        return attributes;
    }

    /**
     * Creates difficulty attributes to describe a beatmap's difficulty.
     *
     * @param beatmap The beatmap whose difficulty was calculated.
     * @param skills The skills which processed the beatmap.
     * @param parameters The difficulty calculation parameters used.
     * @return Difficulty attributes describing the beatmap's difficulty.
     */
    private DifficultyAttributes createDifficultyAttributes(final DifficultyBeatmap beatmap, final Skill[] skills,
                                                              final DifficultyCalculationParameters parameters) {
        DifficultyAttributes attributes = new DifficultyAttributes();

        if (parameters != null) {
            attributes.mods = parameters.mods.clone();
        }

        attributes.aimDifficulty = calculateRating(skills[0]);
        attributes.speedDifficulty = calculateRating(skills[2]);
        attributes.speedNoteCount = ((Speed) skills[2]).relevantNoteCount();
        attributes.flashlightDifficulty = calculateRating(skills[3]);

        double aimRatingNoSliders = calculateRating(skills[1]);
        attributes.aimSliderFactor = attributes.aimDifficulty > 0 ? aimRatingNoSliders / attributes.aimDifficulty : 1;

        if (parameters != null && parameters.mods.contains(GameMod.MOD_RELAX)) {
            attributes.aimDifficulty *= 1;
            attributes.speedDifficulty *= 1;
            attributes.flashlightDifficulty *= 1;
        }

        double baseAimPerformance = Math.pow(5 * Math.max(1, attributes.aimDifficulty / 0.0675) - 4, 3) / 100000;
        double baseSpeedPerformance = Math.pow(5 * Math.max(1, attributes.speedDifficulty / 0.0675) - 4, 3) / 100000;
        double baseFlashlightPerformance = 0;

        if (parameters != null && parameters.mods.contains(GameMod.MOD_FLASHLIGHT)) {
            baseFlashlightPerformance = Math.pow(attributes.flashlightDifficulty, 2) * 25.0;
        }

        double basePerformance = Math.pow(
                Math.pow(baseAimPerformance, 1.1) +
                        Math.pow(baseSpeedPerformance, 1.1) +
                        Math.pow(baseFlashlightPerformance, 1.1),
                1.0 / 1.1
        );

        // Document for formula derivation:
        // https://docs.google.com/document/d/10DZGYYSsT_yjz2Mtp6yIJld0Rqx4E-vVHupCqiM4TNI/edit
        attributes.starRating = basePerformance > 1e-5
                ? Math.cbrt(PerformanceCalculator.finalMultiplier) * 0.027 * (Math.cbrt(100000 / Math.pow(2, 1 / 1.1) * basePerformance) + 4)
                : 0;

        float ar = beatmap.getDifficultyManager().getAR();
        double preempt = (ar <= 5) ? (1800 - 120 * ar) : (1950 - 150 * ar);

        if (parameters != null && !parameters.isForceAR()) {
            preempt /= parameters.getTotalSpeedMultiplier();
        }

        attributes.approachRate = preempt > 1200 ? (1800 - preempt) / 120 : (1200 - preempt) / 150 + 5;

        float od = beatmap.getDifficultyManager().getOD();
        double odMS = HitWindowConverter.odToHitWindow300(od) / (parameters != null ? parameters.getTotalSpeedMultiplier() : 1);

        attributes.overallDifficulty = HitWindowConverter.hitWindow300ToOD(odMS);

        attributes.maxCombo = beatmap.getMaxCombo();
        attributes.hitCircleCount = beatmap.getHitObjectsManager().getCircleCount();
        attributes.sliderCount = beatmap.getHitObjectsManager().getSliderCount();
        attributes.spinnerCount = beatmap.getHitObjectsManager().getSpinnerCount();

        return attributes;
    }

    /**
     * Applies difficulty calculation parameters to the given beatmap.
     *
     * @param beatmap The beatmap.
     * @param parameters The difficulty calculation parameters.
     */
    private void applyParameters(DifficultyBeatmap beatmap, DifficultyCalculationParameters parameters) {
        final BeatmapDifficultyManager manager = beatmap.getDifficultyManager();
        float initialAR = manager.getAR();

        processCS(manager, parameters);
        processAR(manager, parameters);
        processOD(manager, parameters);
        processHP(manager, parameters);

        if (initialAR != manager.getAR()) {
            beatmap.getHitObjectsManager().resetStacking();

            HitObjectStackEvaluator.applyStacking(
                    beatmap.getFormatVersion(),
                    beatmap.getHitObjectsManager().getObjects(),
                    manager.getAR(),
                    beatmap.getStackLeniency()
            );
        }
    }

    /**
     * Creates the skills to calculate the difficulty of a beatmap.
     *
     * @param beatmap The beatmap whose difficulty will be calculated.
     * @param parameters The difficulty calculation parameter being used.
     * @return The skills.
     */
    private Skill[] createSkills(DifficultyBeatmap beatmap, DifficultyCalculationParameters parameters) {
        EnumSet<GameMod> mods = EnumSet.noneOf(GameMod.class);
        float od = beatmap.getDifficultyManager().getOD();
        double greatWindow = HitWindowConverter.odToHitWindow300(od);

        if (parameters != null) {
            mods = parameters.mods;
            greatWindow /= parameters.getTotalSpeedMultiplier();
        }

        return new Skill[] {
                new Aim(mods, true),
                new Aim(mods, false),
                new Speed(mods, greatWindow),
                new Flashlight(mods),
        };
    }

    private double calculateRating(Skill skill) {
        return Math.sqrt(skill.difficultyValue()) * difficultyMultiplier;
    }

    private void processCS(BeatmapDifficultyManager manager, DifficultyCalculationParameters parameters) {
        float cs = manager.getCS();

        if (parameters != null) {
            if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
                ++cs;
            }
            if (parameters.mods.contains(GameMod.MOD_EASY)) {
                --cs;
            }
            if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
                --cs;
            }
            if (parameters.mods.contains(GameMod.MOD_SMALLCIRCLE)) {
                cs += 4f;
            }
        }

        // 12.14 is the point at which the object radius approaches 0. Use the _very_ minimum value.
        manager.setCS(Math.min(cs, 12.13f));
    }

    private void processAR(BeatmapDifficultyManager manager, DifficultyCalculationParameters parameters) {
        float ar = manager.getAR();

        if (parameters == null) {
            manager.setAR(Math.min(ar, 10f));
            return;
        }

        if (parameters.isForceAR()) {
            manager.setAR(parameters.forcedAR);
        } else {
            if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
                ar *= 1.4f;
            }
            if (parameters.mods.contains(GameMod.MOD_EASY)) {
                ar /= 2f;
            }
            if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
                if (parameters.mods.contains(GameMod.MOD_EASY)) {
                    ar *= 2f;
                    ar -= 0.5f;
                }

                ar -= 0.5f;
                ar -= parameters.customSpeedMultiplier - 1f;
            }

            manager.setAR(Math.min(ar, 10f));
        }
    }

    private void processOD(BeatmapDifficultyManager manager, DifficultyCalculationParameters parameters) {
        float od = manager.getOD();

        if (parameters != null) {
            if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
                od *= 1.4f;
            }
            if (parameters.mods.contains(GameMod.MOD_EASY)) {
                od /= 2f;
            }
            if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
                od /= 2f;
            }
        }

        manager.setOD(Math.min(od, 10f));
    }

    private void processHP(BeatmapDifficultyManager manager, DifficultyCalculationParameters parameters) {
        float hp = manager.getHP();

        if (parameters != null) {
            if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
                hp *= 1.4f;
            }
            if (parameters.mods.contains(GameMod.MOD_EASY)) {
                hp /= 2f;
            }
            if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
                hp /= 2f;
            }
        }

        manager.setHP(Math.min(hp, 10f));
    }

    /**
     * Retrieves the difficulty hit objects to calculate against.
     *
     * @param beatmap The beatmap providing the hit objects to generate from.
     * @param parameters The difficulty calculation parameter being used.
     * @return The generated difficulty hit objects.
     */
    private List<DifficultyHitObject> createDifficultyHitObjects(
            final DifficultyBeatmap beatmap, final DifficultyCalculationParameters parameters) {
        ArrayList<DifficultyHitObject> objects = new ArrayList<>();
        List<HitObject> rawObjects = beatmap.getHitObjectsManager().getObjects();

        float ar = beatmap.getDifficultyManager().getAR();
        double timePreempt = (ar <= 5) ? (1800 - 120 * ar) : (1950 - 150 * ar);
        float objectScale = (1 - 0.7f * (beatmap.getDifficultyManager().getCS() - 5) / 5) / 2;

        for (int i = 1; i < rawObjects.size(); ++i) {
            rawObjects.get(i).setScale(objectScale);
            rawObjects.get(i - 1).setScale(objectScale);

            HitObject lastLast = i > 1 ? rawObjects.get(i - 2) : null;

            objects.add(new DifficultyHitObject(
                    rawObjects.get(i),
                    rawObjects.get(i - 1),
                    lastLast,
                    parameters != null ? parameters.getTotalSpeedMultiplier() : 1,
                    objects,
                    objects.size(),
                    timePreempt,
                    parameters != null && parameters.isForceAR()
            ));
        }

        return objects;
    }
}
