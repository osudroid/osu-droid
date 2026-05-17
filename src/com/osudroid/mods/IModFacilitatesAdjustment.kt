package com.osudroid.mods

import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.sections.BeatmapDifficulty

/**
 * An interface for [Mod]s that aids in adjustments to a [HitObject] or [BeatmapDifficulty].
 *
 * [Mod]s marked by this interface will be passed into [IModApplicableToDifficulty]s and [IModApplicableToHitObject]s.
 */
interface IModFacilitatesAdjustment