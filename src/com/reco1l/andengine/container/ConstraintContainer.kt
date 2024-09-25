package com.reco1l.andengine.container

import com.reco1l.andengine.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.shape.*
import org.anddev.andengine.util.ParameterCallable
import javax.microedition.khronos.opengles.*

/**
 * Container that allows to constrain nested entities to other entities in the same container.
 *
 * This is useful for creating complex layouts.
 */
class ConstraintContainer : Container() {


    private val constraints = mutableMapOf<ExtendedEntity, IShape>()


    override fun onApplyChildTranslation(gl: GL10, child: ExtendedEntity) {

        val constraint = constraints[child] ?: this

        val originOffsetX = child.width * child.originX
        val originOffsetY = child.height * child.originY

        val anchorOffsetX = constraint.width * child.anchorX
        val anchorOffsetY = constraint.height * child.anchorY

        var finalX = anchorOffsetX + child.x - originOffsetX + child.translationX
        var finalY = anchorOffsetY + child.y - originOffsetY + child.translationY

        // Apply the constraint's if it's not the container itself.
        if (constraint != this)  {
            finalX += constraint.x
            finalY += constraint.y
        }

        if (finalX != 0f || finalY != 0f) {
            gl.glTranslatef(finalX, finalY, 0f)
        }
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


    override fun detachChild(pEntity: IEntity): Boolean {
        return mChildren?.remove(pEntity, onDetachChild) ?: false
    }

    override fun detachChildren() {
        constraints.clear()
        mChildren?.clear(onDetachChild)
    }

    override fun detachChild(pEntityMatcher: IEntity.IEntityMatcher): IEntity? {
        return mChildren?.remove(pEntityMatcher, onDetachChild)
    }


    companion object {

        private val onDetachChild = ParameterCallable<IEntity> {
            if (it != null) {
                (it.parent as? ConstraintContainer)?.removeConstraint(it as? ExtendedEntity)
                it.parent = null
                it.onDetached()
            }
        }

    }
}