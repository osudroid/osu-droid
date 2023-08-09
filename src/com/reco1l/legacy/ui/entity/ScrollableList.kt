package com.reco1l.legacy.ui.entity

import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.input.touch.detector.ScrollDetector
import org.anddev.andengine.input.touch.detector.ScrollDetector.IScrollDetectorListener
import org.anddev.andengine.input.touch.detector.SurfaceScrollDetector
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
    private var velocityY = 0f
    private var secPassed = 0f
    private var tapTime = 0f

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

    override fun onManagedUpdate(pSecondsElapsed: Float)
    {
        super.onManagedUpdate(pSecondsElapsed)

        secPassed += pSecondsElapsed

        if (childCount == 0)
            return

        if (percentShow == -1f)
        {
            var y = -camY

            for (i in 0 until childCount)
            {
                val sprite = getChild(i) as? Shape ?: continue

                sprite.setPosition(sprite.x, y)
                y += sprite.height + 5f
            }

            y += camY
            camY += velocityY * pSecondsElapsed
            maxY = y - 0.8f * (Config.getRES_HEIGHT() - 110 - (itemHeight - 32))
            if (camY <= -146 && velocityY < 0 || camY > maxY && velocityY > 0)
            {
                camY -= velocityY * pSecondsElapsed
                velocityY = 0f
                isScroll = false
            }
            if (abs(velocityY) > Utils.toRes(500) * pSecondsElapsed)
            {
                velocityY -= Utils.toRes(10) * pSecondsElapsed * sign(velocityY)
            }
            else
            {
                velocityY = 0f
                isScroll = false
            }
        }
        else
        {
            percentShow += pSecondsElapsed * 4
            if (percentShow > 1)
            {
                percentShow = 1f
            }

            for (i in 0 until childCount)
            {
                val sprite = getChild(i) as? Shape ?: continue
                sprite.setPosition(-160f, 146 + 0.8f * percentShow * i * (sprite.height + 5f))
            }

            if (percentShow == 1f)
            {
                percentShow = -1f
            }
        }
    }

    override fun onScroll(pScrollDetector: ScrollDetector?, pTouchEvent: TouchEvent, pDistanceX: Float, pDistanceY: Float)
    {
        when (pTouchEvent.action)
        {
            TouchEvent.ACTION_DOWN ->
            {
                velocityY = 0f
                touchY = pTouchEvent.y
                pointerId = pTouchEvent.pointerID
                tapTime = secPassed
                initialY = touchY!!
                isScroll = true
            }

            TouchEvent.ACTION_MOVE -> if (pointerId == -1 || pointerId == pTouchEvent.pointerID)
            {
                isScroll = true
                if (initialY == -1f)
                {
                    velocityY = 0f
                    touchY = pTouchEvent.y
                    pointerId = pTouchEvent.pointerID
                    tapTime = secPassed
                    initialY = touchY!!
                }
                val dy = pTouchEvent.y - touchY!!
                camY -= dy
                touchY = pTouchEvent.y
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

            else -> if (pointerId == -1 || pointerId == pTouchEvent.pointerID)
            {
                touchY = null
                if (secPassed - tapTime < 0.001f || initialY == -1f)
                {
                    velocityY = 0f
                    isScroll = false
                }
                else
                {
                    velocityY = (initialY - pTouchEvent.y) / (secPassed - tapTime)
                    isScroll = true
                }
                pointerId = -1
                initialY = -1f
            }
        }
    }
}
