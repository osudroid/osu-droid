package org.andengine.engine.options

/**
 * The color depth of the window surface the engine renders to. A device that has no config with the requested
 * depth falls back to the other one, so the depth the engine gets can differ from the one requested.
 */
enum class ColorDepth {
    /** 16-bit color (R5 G6 B5). Less memory bandwidth, but visible banding in gradients. */
    RGB565,

    /** 24-bit color (R8 G8 B8). */
    RGB888,
}
