package com.osudroid.ui.v2.songselect

import com.osudroid.data.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.theme.rem
import com.reco1l.framework.math.*
import org.andengine.engine.camera.Camera
import org.andengine.input.touch.TouchEvent
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import kotlin.math.*

class BeatmapSetModel(
    val beatmapSetInfo: BeatmapSetInfo,
    var coverTexture: TextureRegion? = null,
    var isLoadingCover: Boolean = false,
) {
    override fun toString(): String {
        return beatmapSetInfo.directory
    }
}

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
     * Whether the carousel moves too fast for panels to be worth loading covers for.
     */
    val isFastScrolling: Boolean
        get() = isDraggingIndicator || abs(velocityY) > FAST_SCROLL_VELOCITY

    /**
     * Whether to automatically scroll to the selected panel.
     */
    var autoScrollToSelectedPanel = false


    init {
        scrollAxes = Axes.Y
        width = FillParent
        height = FillParent

        // Scrolling like the legacy song menu, immediate drag, fling from the whole gesture,
        // long linear deceleration, no overscroll bounce.
        minimumTravel = Vec2(6f)
        maxVelocity = Vec2(12000f)
        velocityFromGesture = true
        flingDeceleration = Vec2(1200f)
        overflowAxes = Axes.None

        // Grab the scroll indicator at the right edge to jump through the whole list.
        isVerticalIndicatorDraggable = true
        verticalIndicator?.width = 10f
        anchor = Anchor.TopRight
        origin = Anchor.TopRight

        componentWrapper.apply {
            orientation = Orientation.Vertical
            width = FillParent
            spacing = -2f
            // Left padding leaves room for the expanded set button, which grows 3rem past the panel width,
            // without it the button would be clipped at the carousel's left edge.
            padding = Vec4(3f.rem, 200f, 0f, 200f)
        }

        onCreateComponent = { BeatmapSetPanel(this) }
    }


    /**
     * Difficulty panels not listed under any set, shared by all set panels so expanding a set
     * does not create new panels every time.
     */
    private val beatmapPanelPool = ArrayDeque<BeatmapPanel>()

    fun obtainBeatmapPanel(setPanel: BeatmapSetPanel, beatmapInfo: BeatmapInfo): BeatmapPanel {
        val panel = beatmapPanelPool.removeLastOrNull() ?: BeatmapPanel()
        panel.bind(setPanel, beatmapInfo)
        return panel
    }

    /**
     * Returns a panel to the pool, the caller detaches it from its set panel.
     */
    fun releaseBeatmapPanel(panel: BeatmapPanel) {
        if (beatmapPanelPool.size < BEATMAP_PANEL_POOL_LIMIT) {
            beatmapPanelPool.addLast(panel)
        }
    }


    private fun getPositionPercentage(componentY: Float, componentHeight: Float, parentScroll: Float, parentHeight: Float): Float {
        val positionInScreen = componentY + componentHeight / 2f - parentScroll
        return positionInScreen / parentHeight
    }

    /**
     * Horizontal inset of a panel on the arc, panels at the vertical center of the carousel reach furthest
     * to the left, panels get narrower the further they are from the center (like the legacy song menu).
     * The right edge of every panel stays flush with the carousel's right edge.
     */
    private fun getArcInset(positionPercentage: Float): Float {
        // -1 at the top edge, 0 at the center, 1 at the bottom edge.
        val distanceFromCenter = ((positionPercentage - 0.5f) * 2f).coerceIn(-1f, 1f)
        return ARC_DEPTH * (1f - cos(distanceFromCenter * PI.toFloat() / 2f))
    }

    override fun onDrawComponent(pGLState: GLState, camera: Camera, component: BeatmapSetPanel) {

        val buttonAbsoluteY = component.absoluteY + component.button.absoluteY
        val mainPanelYPercentage = getPositionPercentage(buttonAbsoluteY, component.button.height, scrollY, height)

        component.setArcInset(getArcInset(mainPanelYPercentage))

        if (selectedPanel == component) {
            val panelsAbsoluteY = component.absoluteY + component.panelsContainer.absoluteY

            component.panelsContainer.forEach { panel -> panel as BeatmapPanel
                // Panels are stacked from the top, their y equals their absolute y.
                val panelY = panelsAbsoluteY + panel.y
                val panelHeight = panel.height
                val panelTop = panelY - scrollY

                panel.isOnScreen = panelTop + panelHeight > -OFF_SCREEN_MARGIN && panelTop < height + OFF_SCREEN_MARGIN

                if (panel.isOnScreen) {
                    panel.setArcInset(getArcInset(getPositionPercentage(panelY, panelHeight, scrollY, height)))
                }
            }
        }

        super.onDrawComponent(pGLState, camera, component)
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
        /**
         * How much narrower panels at the top and bottom edge are compared to the center.
         */
        private const val ARC_DEPTH = 220f

        /**
         * Fling speed in units per second above which covers are not loaded.
         */
        private const val FAST_SCROLL_VELOCITY = 3000f

        /**
         * Distance beyond the carousel's edges within which difficulty panels still count as on screen.
         */
        private const val OFF_SCREEN_MARGIN = 100f

        /**
         * Upper bound of pooled difficulty panels, sets with more difficulties create the rest on demand.
         */
        private const val BEATMAP_PANEL_POOL_LIMIT = 32
    }
}
