package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.constants.HitObjectType
import com.rian.osu.beatmap.constants.SampleBank
import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.math.Precision.almostEquals
import com.rian.osu.math.Vector2
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A parser for parsing a beatmap's hit objects section.
 */
object BeatmapHitObjectsParser : BeatmapSectionParser() {
    private val pipePropertyRegex = "[|]".toRegex()

    override fun parse(beatmap: Beatmap, line: String, scope: CoroutineScope?) = line
        .split(COMMA_PROPERTY_REGEX)
        .dropLastWhile {
            scope?.ensureActive()
            it.isEmpty()
        }
        .let {
            if (it.size < 4) {
                throw UnsupportedOperationException("Malformed hit object")
            }

            val time = beatmap.getOffsetTime(parseDouble(it[2]))
            val type = parseInt(it[3])

            var tempType = type

            val comboOffset = tempType and HitObjectType.ComboColorOffset.value shr 4
            tempType = tempType and HitObjectType.ComboColorOffset.value.inv()

            val isNewCombo = tempType and HitObjectType.NewCombo.value != 0

            val position = Vector2(
                parseInt(it[0]).toFloat(),
                parseInt(it[1]).toFloat()
            )

            val soundType = parseInt(it[4])
            val bankInfo = SampleBankInfo()

            scope?.ensureActive()

            val obj = when (HitObjectType.valueOf(type % 16)) {
                HitObjectType.Normal, HitObjectType.NormalNewCombo ->
                    createCircle(it, beatmap, time, position, beatmap.hitObjects.objects.isEmpty() || isNewCombo, comboOffset, bankInfo, scope)

                HitObjectType.Slider, HitObjectType.SliderNewCombo ->
                    createSlider(it, beatmap, time, position, beatmap.hitObjects.objects.isEmpty() || isNewCombo, comboOffset, soundType, bankInfo, scope)

                HitObjectType.Spinner ->
                    createSpinner(it, beatmap, time, isNewCombo, bankInfo, scope)

                else -> throw UnsupportedOperationException("Malformed hit object")
            }.also { h -> h.samples.addAll(convertSoundType(soundType, bankInfo)) }

            beatmap.hitObjects.add(obj)
        }

    private fun createCircle(pars: List<String>, beatmap: Beatmap, time: Double, position: Vector2, isNewCombo: Boolean, comboOffset: Int, bankInfo: SampleBankInfo, scope: CoroutineScope?) =
        HitCircle(
            time,
            position,
            // First object
            beatmap.hitObjects.objects.isEmpty() ||
                // The last object was a spinner
                beatmap.hitObjects.objects.lastOrNull() is Spinner || isNewCombo,
            comboOffset
        ).also { readCustomSampleBanks(bankInfo, pars.getOrNull(5), scope = scope) }

