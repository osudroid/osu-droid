package com.reco1l.andengine.container

import com.reco1l.andengine.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.shape.*
import kotlin.math.max

/**
 * Container that allows to constrain nested entities to other entities in the same container.
 *
 * This is useful for creating complex layouts.
 */
class ConstraintContainer : Container() {


    private val constraints = mutableMapOf<ExtendedEntity, IShape>()


    override fun getChildDrawX(child: ExtendedEntity): Float {

        val target = constraints[child] ?: this

        var targetX = target.getDrawX()
        var targetWidth = target.getDrawWidth()

        if (target == this) {
            targetX = 0f
            targetWidth = getPaddedWidth()
        }

        val anchorOffsetX = targetWidth * child.anchor.x

        var childX = child.x
        if (child.relativePositionAxes.isHorizontal) {

            // Relative positions will be multiplied by the remaining space from the
            // target's position to the edge of the container.
            childX *= getPaddedWidth() - targetX
        }

        return targetX + childX + child.originOffsetX + anchorOffsetX + child.translationX
    }

    override fun getChildDrawY(child: ExtendedEntity): Float {

        val target = constraints[child] ?: this

        var targetY = target.getDrawY()
        var targetHeight = target.getDrawHeight()

        if (target == this) {
            targetY = 0f
            targetHeight = getPaddedHeight()
        }

        val anchorOffsetY = targetHeight * child.anchor.y

        var childY = child.y
        if (child.relativePositionAxes.isVertical) {

            // Relative positions will be multiplied by the remaining space from the
            // target's position to the edge of the container.
            childY *= getPaddedHeight() - targetY
        }

        return targetY + childY + child.originOffsetY + anchorOffsetY + child.translationY
    }


    /**
     * Adds a constraint to a child.
     */
    fun addConstraint(child: ExtendedEntity, target: IShape) {

        if (child == target) {
            throw IllegalArgumentException("Cannot constrain a child to itself.")
        }

        if (child == this) {
            throw IllegalArgumentException("Cannot constrain the container itself. Use anchorX and anchorY child's properties instead.")
        }

        if (mChildren == null || target !in mChildren) {
            throw IllegalArgumentException("The target must be a child of the container.")
        }

        constraints[child] = target
    }

    /**
     * Removes a constraint from a child.
     */
    fun removeConstraint(child: ExtendedEntity?) {
        constraints.remove(child ?: return)
    }


    override fun onChildDetached(child: IEntity) {
        super.onChildDetached(child)
        removeConstraint(child as? ExtendedEntity)
    }

}