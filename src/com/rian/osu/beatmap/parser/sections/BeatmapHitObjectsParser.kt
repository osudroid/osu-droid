package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.constants.HitObjectType
import com.rian.osu.beatmap.constants.SampleBank
import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.math.Precision.almostEqualsNumber
import com.rian.osu.math.Vector2
import kotlin.math.max
import kotlin.math.min

/**
 * A parser for parsing a beatmap's hit objects section.
 */
class BeatmapHitObjectsParser : BeatmapSectionParser() {
    private var extraComboColorOffset = 0
    private var forceNewCombo = false

    override fun parse(beatmap: Beatmap, line: String) = line
        .split(",".toRegex())
        .dropLastWhile { it.isEmpty() }
        .let {
            if (it.size < 4) {
                throw UnsupportedOperationException("Malformed hit object")
            }

            val time = beatmap.getOffsetTime(parseDouble(it[2]))
            val type = parseInt(it[3])

            var tempType = type

            val comboColorOffset = (tempType and HitObjectType.ComboColorOffset.value) shr 4
            tempType = tempType and HitObjectType.ComboColorOffset.value.inv()

            val isNewCombo = (tempType and HitObjectType.NewCombo.value) != 0

            val position = Vector2(
                parseInt(it[0]).toFloat(),
                parseInt(it[1]).toFloat()
            )

            val soundType = HitSoundType.parse(parseInt(it[4]))
            val bankInfo = SampleBankInfo()

            val obj = when (HitObjectType.valueOf(type % 16)) {
                HitObjectType.Normal, HitObjectType.NormalNewCombo ->
                    createCircle(it, time, position, beatmap.hitObjects.objects.isEmpty() || isNewCombo, comboColorOffset, bankInfo)

                HitObjectType.Slider, HitObjectType.SliderNewCombo ->
                    createSlider(it, beatmap, time, position, beatmap.hitObjects.objects.isEmpty() || isNewCombo, comboColorOffset, soundType, bankInfo)

                HitObjectType.Spinner ->
                    createSpinner(it, beatmap, time, isNewCombo, comboColorOffset, bankInfo)

                else -> throw UnsupportedOperationException("Malformed hit object")
            }.apply {
                samples.addAll(convertSoundType(soundType, bankInfo))
            }

            beatmap.rawHitObjects.add(line)
            beatmap.hitObjects.add(obj)
        }

    override fun reset() {
        super.reset()

        forceNewCombo = false
        extraComboColorOffset = 0
    }

    private fun createCircle(pars: List<String>, time: Double, position: Vector2, isNewCombo: Boolean, comboColorOffset: Int, bankInfo: SampleBankInfo) =
        HitCircle(time, position, isNewCombo || forceNewCombo, comboColorOffset + extraComboColorOffset).also {
            readCustomSampleBanks(bankInfo, pars.getOrNull(5))

            forceNewCombo = false
            extraComboColorOffset = 0
        }

