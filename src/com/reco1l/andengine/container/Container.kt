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
        width = MatchContent
        height = MatchContent
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

        contentWidth = right - padding.left
        contentHeight = bottom - padding.top
        invalidate(InvalidationFlag.ContentSize)
    }


    //region Operators

    inline fun <reified T : IEntity>firstOf(): T? {
        return findChild { it is T } as? T
    }

    operator fun ExtendedEntity.unaryPlus() {
        this@Container.attachChild(this@unaryPlus)
    }

    operator fun ExtendedEntity.unaryMinus() {
        this@Container.detachChild(this@unaryMinus)
    }

    operator fun <T : IEntity> get(index: Int): T {
        @Suppress("UNCHECKED_CAST")
        return getChild(index) as T
    }

    operator fun set(index: Int, entity: IEntity) {
        attachChild(entity, index)
    }

    operator fun plusAssign(entity: IEntity) {
        attachChild(entity)
    }

    operator fun minusAssign(entity: IEntity) {
        detachChild(entity)
    }

    //endregion
}


