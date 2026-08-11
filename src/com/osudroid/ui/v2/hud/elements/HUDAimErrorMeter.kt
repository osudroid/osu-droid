package com.osudroid.ui.v2.hud.elements

import android.graphics.PointF
import com.edlplan.framework.easing.Easing
import com.osudroid.math.toRadians
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.utils.*
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.box
import com.reco1l.andengine.circle
import com.reco1l.andengine.container
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.UIBox
import com.reco1l.framework.Color4
import org.anddev.andengine.engine.camera.Camera
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Shows where circle and slider head judgements land relative to the object's center.
 *
 * Positions are normalized against the direction from the previous object to the current one, so a consistent aim bias
 * shows up as a consistent offset regardless of approach angle.
 */
class HUDAimErrorMeter : HUDElement() {
    private val greatColor = Color4(70, 180, 220)
    private val okColor = Color4(100, 220, 40)
    private val mehColor = Color4(200, 180, 110)
    private val missColor = Color4(255, 9, 9)

    private val expiredIndicators = SynchronizedPool<Indicator>(30)
    private val activeIndicators = mutableListOf<Indicator>()

    // Shared shape reused to draw every "X" hit marker at different positions, colors and alpha.
    private val markerBar = UIBox().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        setSize(HIT_MARKER_SIZE, HIT_MARKER_THICKNESS)
        color = Color4.White

