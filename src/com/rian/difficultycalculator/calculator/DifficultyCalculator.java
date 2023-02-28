package com.rian.difficultycalculator.calculator;

import com.rian.difficultycalculator.attributes.DifficultyAttributes;
import com.rian.difficultycalculator.beatmap.DifficultyBeatmap;
import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.skills.Skill;
import com.rian.difficultycalculator.utils.GameMode;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;

/**
 * A difficulty calculator for calculating star rating.
 */
public abstract class DifficultyCalculator {
    /**
     * The beatmap that is being calculated.
     */
    public final DifficultyBeatmap beatmap;

    /**
     * The game mode that is being calculated.
     */
    public final GameMode mode;

    /**
     * @param beatmap The beatmap to calculate.
     * @param mode The game mode to calculate for.
     */
    public DifficultyCalculator(final DifficultyBeatmap beatmap, final GameMode mode) {
        this.beatmap = beatmap;
        this.mode = mode;
    }

    /**
     * Calculates the difficulty of the beatmap without specific parameters.
     *
     * @return A structure describing the difficulty of the beatmap.
     */
    public DifficultyAttributes calculate() {
        return calculate(null);
    }

    /**
     * Calculates the difficulty of the beatmap with specific parameters.
     *
     * @param parameters The calculation parameters that should be applied to the beatmap.
     * @return A structure describing the difficulty of the beatmap.
     */
    public DifficultyAttributes calculate(DifficultyCalculationParameters parameters) {
        DifficultyBeatmap beatmap = this.beatmap;

        if (parameters != null) {
            // Always operate on a clone of the original beatmap, to not modify it game-wide
            beatmap = beatmap.deepClone();
            applyParameters(beatmap, parameters);
        }

        Skill[] skills = createSkills(beatmap, parameters);

        for (DifficultyHitObject object : createDifficultyHitObjects(beatmap, parameters)) {
            for (Skill skill : skills) {
                skill.process(object);
            }
        }

        return createDifficultyAttributes(beatmap, skills, parameters);
    }

    /**
     * Applies difficulty calculation parameters to the given beatmap.
     *
     * @param beatmap The beatmap.
     * @param parameters The difficulty calculation parameters.
     */
    protected abstract void applyParameters(DifficultyBeatmap beatmap, DifficultyCalculationParameters parameters);

    /**
     * Creates the skills to calculate the difficulty of a beatmap.
     *
     * @param beatmap The beatmap whose difficulty will be calculated.
     * @param parameters The difficulty calculation parameter being used.
     * @return The skills.
     */
    protected abstract Skill[] createSkills(DifficultyBeatmap beatmap, DifficultyCalculationParameters parameters);

    /**
     * Creates difficulty attributes to describe a beatmap's difficulty.
     *
     * @param beatmap The beatmap whose difficulty was calculated.
     * @param skills The skills which processed the beatmap.
     * @param parameters The difficulty calculation parameters used.
     * @return Difficulty attributes describing the beatmap's difficulty.
     */
    protected abstract DifficultyAttributes createDifficultyAttributes(DifficultyBeatmap beatmap, Skill[] skills, DifficultyCalculationParameters parameters);

    /**
     * Retrieves the difficulty hit objects to calculate against.
     *
     * @param beatmap The beatmap providing the hit objects to generate from.
     * @param parameters The difficulty calculation parameter being used.
     * @return The generated difficulty hit objects.
     */
    private ArrayList<DifficultyHitObject> createDifficultyHitObjects(
            DifficultyBeatmap beatmap, DifficultyCalculationParameters parameters) {
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
