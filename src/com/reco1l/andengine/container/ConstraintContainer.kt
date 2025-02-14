package com.reco1l.andengine.container

import com.reco1l.andengine.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.shape.*

/**
 * Container that allows to constrain nested entities to other entities in the same container.
 *
 * This is useful for creating complex layouts.
 */
open class ConstraintContainer : Container() {


    /**
     * Whether the container should account for the scales of the children when calculating the draw position.
     */
    var accountForScaleAxes = Axes.Both


    private val constraints = mutableMapOf<ExtendedEntity, IShape>()


    override fun getChildDrawX(child: ExtendedEntity): Float {

        val target = constraints[child] ?: this

        var targetX = target.getDrawX()
        var targetWidth = target.getDrawWidth()

        if (target == this) {
            targetX = 0f
            targetWidth = getPaddedWidth()
        } else if (accountForScaleAxes.isHorizontal) {
            targetWidth *= target.scaleX
        }

        val anchorOffsetX = targetWidth * child.anchor.x

        var childX = child.x
        if (child.relativePositionAxes.isHorizontal) {

            // Relative positions will be multiplied by the remaining space from the
            // target's position to the edge of the container.
            childX *= getPaddedWidth() - targetX
        }

        if (target != this && accountForScaleAxes.isHorizontal) {
            childX += targetWidth * (1 - target.scaleX)
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
        } else if (accountForScaleAxes.isVertical) {
            targetHeight *= target.scaleY
        }

        val anchorOffsetY = targetHeight * child.anchor.y

        var childY = child.y
        if (child.relativePositionAxes.isVertical) {

            // Relative positions will be multiplied by the remaining space from the
            // target's position to the edge of the container.
            childY *= getPaddedHeight() - targetY
        }

        if (target != this && accountForScaleAxes.isVertical) {
            childY += targetHeight * (1 - target.scaleY)
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

    override fun onChildPositionChanged(child: IEntity) {
        constraints.forEach { (source, target) ->
            if (target == child) {
                source.invalidateTransformations()
            }
        }
        super.onChildPositionChanged(child)
    }

}