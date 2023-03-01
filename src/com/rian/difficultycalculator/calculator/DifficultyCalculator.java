package com.rian.difficultycalculator.calculator;

import com.rian.difficultycalculator.attributes.DifficultyAttributes;
import com.rian.difficultycalculator.beatmap.DifficultyBeatmap;
import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.skills.Skill;
import com.rian.difficultycalculator.utils.GameMode;

import java.util.ArrayList;
import java.util.List;

/**
 * A difficulty calculator for calculating star rating.
 */
public abstract class DifficultyCalculator {
    /**
     * The game mode that is being calculated.
     */
    public final GameMode mode;

    /**
     * @param mode The game mode to calculate for.
     */
    protected DifficultyCalculator(final GameMode mode) {
        this.mode = mode;
    }

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

        List<DifficultyHitObject> objects = createDifficultyHitObjects(beatmapToCalculate, parameters);

        for (DifficultyHitObject object : objects) {
            for (Skill skill : skills) {
                skill.process(object);
            }
        }

        return createDifficultyAttributes(beatmap, skills, objects, parameters);
    }

    /**
     * Applies difficulty calculation parameters to the given beatmap.
     *
     * @param beatmap The beatmap.
     * @param parameters The difficulty calculation parameters.
     */
    protected abstract void applyParameters(final DifficultyBeatmap beatmap, final DifficultyCalculationParameters parameters);

    /**
     * Creates the skills to calculate the difficulty of a beatmap.
     *
     * @param beatmap The beatmap whose difficulty will be calculated.
     * @param parameters The difficulty calculation parameter being used.
     * @return The skills.
     */
    protected abstract Skill[] createSkills(final DifficultyBeatmap beatmap, final DifficultyCalculationParameters parameters);

    /**
     * Creates difficulty attributes to describe a beatmap's difficulty.
     *
     * @param beatmap The beatmap whose difficulty was calculated.
     * @param skills The skills which processed the beatmap.
     * @param objects The difficulty objects that were processed.
     * @param parameters The difficulty calculation parameters used.
     * @return Difficulty attributes describing the beatmap's difficulty.
     */
    protected abstract DifficultyAttributes createDifficultyAttributes(final DifficultyBeatmap beatmap, final Skill[] skills, final List<DifficultyHitObject> objects, final DifficultyCalculationParameters parameters);

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

        double ar = beatmap.getDifficultyManager().getAR();
        double timePreempt = (ar <= 5) ? (1800 - 120 * ar) : (1950 - 150 * ar);

        for (int i = 0; i < rawObjects.size(); ++i) {
            objects.add(new DifficultyHitObject(
                    rawObjects.get(i),
                    beatmap.getHitObjectsManager().getObjects(),
                    objects,
                    mode,
                    parameters.getTotalSpeedMultiplier(),
                    timePreempt,
                    parameters.isForceAR()
            ));
        }

        return objects;
    }
}
