package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.CircleSizeCalculator
import com.rian.osu.utils.HitObjectGenerationUtils
import kotlin.math.min

/**
 * Represents the Hard Rock mod.
 */
class ModHardRock : Mod(), IModApplicableToDifficulty, IModApplicableToHitObject {
    override val name = "Hard Rock"
    override val acronym = "HR"
    override val description = "Everything just got a bit harder..."
    override val type = ModType.DifficultyIncrease
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModEasy::class, ModMirror::class)

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.06f

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty) = difficulty.run {
        difficultyCS = when (mode) {
            GameMode.Droid -> {
                val scale = CircleSizeCalculator.droidCSToDroidDifficultyScale(difficultyCS)

                CircleSizeCalculator.droidDifficultyScaleToDroidCS(scale - 0.125f)
            }

            // CS uses a custom 1.3 ratio.
            GameMode.Standard -> applySetting(difficultyCS, 1.3f)
        }

        gameplayCS = when (mode) {
            GameMode.Droid -> {
                val scale = CircleSizeCalculator.droidCSToDroidGameplayScale(gameplayCS)

                CircleSizeCalculator.droidGameplayScaleToDroidCS(scale - 0.125f)
            }

            // CS uses a custom 1.3 ratio.
            GameMode.Standard -> applySetting(gameplayCS, 1.3f)
        }

        ar = applySetting(ar)
        od = applySetting(od)
        hp = applySetting(hp)
    }

    override fun applyToHitObject(mode: GameMode, hitObject: HitObject) {
        HitObjectGenerationUtils.reflectVerticallyAlongPlayfield(hitObject)
    }

    private fun applySetting(value: Float, ratio: Float = ADJUST_RATIO) = min(value * ratio, 10f)

    override fun equals(other: Any?) = other === this || other is ModHardRock
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModHardRock()

    companion object {
        private const val ADJUST_RATIO = 1.4f
    }
}