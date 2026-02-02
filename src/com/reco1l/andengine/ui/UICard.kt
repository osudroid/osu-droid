package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*

@Suppress("LeakingThis")
open class UICard(

    /**
     * The content of the card.
     */
    val content: UIContainer = UILinearContainer().apply {
        orientation = Orientation.Vertical
        width = Size.Full
        clipToBounds = true
    },

) : UILinearContainer() {

    /**
     * The title of the card.
     */
    var title
        get() = titleBar.firstOf<UIText>()?.text ?: ""
        set(value) { titleBar.firstOf<UIText>()?.text = value }

    /**
     * A function that is called when this [UICard] is collapsed or expanded.
     */
    var onExpandStatusChange: ((isExpanded: Boolean) -> Unit)? = null

    private val titleBar = object : UIContainer() {
        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (event.isActionUp) {
                (parent as UICard).apply {
                    if (content.isVisible) {
                        collapse()
                    } else {
                        expand()
                    }
                }
            }
            return true
        }
    }


    init {
        orientation = Orientation.Vertical
        style = {
            backgroundColor = it.accentColor * 0.15f
            radius = Radius.MD
        }

        +titleBar.apply {
            width = Size.Full
            style = {
                padding = Vec4(2.25f.srem)
            }

            +UIText().apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                style = {
                    color = it.accentColor
                    fontSize = FontSize.SM
                }
            }

            +FontAwesomeIcon(Icon.ChevronDown).apply {
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
                rotationCenter = Anchor.Center
                style = {
                    color = it.accentColor
                    alpha = 0.5f
                }
            }
        }

        box {
            width = Size.Full
            height = 2f
            style = {
                color = it.accentColor.copy(alpha = 0.1f)
            }
        }

        +content
    }


    /**
     * Collapses the card content.
     *
     * @param immediate Whether to collapse immediately (without animation).
     */
    @JvmOverloads
    fun collapse(immediate: Boolean = false) {
        if (content.isVisible) {
            val triangle = titleBar.firstOf<FontAwesomeIcon>()
            triangle?.clearModifiers(ModifierType.Rotation)
            triangle?.rotateTo(180f, if (immediate) 0f else 0.1f)

            content.clearModifiers(ModifierType.SizeY)
            content.sizeToY(0f, if (immediate) 0f else 0.1f).after {
                it.isVisible = false
                onContentChanged()
            }

            onExpandStatusChange?.invoke(false)
        }
    }

    /**
     * Expands the card content.
     *
     * @param immediate Whether to expand immediately (without animation).
     */
    @JvmOverloads
    fun expand(immediate: Boolean = false) {
        if (!content.isVisible) {
            val triangle = titleBar.firstOf<FontAwesomeIcon>()
            triangle?.clearModifiers(ModifierType.Rotation)
            triangle?.rotateTo(0f, if (immediate) 0f else 0.1f)

            content.clearModifiers(ModifierType.SizeY)
            content.isVisible = true
            content.sizeToY(content.contentHeight, if (immediate) 0f else 0.1f)

            onExpandStatusChange?.invoke(true)
        }
    }

}