        // Used to get anchor working properly.
        parent = this@HUDAimErrorMeter
    }

    // Still attached as a normal child (so its `moveTo` modifier keeps ticking via the regular update
    // traversal), but manually re-drawn after the hit indicators in `onDrawChildren` - see there for why.
    private val averageMarker = UIContainer().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        setSize(AVERAGE_MARKER_SIZE, AVERAGE_MARKER_SIZE)

        box {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(AVERAGE_MARKER_SIZE, AVERAGE_MARKER_THICKNESS)
            color = Color4.White
        }

        box {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(AVERAGE_MARKER_SIZE, AVERAGE_MARKER_THICKNESS)
            rotation = 90f
            color = Color4.White
        }
    }

    // Scratch fields reused every judgement to avoid Vec2/PointF allocations in this hot path.
    private var hitPositionX = 0f
    private var hitPositionY = 0f

    private var hasAveragePosition = false
    private var averagePositionX = 0f
    private var averagePositionY = 0f

    private var hasLastObjectPosition = false
    private var lastObjectPositionX = 0f
    private var lastObjectPositionY = 0f


    init {
        setSize(SIZE, SIZE)

        circle {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(SIZE * INNER_PORTION, SIZE * INNER_PORTION)
            paintStyle = PaintStyle.Fill
            color = Color4(128, 128, 128)
            alpha = 0.3f
        }

        circle {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(SIZE * INNER_PORTION, SIZE * INNER_PORTION)
            paintStyle = PaintStyle.Outline
            lineWidth = LINE_THICKNESS
            color = Color4.White
        }

        // Cross background
        // Cardinal guides at higher opacity
        +createCrossGuide(0f, LINE_THICKNESS, 0.5f)
        +createCrossGuide(90f, LINE_THICKNESS, 0.5f)
        // Diagonal guides at lower opacity
        +createCrossGuide(45f, LINE_THICKNESS / 2f, 0.2f)
        +createCrossGuide(135f, LINE_THICKNESS / 2f, 0.2f)

        // Arrow background
        // A fixed 45-degree reference axis for the normalized flow direction, with a chevron tip at one end.
        val shaftLength = SIZE * (INNER_PORTION + 0.2f)

        container {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(SIZE, SIZE)
            rotation = 45f

            box {
                anchor = Anchor.Center
                origin = Anchor.Center
                setSize(LINE_THICKNESS, shaftLength)
                color = Color4.White
                alpha = 0.7f
            }

            // Chevron tip
            // Each wing hangs down from the shaft's tip point (its own top-center, used as the rotation
            // pivot) and is splayed outwards by rotating around that point.
            box {
                anchor = Anchor.Center
                origin = Anchor.TopCenter
                setSize(LINE_THICKNESS, ARROW_TIP_LENGTH)
                y = -shaftLength / 2f
                rotation = -45f
                color = Color4.White
                alpha = 0.7f
            }

            box {
                anchor = Anchor.Center
                origin = Anchor.TopCenter
                setSize(LINE_THICKNESS, ARROW_TIP_LENGTH)
                y = -shaftLength / 2f
                rotation = 45f
                color = Color4.White
                alpha = 0.7f
            }
        }

        +averageMarker
    }

    private fun createCrossGuide(rotationDegrees: Float, thickness: Float, guideAlpha: Float) = UIBox().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        setSize(thickness, SIZE * INNER_PORTION * 0.9f)
        rotation = rotationDegrees
        color = Color4.White
        alpha = guideAlpha
    }

    override fun onAimJudgement(objectPosition: PointF, cursorPosition: PointF, objectRadius: Float) {
        if (objectRadius <= 0f) {
            return
        }

        computeHitPosition(objectPosition, cursorPosition, objectRadius)

        val distance = hypot(hitPositionX, hitPositionY)

        val color = when {
            distance >= 0.5f * INNER_PORTION -> missColor
            distance >= 0.35f * INNER_PORTION -> mehColor
            distance >= 0.2f * INNER_PORTION -> okColor
            else -> greatColor
        }

        val indicator = expiredIndicators.acquire() ?: Indicator(0f, 0f, Color4.White, 0f)

        indicator.x = hitPositionX * SIZE
        indicator.y = hitPositionY * SIZE
        indicator.color = color
        indicator.alpha = 1f

        activeIndicators.add(indicator)

        if (hasAveragePosition) {
            averagePositionX = hitPositionX * 0.1f + averagePositionX * 0.9f
            averagePositionY = hitPositionY * 0.1f + averagePositionY * 0.9f
        } else {
            averagePositionX = hitPositionX
            averagePositionY = hitPositionY
            hasAveragePosition = true
        }

        averageMarker.moveTo(
            averagePositionX * SIZE,
            averagePositionY * SIZE,
            AVERAGE_MARKER_MOVE_DURATION,
            Easing.OutQuint
        )

        lastObjectPositionX = objectPosition.x
        lastObjectPositionY = objectPosition.y
        hasLastObjectPosition = true
    }

    /**
     * Rotates the hit offset so that the previous-to-current object flow direction always points along a fixed
     * 45-degree axis, falling back to a raw (unrotated) offset for the very first judgement of a play.
     */
    private fun computeHitPosition(objectPosition: PointF, cursorPosition: PointF, objectRadius: Float) {
        if (hasLastObjectPosition) {
            val angle1 = atan2(objectPosition.y - cursorPosition.y, cursorPosition.x - objectPosition.x)
            val angle2 = atan2(objectPosition.y - lastObjectPositionY, lastObjectPositionX - objectPosition.x)
            val finalAngle = angle2 - angle1

            val normalizedDistance =
                hypot(cursorPosition.x - objectPosition.x, cursorPosition.y - objectPosition.y) / objectRadius

            val rotatedAngle = finalAngle - 45f.toRadians()

            hitPositionX = -normalizedDistance * cos(rotatedAngle) * (INNER_PORTION / 2f)
            hitPositionY = -normalizedDistance * sin(rotatedAngle) * (INNER_PORTION / 2f)
        } else {
            hitPositionX = (cursorPosition.x - objectPosition.x) / objectRadius * (INNER_PORTION / 2f)
            hitPositionY = (cursorPosition.y - objectPosition.y) / objectRadius * (INNER_PORTION / 2f)
        }

        hitPositionX = hitPositionX.coerceIn(-0.5f, 0.5f)
        hitPositionY = hitPositionY.coerceIn(-0.5f, 0.5f)
    }

    override fun onSeek() {
        // Note that this only clears current active indicators. While reconstructing indicators that should still be
        // active is possible, it is not worth the complexity.
        activeIndicators.forEach {
            if (!it.isRecycled) {
                expiredIndicators.release(it)
            }
        }

        activeIndicators.clear()

        hasAveragePosition = false
        hasLastObjectPosition = false

        // Snap back to center immediately rather than animating from the pre-seek position (which is now stale).
        averageMarker.clearEntityModifiers()
        averageMarker.x = 0f
        averageMarker.y = 0f
    }

    //region Indicator update & draw

    override fun onDrawChildren(gl: GL10, camera: Camera) {
        // averageMarker is hidden for this pass and re-drawn manually below (after the hit indicators) so
        // it always renders on top of them.
        averageMarker.isVisible = false
        super.onDrawChildren(gl, camera)
        averageMarker.isVisible = true

        activeIndicators.forEach {
            markerBar.x = it.x
            markerBar.y = it.y
            markerBar.color = it.color
            markerBar.alpha = it.alpha

            markerBar.rotation = -45f
            markerBar.onDraw(gl, camera)

            markerBar.rotation = 45f
            markerBar.onDraw(gl, camera)
        }

        averageMarker.onDraw(gl, camera)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        activeIndicators.forEach(Indicator::update)

        super.onManagedUpdate(deltaTimeSec)
    }

    //endregion

    private inner class Indicator(var x: Float, var y: Float, var color: Color4, var alpha: Float) : IPoolable {
        override var isRecycled = false

        fun update() {
            if (alpha > 0f) {
                alpha -= ALPHA_DECAY
            }

            if (alpha <= 0f) {
                alpha = 0f
                expiredIndicators.release(this)

                updateThread {
                    activeIndicators.remove(this)
                }
            }
        }
    }

    companion object {
        private const val SIZE = 125f
        private const val INNER_PORTION = 0.85f
        private const val LINE_THICKNESS = 2.5f
        private const val ARROW_TIP_LENGTH = 12f

        private const val HIT_MARKER_SIZE = 9f
        private const val HIT_MARKER_THICKNESS = 2f

        private const val AVERAGE_MARKER_SIZE = 15f
        private const val AVERAGE_MARKER_THICKNESS = 3f
        private const val AVERAGE_MARKER_MOVE_DURATION = 0.8f

        // Fades a marker out over ~5 seconds, assuming a 60 FPS update rate.
        private const val ALPHA_DECAY = 1f / 300f
    }
}
