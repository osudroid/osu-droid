package com.rian.osu.replay

import com.rian.osu.difficulty.attributes.HighStrainSection

/**
 * An extension of [HighStrainSection] for assigning dragged sections in three-finger detection.
 */
class ThreeFingerBeatmapSection(
    section: HighStrainSection,

    /**
     * The [ThreeFingerObject]s in this section.
     */
    @JvmField
    val objects: List<ThreeFingerObject>,
) : HighStrainSection(section.firstObjectIndex, section.lastObjectIndex, section.sumStrain)