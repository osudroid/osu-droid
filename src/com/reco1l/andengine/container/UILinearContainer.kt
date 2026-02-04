package com.reco1l.andengine.container

import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.Orientation.*
import com.reco1l.andengine.theme.Size
import kotlin.math.*

open class UILinearContainer : UIContainer() {

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
        var contentWidth = 0f
        var contentHeight = 0f

        for (i in 0 until childCount) {

            val child = getChild(i) as? UIComponent ?: continue

            if (!child.isVisible) {
                continue
            }

            val childWidth = if (child.rawWidth in Size.relativeSizeRange) child.intrinsicWidth else child.width
            val childHeight = if (child.rawHeight in Size.relativeSizeRange) child.intrinsicHeight else child.height

            when (orientation) {

                Horizontal -> {
                    child.x = contentWidth

                    contentWidth += childWidth
                    contentHeight = max(contentHeight, childHeight)

                    if (childCount > 1 && i < childCount - 1) {
                        contentWidth += spacing
                    }
                }

                Vertical -> {
                    child.y = contentHeight

                    contentWidth = max(contentWidth, childWidth)
                    contentHeight += childHeight

                    if (childCount > 1 && i < childCount - 1) {
                        contentHeight += spacing
                    }
                }
            }
        }

        this.contentWidth = contentWidth
        this.contentHeight = contentHeight
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