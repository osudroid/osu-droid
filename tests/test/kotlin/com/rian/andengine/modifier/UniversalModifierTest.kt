package com.rian.andengine.modifier

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.UIDummyComponent
import com.reco1l.andengine.component.UIComponent
import com.rian.andengine.timing.ManualFramedClock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UniversalModifierTest {
    private lateinit var clock: ManualFramedClock
    private lateinit var component: UIDummyComponent

    @Before
    fun setUp() {
        clock = ManualFramedClock()
        component = UIDummyComponent().also { it.clock = clock }

        // Perform the initial load.
        advanceTo(0f)
    }

    private fun advanceTo(time: Float) {
        clock.currentTime = time
        component.onUpdate(0f)
    }

    //region Easing

    @Test
    fun `Overshooting easing exceeds the final value mid-modifier`() {
        component.setScale(0f)
        component.scaleTo(1f, 1f, Easing.OutBack)

        var maxScale = 0f

        for (i in 1..99) {
            advanceTo(i / 100f)
            maxScale = maxOf(maxScale, component.scaleX)
        }

        assertTrue("OutBack should overshoot past 1, max was $maxScale", maxScale > 1f)
    }

    @Test
    fun `Overshooting easing settles on the final value at the end`() {
        component.setScale(0f)
        component.scaleTo(1f, 1f, Easing.OutBack)

        advanceTo(1f)

        assertEquals(1f, component.scaleX, 0f)
    }

    @Test
    fun `Applying before start time keeps the initial value`() {
        component.alpha = 0f

        val modifier = component.fadeTo(1f, 1f, Easing.OutBack)
        modifier.apply(-0.5f)

        assertEquals(0f, component.alpha, 0f)
    }

    //endregion

    //region Finishing

    @Test
    fun `Finishing a type does not finish other types in the same group`() {
        component.scaleToX(2f, 1f)
        component.scaleToY(3f, 1f)

        component.finishModifiers(type = ModifierType.ScaleX)

        assertEquals(2f, component.scaleX, 0f)
        assertEquals(1f, component.scaleY, 0f)

        // The Y modifier must still be running.
        advanceTo(0.5f)
        assertEquals(2f, component.scaleY, 1e-4f)
    }

    @Test
    fun `Finishing does not finish modifiers added by callbacks`() {
        component.alpha = 1f
        component.fadeOut(1f).after { it.fadeIn(1f) }

        component.finishModifiers()

        assertEquals(0f, component.alpha, 0f)

        // The fade in added by the callback plays normally.
        advanceTo(0.5f)
        assertEquals(0.5f, component.alpha, 1e-4f)
    }

    @Test
    fun `Finishing does not chase a self-rescheduling callback`() {
        var invocations = 0

        // Bounded so that a regression fails instead of looping forever.
        fun pulse(target: UIComponent) {
            target.fadeTo(if (target.alpha > 0.5f) 0f else 1f, 1f).after {
                if (++invocations < 1000) {
                    pulse(it)
                }
            }
        }

        pulse(component)
        component.finishModifiers()

        assertEquals(1, invocations)
        assertTrue(component.isAnimating)
    }

    //endregion

    //region Overriding

    @Test
    fun `Modifier added at the current time aborts the running modifier it overrides`() {
        var finished = false

        component.alpha = 1f
        component.fadeOut(1f).after { finished = true }

        advanceTo(0.1f)
        // The fade modifier below cancels out the fade out above, so the callback must not be invoked.
        component.fadeIn(0.5f)

        advanceTo(2f)

        assertFalse(finished)
        assertEquals(1f, component.alpha, 0f)
    }

    @Test
    fun `Scheduled modifier aborts the running modifier it overrides when it starts`() {
        var finished = false

        component.alpha = 1f
        component.fadeOut(1f).after { finished = true }
        component.beginDelayedSequence(0.5f) { fadeIn(0.5f) }

        // The fade in starts during this update, while the fade out is still running.
        advanceTo(0.6f)
        // By this time, the fade out must have been aborted, so the callback must not be invoked.
        advanceTo(2f)

        assertFalse(finished)
        assertEquals(1f, component.alpha, 0f)
    }

    @Test
    fun `Adding a modifier drops later-scheduled modifiers of the same type`() {
        component.alpha = 0f
        component.beginDelayedSequence(1f) { fadeOut(0.1f) }

        // The fade modifier below cancels out the fade out above (even though it starts later).
        component.fadeIn(0.1f)

        // Advance enough to ensure that the delayed sequence should have fired.
        advanceTo(2f)

        assertEquals(1f, component.alpha, 0f)
    }

    @Test
    fun `Adding a modifier keeps later-scheduled modifiers of other types`() {
        component.beginDelayedSequence(1f) { scaleTo(2f, 0.1f) }

        component.fadeIn(0.1f)

        advanceTo(2f)
        assertEquals(2f, component.scaleX, 0f)
    }

    @Test
    fun `Completed modifier invokes its callback when followed by the same type`() {
        var finished = false

        component.alpha = 1f
        component.fadeOut(0.5f).after { finished = true }
        component.beginDelayedSequence(0.6f) { fadeIn(0.5f) }

        // Both the completion and the start of the next modifier happen in this frame.
        advanceTo(0.7f)

        assertTrue(finished)
    }

    @Test
    fun `Different types in the same group do not abort each other`() {
        component.setPosition(0f, 0f)
        component.moveTo(100f, 100f, 1f)

        component.beginAbsoluteSequence(0.5f) { moveToX(0f, 0.1f) }

        // Step through frames.
        // A single jump past both end times would let the X modifier read
        // its start value from the already-finished XY modifier and win.
        advanceTo(0.55f)
        advanceTo(0.65f)
        advanceTo(2f)

        // The XY modifier keeps running after the X modifier ends.
        assertEquals(100f, component.x, 0f)
        assertEquals(100f, component.y, 0f)
    }

    @Test
    fun `Starting a modifier does not rewind other types in the same group`() {
        component.setPosition(0f, 0f)
        component.moveTo(100f, 100f, 1f)

        component.beginAbsoluteSequence(0.5f) { moveToX(0f, 0.5f) }

        advanceTo(0.7f)

        // Y is only driven by the XY modifier, so it must reflect the current frame's time.
        assertEquals(70f, component.y, 1e-3f)
    }

    //endregion

    //region Sequence

    @Test
    fun `Then respects a pending delay`() {
        component.alpha = 0f

        component.beginModifierSequence {
            fadeIn(0.1f).delay(0.5f).then().fadeOut(0.1f)
        }

        // delay() advances from the sequence's current time (0s), so the fade out starts at 0.5s.
        advanceTo(0.3f)
        assertEquals("Fade out must not start before 0.5s", 1f, component.alpha, 0f)

        advanceTo(0.55f)
        assertEquals(0.5f, component.alpha, 1e-4f)
    }

    @Test
    fun `Then waits for the latest end time when it exceeds the pending delay`() {
        component.alpha = 0f

        component.beginModifierSequence {
            fadeIn(1f).delay(0.2f).then().fadeOut(0.1f)
        }

        advanceTo(0.95f)
        assertTrue("Fade out must not start before 1s", component.alpha > 0.9f)
    }

    @Test(expected = IllegalStateException::class)
    fun `Unbalanced delay inside absolute sequence throws`() {
        component.beginAbsoluteSequence(1f) {
            component.addDelay(0.5f)
        }
    }

    @Test
    fun `Balanced absolute sequence restores modifier start time`() {
        clock.currentTime = 2f

        component.beginAbsoluteSequence(5f) {
            component.addDelay(0.5f)
            component.addDelay(-0.5f)
        }

        assertEquals(2f, component.modifierStartTime, 1e-4f)
    }

    //endregion
}
