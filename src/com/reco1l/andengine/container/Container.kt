package com.reco1l.andengine.container

import com.reco1l.andengine.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.IEntity.*
import org.anddev.andengine.util.*
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

open class Container : ExtendedEntity() {


    override var autoSizeAxes = Axes.Both
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
            onMeasureContentSize()
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


    open fun getChildDrawX(child: ExtendedEntity): Float {

        var x = child.x
        if (child.relativePositionAxes.isHorizontal) {
            x *= getPaddedWidth()
        }

        return getPadding().left + x + child.totalOffsetX
    }

    open fun getChildDrawY(child: ExtendedEntity): Float {

        var y = child.y
        if (child.relativePositionAxes.isVertical) {
            y *= getPaddedHeight()
        }

        return getPadding().top + y + child.totalOffsetY
    }


    /**
     * Called when the size of the container should be calculated.
     *
     * The default implementation of this method will take the farthest child's
     * position and size as the width and height respectively.
     */
    protected open fun onMeasureContentSize() {

        contentWidth = 0f
        contentHeight = 0f

        if (mChildren != null) {
            for (i in mChildren.indices) {

                val child = mChildren.getOrNull(i) ?: continue

                val x = max(0f, child.getDrawX())
                val y = max(0f, child.getDrawY())

                contentWidth = max(contentWidth, x + child.getDrawWidth())
                contentHeight = max(contentHeight, y + child.getDrawHeight())
            }
        }

        onContentSizeMeasured()
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
    override fun drawVertices(pGL: GL10, pCamera: Camera) {}

}


operator fun <T : IEntity> Container.get(index: Int): T {
    @Suppress("UNCHECKED_CAST")
    return getChild(index) as T
}

operator fun Container.set(index: Int, entity: IEntity) {
    attachChild(entity, index)
}

operator fun Container.plusAssign(entity: IEntity) {
    attachChild(entity)
}

operator fun Container.minusAssign(entity: IEntity) {
    detachChild(entity)
}