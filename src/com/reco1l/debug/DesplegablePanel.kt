package com.reco1l.debug

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

@Suppress("LeakingThis")
open class DesplegablePanel : Container() {

    /**
     * Whether the panel is expanded or collapsed.
     */
    var isExpanded = true
        set(value) {
            if (field != value) {
                field = value

                if (value) {
                    expand()
                } else {
                    collapse()
                }
            }
        }


    /**
     * The title of the panel.
     */
    val title = ExtendedText()

    /**
     * The container that holds the content of the panel.
     */
    val content = ScrollableContainer()


    private val collapse = object : Triangle() {
        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (event.isActionUp) {
                isExpanded = !isExpanded
            }
            return true
        }
    }


    private var initialX = 0f
    private var initialY = 0f


    init {
        background = Box().apply { color = ColorARGB(0xFF161622) }
        foreground = Box().apply {
            color = ColorARGB(0xFF383852)
            paintStyle = PaintStyle.Outline
        }

        +content.apply {
            clipChildren = true
            scrollAxes = Axes.Both
            overflowAxes = Axes.Y
            y = TITLE_BAR_HEIGHT
        }
        
        +object : Container() {

            init {
                background = Box().apply { color = ColorARGB(0xFF383852) }
                width = FitParent
                height = TITLE_BAR_HEIGHT

                +title.apply {
                    font = ResourceManager.getInstance().getFont("smallFont")
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                }

                +collapse.apply {
                    color = ColorARGB.White
                    padding = Vec4(12f, 8f)
                    width = 12f
                    height = 12f
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    rotationCenter = Anchor.Center
                }
            }
        }
    }


    fun collapse() {
        content.isVisible = false
        collapse.rotation = 0f
    }

    fun expand() {
        content.isVisible = true
        collapse.rotation = 180f
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        height = if (isExpanded) FitContent else TITLE_BAR_HEIGHT
        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (!super.onAreaTouched(event, localX, localY)) {
            if (event.isActionDown) {
                initialX = event.x
                initialY = event.y
            }

            if (event.isActionMove) {
                x += (event.x - initialX)
                y += (event.y - initialY)

                initialX = event.x
                initialY = event.y
            }
        }
        return true
    }


    companion object {
        const val TITLE_BAR_HEIGHT = 40f
    }
}