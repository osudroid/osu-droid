@file:Suppress("MemberVisibilityCanBePrivate")

package com.reco1l.andengine.container

import com.reco1l.andengine.component.*

open class UIFlexContainer : UIContainer() {

    /**
     * The gap between the children of the container.
     *
     * This will only take effect if justify content is set to [Start][JustifyContent.Start],
     * [End][JustifyContent.End] or [Center][JustifyContent.Center].
     */
    var gap = 0f
        set(value) {
            field = value
            invalidate(InvalidationFlag.Content)
        }

    /**
     * The direction of the flex container.
     */
    var direction = FlexDirection.Row
        set(value) {
            field = value
            invalidate(InvalidationFlag.Content)
        }

    /**
     * The justification of the content inside the flex container.
     *
     * This will affect how the children are positioned and sized inside the container.
     */
    var justifyContent = JustifyContent.Start
        set(value) {
            field = value
            invalidate(InvalidationFlag.Content)
        }


    private var rules = mutableMapOf<UIComponent, FlexRules>()


    //region Utilities

    private var UIComponent.directionSize
        get() = if (direction == FlexDirection.Row) width else height
        set(value) = if (direction == FlexDirection.Row) {
            minWidth = value
            maxWidth = value
        } else {
            minHeight = value
            maxHeight = value
        }

    private val UIComponent.directionBaseSize
        get() = if (direction == FlexDirection.Row) contentWidth + padding.horizontal else contentHeight + padding.vertical

    private val UIComponent.directionInnerSize
        get() = if (direction == FlexDirection.Row) innerWidth else innerHeight

    private var UIComponent.directionPosition
        get() = if (direction == FlexDirection.Row) x else y
        set(value) = (if (direction == FlexDirection.Row) x = value else y = value)

    //endregion

    override fun onContentChanged() {

        var spaceSum = 0f
        var growSum = 0f

        forEach { child -> child as UIComponent
            val rules = rules[child]

            spaceSum += rules?.basis ?: child.directionBaseSize
            growSum += rules?.grow ?: 0f
        }

        val totalGapSpace = gap * (childCount - 1)
        val freeSpace = directionInnerSize - spaceSum - totalGapSpace
        val freeSpaceToJustify = if (growSum > 0f) 0f else freeSpace

        var currentPosition = when (justifyContent) {
            JustifyContent.End -> freeSpaceToJustify
            JustifyContent.Center -> freeSpaceToJustify / 2f
            else -> 0f
        }

        forEachIndexed { _, child -> child as UIComponent

            val rules = rules[child]
            val assignedSpace = if (growSum > 0f) ((rules?.grow ?: 0f) / growSum) * freeSpace else 0f

            child.directionSize = (rules?.basis ?: child.directionBaseSize) + assignedSpace

            when (justifyContent) {

                JustifyContent.Start, JustifyContent.End, JustifyContent.Center -> {
                    child.directionPosition = currentPosition
                    currentPosition += child.directionSize + gap
                }

                JustifyContent.SpaceBetween, JustifyContent.SpaceAround -> {

                    val space = when (justifyContent) {
                        JustifyContent.SpaceBetween -> if (childCount == 1) 0f else freeSpaceToJustify / (childCount - 1)
                        JustifyContent.SpaceAround -> freeSpaceToJustify / (childCount + 1)

                        else -> 0f // Unreachable
                    }

                    if (justifyContent == JustifyContent.SpaceAround) {
                        currentPosition += space / 2f
                    }

                    child.directionPosition = currentPosition
                    currentPosition += child.directionSize + space
                }
            }
        }

        super.onContentChanged()
    }


    /**
     * Sets the flex rules for the given [UIComponent].
     */
    fun UIComponent.flexRules(block: FlexRules.() -> Unit) {
        rules.getOrPut(this) { FlexRules() }.block()
        invalidate(InvalidationFlag.Content)
    }

}

/**
 * The flex rules for a [UIComponent] inside a [UIFlexContainer].
 */
data class FlexRules(
    /**
     * The grow factor of the component.
     *
     * This will determine how much space the component will take relative to other components
     * with a grow factor.
     */
    var grow: Float = 0f,

    /**
     * The basis size of the component.
     *
     * This will be the initial size of the component before any grow factor is applied.
     */
    var basis: Float? = null
)


/**
 * The direction of the flex container.
 */
enum class FlexDirection {
    Row,
    Column
}

/**
 * The justification of the content inside a [UIFlexContainer].
 */
enum class JustifyContent {
    Start,
    End,
    Center,
    SpaceBetween,
    SpaceAround
}
