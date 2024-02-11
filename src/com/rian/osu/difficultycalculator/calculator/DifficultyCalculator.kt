package com.rian.osu.difficultycalculator.calculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.difficultycalculator.DifficultyHitObject
import com.rian.osu.difficultycalculator.attributes.DifficultyAttributes
import com.rian.osu.difficultycalculator.attributes.TimedDifficultyAttributes
import com.rian.osu.difficultycalculator.skills.Aim
import com.rian.osu.difficultycalculator.skills.Flashlight
import com.rian.osu.difficultycalculator.skills.Skill
import com.rian.osu.difficultycalculator.skills.Speed
import com.rian.osu.utils.HitObjectStackEvaluator.applyStacking
import com.rian.osu.utils.HitWindowConverter.hitWindow300ToOD
import com.rian.osu.utils.HitWindowConverter.odToHitWindow300
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A difficulty calculator for calculating star rating.
 */
object DifficultyCalculator {
    /**
     * Mods that can alter the star rating when they are used in calculation with one or more mods.
     */
    @JvmField
    val difficultyAdjustmentMods: EnumSet<GameMod> = EnumSet.of(
        GameMod.MOD_DOUBLETIME, GameMod.MOD_HALFTIME, GameMod.MOD_NIGHTCORE,
        GameMod.MOD_RELAX, GameMod.MOD_EASY, GameMod.MOD_REALLYEASY,
        GameMod.MOD_HARDROCK, GameMod.MOD_HIDDEN, GameMod.MOD_FLASHLIGHT
    )

    private const val DIFFICULTY_MULTIPLIER = 0.0675

    /**
     * Calculates the difficulty of a [Beatmap] with specific parameters.
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param parameters The calculation parameters that should be applied to the [Beatmap].
     * @return A structure describing the difficulty of the [Beatmap].
     */
    @JvmOverloads
    fun calculate(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters? = null
    ): DifficultyAttributes {
        // Always operate on a clone of the original beatmap when needed, to not modify it game-wide
        val beatmapToCalculate =
            if (needToCopyBeatmap(beatmap, parameters)) beatmap.clone()
            else beatmap

        if (parameters != null) {
            applyParameters(beatmapToCalculate, parameters)
        }

        val skills = createSkills(beatmapToCalculate, parameters)

        for (obj in createDifficultyHitObjects(beatmapToCalculate, parameters)) {
            for (skill in skills) {
                skill.process(obj)
            }
        }

        return createDifficultyAttributes(beatmapToCalculate, skills, parameters)
    }

    /**
     * Calculates the difficulty of a [Beatmap] with specific parameters and returns a set of
     * [TimedDifficultyAttributes] representing the difficulty at every relevant time
     * value in the [Beatmap].
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param parameters The calculation parameters that should be applied to the [Beatmap].
     * @return The set of [TimedDifficultyAttributes].
     */
    @JvmOverloads
    fun calculateTimed(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters? = null
    ): List<TimedDifficultyAttributes> {
        // Always operate on a clone of the original beatmap when needed, to not modify it game-wide
        val beatmapToCalculate =
            if (needToCopyBeatmap(beatmap, parameters)) beatmap.clone()
            else beatmap

        if (parameters != null) {
            applyParameters(beatmapToCalculate, parameters)
        }

        val skills = createSkills(beatmapToCalculate, parameters)
        val attributes = ArrayList<TimedDifficultyAttributes>()

        if (beatmapToCalculate.hitObjects.objects.isEmpty()) {
            return attributes
        }

        val progressiveBeatmap = Beatmap().apply {
            difficulty.apply(beatmapToCalculate.difficulty)
        }

        // Add the first object in the beatmap, otherwise it will be ignored.
        progressiveBeatmap.hitObjects.add(beatmapToCalculate.hitObjects.objects.first())

        for (obj in createDifficultyHitObjects(beatmapToCalculate, parameters)) {
            progressiveBeatmap.hitObjects.add(obj.obj)

            for (skill in skills) {
                skill.process(obj)
            }

            attributes.add(
                TimedDifficultyAttributes(
                    obj.endTime * (parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0),
                    createDifficultyAttributes(progressiveBeatmap, skills, parameters)
                )
            )
        }

        return attributes
    }

