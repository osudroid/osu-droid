package org.andengine.graphics.gles

import org.andengine.graphics.gles.GlesConfigChooser.Matcher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GlesConfigChooserTest {

    @Test
    fun strict_prefersExact565Depth16() {
        val configs = listOf(config(8, 8, 8, 8, 24, 0), config(5, 6, 5, 0, 16, 0), config(5, 6, 5, 0, 24, 0))

        assertEquals(1, GlesConfigChooser.chooseFrom(configs, Matcher.STRICT))
    }

    @Test
    fun looseStencil_acceptsStencil() {
        val configs = listOf(config(8, 8, 8, 8, 24, 8), config(5, 6, 5, 0, 16, 8))

        assertEquals(-1, GlesConfigChooser.chooseFrom(configs, Matcher.STRICT))
        assertEquals(1, GlesConfigChooser.chooseFrom(configs, Matcher.LOOSE_STENCIL))
    }

    @Test
    fun looseDepthAndStencil_acceptsAnyDepth() {
        val configs = listOf(config(5, 6, 5, 0, 24, 8))

        assertEquals(-1, GlesConfigChooser.chooseFrom(configs, Matcher.LOOSE_STENCIL))
        assertEquals(0, GlesConfigChooser.chooseFrom(configs, Matcher.LOOSE_DEPTH_AND_STENCIL))
    }

    @Test
    fun any_takesFirstConfig() {
        val configs = listOf(config(8, 8, 8, 8, 24, 0))

        assertEquals(-1, GlesConfigChooser.chooseFrom(configs, Matcher.LOOSE_DEPTH_AND_STENCIL))
        assertEquals(0, GlesConfigChooser.chooseFrom(configs, Matcher.ANY))
    }

    @Test
    fun attributeLists_matchToday() {
        assertArrayEquals(MULTISAMPLE, GlesConfigChooser.MULTISAMPLE_ATTRIBUTES)
        assertArrayEquals(COVERAGE_MULTISAMPLE_NV, GlesConfigChooser.COVERAGE_MULTISAMPLE_NV_ATTRIBUTES)
        assertArrayEquals(FALLBACK, GlesConfigChooser.FALLBACK_ATTRIBUTES)
    }

    private fun config(r: Int, g: Int, b: Int, a: Int, d: Int, s: Int) = intArrayOf(r, g, b, a, d, s)

    private companion object {
        const val EGL_RED_SIZE = 0x3024
        const val EGL_GREEN_SIZE = 0x3023
        const val EGL_BLUE_SIZE = 0x3022
        const val EGL_ALPHA_SIZE = 0x3021
        const val EGL_DEPTH_SIZE = 0x3025
        const val EGL_STENCIL_SIZE = 0x3026
        const val EGL_RENDERABLE_TYPE = 0x3040
        const val EGL_SAMPLE_BUFFERS = 0x3032
        const val EGL_SAMPLES = 0x3031
        const val EGL_NONE = 0x3038
        const val EGL_COVERAGE_BUFFERS_NV = 0x30E0
        const val EGL_COVERAGE_SAMPLES_NV = 0x30E1
        const val EGL_GLES2_BIT = 4

        val BASE = intArrayOf(
            EGL_RED_SIZE, 5,
            EGL_GREEN_SIZE, 6,
            EGL_BLUE_SIZE, 5,
            EGL_ALPHA_SIZE, 0,
            EGL_DEPTH_SIZE, 16,
            EGL_STENCIL_SIZE, 0,
            EGL_RENDERABLE_TYPE, EGL_GLES2_BIT,
        )

        val MULTISAMPLE = BASE + intArrayOf(EGL_SAMPLE_BUFFERS, 1, EGL_SAMPLES, 2, EGL_NONE)
        val COVERAGE_MULTISAMPLE_NV = BASE + intArrayOf(EGL_COVERAGE_BUFFERS_NV, 1, EGL_COVERAGE_SAMPLES_NV, 2, EGL_NONE)
        val FALLBACK = BASE + intArrayOf(EGL_NONE)
    }
}
