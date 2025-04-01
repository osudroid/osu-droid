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
                shouldMeasureSize = true
            }
        }


    override fun onMeasureContentSize() {
        shouldMeasureSize = false

        contentWidth = 0f
        contentHeight = 0f

        for (i in 0 until childCount) {

            val child = getChild(i) ?: continue
            if (child !is ExtendedEntity) {
                continue
            }

            when (orientation) {

                Horizontal -> {
                    child.x = contentWidth

                    contentWidth += child.getWidth()
                    contentHeight = max(contentHeight, child.getHeight())

                    if (i < childCount - 1) {
                        contentWidth += spacing
                    }
                }

                Vertical -> {
                    child.y = contentHeight

                    contentWidth = max(contentWidth, child.getWidth())
                    contentHeight += child.getHeight()

                    if (i < childCount - 1) {
                        contentHeight += spacing
                    }
                }
            }
        }

        invalidate(InvalidationFlag.ContentSize)
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