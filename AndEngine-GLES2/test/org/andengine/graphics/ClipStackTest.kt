package org.andengine.graphics

import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.endOpenPass
import org.andengine.graphics.gles.installRecordingGL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClipStackTest {

    private lateinit var gl: RecordingGL

    @Before
    fun setUp() {
        gl = installRecordingGL()
    }

    @After
    fun tearDown() {
        ClipStack.clear()
        QuadBatcher.releaseResources()
        endOpenPass()
    }

    private fun lastScissor() = gl.calls.last { it.startsWith("glScissor(") }

    @Test
    fun push_intersectsWithTheClipBelow() {
        ClipStack.push(10f, 20f, 50f, 50f)
        ClipStack.push(30f, 10f, 50f, 50f)

        assertEquals("glScissor(30,20,30,40)", lastScissor())
    }

    @Test
    fun push_disjointClipsIntersectInNothing() {
        ClipStack.push(0f, 0f, 10f, 10f)
        ClipStack.push(20f, 20f, 10f, 10f)

        assertEquals("glScissor(20,20,0,0)", lastScissor())
    }

    @Test
    fun pop_restoresTheClipBelow() {
        ClipStack.push(0f, 0f, 50f, 50f)
        ClipStack.push(0f, 0f, 20f, 20f)
        ClipStack.pop()

        assertEquals("glScissor(0,0,50,50)", lastScissor())
    }

    @Test
    fun scissorTest_isEnabledForTheStackAndRestoredAfter() {
        assertFalse(GlesBackend.state.isScissorEnabled)

        ClipStack.push(0f, 0f, 50f, 50f)
        ClipStack.push(0f, 0f, 20f, 20f)
        assertTrue(GlesBackend.state.isScissorEnabled)

        ClipStack.pop()
        assertTrue(GlesBackend.state.isScissorEnabled)

        ClipStack.pop()
        assertFalse(GlesBackend.state.isScissorEnabled)
        assertTrue(ClipStack.isEmpty)
    }

    @Test
    fun scissorTest_staysEnabledIfItWasBefore() {
        GlesBackend.state.setScissorEnabled(true)

        ClipStack.push(0f, 0f, 50f, 50f)
        ClipStack.pop()

        assertTrue(GlesBackend.state.isScissorEnabled)
        GlesBackend.state.setScissorEnabled(false)
    }

    @Test
    fun pop_withoutPushDoesNothing() {
        ClipStack.pop()

        assertTrue(ClipStack.isEmpty)
    }

    @Test
    fun clear_dropsLeftoverClipsAndRestoresTheScissorTest() {
        ClipStack.push(0f, 0f, 10f, 10f)
        ClipStack.push(0f, 0f, 5f, 5f)

        ClipStack.clear()
        ClipStack.push(20f, 20f, 30f, 30f)

        assertEquals("glScissor(20,20,30,30)", lastScissor())
        ClipStack.clear()
        assertFalse(GlesBackend.state.isScissorEnabled)
    }
}
