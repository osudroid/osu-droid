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
    override val scoreMultiplier = 1.06f

    override fun isCompatibleWith(other: Mod): Boolean {
        if (other is ModDifficultyAdjust) {
            return other.cs == null || other.ar == null || other.od == null || other.hp == null
        }

        return super.isCompatibleWith(other)
    }

    override fun applyToDifficulty(
        mode: GameMode,
        difficulty: BeatmapDifficulty,
        adjustmentMods: Iterable<IModFacilitatesAdjustment>
    ) = difficulty.run {
        if (mode == GameMode.Standard || adjustmentMods.none { it is ModReplayV6 }) {
            // CS uses a custom 1.3 ratio.
            difficultyCS = applySetting(difficultyCS, 1.3f)
            gameplayCS = applySetting(gameplayCS, 1.3f)
        } else {
            val difficultyScale = CircleSizeCalculator.droidCSToOldDroidDifficultyScale(difficultyCS)
            val gameplayScale = CircleSizeCalculator.droidCSToOldDroidGameplayScale(gameplayCS)

            // The 0.125f scale that was added before replay version 7 was in screen pixels. We need it in osu! pixels.
            val scaleAdjustment = 0.125f

            difficultyCS = CircleSizeCalculator.droidOldDifficultyScaleToDroidCS(
                difficultyScale - CircleSizeCalculator.droidOldDifficultyScaleScreenPixelsToOsuPixels(scaleAdjustment)
            )

            gameplayCS = CircleSizeCalculator.droidOldGameplayScaleToDroidCS(
                gameplayScale - CircleSizeCalculator.droidOldGameplayScaleScreenPixelsToOsuPixels(scaleAdjustment)
            )
        }

        ar = applySetting(ar)
        od = applySetting(od)
        hp = applySetting(hp)
    }

    override fun applyToHitObject(
        mode: GameMode,
        hitObject: HitObject,
        adjustmentMods: Iterable<IModFacilitatesAdjustment>
    ) {
        HitObjectGenerationUtils.reflectVerticallyAlongPlayfield(hitObject)
    }

    private fun applySetting(value: Float, ratio: Float = ADJUST_RATIO) = min(value * ratio, 10f)

    override fun deepCopy() = ModHardRock()

    companion object {
        private const val ADJUST_RATIO = 1.4f
    }
}