    /**
     * Creates difficulty attributes to describe a beatmap's difficulty.
     *
     * @param beatmap The beatmap whose difficulty was calculated.
     * @param skills The skills which processed the beatmap.
     * @param parameters The difficulty calculation parameters used.
     * @return Difficulty attributes describing the beatmap's difficulty.
     */
    private fun createDifficultyAttributes(
        beatmap: Beatmap, skills: Array<Skill>,
        parameters: DifficultyCalculationParameters?
    ) = DifficultyAttributes().apply {
            mods = parameters?.mods?.clone() ?: mods

            aimDifficulty = calculateRating(skills[0])
            speedDifficulty = calculateRating(skills[2])
            speedNoteCount = (skills[2] as Speed).relevantNoteCount()
            flashlightDifficulty = calculateRating(skills[3])

            aimSliderFactor = if (aimDifficulty > 0) calculateRating(skills[1]) / aimDifficulty else 1.0

            if (parameters?.mods?.contains(GameMod.MOD_RELAX) == true) {
                aimDifficulty *= 0.9
                speedDifficulty = 0.0
                flashlightDifficulty *= 0.7
            }

            val baseAimPerformance = (5 * max(1.0, aimDifficulty / 0.0675) - 4).pow(3.0) / 100000
            val baseSpeedPerformance = (5 * max(1.0, speedDifficulty / 0.0675) - 4).pow(3.0) / 100000
            var baseFlashlightPerformance = 0.0
            if (parameters?.mods?.contains(GameMod.MOD_FLASHLIGHT) == true) {
                baseFlashlightPerformance = flashlightDifficulty.pow(2.0) * 25.0
            }

            val basePerformance = (
                baseAimPerformance.pow(1.1) +
                baseSpeedPerformance.pow(1.1) +
                baseFlashlightPerformance.pow(1.1)
            ).pow(1 / 1.1)

            // Document for formula derivation:
            // https://docs.google.com/document/d/10DZGYYSsT_yjz2Mtp6yIJld0Rqx4E-vVHupCqiM4TNI/edit
            starRating =
                if (basePerformance > 1e-5)
                    Math.cbrt(PerformanceCalculator.FINAL_MULTIPLIER) * 0.027 *
                    (Math.cbrt(100000 / 2.0.pow(1 / 1.1) * basePerformance) + 4)
                else 0.0

            val ar = beatmap.difficulty.ar
            var preempt = (if (ar <= 5) 1800 - 120 * ar else 1950 - 150 * ar).toDouble()

            if (parameters?.isCustomAR() == false) {
                preempt /= parameters.totalSpeedMultiplier.toDouble()
            }

            approachRate = if (preempt > 1200) (1800 - preempt) / 120 else (1200 - preempt) / 150 + 5

            val greatWindow =
                odToHitWindow300(beatmap.difficulty.od) /
                (parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0)

            overallDifficulty = hitWindow300ToOD(greatWindow).toDouble()
            maxCombo = beatmap.maxCombo
            hitCircleCount = beatmap.hitObjects.circleCount
            sliderCount = beatmap.hitObjects.sliderCount
            spinnerCount = beatmap.hitObjects.spinnerCount
        }

    /**
     * Applies difficulty calculation parameters to the given beatmap.
     *
     * @param beatmap The beatmap.
     * @param parameters The difficulty calculation parameters.
     */
    private fun applyParameters(beatmap: Beatmap, parameters: DifficultyCalculationParameters) = beatmap.run {
        val initialAR = difficulty.ar

        processCS(difficulty, parameters)
        processAR(difficulty, parameters)
        processOD(difficulty, parameters)
        processHP(difficulty, parameters)

        if (initialAR != difficulty.ar) {
            hitObjects.resetStacking()

            applyStacking(
                formatVersion,
                hitObjects.objects,
                difficulty.ar,
                general.stackLeniency
            )
        }
    }

    /**
     * Creates the skills to calculate the difficulty of a beatmap.
     *
     * @param beatmap The beatmap whose difficulty will be calculated.
     * @param parameters The difficulty calculation parameter being used.
     * @return The skills.
     */
    private fun createSkills(beatmap: Beatmap, parameters: DifficultyCalculationParameters?): Array<Skill> {
        val mods = parameters?.mods ?: EnumSet.noneOf(GameMod::class.java)
        val greatWindow =
            odToHitWindow300(beatmap.difficulty.od) /
            (parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0)

        return arrayOf(
            Aim(mods, true),
            Aim(mods, false),
            Speed(mods, greatWindow),
            Flashlight(mods)
        )
    }

    private fun calculateRating(skill: Skill) = sqrt(skill.difficultyValue()) * DIFFICULTY_MULTIPLIER