    @Throws(UnsupportedOperationException::class)
    private fun createSlider(pars: List<String>, beatmap: Beatmap, time: Double, startPosition: Vector2, isNewCombo: Boolean, comboColorOffset: Int, soundType: HitSoundType, bankInfo: SampleBankInfo): Slider {
        if (pars.size < 8) {
            throw UnsupportedOperationException("Malformed slider")
        }

        val repeat = parseInt(pars[6])
        val rawLength = max(0.0, parseDouble(pars[7]))

        if (repeat > 9000) {
            throw UnsupportedOperationException("Repeat count is way too high")
        }

        val curvePointsData = pars[5].split("[|]".toRegex()).dropLastWhile { it.isEmpty() }
        var sliderType = SliderPathType.parse(curvePointsData[0][0])

        val curvePoints = mutableListOf<Vector2>().apply { add(Vector2(0f)) }

        curvePointsData.run {
            for (i in 1 until size) {
                this[i].split(":".toRegex()).dropLastWhile { it.isEmpty() }.let {
                    val curvePointPosition = Vector2(
                        parseInt(it[0]).toFloat(),
                        parseInt(it[1]).toFloat()
                    )

                    curvePoints.add(curvePointPosition - startPosition)
                }
            }
        }

        curvePoints.let {
            // A special case for old beatmaps where the first
            // control point is in the position of the slider.
            if (it.size >= 2 && it[0] == it[1]) {
                it.removeFirst()
            }

            // Edge-case rules (to match stable).
            if (sliderType === SliderPathType.PerfectCurve) {
                if (it.size != 3) {
                    sliderType = SliderPathType.Bezier
                } else if (almostEqualsNumber(
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

        val path = SliderPath(sliderType, curvePoints, rawLength)

        readCustomSampleBanks(bankInfo, pars.getOrNull(10))

        // One node for each repeat + the start and end nodes
        val nodes = repeat + 1

        val nodeBankInfo = mutableListOf<SampleBankInfo>().apply {
            // Populate node sample bank info with the default hit object sample bank
            for (i in 0 until nodes) {
                add(bankInfo.copy())
            }

            // Read any per-node sample banks
            val sets = pars.getOrNull(9)?.split("\\|".toRegex())
            if (sets != null) {
                for (i in 0 until min(sets.size, nodes)) {
                    readCustomSampleBanks(this[i], sets[i])
                }
            }
        }

        val nodeSoundTypes = mutableListOf<HitSoundType>().apply {
            // Populate node sound types with the default hit object sound type
            for (i in 0 until nodes) {
                add(soundType)
            }

            // Read any per-node sound types
            val adds = pars.getOrNull(8)?.split("\\|".toRegex())
            if (adds != null) {
                for (i in 0 until min(adds.size, nodes)) {
                    set(i, HitSoundType.parse(parseInt(adds[i])))
                }
            }
        }

        // Generate the final per-node samples
        val nodeSamples = mutableListOf<MutableList<HitSampleInfo>>().apply {
            for (i in 0 until nodes) {
                add(convertSoundType(nodeSoundTypes[i], nodeBankInfo[i]))
            }
        }

        forceNewCombo = false
        extraComboColorOffset = 0

        val difficultyControlPoint = beatmap.controlPoints.difficulty.controlPointAt(time)

        return Slider(
            time,
            startPosition,
            repeat,
            path,
            isNewCombo || forceNewCombo,
            comboColorOffset + extraComboColorOffset,
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

    private fun createSpinner(pars: List<String>, beatmap: Beatmap, time: Double, isNewCombo: Boolean, comboColorOffset: Int, bankInfo: SampleBankInfo) =
        Spinner(time, beatmap.getOffsetTime(parseInt(pars[5])).toDouble()).also {
            readCustomSampleBanks(bankInfo, pars.getOrNull(6))

            // Spinners don't create the new combo themselves, but force the next non-spinner hit object to create a new combo.
            // Their combo offset is still added to that next hit object's combo index.
            forceNewCombo = forceNewCombo || beatmap.formatVersion <= 8 || isNewCombo
            extraComboColorOffset += comboColorOffset
        }

    /**
     * Converts the sound type of hit object to hit samples.
     *
     * @param type The sound type.
     * @param bankInfo The sample bank info of the hit object.
     * @return A list of [HitSampleInfo] representing the hit samples of the hit object.
     */
    private fun convertSoundType(type: HitSoundType, bankInfo: SampleBankInfo) = mutableListOf<HitSampleInfo>().apply {
        if (bankInfo.filename.isNotEmpty()) {
            add(FileHitSampleInfo(bankInfo.filename, bankInfo.volume))
            return this
        }

        add(
            BankHitSampleInfo(BankHitSampleInfo.HIT_NORMAL, bankInfo.normal, bankInfo.customSampleBank, bankInfo.volume,
                // If the sound type doesn't have the Normal flag set, attach it anyway as a layered sample.
                // None also counts as a normal non-layered sample: https://osu.ppy.sh/help/wiki/osu!_File_Formats/Osu_(file_format)#hitsounds
                type != HitSoundType.None && (type.bit and HitSoundType.Normal.bit) == 0
            )
        )

        fun addBankSample(name: String) = add(BankHitSampleInfo(name, bankInfo.add, bankInfo.customSampleBank, bankInfo.volume))

        if (type.bit and HitSoundType.Finish.bit != 0) {
            addBankSample(BankHitSampleInfo.HIT_FINISH)
        }

        if (type.bit and HitSoundType.Whistle.bit != 0) {
            addBankSample(BankHitSampleInfo.HIT_WHISTLE)
        }

        if (type.bit and HitSoundType.Clap.bit != 0) {
            addBankSample(BankHitSampleInfo.HIT_CLAP)
        }
    }

    /**
     * Populates a sample bank info with custom sample bank information.
     *
     * @param bankInfo The sample bank info to populate.
     * @param str The information.
     */
    private fun readCustomSampleBanks(bankInfo: SampleBankInfo, str: String?) {
        if (str.isNullOrEmpty()) {
            return
        }

        val s = str.split(":".toRegex())

        bankInfo.normal = SampleBank.parse(parseInt(s[0]))
        bankInfo.add = SampleBank.parse(parseInt(s[1])).takeIf { it != SampleBank.Normal } ?: bankInfo.normal

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
        Clap(1 shl 3);

        companion object {
            /**
             * Converts an integer value to its hitsound type counterpart.
             *
             * @param value The value to convert.
             * @return The hitsound type counterpart of the given value.
             */
            @JvmStatic
            fun parse(value: Int) =
                when (value) {
                    1 -> Normal
                    1 shl 1 -> Whistle
                    1 shl 2 -> Finish
                    1 shl 3 -> Clap
                    else -> None
                }
        }
    }
}
