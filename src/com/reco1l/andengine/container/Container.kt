package com.reco1l.andengine.container

import com.reco1l.andengine.*
import org.anddev.andengine.entity.*
import kotlin.math.*

open class Container : ExtendedEntity() {

    init {
        width = FitContent
        height = FitContent
    }


    protected var shouldMeasureSize = true


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (shouldMeasureSize) {
            shouldMeasureSize = false
            onMeasureContentSize()
        }

        super.onManagedUpdate(pSecondsElapsed)
    }


    override fun onChildDetached(child: IEntity) {
        shouldMeasureSize = true
    }

    override fun onChildAttached(child: IEntity) {
        shouldMeasureSize = true
    }

    override fun onChildPositionChanged(child: IEntity) {
        shouldMeasureSize = true
    }

    override fun onChildSizeChanged(child: IEntity) {
        shouldMeasureSize = true
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

                val x = max(0f, child.absoluteX)
                val y = max(0f, child.absoluteY)

                contentWidth = max(contentWidth, x + child.getWidth())
                contentHeight = max(contentHeight, y + child.getHeight())
            }
        }

        invalidate(InvalidationFlag.ContentSize)
    }

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