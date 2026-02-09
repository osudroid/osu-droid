package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*

@Suppress("LeakingThis")
open class ModMenuSection(name: String, private val toggles: List<UIButton> = listOf()) : UIFillContainer() {

    protected val toggleContainer: UILinearContainer


    init {
        orientation = Orientation.Vertical
        height = Size.Full
        cullingMode = CullingMode.CameraBounds
        style = {
            width = 14f.rem
            backgroundColor = it.accentColor * 0.1f
            radius = Radius.LG
        }

        +UIText().apply {
            width = Size.Full
            text = name.uppercase()
            alignment = Anchor.Center
            style = {
                fontSize = FontSize.XS
                padding = Vec4(3f.srem)
                color = it.accentColor
                alpha = 0.75f
            }
        }

        +UIScrollableContainer().apply {
            scrollAxes = Axes.Y
            width = Size.Full
            height = Size.Full
            clipToBounds = true

            +UILinearContainer().apply {
                width = Size.Full
                orientation = Orientation.Vertical
                style = {
                    padding = Vec4(2f.srem, 0f, 2f.srem, 2f.srem)
                    spacing = 2f.srem
                }

                toggles.fastForEach { +it }
                toggleContainer = this
            }
        }

        updateVisibility()
    }

    fun updateVisibility() {
        isVisible = toggles.any { it.isVisible }
    }

    open fun onSearchTermUpdate(searchTerm: String) {
        // Not using updateVisibility() here to avoid iterating over the toggles twice.
        var anyVisible = false

        toggles.fastForEach {
            if (it is ModMenuToggle) {
                it.updateVisibility(searchTerm)
            }

            if (it.isVisible) {
                anyVisible = true
            }
        }

        isVisible = anyVisible
    }

}