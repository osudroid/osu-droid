package com.reco1l.andengine.container

import com.reco1l.andengine.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import javax.microedition.khronos.opengles.*

/**
 * Container that allows to constrain nested entities to other entities in the same container.
 *
 * This is useful for creating complex layouts.
 */
open class ConstraintContainer : Container() {

    private val constraints = mutableMapOf<ExtendedEntity, ExtendedEntity>()


    override fun onManagedDrawChildren(pGL: GL10, pCamera: Camera) {

        mChildren?.fastForEach { child ->

            if (child !is ExtendedEntity) {
                return@fastForEach
            }

            val target = constraints[child]

            if (target == this || target == null) {
                return@fastForEach
            }

            val targetX = target.absX
            val targetY = target.absY
            val targetWidth = target.transformedWidth
            val targetHeight = target.transformedHeight

            child.x = targetX - child.anchorPositionX + targetWidth * child.anchor.x
            child.y = targetY - child.anchorPositionY + targetHeight * child.anchor.y
        }

        super.onManagedDrawChildren(pGL, pCamera)
    }

    /**
     * Adds a constraint to a child.
     */
    fun addConstraint(child: ExtendedEntity, target: ExtendedEntity) {

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
                source.invalidate(InvalidationFlag.Transformations)
                onChildPositionChanged(source)
            }
        }
        super.onChildPositionChanged(child)
    }

}