@file:Suppress("MemberVisibilityCanBePrivate")

package com.reco1l.andengine.container

import com.reco1l.andengine.component.*
import com.reco1l.andengine.theme.Size
import kotlin.math.max

open class UIFillContainer : UILinearContainer() {

    override fun onContentChanged() {

        // This will not have any free space to distribute.
        if (orientation == Orientation.Horizontal && rawWidth == Size.Auto
            || orientation == Orientation.Vertical && rawHeight == Size.Auto) {
            super.onContentChanged()
            return
        }

        var totalWidth = 0f
        var totalHeight = 0f
        var totalWeight = 0f

        var visibleCount = 0

        // First pass - calculate total weight and natural sizes (content + padding)
        forEach { child ->

            if (child !is UIComponent || !child.isVisible) {
                return@forEach
            }
            visibleCount++

            // In the layout system dimensions below zero are not allowed, so we use negative
            // values to indicate Auto or Full sizes but we can't use them directly here,
            // instead we need to use intrinsic sizes. But if raw size is greater or equal to
            // zero it means the size is fixed and we should use it as is.
            totalWidth += if (child.rawWidth >= 0f) child.rawWidth else child.intrinsicWidth
            totalHeight += if (child.rawHeight >= 0f) child.rawHeight else child.intrinsicHeight

            when (orientation) {
                Orientation.Horizontal -> if (child.rawWidth == Size.Full) totalWeight++
                Orientation.Vertical -> if (child.rawHeight == Size.Full) totalWeight++
            }
        }

        // Nothing to distribute.
        if (totalWeight == 0f) {
            super.onContentChanged()
            return
        }

        val totalSpacing = spacing * (visibleCount - 1)
        val allChildrenAreFull = (totalWeight == visibleCount.toFloat())

        // When all children are Full, divide container evenly instead of distributing free space
        val freeSpace = when (orientation) {
            Orientation.Horizontal -> innerWidth - totalWidth - totalSpacing
            Orientation.Vertical -> innerHeight - totalHeight - totalSpacing
        }

        val evenSize = when (orientation) {
            Orientation.Horizontal -> (innerWidth - totalSpacing) / totalWeight
            Orientation.Vertical -> (innerHeight - totalSpacing) / totalWeight
        }

        // Second pass - place items, distribute remaining space according to weights
        var contentWidth = 0f
        var contentHeight = 0f

        mChildren?.forEachIndexed { index, child ->

            if (child !is UIComponent || !child.isVisible) {
                return@forEachIndexed
            }

            val weightPortion = 1f / totalWeight
            val extraSpace = freeSpace * weightPortion
            val spacing = (if (index < visibleCount - 1) spacing else 0f)

            when (orientation) {

                Orientation.Horizontal -> {
                    if (child.rawWidth == Size.Full) {
                        val assignedWidth = if (allChildrenAreFull) evenSize else child.intrinsicWidth + extraSpace
                        child.minWidth = assignedWidth
                        child.maxWidth = assignedWidth
                    }
                    child.x = contentWidth

                    contentWidth += child.width + spacing
                    contentHeight = max(contentHeight, child.height)
                }

                Orientation.Vertical -> {
                    if (child.rawHeight == Size.Full) {
                        val assignedHeight = if (allChildrenAreFull) evenSize else child.intrinsicHeight + extraSpace
                        child.minHeight = assignedHeight
                        child.maxHeight = assignedHeight
                    }
                    child.y = contentHeight

                    contentWidth = max(contentWidth, child.width)
                    contentHeight += child.height + spacing
                }
            }
        }

        this.contentWidth = contentWidth
        this.contentHeight = contentHeight
    }

}