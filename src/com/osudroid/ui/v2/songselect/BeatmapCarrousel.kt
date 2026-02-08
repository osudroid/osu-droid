package com.osudroid.ui.v2.songselect

import com.osudroid.data.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.theme.Size
import com.reco1l.framework.math.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.opengl.texture.region.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*

class BeatmapSetModel(
    val beatmapSetInfo: BeatmapSetInfo,
    var coverTexture: TextureRegion? = null,
    var isLoadingCover: Boolean = false,
)

class BeatmapCarrousel : UIRecyclerContainer<BeatmapSetModel, BeatmapSetPanel>(), IPanelContainer<BeatmapSetPanel> {

    /**
     * The currently selected beatmap set panel.
     */
    override var selectedPanel: BeatmapSetPanel? = null
        set(value) {
            if (field != value) {
                field = value

                boundComponents.values.forEach { panel ->
                    if (panel != value) {
                        panel.collapse()
                    } else {
                        panel.expand()
                    }
                }
            }
        }

    /**
     * Whether to automatically scroll to the selected panel.
     */
    var autoScrollToSelectedPanel = false


    init {
        scrollAxes = Axes.Y
        width = Size.Full
        height = Size.Full
        anchor = Anchor.TopRight
        origin = Anchor.TopRight

        componentWrapper.apply {
            orientation = Orientation.Vertical
            width = Size.Full
            spacing = -2f
            padding = Vec4(0f, 200f)
            translationX = 14f
        }

        onCreateComponent = { BeatmapSetPanel(this) }
    }


    private fun getPositionPercentage(componentY: Float, componentHeight: Float, parentScroll: Float, parentHeight: Float): Float {
        val positionInScreen = componentY + componentHeight / 2f - parentScroll
        val percentage = positionInScreen / parentHeight

        return percentage
    }

    override fun onDrawComponent(gl: GL10, camera: Camera, component: BeatmapSetPanel) {

        val buttonAbsoluteY = component.absoluteY + component.button.absoluteY
        val mainPanelYPercentage = getPositionPercentage(buttonAbsoluteY, component.button.height, scrollY, height)

        component.button.translationX = SHEAR * (1f - mainPanelYPercentage)

        if (selectedPanel == component) {
            component.panelContainer.forEach { panel -> panel as BeatmapPanel
                val panelYPercentage = getPositionPercentage(buttonAbsoluteY + panel.absoluteY, panel.height, scrollY, height)

                panel.translationX = -component.translationX + SHEAR * (1f - panelYPercentage)
            }
        }

        super.onDrawComponent(gl, camera, component)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        val panel = selectedPanel
        if (panel != null && autoScrollToSelectedPanel) {

            val panelY = panel.absoluteY + panel.selectedPanelY - height / 2f
            val difference = panelY - scrollY

            var targetY = scrollY

            if (abs(difference) > 5f) {
                targetY += difference * 0.2f
            } else {
                targetY = panelY
                autoScrollToSelectedPanel = false
            }

            scrollY = targetY.coerceAtLeast(0f).coerceAtMost(maxScrollY)
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionDown) {
            autoScrollToSelectedPanel = false
        }
        return super.onAreaTouched(event, localX, localY)
    }


    companion object {
        private const val SHEAR = 180f
    }
}