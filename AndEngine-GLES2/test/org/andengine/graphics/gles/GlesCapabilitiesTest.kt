package org.andengine.graphics.gles

import org.andengine.opengl.exception.GLESVersionUnsupportedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlesCapabilitiesTest {

    private fun detect(version: String, extensions: String, simulateEs30: Boolean = false): GlesCapabilities {
        GlesDebugOptions.simulateEs30 = simulateEs30
        try {
            return GlesCapabilities.detect(version, extensions)
        } finally {
            GlesDebugOptions.simulateEs30 = false
        }
    }

    @Test
    fun es32_allLevels() {
        val c = detect("OpenGL ES 3.2 x", "")
        assertTrue(c.hasGl31)
        assertTrue(c.hasGl32)
        assertTrue(c.hasDebugOutput)
    }

    @Test
    fun es31_noGl32() {
        val c = detect("OpenGL ES 3.1 x", "")
        assertTrue(c.hasGl31)
        assertFalse(c.hasGl32)
    }

    @Test
    fun simulateEs30_hidesGl31AndGl32() {
        val c = detect("OpenGL ES 3.2 x", "", simulateEs30 = true)
        assertEquals(GlesVersion.ES30, c.version)
        assertFalse(c.hasGl31)
        assertFalse(c.hasGl32)
        assertFalse(c.hasDebugOutput)
    }

    @Test(expected = GLESVersionUnsupportedException::class)
    fun es20_rejected() {
        detect("OpenGL ES 2.0", "")
    }

    @Test
    fun extensions_parsed() {
        val c = detect("OpenGL ES 3.0 x", "GL_KHR_debug GL_EXT_disjoint_timer_query")
        assertTrue(c.hasDebugOutput)
        assertTrue(c.hasTimerQuery)
    }
}
