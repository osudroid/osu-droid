package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

@Suppress("LeakingThis")
open class UICard(

    /**
     * The content of the card.
     */
    val content: UIContainer = UILinearContainer().apply {
        orientation = Orientation.Vertical
        width = FillParent
        clipToBounds = true
    },

) : UILinearContainer() {

    /**
     * The title of the card.
     */
    var title
        get() = titleBar.firstOf<UIText>()?.text ?: ""
        set(value) { titleBar.firstOf<UIText>()?.text = value }


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
        background = UIBox().apply {
            cornerRadius = 14f
            applyTheme = { color = it.accentColor * 0.15f }
        }

        +titleBar.apply {
            width = FillParent
            padding = Vec4(12f, 8f)

            +UIText().apply {
                font = ResourceManager.getInstance().getFont("smallFont")
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                applyTheme = { color = it.accentColor }
            }

            +UITriangle().apply {
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
                rotationCenter = Anchor.Center
                width = 16f
                height = 12f
                applyTheme = {
                    color = it.accentColor
                    alpha = 0.5f
                }
            }
        }

        +UIBox().apply {
            width = FillParent
            height = 1f
            applyTheme = {
                color = it.accentColor
                alpha = 0.025f
            }
        }

        +content
    }


    /**
     * Collapses the card content.
     */
    fun collapse() {
        if (content.isVisible) {
            val triangle = titleBar.firstOf<UITriangle>()
            triangle?.clearModifiers(ModifierType.Rotation)
            triangle?.rotateTo(180f, 0.1f)

            content.clearModifiers(ModifierType.SizeY)
            content.sizeToY(0f, 0.1f).then {
                it.isVisible = false
                onContentChanged()
            }
        }
    }

    /**
     * Expands the card content.
     */
    fun expand() {
        if (!content.isVisible) {
            val triangle = titleBar.firstOf<UITriangle>()
            triangle?.clearModifiers(ModifierType.Rotation)
            triangle?.rotateTo(0f, 0.1f)

            content.clearModifiers(ModifierType.SizeY)
            content.isVisible = true
            content.sizeToY(content.contentHeight, 0.1f)
        }
    }

}