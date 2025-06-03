package com.reco1l.andengine.container

import com.reco1l.andengine.component.*
import org.anddev.andengine.entity.*
import kotlin.math.*

open class UIContainer : UIComponent() {

    init {
        width = MatchContent
        height = MatchContent
    }


    override fun onContentChanged() {
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
    }


    //region Operators

    inline fun <reified T : IEntity>firstOf(): T? {
        return findChild { it is T } as? T
    }

    operator fun UIComponent.unaryPlus() {
        this@UIContainer.attachChild(this@unaryPlus)
    }

    operator fun UIComponent.unaryMinus() {
        this@UIContainer.detachChild(this@unaryMinus)
    }

    operator fun <T : IEntity> get(index: Int): T? {
        @Suppress("UNCHECKED_CAST")
        return getChild(index) as? T
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


