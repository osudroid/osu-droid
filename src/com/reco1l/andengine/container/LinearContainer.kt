package com.reco1l.andengine.container

import com.reco1l.andengine.*
import com.reco1l.andengine.container.Orientation.*
import kotlin.math.*

open class LinearContainer : Container() {

    /**
     * The orientation of the container.
     */
    var orientation = Horizontal

    /**
     * The spacing between children.
     */
    var spacing = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }


    override fun onContentChanged() {

        var right = 0f
        var bottom = 0f

        for (i in 0 until childCount) {

            val child = getChild(i) as? ExtendedEntity ?: continue

            if (!child.isVisible) {
                continue
            }

            when (orientation) {

                Horizontal -> {
                    child.x = right

                    right += child.getWidth()
                    bottom = max(bottom, child.getHeight())

                    if (i < childCount - 1) {
                        right += spacing
                    }
                }

                Vertical -> {
                    child.y = bottom

                    right = max(right, child.getWidth())
                    bottom += child.getHeight()

                    if (i < childCount - 1) {
                        bottom += spacing
                    }
                }
            }
        }

        contentWidth = right
        contentHeight = bottom
    }
}

/**
 * Defines the orientation of the container.
 */
enum class Orientation {

    /**
     * The children are placed horizontally.
     */
    Horizontal,

    /**
     * The children are placed vertically.
     */
    Vertical
}