package com.reco1l.andengine.container

import com.reco1l.andengine.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.IEntity.*
import org.anddev.andengine.entity.shape.IShape
import org.anddev.andengine.util.*
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

open class Container : ExtendedEntity() {


    override var autoSizeAxes = Axes.None
        set(value) {
            if (field != value) {
                field = value
                shouldMeasureSize = true
            }
        }


    protected var shouldMeasureSize = true


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (shouldMeasureSize) {
            shouldMeasureSize = false
            onMeasureSize()
        }

        super.onManagedUpdate(pSecondsElapsed)
    }


    protected open fun onChildDetached(child: IEntity) {
        child.parent = null
        child.onDetached()
        shouldMeasureSize = true
    }

    protected open fun onChildAttached(child: IEntity) {
        child.parent = this
        child.onAttached()
        shouldMeasureSize = true
    }


    open fun onChildPositionChanged(child: IEntity) {
        shouldMeasureSize = true
    }

    open fun onChildSizeChanged(child: IEntity) {
        shouldMeasureSize = true
    }


    open fun onApplyChildTranslation(gl: GL10, child: ExtendedEntity) {

        val originOffsetX = child.width * child.originX
        val originOffsetY = child.height * child.originY

        val anchorOffsetX = width * child.anchorX
        val anchorOffsetY = height * child.anchorY

        val finalX = anchorOffsetX + child.x - originOffsetX + child.translationX
        val finalY = anchorOffsetY + child.y - originOffsetY + child.translationY

        if (finalX != 0f || finalY != 0f) {
            gl.glTranslatef(finalX, finalY, 0f)
        }
    }


    /**
     * Called when the size of the container should be calculated.
     *
     * The default implementation of this method will take the farthest child's
     * position and size as the width and height respectively.
     */
    protected open fun onMeasureSize() {

        var maxWidth = 0f
        var maxHeight = 0f

        if (mChildren != null) {
            for (i in mChildren.indices) {

                val child = mChildren.getOrNull(i) ?: continue

                if (child is IShape) {
                    maxWidth = max(maxWidth, child.x + child.width)
                    maxHeight = max(maxHeight, child.y + child.height)
                }
            }
        }

        onApplySize(maxWidth, maxHeight)
    }

    /**
     * Applies the size of the container.
     *
     * Should be called right after [onMeasureSize] to update the size of the container
     * only if [autoSizeAxes] is not [Axes.None].
     */
    protected fun onApplySize(width: Float, height: Float) {

        if (autoSizeAxes == Axes.None) {
            return
        }

        if (internalWidth != width || internalHeight != height) {

            if (autoSizeAxes == Axes.X || autoSizeAxes == Axes.Both) {
                internalWidth = width
            }

            if (autoSizeAxes == Axes.Y || autoSizeAxes == Axes.Both) {
                internalHeight = height
            }

            updateVertexBuffer()
        }
    }


    override fun detachChild(pEntity: IEntity): Boolean {
        if (mChildren?.remove(pEntity) == true) {
            onChildDetached(pEntity)
            return true
        }
        return false
    }

    override fun detachChildren() {
        mChildren?.forEachTrim {
            onChildDetached(it)
        }
    }

    override fun detachChild(pEntityMatcher: IEntityMatcher): IEntity? {

        if (mChildren == null) {
            return null
        }

        for (i in mChildren.size downTo 0) {
            val child = mChildren[i]

            if (pEntityMatcher.matches(child)) {
                onChildDetached(child)
                return child
            }
        }
        return null
    }


    override fun attachChild(pEntity: IEntity) {
        attachChild(pEntity, mChildren?.size ?: 0)
    }

    override fun attachChild(pEntity: IEntity, pIndex: Int): Boolean {

        if (pEntity == this) {
            throw IllegalArgumentException("Cannot attach a child to itself.")
        }

        if (mChildren == null) {
            mChildren = SmartList(4)
        }

        if (pEntity !in mChildren) {
            mChildren.add(pIndex, pEntity)
            onChildAttached(pEntity)
            return true
        }
        return false
    }



    override fun onUpdateVertexBuffer() {}

}

