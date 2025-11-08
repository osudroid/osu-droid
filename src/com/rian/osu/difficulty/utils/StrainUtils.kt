package com.rian.osu.difficulty.utils

object StrainUtils {
    @JvmStatic
    fun countTopWeightedSliders(sliderStrains: List<Double>, difficultyValue: Double): Double {
        if (sliderStrains.isEmpty()) {
            return 0.0
        }

        // What would the top strain be if all strain values were identical
        val consistentTopStrain = difficultyValue / 10

        if (consistentTopStrain == 0.0) {
            return 0.0
        }

        // Use a weighted sum of all strains. Constants are arbitrary and give nice values
        return sliderStrains.sumOf { DifficultyCalculationUtils.logistic(it / consistentTopStrain, 0.88, 10.0, 1.1) }
    }
}