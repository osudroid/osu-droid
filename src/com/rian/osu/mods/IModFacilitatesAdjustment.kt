package com.rian.osu.mods

import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * An interface for [Mod]s that aids in adjustments to a [HitObject] or [BeatmapDifficulty].
 *
 * [Mod]s marked by this interface will be passed into [IModApplicableToDifficulty]s and [IModApplicableToHitObject]s.
 */
interface IModFacilitatesAdjustment