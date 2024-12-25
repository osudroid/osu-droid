package com.reco1l.andengine.container

import com.reco1l.andengine.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.shape.*

/**
 * Container that allows to constrain nested entities to other entities in the same container.
 *
 * This is useful for creating complex layouts.
 */
class ConstraintContainer : Container() {


    private val constraints = mutableMapOf<ExtendedEntity, IShape>()


    override fun getChildDrawX(child: ExtendedEntity): Float {

        val target = constraints[child] ?: this

        val targetX = target.getDrawX()
        val anchorOffsetX = target.getDrawWidth() * child.anchor.x

        var childX = child.x

        // Relative positions will be multiplied by the remaining space since the
        // target's position to the edge of the container.
        if (child.relativePositionAxes.isHorizontal) {
            childX *= drawWidth - targetX
        }

        return targetX + childX + child.originOffsetX + anchorOffsetX + child.translationX
    }

    override fun getChildDrawY(child: ExtendedEntity): Float {

        val target = constraints[child] ?: this

        val targetY = target.getDrawY()
        val anchorOffsetY = target.getDrawHeight() * child.anchor.y

        var childY = child.y

        // Relative positions will be multiplied by the remaining space since the
        // target's position to the edge of the container.
        if (child.relativePositionAxes.isVertical) {
            childY *= drawHeight - targetY
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