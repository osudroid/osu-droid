package com.rian.osu.mods

import com.edlplan.framework.easing.Easing
import org.junit.Assert
import org.junit.Test

class ModApproachDifferentTest {
    @Test
    fun `Test animation style`() {
        val mod = ModApproachDifferent()

        listOf(
            ModApproachDifferent.AnimationStyle.Linear to Easing.None,
            ModApproachDifferent.AnimationStyle.Gravity to Easing.InBack,
            ModApproachDifferent.AnimationStyle.InOut1 to Easing.InOutCubic,
            ModApproachDifferent.AnimationStyle.InOut2 to Easing.InOutQuint,
            ModApproachDifferent.AnimationStyle.Accelerate1 to Easing.In,
            ModApproachDifferent.AnimationStyle.Accelerate2 to Easing.InCubic,
            ModApproachDifferent.AnimationStyle.Accelerate3 to Easing.InQuint,
            ModApproachDifferent.AnimationStyle.Decelerate1 to Easing.Out,
            ModApproachDifferent.AnimationStyle.Decelerate2 to Easing.OutCubic,
            ModApproachDifferent.AnimationStyle.Decelerate3 to Easing.OutQuint,
            ModApproachDifferent.AnimationStyle.BounceIn to Easing.InBounce,
            ModApproachDifferent.AnimationStyle.BounceOut to Easing.OutBounce,
            ModApproachDifferent.AnimationStyle.BounceInOut to Easing.InOutBounce
        ).forEach { (style, expectedEasing) ->
            mod.style = style
            Assert.assertEquals("Easing mismatch for style $style", expectedEasing, mod.easing)
        }
    }
}