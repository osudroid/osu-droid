package com.rian.osu.difficulty.attributes

/**
 * Holds data that can be used to calculate osu!standard performance points.
 */
class StandardDifficultyAttributes : DifficultyAttributes() {
    /**
     * The difficulty corresponding to the speed skill.
     */
    @JvmField
    var speedDifficulty = 0.0
}