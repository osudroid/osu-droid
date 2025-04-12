package com.reco1l.andengine.container

import com.reco1l.andengine.*
import org.anddev.andengine.entity.*
import kotlin.math.*

open class Container : ExtendedEntity() {

    /**
     * Whether the size of the container should be measured.
     */
    protected var shouldMeasureSize = true


    init {
        width = FitContent
        height = FitContent
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (shouldMeasureSize) {
            shouldMeasureSize = false
            onMeasureContentSize()
        }

        super.onManagedUpdate(deltaTimeSec)
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

        var right = 0f
        var bottom = 0f

        if (mChildren != null) {
            for (i in mChildren.indices) {

                val child = mChildren.getOrNull(i) ?: continue

                val x = max(0f, child.absoluteX)
                val y = max(0f, child.absoluteY)

                right = max(right, x + child.getWidth())
                bottom = max(bottom, y + child.getHeight())
            }
        }

        contentWidth = right
        contentHeight = bottom
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