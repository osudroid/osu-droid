package com.reco1l.andengine

/**
 * The axes in a bi-dimensional space.
 */
enum class Axes {

    /**
     * Horizontal axis.
     */
    X,

    /**
     * Vertical axis.
     */
    Y,

    /**
     * Both axes.
     */
    Both,

    /**
     * None of the axes.
     */
    None;


    /**
     * Whether this axis [Y] or [Both].
     */
    val isVertical: Boolean
        get() = this == Y || this == Both

    /**
     * Whether this axis is [X] or [Both].
     */
    val isHorizontal: Boolean
        get() = this == X || this == Both


}