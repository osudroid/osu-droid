package com.reco1l.osu.graphics.container

import com.reco1l.osu.graphics.*
import com.reco1l.osu.graphics.container.Orientation.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.shape.IShape
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


    private var lastChildX = 0f

    private var lastChildY = 0f


    override fun onMeasureSize() {

        var cumulativeWidth = 0f
        var cumulativeHeight = 0f

        if (mChildren != null) {
            for (i in mChildren.indices) {

                val child = mChildren.getOrNull(i) ?: continue
                if (child !is IShape) {
                    continue
                }

                val spacing = if (i == 0) 0f else spacing

                when (orientation) {

                    Horizontal -> {
                        cumulativeWidth += child.width + spacing
                        cumulativeHeight = max(cumulativeHeight, child.height)
                    }

                    Vertical -> {
                        cumulativeWidth = max(cumulativeWidth, child.width)
                        cumulativeHeight += child.height + spacing
                    }
                }
            }
        }

        onApplySize(cumulativeWidth, cumulativeHeight)
    }


    override fun onManagedDrawChildren(pGL: GL10, pCamera: Camera) {
        lastChildX = 0f
        lastChildY = 0f
        super.onManagedDrawChildren(pGL, pCamera)
    }

    override fun onApplyChildTranslation(gl: GL10, child: ExtendedEntity) {

        val originOffsetX = child.width * child.originX
        val originOffsetY = child.height * child.originY

        val anchorOffsetX = width * child.anchorX
        val anchorOffsetY = height * child.anchorY

        var finalX = anchorOffsetX - originOffsetX + child.translationX
        var finalY = anchorOffsetY - originOffsetY + child.translationY

        when (orientation) {

            Horizontal -> {
                finalX += lastChildX
                lastChildX += child.width + spacing
            }

            Vertical -> {
                finalY += lastChildY
                lastChildY += child.height + spacing
            }
        }

        if (finalX != 0f || finalY != 0f) {
            gl.glTranslatef(finalX, finalY, 0f)
        }
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