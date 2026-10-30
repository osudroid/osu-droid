package com.osudroid.game.replay.analysis

import com.edlplan.framework.utils.FloatArraySlice
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.UITriangleMesh
import com.reco1l.framework.Color4
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.opengl.util.GLHelper

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

    private val pathMesh = UITriangleMesh().apply { color = Color4(0xFFEB4791) }

    private val frameDotMesh = UITriangleMesh().apply { color = Color4(0xFFEB4791) }

    private val inducedTapMesh = UITriangleMesh().apply {
        color = Color4(0xFFFFCC22)
        alpha = 0.4f
    }

    private val directTapMesh = UITriangleMesh().apply { color = Color4(0xFFFFCC22) }

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

    override fun onManagedDraw(gl: GL10, camera: Camera) {
        if (isDirty) {
            isDirty = false
            geometry.build(timeMs, displayLengthMs, showCursorPath, showFrameMarkers, showTapMarkers)

            // Re-pointed on every rebuild, since build may have replaced the backing arrays.
            pathMesh.vertices.shareFrom(geometry.path)
            frameDotMesh.vertices.shareFrom(geometry.frameDots)
            inducedTapMesh.vertices.shareFrom(geometry.inducedTaps)
            directTapMesh.vertices.shareFrom(geometry.directTaps)
        }

        // UITriangleMesh does not set a blend function, so the state left by the previous draw (e.g. premultiplied
        // alpha from storyboard sprites) would otherwise apply and ignore the induced taps' alpha.
        GLHelper.enableBlend(gl)
        GLHelper.blendFunction(gl, GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)

        super.onManagedDraw(gl, camera)
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
