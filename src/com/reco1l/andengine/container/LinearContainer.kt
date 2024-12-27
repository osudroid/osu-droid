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


    override fun onMeasureContentSize() {
        shouldMeasureSize = false

        contentWidth = 0f
        contentHeight = 0f

        if (mChildren != null) {

            for (i in mChildren.indices) {

                val child = mChildren.getOrNull(i) ?: continue
                if (child !is ExtendedEntity) {
                    continue
                }

                val spacing = if (i == mChildren.size - 1) 0f else spacing

                when (orientation) {

                    Horizontal -> {
                        child.x = contentWidth
                        contentWidth += child.getDrawWidth() + spacing
                        contentHeight = max(contentHeight, child.getDrawHeight())
                    }

                    Vertical -> {
                        child.y = contentHeight
                        contentWidth = max(contentWidth, child.getDrawWidth())
                        contentHeight += child.getDrawHeight() + spacing
                    }
                }
            }
        }

        onContentSizeMeasured()
    }


    override fun getChildDrawX(child: ExtendedEntity): Float {

        val drawX = super.getChildDrawX(child)

        if (orientation == Vertical) {
            return drawX
        }

        // Subtract the anchor offset for the X axis because it should be ignored in this case.
        return drawX - child.anchorOffsetX
    }

    override fun getChildDrawY(child: ExtendedEntity): Float {

        val drawY = super.getChildDrawY(child)

        if (orientation == Horizontal) {
            return drawY
        }

        // Subtract the anchor offset for the Y axis because it should be ignored in this case.
        return drawY - child.anchorOffsetY
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