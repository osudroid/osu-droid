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
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.math.*

class BeatmapSetModel(
    val beatmapSetInfo: BeatmapSetInfo,
    var coverTexture: TextureRegion? = null,
    var isLoadingCover: Boolean = false,
) {
    /**
     * Whether the set has no cover to show, e.g. because it has no background. It is not loaded again then.
     */
    var isCoverUnavailable = false

    /**
     * The name of the cover texture, duplicate imports of a set share the same cover.
     */
    val coverTextureName: String?
        get() = beatmapSetInfo.beatmaps.getOrNull(0)?.let { "cover@${it.md5}" }

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
     * Loaded covers by texture name and the models showing them.
     */
    private val loadedCovers = HashMap<String, MutableSet<BeatmapSetModel>>()

    private var dataIndices = emptyMap<BeatmapSetModel, Int>()
    private var dataIndicesSource: List<BeatmapSetModel>? = null

    /**
     * Keeps only the covers of the panels on screen and [COVER_KEEP_MARGIN] panels above and below loaded,
     * otherwise scrolling through a large library keeps every cover in memory.
     */
    private fun updateCoverCache() {
        val data = data

        if (dataIndicesSource !== data) {
            dataIndicesSource = data
            dataIndices = data.withIndex().associate { (index, model) -> model to index }
        }

        var firstVisible = Int.MAX_VALUE
        var lastVisible = Int.MIN_VALUE

        for ((model, panel) in boundComponents) {
            val index = dataIndices[model] ?: continue

            // The expanded set stays bound while scrolled away, only the others tell which part is on screen.
            if (panel.isRecyclable) {
                firstVisible = min(firstVisible, index)
                lastVisible = max(lastVisible, index)
            }

            registerCover(model)
        }

        val keepFrom = max(0, firstVisible - COVER_KEEP_MARGIN)
        val keepTo = min(data.size - 1, lastVisible + COVER_KEEP_MARGIN)

        for (index in keepFrom..keepTo) {
            registerCover(data[index])
        }

        loadCoversAround(firstVisible, lastVisible, keepFrom, keepTo)

        val iterator = loadedCovers.entries.iterator()

        while (iterator.hasNext()) {
            val (name, models) = iterator.next()

            val isKept = models.any { model ->
                val index = dataIndices[model]
                model.isLoadingCover || model in boundComponents
                    || index != null && index >= firstVisible - COVER_KEEP_MARGIN && index <= lastVisible + COVER_KEEP_MARGIN
            }

            if (!isKept) {
                models.forEach { it.coverTexture = null }
                ResourceManager.getInstance().unloadTexture(name)
                iterator.remove()
            }
        }
    }


    private fun registerCover(model: BeatmapSetModel) {
        if (model.coverTexture != null) {
            val name = model.coverTextureName ?: return
            loadedCovers.getOrPut(name) { mutableSetOf() }.add(model)
        }
    }

    /**
     * Starts loading the missing covers between [from] and [to], the ones closest to the middle of the screen first.
     * Covers just outside the screen are loaded before they become visible.
     */
    private fun loadCoversAround(firstVisible: Int, lastVisible: Int, from: Int, to: Int) {
        if (from > to || isFastScrolling) {
            return
        }

        var loading = (from..to).count { data[it].isLoadingCover }
        val center = (firstVisible + lastVisible) / 2

        for (distance in 0..max(center - from, to - center)) {
            for (index in intArrayOf(center - distance, center + distance)) {
                if (loading >= MAX_CONCURRENT_COVER_LOADS) {
                    return
                }

                val model = data.getOrNull(index) ?: continue

                if (index < from || index > to || model.coverTexture != null || model.isLoadingCover || model.isCoverUnavailable || model.beatmapSetInfo.beatmaps.isEmpty()) {
                    continue
                }

                BeatmapCoverCache.load(model)
                loading++
            }
        }
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
        updateCoverCache()

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
         * Number of panels above and below the screen whose covers stay loaded.
         */
        private const val COVER_KEEP_MARGIN = 3

        /**
         * Covers decoded at the same time, more would compete with the game for the CPU on slow devices.
         */
        private const val MAX_CONCURRENT_COVER_LOADS = 2

        /**
         * Upper bound of pooled difficulty panels, sets with more difficulties create the rest on demand.
         */
        private const val BEATMAP_PANEL_POOL_LIMIT = 32
    }
}
