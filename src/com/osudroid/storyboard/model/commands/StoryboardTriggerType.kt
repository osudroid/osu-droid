package com.osudroid.storyboard.model.commands

import com.osudroid.beatmaps.constants.SampleBank
import com.osudroid.beatmaps.hitobjects.BankHitSampleInfo

/**
 * Represents the condition of a storyboard `T` (trigger) command.
 */
sealed class StoryboardTriggerType {
    /**
     * Activated while the player is in a passing state.
     */
    object Passing : StoryboardTriggerType()

    /**
     * Activated while the player is in a failing state.
     */
    object Failing : StoryboardTriggerType()

    /**
     * Activated when a hit object is hit.
     */
    object HitObjectHit : StoryboardTriggerType()

    /**
     * A trigger whose condition is not supported and therefore never activates. Its commands are
     * still parsed into the trigger so that they do not leak into the element's own timelines.
     */
    object Unsupported : StoryboardTriggerType()

    /**
     * Activated when a hit sample matching the given constraints is played.
     *
     * The full trigger name follows the format
     * `HitSound[SampleSet][AdditionsSampleSet][Addition][CustomSampleSet]`, where every part is
     * optional.
     */
    class HitSound(
        /**
         * The [SampleBank] the sample must belong to, or `null` for any bank.
         */
        @JvmField
        val sampleBank: SampleBank?,

        /**
         * The [SampleBank] the addition sample must belong to, or `null` for any bank.
         */
        @JvmField
        val additionsSampleBank: SampleBank?,

        /**
         * The addition sample name (`hitwhistle`, `hitfinish` or `hitclap`) the sample must
         * match, or `null` for any hit sample.
         */
        @JvmField
        val addition: String?,

        /**
         * The custom sample bank index the sample must use, or `0` for any index.
         */
        @JvmField
        val customSampleBank: Int
    ) : StoryboardTriggerType() {
        /**
         * Determines whether the full set of hit samples played for a single hit object matches
         * this trigger, mirroring osu!lazer's `HitSampleTriggerDefinition.Matches`.
         *
         * The normal sample (`hitnormal`) is checked against [sampleBank]; every other (addition)
         * sample is checked against [addition] and [additionsSampleBank]. Both a bank and an
         * addition name may need to be satisfied by *different* played samples (e.g.
         * `HitSoundNormalSoftClap` requires a `hitnormal` sample on the `Normal` bank AND a
         * `hitclap` sample on the `Soft` bank), so the whole set must be evaluated together rather
         * than sample-by-sample.
         *
         * @param samples The samples played for a single hit object.
         */
        fun matches(samples: List<BankHitSampleInfo>): Boolean {
            var foundAddition = addition == null
            var additionBankCorrect = additionsSampleBank == null

            for (sample in samples) {
                if (customSampleBank > 0 && sample.customSampleBank != customSampleBank) {
                    return false
                }

                if (sample.name == BankHitSampleInfo.HIT_NORMAL) {
                    if (sampleBank != null && sample.bank != sampleBank) {
                        return false
                    }
                } else {
                    if (addition != null && sample.name == addition) {
                        foundAddition = true
                    }

                    if (additionsSampleBank != null && sample.bank == additionsSampleBank) {
                        additionBankCorrect = true
                    }
                }
            }

            return foundAddition && additionBankCorrect
        }
    }

    companion object {
        private const val HIT_SOUND_PREFIX = "HitSound"
        private val TRAILING_DIGITS_REGEX = "(\\d+)$".toRegex()

        /**
         * Parses a trigger type from its name (e.g. `HitSoundSoftWhistle`, `Passing`).
         *
         * @param value The value to parse.
         * @return The parsed [StoryboardTriggerType], or `null` if the value is not a valid
         * trigger type.
         */
        @JvmStatic
        fun parse(value: String): StoryboardTriggerType? {
            if (value == "Passing") {
                return Passing
            }

            if (value == "Failing") {
                return Failing
            }

            if (value == "HitObjectHit") {
                return HitObjectHit
            }

            // Hit sound trigger names are matched case-insensitively, as in osu!lazer.
            if (!value.startsWith(HIT_SOUND_PREFIX, ignoreCase = true)) {
                return null
            }

            var rest = value.substring(HIT_SOUND_PREFIX.length)

            val customSampleBank = TRAILING_DIGITS_REGEX.find(rest)?.value?.let {
                rest = rest.removeSuffix(it)
                it.toIntOrNull()
            } ?: 0

            // Returns the bank consumed at the current position paired with whether a token was
            // present at all. The bank is `null` for a literal "All" (no filter), which must
            // still be distinguished from "no token present".
            fun takeBank(): Pair<SampleBank?, Boolean> {
                for (bank in arrayOf(SampleBank.Normal, SampleBank.Soft, SampleBank.Drum)) {
                    if (rest.startsWith(bank.name, ignoreCase = true)) {
                        rest = rest.substring(bank.name.length)
                        return bank to true
                    }
                }

                if (rest.startsWith("All", ignoreCase = true)) {
                    rest = rest.substring(3)
                    return null to true
                }

                return null to false
            }

            val (bank1, bank1Present) = takeBank()
            val (bank2, bank2Present) = takeBank()

            val addition = when (rest.lowercase()) {
                "whistle" -> "hitwhistle"
                "finish" -> "hitfinish"
                "clap" -> "hitclap"
                "" -> null
                // Unknown trailing text makes the whole trigger invalid.
                else -> return null
            }

            // A single sample set token immediately followed by an addition name describes that
            // addition's bank rather than the normal sample's bank (e.g. `HitSoundSoftClap` means
            // "a clap on the Soft bank", not "a clap with the normal sample on the Soft bank").
            // This matches stable's and lazer's trigger name parsing.
            val bank1IsAddition = bank1Present && !bank2Present && addition != null

            val sampleBank = if (bank1IsAddition) bank2 else bank1
            val additionsSampleBank = if (bank1IsAddition) bank1 else bank2

            return HitSound(sampleBank, additionsSampleBank, addition, customSampleBank)
        }
    }
}
