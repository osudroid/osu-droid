package com.reco1l.andengine.container

import com.reco1l.andengine.component.*
import com.reco1l.andengine.text
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.theme.Size
import org.anddev.andengine.entity.*
import kotlin.math.*

open class UIContainer : UIComponent() {

    init {
        width = Size.Auto
        height = Size.Auto
    }


    override fun onContentChanged() {
        var contentWidth = 0f
        var contentHeight = 0f

        if (mChildren != null) {
            for (i in mChildren.indices) {

                val child = mChildren.getOrNull(i) ?: continue

                val x = max(0f, child.absoluteX)
                val y = max(0f, child.absoluteY)

                contentWidth = max(contentWidth, x + child.width)
                contentHeight = max(contentHeight, y + child.height)
            }
        }

        this.contentWidth = contentWidth - padding.left
        this.contentHeight = contentHeight - padding.top
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

    operator fun String.unaryPlus(): UIText {
        return text {
            text = this@unaryPlus
        }
    }

    //endregion
}


