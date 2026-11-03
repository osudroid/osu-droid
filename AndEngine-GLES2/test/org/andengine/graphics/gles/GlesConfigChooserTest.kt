package org.andengine.graphics.gles

import org.andengine.engine.options.ColorDepth
import org.andengine.graphics.gles.GlesConfigChooser.ColorFormat
import org.andengine.graphics.gles.GlesConfigChooser.Matcher
import org.andengine.graphics.gles.GlesConfigChooser.SearchStep
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GlesConfigChooserTest {

    @Test
    fun strict_prefersExact565Depth16() {
        val configs = listOf(config(8, 8, 8, 8, 24, 0), config(5, 6, 5, 0, 16, 0), config(5, 6, 5, 0, 24, 0))

        assertEquals(1, GlesConfigChooser.chooseFrom(configs, step565(Matcher.STRICT)))
    }

    @Test
    fun looseStencil_acceptsStencil() {
        val configs = listOf(config(8, 8, 8, 8, 24, 8), config(5, 6, 5, 0, 16, 8))

        assertEquals(-1, GlesConfigChooser.chooseFrom(configs, step565(Matcher.STRICT)))
        assertEquals(1, GlesConfigChooser.chooseFrom(configs, step565(Matcher.LOOSE_STENCIL)))
    }

    @Test
    fun looseDepthAndStencil_acceptsAnyDepth() {
        val configs = listOf(config(5, 6, 5, 0, 24, 8))

        assertEquals(-1, GlesConfigChooser.chooseFrom(configs, step565(Matcher.LOOSE_STENCIL)))
        assertEquals(0, GlesConfigChooser.chooseFrom(configs, step565(Matcher.LOOSE_DEPTH_AND_STENCIL)))
    }

    @Test
    fun any_takesFirstConfig() {
        val configs = listOf(config(8, 8, 8, 8, 24, 0))

        assertEquals(-1, GlesConfigChooser.chooseFrom(configs, step565(Matcher.LOOSE_DEPTH_AND_STENCIL)))
        assertEquals(0, GlesConfigChooser.chooseFrom(configs, SearchStep(null, Matcher.ANY)))
    }

    @Test
    fun rgb888_rejectsAlphaAnd565() {
        val configs = listOf(config(5, 6, 5, 0, 16, 0), config(8, 8, 8, 8, 16, 0), config(8, 8, 8, 0, 24, 8))
        val step = SearchStep(ColorFormat.RGB888, Matcher.LOOSE_DEPTH_AND_STENCIL)

        assertEquals(-1, GlesConfigChooser.chooseFrom(configs, SearchStep(ColorFormat.RGB888, Matcher.STRICT)))
        assertEquals(2, GlesConfigChooser.chooseFrom(configs, step))
    }

    @Test
    fun searchOrder565_isTodaysOrderThen888ThenAny() {
        assertEquals(
            colorSteps(ColorFormat.RGB565, ColorFormat.RGB888, ColorFormat.RGBA8888) + SearchStep(null, Matcher.ANY),
            GlesConfigChooser.searchOrder(ColorDepth.RGB565),
        )
    }

    @Test
    fun searchOrder888_triesAlphaThen565BeforeAny() {
        assertEquals(
            colorSteps(ColorFormat.RGB888, ColorFormat.RGBA8888, ColorFormat.RGB565) + SearchStep(null, Matcher.ANY),
            GlesConfigChooser.searchOrder(ColorDepth.RGB888),
        )
    }

    @Test
    fun searchOrder888_fallsBackTo565OnDeviceWithout888() {
        val configs = listOf(config(5, 6, 5, 0, 16, 0), config(4, 4, 4, 4, 16, 0))

        val first = GlesConfigChooser.searchOrder(ColorDepth.RGB888)
            .first { GlesConfigChooser.chooseFrom(configs, it) >= 0 }

        assertEquals(SearchStep(ColorFormat.RGB565, Matcher.STRICT), first)
    }

    @Test
    fun searchOrder565_fallsBackTo888OnDeviceWithout565() {
        val configs = listOf(config(8, 8, 8, 8, 24, 8), config(8, 8, 8, 0, 16, 0))

        val first = GlesConfigChooser.searchOrder(ColorDepth.RGB565)
            .first { GlesConfigChooser.chooseFrom(configs, it) >= 0 }

        assertEquals(SearchStep(ColorFormat.RGB888, Matcher.STRICT), first)
    }

    @Test
    fun rgba8888_acceptsAlpha() {
        val configs = listOf(config(8, 8, 8, 0, 16, 0), config(8, 8, 8, 8, 16, 0))

        assertEquals(1, GlesConfigChooser.chooseFrom(configs, SearchStep(ColorFormat.RGBA8888, Matcher.STRICT)))
    }

    @Test
    fun attributeLists_matchToday() {
        assertArrayEquals(MULTISAMPLE, GlesConfigChooser.MULTISAMPLE_ATTRIBUTES)
        assertArrayEquals(COVERAGE_MULTISAMPLE_NV, GlesConfigChooser.COVERAGE_MULTISAMPLE_NV_ATTRIBUTES)
        assertArrayEquals(FALLBACK, GlesConfigChooser.FALLBACK_ATTRIBUTES)
    }

    private fun config(r: Int, g: Int, b: Int, a: Int, d: Int, s: Int) = intArrayOf(r, g, b, a, d, s)

    private fun step565(matcher: Matcher) = SearchStep(ColorFormat.RGB565, matcher)

    private fun colorSteps(vararg colors: ColorFormat) = colors.flatMap { color ->
        listOf(Matcher.STRICT, Matcher.LOOSE_STENCIL, Matcher.LOOSE_DEPTH_AND_STENCIL).map { SearchStep(color, it) }
    }

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
