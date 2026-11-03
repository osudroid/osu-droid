/*
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 15:31:48 - 04.08.2011
 */
package org.andengine.graphics.gles

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import androidx.annotation.VisibleForTesting
import org.andengine.engine.options.ColorDepth

/**
 * Picks the [EGLConfig] the GL context and window surfaces are created with: the requested [colorDepth] with
 * D16 S0, ES2 renderable, optionally with 2x multisampling (or NVIDIA coverage sampling). Each step of
 * [searchOrder] is tried in turn: for each color format, an exact match wins over a config with a stencil buffer,
 * which wins over a deeper depth buffer; only when no color format fits is any config taken. When multisampling
 * is requested and the device has multisampled configs, only those are searched, so multisampling wins over the
 * color depth.
 * The chosen config's sizes are exposed by the getters once [chooseConfig] succeeded.
 */
class GlesConfigChooser @JvmOverloads constructor(
    private val multiSamplingRequested: Boolean,
    private val colorDepth: ColorDepth = ColorDepth.RGB565,
) {

    var isMultiSampling = false
        private set

    // Test seam: settable from tests, since choosing a config needs a real EGL display.
    @set:VisibleForTesting
    var isCoverageMultiSampling = false

    var redSize = -1
        private set

    var greenSize = -1
        private set

    var blueSize = -1
        private set

    var alphaSize = -1
        private set

    var depthSize = -1
        private set

    var stencilSize = -1
        private set

    private val count = IntArray(1)

    /** @throws IllegalArgumentException If no matcher finds a config on [display]. */
    fun chooseConfig(display: EGLDisplay): EGLConfig {
        val steps = searchOrder(colorDepth)
        for (i in 0 until steps.size - 1) {
            try {
                return chooseConfig(display, steps[i])
            } catch (_: IllegalArgumentException) {
            }
        }
        return chooseConfig(display, steps.last())
    }

    /**
     * Initializes the default display, chooses a config on it and terminates the display again, to find out
     * up front whether the device can run the engine at all.
     *
     * @throws IllegalArgumentException If no config is found.
     */
    fun probeDefaultDisplay() {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(display, version, 0, version, 1)
        try {
            chooseConfig(display)
        } finally {
            EGL14.eglTerminate(display)
        }
    }

    private fun chooseConfig(display: EGLDisplay, step: SearchStep): EGLConfig {
        // The multisampling flags are set as soon as such configs exist, even if the matcher then rejects all
        // of them; the next matcher queries the same list again.
        if (multiSamplingRequested) {
            var configs = queryConfigs(display, MULTISAMPLE_ATTRIBUTES)
            if (configs.isNotEmpty()) {
                isMultiSampling = true
                return findConfig(display, configs, step)
            }

            configs = queryConfigs(display, COVERAGE_MULTISAMPLE_NV_ATTRIBUTES)
            if (configs.isNotEmpty()) {
                isCoverageMultiSampling = true
                return findConfig(display, configs, step)
            }
        }

        val configs = queryConfigs(display, FALLBACK_ATTRIBUTES)
        require(configs.isNotEmpty()) { "No EGLConfig found!" }
        return findConfig(display, configs, step)
    }

    /**
     * EGL14 does not accept a null config array to only count the matches, so the array grows until it holds
     * every matching config.
     */
    private fun queryConfigs(display: EGLDisplay, attributes: IntArray): Array<EGLConfig?> {
        var capacity = INITIAL_CONFIG_CAPACITY
        while (true) {
            val configs = arrayOfNulls<EGLConfig>(capacity)
            require(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, capacity, count, 0)) {
                "eglChooseConfig failed!"
            }
            if (count[0] < capacity) {
                return configs.copyOf(count[0])
            }
            capacity *= 2
        }
    }

    private fun findConfig(display: EGLDisplay, configs: Array<EGLConfig?>, step: SearchStep): EGLConfig {
        val sizes = ArrayList<IntArray>(configs.size)
        for (config in configs) {
            sizes += if (config == null) NULL_CONFIG_SIZES else intArrayOf(
                configAttribute(display, config, EGL14.EGL_RED_SIZE),
                configAttribute(display, config, EGL14.EGL_GREEN_SIZE),
                configAttribute(display, config, EGL14.EGL_BLUE_SIZE),
                configAttribute(display, config, EGL14.EGL_ALPHA_SIZE),
                configAttribute(display, config, EGL14.EGL_DEPTH_SIZE),
                configAttribute(display, config, EGL14.EGL_STENCIL_SIZE),
            )
        }

        val index = chooseFrom(sizes, step)
        require(index >= 0) { "No EGLConfig found!" }

        val chosen = sizes[index]
        redSize = chosen[0]
        greenSize = chosen[1]
        blueSize = chosen[2]
        alphaSize = chosen[3]
        depthSize = chosen[4]
        stencilSize = chosen[5]
        return configs[index]!!
    }

    private fun configAttribute(display: EGLDisplay, config: EGLConfig, attribute: Int): Int =
        if (EGL14.eglGetConfigAttrib(display, config, attribute, count, 0)) count[0] else 0

    /** How strictly a config's depth and stencil sizes have to match D16 S0. */
    enum class Matcher {
        STRICT {
            override fun matches(depth: Int, stencil: Int) = depth == DEPTH_SIZE && stencil == STENCIL_SIZE
        },
        LOOSE_STENCIL {
            override fun matches(depth: Int, stencil: Int) = depth == DEPTH_SIZE && stencil >= STENCIL_SIZE
        },
        LOOSE_DEPTH_AND_STENCIL {
            override fun matches(depth: Int, stencil: Int) = depth >= DEPTH_SIZE && stencil >= STENCIL_SIZE
        },
        ANY {
            override fun matches(depth: Int, stencil: Int) = true
        };

        abstract fun matches(depth: Int, stencil: Int): Boolean
    }

    /** The exact R, G, B and A sizes a config must have. */
    enum class ColorFormat(val red: Int, val green: Int, val blue: Int, val alpha: Int) {
        RGB565(5, 6, 5, 0),
        RGB888(8, 8, 8, 0),

        /** Only a fallback: the window is opaque, so the alpha channel is unused. */
        RGBA8888(8, 8, 8, 8);

        fun matches(red: Int, green: Int, blue: Int, alpha: Int) =
            red == this.red && green == this.green && blue == this.blue && alpha == this.alpha
    }

    /** One attempt of [chooseConfig]; a null [color] accepts any color sizes. */
    data class SearchStep(val color: ColorFormat?, val matcher: Matcher)

    companion object {
        // Minimum color sizes for eglChooseConfig; 888 configs meet them too, so one query serves every step.
        private const val RED_SIZE = 5
        private const val GREEN_SIZE = 6
        private const val BLUE_SIZE = 5
        private const val ALPHA_SIZE = 0
        private const val DEPTH_SIZE = 16
        private const val STENCIL_SIZE = 0

        private const val MULTISAMPLE_COUNT = 2

        private const val EGL_COVERAGE_BUFFERS_NV = 0x30E0
        private const val EGL_COVERAGE_SAMPLES_NV = 0x30E1

        private const val INITIAL_CONFIG_CAPACITY = 64

        /** Stands in for a null config so it never matches; [Matcher.ANY] is never offered a null config. */
        private val NULL_CONFIG_SIZES = intArrayOf(-1, -1, -1, -1, -1, -1)

        private val COLOR_MATCHERS = listOf(Matcher.STRICT, Matcher.LOOSE_STENCIL, Matcher.LOOSE_DEPTH_AND_STENCIL)

        private val SEARCH_ORDER_RGB565 = stepsFor(ColorFormat.RGB565, ColorFormat.RGB888, ColorFormat.RGBA8888)
        private val SEARCH_ORDER_RGB888 = stepsFor(ColorFormat.RGB888, ColorFormat.RGBA8888, ColorFormat.RGB565)

        private fun stepsFor(vararg colors: ColorFormat): List<SearchStep> =
            colors.flatMap { color -> COLOR_MATCHERS.map { SearchStep(color, it) } } + SearchStep(null, Matcher.ANY)

        /**
         * The steps [chooseConfig] tries in turn for [colorDepth]: the requested color format first, then the
         * other ones, each with every [Matcher] except [Matcher.ANY], which only comes last and takes any color.
         */
        @JvmStatic
        fun searchOrder(colorDepth: ColorDepth): List<SearchStep> = when (colorDepth) {
            ColorDepth.RGB565 -> SEARCH_ORDER_RGB565
            ColorDepth.RGB888 -> SEARCH_ORDER_RGB888
        }

        @JvmField
        val MULTISAMPLE_ATTRIBUTES = intArrayOf(
            EGL14.EGL_RED_SIZE, RED_SIZE,
            EGL14.EGL_GREEN_SIZE, GREEN_SIZE,
            EGL14.EGL_BLUE_SIZE, BLUE_SIZE,
            EGL14.EGL_ALPHA_SIZE, ALPHA_SIZE,
            EGL14.EGL_DEPTH_SIZE, DEPTH_SIZE,
            EGL14.EGL_STENCIL_SIZE, STENCIL_SIZE,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SAMPLE_BUFFERS, 1,
            EGL14.EGL_SAMPLES, MULTISAMPLE_COUNT,
            EGL14.EGL_NONE,
        )

        @JvmField
        val COVERAGE_MULTISAMPLE_NV_ATTRIBUTES = intArrayOf(
            EGL14.EGL_RED_SIZE, RED_SIZE,
            EGL14.EGL_GREEN_SIZE, GREEN_SIZE,
            EGL14.EGL_BLUE_SIZE, BLUE_SIZE,
            EGL14.EGL_ALPHA_SIZE, ALPHA_SIZE,
            EGL14.EGL_DEPTH_SIZE, DEPTH_SIZE,
            EGL14.EGL_STENCIL_SIZE, STENCIL_SIZE,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_COVERAGE_BUFFERS_NV, 1,
            EGL_COVERAGE_SAMPLES_NV, MULTISAMPLE_COUNT,
            EGL14.EGL_NONE,
        )

        @JvmField
        val FALLBACK_ATTRIBUTES = intArrayOf(
            EGL14.EGL_RED_SIZE, RED_SIZE,
            EGL14.EGL_GREEN_SIZE, GREEN_SIZE,
            EGL14.EGL_BLUE_SIZE, BLUE_SIZE,
            EGL14.EGL_ALPHA_SIZE, ALPHA_SIZE,
            EGL14.EGL_DEPTH_SIZE, DEPTH_SIZE,
            EGL14.EGL_STENCIL_SIZE, STENCIL_SIZE,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE,
        )

        /**
         * @param configs Per config its R, G, B, A, depth and stencil sizes, in the order EGL returned them.
         * @return The index of the first config [step] accepts, or -1.
         */
        @JvmStatic
        fun chooseFrom(configs: List<IntArray>, step: SearchStep): Int {
            for (i in configs.indices) {
                val c = configs[i]
                if (c === NULL_CONFIG_SIZES) continue
                if (step.color != null && !step.color.matches(c[0], c[1], c[2], c[3])) continue
                if (step.matcher.matches(c[4], c[5])) return i
            }
            return -1
        }
    }
}
