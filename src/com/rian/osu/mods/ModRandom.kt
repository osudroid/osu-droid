package com.rian.osu.mods

import com.reco1l.toolkt.roundBy
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.math.Random
import com.rian.osu.mods.settings.*
import com.rian.osu.utils.HitObjectGenerationUtils
import kotlin.math.exp
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the Random mod.
 */
class ModRandom : Mod(), IModApplicableToBeatmap {
    override val name = "Random"
    override val acronym = "RD"
    override val description = "It never gets boring!"
    override val type = ModType.Conversion

    /**
     * The seed that is used to generate the random numbers.
     *
     * If `null`, a random seed will be generated.
     */
    var seed by NullableIntegerModSetting(
        name = "Seed",
        key = "seed",
        valueFormatter = { it?.toString() ?: "" },
        defaultValue = null,
        minValue = 0,
        orderPosition = 0,
        useManualInput = true
    )

    /**
     * Defines how sharp the angles of [HitObject]s should be.
     */
    var angleSharpness by FloatModSetting(
        name = "Angle sharpness",
        key = "angleSharpness",
        valueFormatter = { it.roundBy(1).toString() },
        defaultValue = 7f,
        minValue = 1f,
        maxValue = 10f,
        step = 0.1f,
        precision = 1
    )

    private var random: Random? = null

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        if (seed == null) {
            seed = Random.nextInt()
        }

        random = Random(seed!!)

        val positionInfos = HitObjectGenerationUtils.generatePositionInfos(beatmap.hitObjects.objects, scope)

        // Offsets the angles of all hit objects in a "section" by the same amount.
        var sectionOffset = 0f
        // Whether the angles are positive or negative (clockwise or counter-clockwise flow).
        var flowDirection = false

        for (i in positionInfos.indices) {
            scope?.ensureActive()

            val positionInfo = positionInfos[i]

            if (shouldStartNewSection(beatmap, positionInfos, i)) {
                sectionOffset = getRandomOffset(0.0008f)
                flowDirection = !flowDirection
            }

            if (positionInfo.hitObject is Slider && random!!.nextDouble() < 0.5) {
                HitObjectGenerationUtils.flipSliderInPlaceHorizontally(positionInfo.hitObject, scope)
            }

            if (i == 0) {
                positionInfo.distanceFromPrevious =
                    (random!!.nextDouble() * HitObjectGenerationUtils.playfieldCenter.y).toFloat()

                positionInfo.relativeAngle = (random!!.nextDouble() * 2 * Math.PI - Math.PI).toFloat()
            } else {
                // Offsets only the angle of the current hit object if a flow change occurs.
                var flowChangeOffset = 0f
                // Offsets only the angle of the current hit object.
                val oneTimeOffset = getRandomOffset(0.002f)

                if (shouldApplyFlowChange(positionInfos, i)) {
                    flowChangeOffset = getRandomOffset(0.002f)
                    flowDirection = !flowDirection
                }

                val totalOffset =
                    // sectionOffset and oneTimeOffset should mainly affect patterns with large spacing.
                    (sectionOffset + oneTimeOffset) * positionInfo.distanceFromPrevious +
                    // flowChangeOffset should mainly affect streams.
                    flowChangeOffset * (playfieldDiagonal - positionInfo.distanceFromPrevious)

                positionInfo.relativeAngle =
                    getRelativeTargetAngle(positionInfo.distanceFromPrevious, totalOffset, flowDirection)
            }
        }

        val repositionedObjects = HitObjectGenerationUtils.repositionHitObjects(positionInfos, scope)

        for (i in repositionedObjects.indices) {
            scope?.ensureActive()

            beatmap.hitObjects.objects[i] = repositionedObjects[i]
        }
    }

    private fun getRandomOffset(stdDev: Float): Float {
        val angleSharpness = getModSettingDelegate<FloatModSetting>(::angleSharpness)

        // Range: [0.5, 2]
        // Higher angle sharpness -> lower multiplier
        val customMultiplier =
            (1.5f * angleSharpness.maxValue - angleSharpness.value) /
            (1.5f * angleSharpness.maxValue - angleSharpness.defaultValue)

        return HitObjectGenerationUtils.randomGaussian(random!!, 0f, stdDev * customMultiplier)
    }

    /**
     * @param targetDistance The target distance between the previous and the current [HitObject].
     * @param offset The angle (in radians) by which the target angle should be offset.
     * @param flowDirection Whether the relative angle should be positive (`false`) or negative (`true`).
     */
    private fun getRelativeTargetAngle(targetDistance: Float, offset: Float, flowDirection: Boolean): Float {
        val angleSharpnessDelegate = getModSettingDelegate<FloatModSetting>(::angleSharpness)

        // Range: [0.1, 1]
        val angleSharpness = angleSharpnessDelegate.value / angleSharpnessDelegate.maxValue
        // Range: [0, 0.9]
        val angleWideness = 1 - angleSharpness

        // Range: [-60, 30]
        val customOffsetX = angleSharpness * 100 - 70
        // Range: [-0.075, 0.15]
        val customOffsetY = angleWideness * 0.25f - 0.075f

        val angle =
            (2.16 / (1 + 200 * exp(0.036 * (targetDistance + customOffsetX * 2 - 310))) + 0.5).toFloat() +
            offset + customOffsetY

        val relativeAngle = Math.PI.toFloat() - angle

        return if (flowDirection) -relativeAngle else relativeAngle
    }

    /**
     * Determines whether a new section should be started at the current [HitObject].
     */
    private fun shouldStartNewSection(
        beatmap: Beatmap,
        positionInfos: List<HitObjectGenerationUtils.HitObjectPositionInfo>,
        i: Int
    ): Boolean {
        if (i == 0) {
            return true
        }

        // Exclude new-combo-spam and 1-2-combos.
        val previousObjectStartedCombo =
            positionInfos[max(0, i - 2)].hitObject.indexInCurrentCombo > 1 && positionInfos[i - 1].hitObject.isNewCombo

        val previousObjectWasOnDownBeat =
            HitObjectGenerationUtils.isHitObjectOnBeat(beatmap, positionInfos[i - 1].hitObject, true)

        val previousObjectWasOnBeat =
            HitObjectGenerationUtils.isHitObjectOnBeat(beatmap, positionInfos[i - 1].hitObject)

        return (previousObjectStartedCombo && random!!.nextDouble() < 0.6f) ||
                previousObjectWasOnDownBeat ||
                (previousObjectWasOnBeat && random!!.nextDouble() < 0.4f)
    }

    private fun shouldApplyFlowChange(
        positionInfos: List<HitObjectGenerationUtils.HitObjectPositionInfo>,
        i: Int
    ): Boolean {
        // Exclude new-combo-spam and 1-2-combos.
        val previousObjectStartedCombo =
            positionInfos[max(0, i - 2)].hitObject.indexInCurrentCombo > 1 && positionInfos[i - 1].hitObject.isNewCombo

        return previousObjectStartedCombo && random!!.nextDouble() < 0.6
    }

    override fun deepCopy() = ModRandom().also {
        it.seed = seed
        it.angleSharpness = angleSharpness
    }

    companion object {
        private val playfieldDiagonal = HitObjectGenerationUtils.playfieldSize.length
    }
}