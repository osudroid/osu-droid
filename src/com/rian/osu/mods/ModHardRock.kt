package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.math.Vector2
import com.rian.osu.utils.CircleSizeCalculator
import kotlin.math.min

/**
 * Represents the Hard Rock mod.
 */
class ModHardRock : Mod(), IModApplicableToDifficulty, IModApplicableToHitObject {
    override val name = "Hard Rock"
    override val acronym = "HR"
    override val type = ModType.DifficultyIncrease
    override val textureNameSuffix = "hardrock"
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + ModEasy::class

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
        // Reflect the position of the hit object.
        hitObject.position = reflectVector(hitObject.position)

        if (hitObject !is Slider) {
            return
        }

        // Reflect the control points of the slider. This will reflect the positions of head and tail circles.
        hitObject.path = SliderPath(
            hitObject.path.pathType,
            hitObject.path.controlPoints.map { reflectControlPoint(it) },
            hitObject.path.expectedDistance
        )

        // Reflect the position of slider ticks and repeats.
        for (i in 1 until hitObject.nestedHitObjects.size - 1) {
            val obj = hitObject.nestedHitObjects[i]

            obj.position = reflectVector(obj.position)
        }
    }

    private fun applySetting(value: Float, ratio: Float = ADJUST_RATIO) = min(value * ratio, 10f)

    override fun equals(other: Any?) = other === this || other is ModHardRock
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModHardRock()

    companion object {
        private const val ADJUST_RATIO = 1.4f

        private fun reflectVector(vector: Vector2) = Vector2(vector.x, 384 - vector.y)
        private fun reflectControlPoint(vector: Vector2) = Vector2(vector.x, -vector.y)
    }
}