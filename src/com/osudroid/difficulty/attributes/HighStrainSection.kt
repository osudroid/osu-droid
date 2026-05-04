package com.osudroid.difficulty.attributes

import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.hitobjects.HitObject

/**
 * Represents a [Beatmap]'s section at which the strains of [HitObject]s are considerably high.
 */
open class HighStrainSection(
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