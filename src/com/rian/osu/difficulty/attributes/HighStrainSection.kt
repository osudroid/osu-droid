package com.rian.osu.difficulty.attributes

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject

/**
 * Represents a [Beatmap]'s section at which the strains of [HitObject]s are considerably high.
 */
data class HighStrainSection(
    /**
     * The index of the first [HitObject] in this [HighStrainSection] with respect to the full [Beatmap].
     */
    @JvmField
    val firstObjectIndex: Int,

    /**
     * The index of the last [HitObject] in this [HighStrainSection] with respect to the full [Beatmap].
     */
    @JvmField
    val lastObjectIndex: Int,

    /**
     * The summed strain of this [HighStrainSection].
     */
    @JvmField
    val sumStrain: Double
)