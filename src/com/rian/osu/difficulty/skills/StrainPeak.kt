package com.rian.osu.difficulty.skills

import kotlin.math.round

/**
 * Data class for variable length strain.
 */
class StrainPeak(val value: Double, sectionLength: Double) : Comparable<StrainPeak> {
    val sectionLength = round(sectionLength)

    override fun compareTo(other: StrainPeak) = value.compareTo(other.value)
}