    @Throws(UnsupportedOperationException::class)
    private fun createSlider(pars: List<String>, beatmap: Beatmap, time: Double, startPosition: Vector2, isNewCombo: Boolean, comboOffset: Int, soundType: Int, bankInfo: SampleBankInfo, scope: CoroutineScope?): Slider {
        if (pars.size < 8) {
            throw UnsupportedOperationException("Malformed slider")
        }

        var repeatCount = parseInt(pars[6])
        val rawLength = max(0.0, parseDouble(pars[7]))

        if (repeatCount > 9000) {
            throw UnsupportedOperationException("Repeat count is way too high")
        }

        // osu!stable treated the first span of the slider as a repeat, but no repeats are happening
        repeatCount = max(0, repeatCount - 1)

        val curvePointsData = pars[5].split(pipePropertyRegex).dropLastWhile {
            scope?.ensureActive()
            it.isEmpty()
        }

        var sliderType = SliderPathType.parse(curvePointsData[0][0])
        val curvePoints = mutableListOf<Vector2>().apply { add(Vector2(0f)) }

        for (i in 1 until curvePointsData.size) {
            scope?.ensureActive()

            curvePointsData[i]
                .split(COLON_PROPERTY_REGEX)
                .dropLastWhile {
                    scope?.ensureActive()
                    it.isEmpty()
                }.let {
                    val curvePointPosition = Vector2(
                        parseInt(it[0]).toFloat(),
                        parseInt(it[1]).toFloat()
                    )

                    curvePoints.add(curvePointPosition - startPosition)
                }
        }

        curvePoints.let {
            // A special case for Catmull sliders where the first control point is in the position of the slider.
            // This results in a duplicate (0, 0) point in the path.
            if (sliderType == SliderPathType.Catmull && it.size >= 2 && it[0] == it[1]) {
                it.removeFirst()
            }

            // Edge-case rules (to match stable).
            if (sliderType === SliderPathType.PerfectCurve) {
                if (it.size != 3) {
                    sliderType = SliderPathType.Bezier
                } else if (almostEquals(
                        0f,
                        (it[1].y - it[0].y) * (it[2].x - it[0].x) -
                                (it[1].x - it[0].x) * (it[2].y - it[0].y)
                    )
                ) {
                    // osu-stable special-cased co-linear perfect curves to a linear path
                    sliderType = SliderPathType.Linear
                }
            }
        }

        scope?.ensureActive()

        val path = SliderPath(sliderType, curvePoints, rawLength)

        readCustomSampleBanks(bankInfo, pars.getOrNull(10), true, scope)

        // One node for each repeat + the start and end nodes
        val nodes = repeatCount + 2

        val nodeBankInfo = mutableListOf<SampleBankInfo>().apply {
            // Populate node sample bank info with the default hit object sample bank
            for (i in 0 until nodes) {
                add(bankInfo.copy())
            }

            scope?.ensureActive()

            // Read any per-node sample banks
            val sets = pars.getOrNull(9)?.split(pipePropertyRegex)
            if (sets != null) {
                for (i in 0 until min(sets.size, nodes)) {
                    readCustomSampleBanks(this[i], sets[i])
                }
            }
        }

        val nodeSoundTypes = mutableListOf<Int>().apply {
            // Populate node sound types with the default hit object sound type
            for (i in 0 until nodes) {
                add(soundType)
            }

            scope?.ensureActive()

            // Read any per-node sound types
            val adds = pars.getOrNull(8)?.split(pipePropertyRegex)
            if (adds != null) {
                for (i in 0 until min(adds.size, nodes)) {
                    set(i, parseInt(adds[i]))
                }
            }
        }

        // Generate the final per-node samples
        val nodeSamples = mutableListOf<MutableList<HitSampleInfo>>().apply {
            for (i in 0 until nodes) {
                add(convertSoundType(nodeSoundTypes[i], nodeBankInfo[i]))
            }
        }

        val difficultyControlPoint = beatmap.controlPoints.difficulty.controlPointAt(time)

        scope?.ensureActive()

        return Slider(
            time,
            startPosition,
            repeatCount,
            path,
            // First object
            beatmap.hitObjects.objects.isEmpty() ||
                // The last object was a spinner
                beatmap.hitObjects.objects.lastOrNull() is Spinner || isNewCombo,
            comboOffset,
            nodeSamples
        ).also {
            it.tickDistanceMultiplier = when {
                !difficultyControlPoint.generateTicks -> Double.POSITIVE_INFINITY

                // Prior to v8, speed multipliers don't adjust for how many ticks are generated over the same distance.
                // This results in more (or less) ticks being generated in <v8 maps for the same time duration.
                beatmap.formatVersion < 8 -> 1 / difficultyControlPoint.speedMultiplier

                else -> 1.0
            }
        }
    }

    private fun createSpinner(pars: List<String>, beatmap: Beatmap, time: Double, isNewCombo: Boolean, bankInfo: SampleBankInfo, scope: CoroutineScope?) =
        Spinner(time, beatmap.getOffsetTime(parseInt(pars[5])).toDouble(), isNewCombo).also {
            readCustomSampleBanks(bankInfo, pars.getOrNull(6), scope = scope)
        }

