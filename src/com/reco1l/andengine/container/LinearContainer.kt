package com.reco1l.andengine.container

import com.reco1l.andengine.*
import com.reco1l.andengine.container.Orientation.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.shape.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*

open class LinearContainer : Container() {


    override var autoSizeAxes = Axes.Both


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


    private var lastChildX = 0f

    private var lastChildY = 0f


    override fun onMeasureContentSize() {
        shouldMeasureSize = false

        contentWidth = 0f
        contentHeight = 0f
        var countedChildren = 0

        if (mChildren != null) {

            for (i in mChildren.indices) {
                val child = mChildren.getOrNull(i) ?: continue

                // Non-shape children are ignored as they doesn't have a size there's nothing to do.
                if (child !is IShape) {
                    continue
                }
                countedChildren++

                when (orientation) {

                    Horizontal -> {
                        contentWidth += child.width
                        contentHeight = max(contentHeight, child.height)
                    }

                    Vertical -> {
                        contentWidth = max(contentWidth, child.width)
                        contentHeight += child.height
                    }
                }

                if (i > 0) {
                    when(orientation) {
                        Horizontal -> contentWidth += spacing
                        Vertical -> contentHeight += spacing
                    }
                }
            }
        }

        // Subtract the last children spacing.
        if (countedChildren > 1) {
            when (orientation) {
                Horizontal -> contentWidth -= spacing
                Vertical -> contentHeight -= spacing
            }
        }

        onContentSizeMeasured()
    }


    override fun onManagedDrawChildren(pGL: GL10, pCamera: Camera) {
        lastChildX = 0f
        lastChildY = 0f
        super.onManagedDrawChildren(pGL, pCamera)
    }


    override fun getChildDrawX(child: ExtendedEntity): Float {

        var drawX = super.getChildDrawX(child)

        if (orientation == Horizontal) {
            drawX += lastChildX
            lastChildX += child.width + spacing
        }

        return drawX
    }

    override fun getChildDrawY(child: ExtendedEntity): Float {

        var drawY = super.getChildDrawY(child)

        if (orientation == Vertical) {
            drawY += lastChildY
            lastChildY += child.height + spacing
        }

        return drawY
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