    private fun processCS(difficulty: BeatmapDifficulty, parameters: DifficultyCalculationParameters?) {
        parameters?.apply {
            if (isCustomCS()) {
                difficulty.cs = parameters.customCS
                return@apply
            }

            if (GameMod.MOD_HARDROCK in mods) {
                ++difficulty.cs
            }

            if (GameMod.MOD_EASY in mods) {
                --difficulty.cs
            }

            if (GameMod.MOD_REALLYEASY in mods) {
                --difficulty.cs
            }
        }

        // 12.14 is the point at which the object radius approaches 0. Use the _very_ minimum value.
        difficulty.cs = min(difficulty.cs, 12.13f)
    }

    private fun processAR(difficulty: BeatmapDifficulty, parameters: DifficultyCalculationParameters?) {
        var ar = difficulty.ar

        parameters?.apply {
            difficulty.ar = customAR.takeUnless { it.isNaN() } ?: run {
                if (GameMod.MOD_HARDROCK in mods) {
                    ar *= 1.4f
                }

                if (GameMod.MOD_EASY in mods) {
                    ar /= 2f
                }

                if (GameMod.MOD_REALLYEASY in mods) {
                    if (GameMod.MOD_EASY in mods) {
                        ar *= 2f
                        ar -= 0.5f
                    }

                    ar -= 0.5f
                    ar -= customSpeedMultiplier - 1
                }

                min(ar, 10f)
            }
        } ?: run { difficulty.ar = min(ar, 10f) }
    }

    private fun processOD(difficulty: BeatmapDifficulty, parameters: DifficultyCalculationParameters?) {
        parameters?.apply {
            if (isCustomOD()) {
                difficulty.od = customOD
                return@apply
            }

            if (GameMod.MOD_HARDROCK in mods) {
                difficulty.od *= 1.4f
            }

            if (GameMod.MOD_EASY in mods) {
                difficulty.od /= 2f
            }

            if (GameMod.MOD_REALLYEASY in mods) {
                difficulty.od /= 2f
            }

            difficulty.od = min(difficulty.od, 10f)
        }
    }

    private fun processHP(difficulty: BeatmapDifficulty, parameters: DifficultyCalculationParameters?) {
        parameters?.mods?.apply {
            if (GameMod.MOD_HARDROCK in this) {
                difficulty.hp *= 1.4f
            }

            if (GameMod.MOD_EASY in this) {
                difficulty.hp /= 2f
            }

            if (GameMod.MOD_REALLYEASY in this) {
                difficulty.hp /= 2f
            }
        }

        difficulty.hp = min(difficulty.hp, 10f)
    }

    /**
     * Retrieves the difficulty hit objects to calculate against.
     *
     * @param beatmap The beatmap providing the hit objects to generate from.
     * @param parameters The difficulty calculation parameter being used.
     * @return The generated difficulty hit objects.
     */
    private fun createDifficultyHitObjects(
        beatmap: Beatmap, parameters: DifficultyCalculationParameters?
    ) = mutableListOf<DifficultyHitObject>().apply {
        val ar = beatmap.difficulty.ar
        val timePreempt = if (ar <= 5) 1800 - 120.0 * ar else 1950 - 150.0 * ar

        val objectScale = (1 - 0.7f * (beatmap.difficulty.cs - 5) / 5) / 2

        beatmap.hitObjects.objects.let {
            for (i in 1 until it.size) {
                it[i].scale = objectScale
                it[i - 1].scale = objectScale

                add(
                    DifficultyHitObject(
                        it[i],
                        it[i - 1],
                        it.getOrNull(i - 2),
                        parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0,
                        this,
                        size,
                        timePreempt,
                        parameters?.isCustomAR() == true
                    )
                )
            }
        }
    }

    /**
     * Checks whether a [Beatmap] must be copied with respect to a [DifficultyCalculationParameters].
     *
     * @param beatmap The [Beatmap].
     * @param parameters The [DifficultyCalculationParameters].
     * @return Whether the [Beatmap] should be copied.
     */
    private fun needToCopyBeatmap(beatmap: Beatmap, parameters: DifficultyCalculationParameters?) = parameters?.run {
        (isCustomCS() && customCS != beatmap.difficulty.cs) ||
        (isCustomAR() && customAR != beatmap.difficulty.ar) ||
        (isCustomOD() && customOD != beatmap.difficulty.od) ||
        customSpeedMultiplier != 1.0f ||
        mods.any { difficultyAdjustmentMods.contains(it) }
    } ?: false
}
