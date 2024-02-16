package com.reco1l.legacy.ui.entity

import org.andengine.entity.scene.Scene
import org.andengine.entity.shape.RectangularShape
import org.andengine.entity.shape.Shape
import org.andengine.input.touch.TouchEvent
import org.andengine.input.touch.detector.ScrollDetector
import org.andengine.input.touch.detector.ScrollDetector.IScrollDetectorListener
import org.andengine.input.touch.detector.SurfaceScrollDetector
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Utils
import kotlin.math.abs
import kotlin.math.sign

abstract class ScrollableList : Scene(), IScrollDetectorListener
{

    var isScroll = false
        private set

    private val scrollDetector: SurfaceScrollDetector

    private var percentShow = -1f
    private var maxY = 100500f
    private var pointerId = -1
    private var initialY = -1f
    private var touchY: Float? = null
    private var secPassed = 0f
    private var tapTime = 0f

    protected var velocityY = 0f
    protected var camY = -146f
    protected var itemHeight = 0f

    init
    {
        isBackgroundEnabled = false
        scrollDetector = SurfaceScrollDetector(this)
    }

    fun handleScrolling(event: TouchEvent)
    {
        scrollDetector.onTouchEvent(event)
        scrollDetector.isEnabled = true
    }

    override fun onManagedUpdate(secondsElapsed: Float)
    {
        super.onManagedUpdate(secondsElapsed)

        secPassed += secondsElapsed

        if (childCount == 0)
            return

        if (percentShow == -1f)
        {
            var y = -camY

            for (i in 0 until childCount)
            {
                val sprite = getChildByIndex(i) as? RectangularShape ?: continue

                sprite.setPosition(sprite.x, y)
                y += sprite.height + 5f
            }

            y += camY
            camY += velocityY * secondsElapsed
            maxY = y - 0.8f * (Config.getRES_HEIGHT() - 110 - (itemHeight - 32))

            if (camY <= -146 && velocityY < 0 || camY > maxY && velocityY > 0)
            {
                camY -= velocityY * secondsElapsed
                velocityY = 0f
                isScroll = false
            }

            if (abs(velocityY) <= Utils.toRes(500) * secondsElapsed)
            {
                velocityY = 0f
                isScroll = false
            }
            else velocityY -= Utils.toRes(10) * secondsElapsed * sign(velocityY)
        }
        else
        {
            percentShow += secondsElapsed * 4

            if (percentShow > 1)
                percentShow = 1f

            for (i in 0 until childCount)
            {
                val sprite = getChildByIndex(i) as? RectangularShape ?: continue
                sprite.setPosition(-160f, 146 + 0.8f * percentShow * i * (sprite.height + 5f))
            }

            if (percentShow == 1f)
                percentShow = -1f
        }
    }

    override fun onScrollStarted(pScollDetector: ScrollDetector?, pPointerID: Int, pDistanceX: Float, pDistanceY: Float) {
        velocityY = 0f
        touchY = pDistanceY
        pointerId = pPointerID
        tapTime = secPassed
        initialY = pDistanceY
        isScroll = true
    }

    override fun onScroll(scrollDetector: ScrollDetector, pPointerID: Int, distanceX: Float, distanceY: Float)
    {
        if (pointerId == -1 || pointerId == pPointerID)
        {
            isScroll = true

            if (initialY == -1f)
                onScrollStarted(scrollDetector, pPointerID, distanceX, distanceY)

            val dy = distanceY - touchY!!
            camY -= dy
            touchY = distanceY

            if (camY <= -146)
            {
                camY = -146f
                velocityY = 0f
            }
            else if (camY >= maxY)
            {
                camY = maxY
                velocityY = 0f
            }
        }
    }

    override fun onScrollFinished(pScollDetector: ScrollDetector?, pPointerID: Int, pDistanceX: Float, pDistanceY: Float) {
        if (pointerId == -1 || pointerId == pPointerID)
        {
            touchY = null

            if (secPassed - tapTime < 0.001f || initialY == -1f)
            {
                velocityY = 0f
                isScroll = false
            }
            else
            {
                velocityY = (initialY - pDistanceY) / (secPassed - tapTime)
                isScroll = true
            }
            pointerId = -1
            initialY = -1f
        }
    }
}
