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


    private var lastDrawOffset = 0f


    override fun onMeasureContentSize() {
        shouldMeasureSize = false

        contentWidth = 0f
        contentHeight = 0f

        if (mChildren != null) {

            for (i in mChildren.indices) {

                val child = mChildren.getOrNull(i) ?: continue

                // Non-shape children are ignored as they doesn't have a size there's nothing to do.
                if (child !is IShape) {
                    continue
                }

                val spacing = if (i == mChildren.size - 1) 0f else spacing

                when (orientation) {

                    Horizontal -> {
                        contentWidth += child.width + spacing
                        contentHeight = max(contentHeight, child.height)
                    }

                    Vertical -> {
                        contentWidth = max(contentWidth, child.width)
                        contentHeight += child.height + spacing
                    }
                }
            }
        }

        onContentSizeMeasured()
    }


    override fun onManagedDrawChildren(pGL: GL10, pCamera: Camera) {
        lastDrawOffset = 0f
        super.onManagedDrawChildren(pGL, pCamera)
    }

    override fun getChildDrawX(child: ExtendedEntity): Float {

        if (orientation == Vertical) {
            return super.getChildDrawX(child)
        }

        val drawX = lastDrawOffset + super.getChildDrawX(child)
        lastDrawOffset += child.drawWidth + spacing

        return drawX
    }

    override fun getChildDrawY(child: ExtendedEntity): Float {

        if (orientation == Horizontal) {
            return super.getChildDrawY(child)
        }

        val drawY = lastDrawOffset + super.getChildDrawY(child)
        lastDrawOffset += child.drawHeight + spacing

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