    /**
     * Converts the sound type of hit object to hit samples.
     *
     * @param soundType The sound type.
     * @param bankInfo The sample bank info of the hit object.
     * @return A list of [HitSampleInfo] representing the hit samples of the hit object.
     */
    private fun convertSoundType(soundType: Int, bankInfo: SampleBankInfo) = mutableListOf<HitSampleInfo>().apply {
        if (bankInfo.filename.isNotEmpty()) {
            add(FileHitSampleInfo(bankInfo.filename, bankInfo.volume))
        } else {
            add(
                BankHitSampleInfo(BankHitSampleInfo.HIT_NORMAL, bankInfo.normal, bankInfo.customSampleBank, bankInfo.volume,
                    // If the sound type doesn't have the Normal flag set, attach it anyway as a layered sample.
                    // None also counts as a normal non-layered sample: https://osu.ppy.sh/help/wiki/osu!_File_Formats/Osu_(file_format)#hitsounds
                    soundType != HitSoundType.None.bit && HitSoundType.Normal !in soundType
                )
            )
        }

        fun addBankSample(name: String) = add(BankHitSampleInfo(name, bankInfo.add, bankInfo.customSampleBank, bankInfo.volume))

        if (HitSoundType.Finish in soundType) {
            addBankSample(BankHitSampleInfo.HIT_FINISH)
        }

        if (HitSoundType.Whistle in soundType) {
            addBankSample(BankHitSampleInfo.HIT_WHISTLE)
        }

        if (HitSoundType.Clap in soundType) {
            addBankSample(BankHitSampleInfo.HIT_CLAP)
        }
    }

    /**
     * Populates a sample bank info with custom sample bank information.
     *
     * @param bankInfo The sample bank info to populate.
     * @param str The information.
     * @param banksOnly Whether to only convert banks.
     */
    private fun readCustomSampleBanks(bankInfo: SampleBankInfo, str: String?, banksOnly: Boolean = false, scope: CoroutineScope? = null) {
        if (str.isNullOrEmpty()) {
            return
        }

        scope?.ensureActive()

        val s = str.split(COLON_PROPERTY_REGEX)

        bankInfo.normal = SampleBank.parse(parseInt(s[0]))
        bankInfo.add = SampleBank.parse(parseInt(s[1])).takeIf { it != SampleBank.Normal } ?: bankInfo.normal

        if (banksOnly) {
            return
        }

        if (s.size > 2) {
            bankInfo.customSampleBank = parseInt(s[2])
        }

        if (s.size > 3) {
            bankInfo.volume = max(0, parseInt(s[3]))
        }

        if (s.size > 4) {
            bankInfo.filename = s[4]
        }
    }
}

/**
 * Represents a hit object specific sample bank.
 */
private data class SampleBankInfo(
    /**
     * The name of the sample bank file, if this sample bank uses custom samples.
     */
    @JvmField
    var filename: String = "",

    /**
     * The main sample bank.
     */
    @JvmField
    var normal: SampleBank = SampleBank.None,

    /**
     * The addition sample bank.
     */
    @JvmField
    var add: SampleBank = SampleBank.None,

    /**
     * The volume at which the sample bank is played.
     *
     * If this is 0, the underlying control point's volume should be used instead.
     */
    @JvmField
    var volume: Int = 0,

    /**
     * The index of the sample bank, if this sample bank uses custom samples.
     *
     * If this is 0, the underlying control point's sample index should be used instead.
     */
    @JvmField
    var customSampleBank: Int = 0
)

/**
 * Hit sound types that are provided by the game.
 */
private enum class HitSoundType(
    /**
     * The bit of this hit sound.
     */
    @JvmField
    val bit: Int
) {
    None(0),
    Normal(1),
    Whistle(1 shl 1),
    Finish(1 shl 2),
    Clap(1 shl 3)
}

private operator fun Int.contains(type: HitSoundType) = this and type.bit != 0