package com.osudroid.game.replay.analysis

import com.edlplan.framework.utils.FloatArraySlice
import com.osudroid.ui.OsuColors
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.UITriangleMesh
import kotlin.properties.Delegates
import org.andengine.engine.camera.Camera
import org.andengine.graphics.DrawContext

/**
 * Draws the replay analysis overlay during replay playback, using the layers built by [ReplayAnalysisGeometry].
 *
 * Setters only mark the overlay as dirty. The geometry is rebuilt lazily in [onManagedDraw], not in [setTime],
 * because the game's catch-up loop may run several update ticks before a single draw. Rebuilding on every tick
 * would repeat the same work, while rebuilding once per draw produces the same result for the frame shown.
 *
 * @param data The [ReplayAnalysisData] to visualize.
 * @param scale The scale applied to sizes, used to convert osu! pixels to screen pixels.
 */
class ReplayAnalysisOverlay(data: ReplayAnalysisData, scale: Float) : UIContainer() {
    private val geometry = ReplayAnalysisGeometry(data, scale)

    private val pathMesh = UITriangleMesh().apply { color = OsuColors.pink2 }

    private val frameDotMesh = UITriangleMesh().apply { color = OsuColors.pink2 }

    private val inducedTapMesh = UITriangleMesh().apply {
        color = OsuColors.yellow
        alpha = 0.5f
    }

    private val directTapMesh = UITriangleMesh().apply { color = OsuColors.yellow }

    private var isDirty = true

    private var timeMs = 0f

    /**
     * Whether the tap markers (induced and direct taps) are shown.
     */
    var showTapMarkers by dirtyOnChange(false)

    /**
     * Whether the input frame dots are shown.
     */
    var showFrameMarkers by dirtyOnChange(false)

    /**
     * Whether the recent cursor path is shown.
     */
    var showCursorPath by dirtyOnChange(false)

    /**
     * The length of the displayed window, in milliseconds.
     */
    var displayLengthMs by dirtyOnChange(800)

    init {
        attachChild(pathMesh)
        attachChild(frameDotMesh)
        attachChild(inducedTapMesh)
        attachChild(directTapMesh)
    }

    /**
     * Sets the current replay time. The overlay is rebuilt for the window ending at this time on the next draw.
     *
     * @param timeMs The current replay time, in milliseconds.
     */
    fun setTime(timeMs: Float) {
        if (this.timeMs != timeMs) {
            this.timeMs = timeMs
            isDirty = true
        }
    }

    override fun onManagedDraw(drawContext: DrawContext, camera: Camera) {
        if (isDirty) {
            isDirty = false
            geometry.build(timeMs, displayLengthMs, showCursorPath, showFrameMarkers, showTapMarkers)

            // Re-pointed on every rebuild, since build may have replaced the backing arrays.
            pathMesh.vertices.shareFrom(geometry.path)
            frameDotMesh.vertices.shareFrom(geometry.frameDots)
            inducedTapMesh.vertices.shareFrom(geometry.inducedTaps)
            directTapMesh.vertices.shareFrom(geometry.directTaps)
        }

        super.onManagedDraw(drawContext, camera)
    }

    /**
     * Creates a property delegate that marks the overlay as dirty when the value changes.
     *
     * @param initialValue The initial value of the property.
     */
    private fun <T> dirtyOnChange(initialValue: T) =
        Delegates.observable(initialValue) { _, oldValue, newValue ->
            if (oldValue != newValue) {
                isDirty = true
            }
        }
}

private fun FloatArraySlice.shareFrom(other: FloatArraySlice) {
    ary = other.ary
    offset = 0
    length = other.length
}
