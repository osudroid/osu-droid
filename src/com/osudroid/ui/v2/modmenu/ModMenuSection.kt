package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.mods.*
import ru.nsu.ccfit.zuev.osu.*

class ModMenuSection(name: String, mods: List<Mod>) : LinearContainer() {

    init {
        orientation = Orientation.Vertical
        width = 340f
        height = FillParent

        background = Box().apply {
            applyTheme = { color = it.accentColor * 0.1f }
            cornerRadius = 16f
        }

        +ExtendedText().apply {
            width = FillParent
            text = name
            alignment = Anchor.Center
            font = ResourceManager.getInstance().getFont("smallFont")
            padding = Vec4(12f)
            applyTheme = {
                color = it.accentColor
                alpha = 0.75f
            }
        }

        +ScrollableContainer().apply {
            scrollAxes = Axes.Y
            width = FillParent
            height = FillParent
            clipToBounds = true

            +LinearContainer().apply {
                width = FillParent
                orientation = Orientation.Vertical
                padding = Vec4(12f, 0f, 12f, 12f)
                spacing = 16f

                mods.fastForEach { mod ->
                    +ModMenuToggle(mod).apply { ModMenu.modToggles.add(this) }
                }
            }
        }
    }
}