package com.deltaflyer.osu

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.game.GameScene
import kotlin.math.max
import kotlin.math.min


object BlockAreaManager {
    private const val TAG = "BlockAreaManager"

    private lateinit var config: BlockAreaConfig
    private lateinit var configString: String
    var screenHeight = 0
    var screenWidth = 0

    private val cursorIsIgnored = BooleanArray(GameScene.CursorCount) { false }

    fun setScreenSize(context: Context) {
        val dm = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(dm)
        screenWidth =
            max(dm.widthPixels.toDouble(), dm.heightPixels.toDouble()).toInt()
        screenHeight = min(dm.widthPixels.toDouble(), dm.heightPixels.toDouble()).toInt()
        Log.d(TAG, "setScreenSize: $screenWidth/$screenHeight")
    }

    fun reload(newConfig: String) {
        Log.d(TAG, "reload: $newConfig")
        configString = newConfig
        config = BlockAreaConfig.fromJson(newConfig)
    }

    fun reset() {
        cursorIsIgnored.fill(false)
    }

    fun applyBlock(event: TouchEvent) {
        cursorIsIgnored[event.pointerID] = when {
            event.isActionDown -> true
            event.isActionUp -> false
            else -> cursorIsIgnored[event.pointerID]
        }
    }

    fun needBlock(event: TouchEvent, verbose: Boolean = false): Boolean {
        if (verbose) {
            verbose(event)
        }
        if (!config.activated) {
            return false
        }
        if (event.isActionDown) {
            if (isInBlockArea(event.motionEvent)) {
                return true
            }
        } else if (event.isActionMove) {
            if (cursorIsIgnored[event.pointerID]) {
                return true
            }
        } else if (event.isActionUp) {
            if (cursorIsIgnored[event.pointerID]) {
                return true
            }
        }
        return false
    }

    private fun isInBlockArea(event: MotionEvent): Boolean {
        val relativeX = event.x / screenWidth
        val relativeY = event.y / screenHeight

        for (rect in config.rects) {
            if (relativeY >= rect.top && relativeY <= rect.bottom
                && relativeX >= rect.left && relativeX <= rect.right) {
                return true
            }
        }
        return false
    }

    private fun verbose(event: TouchEvent) {
        val x = event.motionEvent.x
        val y = event.motionEvent.y
        val relativeX = x / screenWidth
        val relativeY = y / screenHeight
        var eventType = "down"
        if (event.isActionMove) {
            return
        }
        if (event.isActionUp) {
            eventType = "up"
        }
        Log.d(
            "TouchTest",
            "eventType:$eventType x:${x}/${relativeX} y:${y}/$relativeY id:${event.pointerID} block:${
                needBlock(event)
            } inSection: ${isInBlockArea(event.motionEvent)}"
        );
    }
}
