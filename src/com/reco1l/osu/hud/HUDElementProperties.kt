package com.reco1l.osu.hud

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.attachTo
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.container.LinearContainer
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.ScrollableContainer
import com.reco1l.andengine.getPaddedHeight
import com.reco1l.andengine.getPaddedWidth
import com.reco1l.andengine.shape.Box
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.andengine.text.ExtendedText
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.math.Vec4
import com.reco1l.osu.hud.data.HUDElementLayoutData
import com.reco1l.osu.hud.data.HUDElementSkinData
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager

class HUDElementProperties(private val hud: GameplayHUD) : Container() {


    init {

        relativeSizeAxes = Axes.Y
        height = 1f
        x = Config.getRES_WIDTH().toFloat() - BUTTON_WIDTH

        // The button to show/hide the element selector
        object : Container() {

            init {
                background = RoundedBox().apply {
                    cornerRadius = BUTTON_RADIUS
                    color = ColorARGB(0xFF181825)
                }
                width = BUTTON_WIDTH + BUTTON_RADIUS
                height = 150f
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft

                ExtendedText().apply {
                    rotation = 90f
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "Properties"
                    x = -(BUTTON_RADIUS / 2)
                } attachTo this
            }

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                if (event.isActionUp) {

                    this@HUDElementProperties.clearEntityModifiers()

                    if (this@HUDElementProperties.x < Config.getRES_WIDTH() - BUTTON_WIDTH) {
                        this@HUDElementProperties.moveToX(Config.getRES_WIDTH() - BUTTON_WIDTH, 0.2f)

                        hud.sizeToX(Config.getRES_WIDTH().toFloat(), 0.2f)
                    } else {
                        this@HUDElementProperties.moveToX(Config.getRES_WIDTH() - BUTTON_WIDTH - MENU_WIDTH, 0.2f)

                        hud.sizeToX(Config.getRES_WIDTH() - MENU_WIDTH, 0.2f)
                    }
                }

                return false
            }

        } attachTo this

        ScrollableContainer().apply {

            scrollAxes = Axes.Y
            relativeSizeAxes = Axes.Y
            height = 1f
            width = MENU_WIDTH
            x = BUTTON_WIDTH

            indicatorY!!.width = 4f

            background = Box().apply {
                color = ColorARGB(0xFF1E1E2E)
            }

            val linearContainer = LinearContainer().apply {
                relativeSizeAxes = Axes.X
                width = 1f
                padding = Vec4(16f)
                spacing = 12f
                orientation = Orientation.Vertical
            } attachTo this


        } attachTo this

    }


    companion object {

        const val MENU_WIDTH = 300f

        const val BUTTON_WIDTH = 48f

        const val BUTTON_RADIUS = 12f

    }
}