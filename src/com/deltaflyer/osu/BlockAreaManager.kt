package com.deltaflyer.osu

import android.util.Log
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameScene

object BlockAreaManager {
    private const val TAG = "BlockAreaManager"

    private lateinit var config: BlockAreaConfig
    private lateinit var configString: String

    private val cursorIsIgnored = BooleanArray(GameScene.CursorCount) { false }

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
            if (isInBlockArea(event.x, event.y)) {
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

    private fun isInBlockArea(x: Float, y: Float): Boolean {
        val relativeX = x / Config.getRES_WIDTH()
        val relativeY = y / Config.getRES_HEIGHT()

        for (rect in config.rects) {
            if (relativeY >= rect.top && relativeY <= rect.bottom && relativeX >= rect.left && relativeX <= rect.right) {
                return true
            }
        }
        return false
    }

    private fun verbose(event: TouchEvent) {
        var eventType = "down"
        if (event.isActionMove) {
            return
        }
        if (event.isActionUp) {
            eventType = "up"
        }
        Log.d(
            "TouchTest",
            "eventType:$eventType x:${event.x} y:${event.y} id:${event.pointerID} block:${
                needBlock(event)
            } inSection: ${isInBlockArea(event.x, event.y)}"
        );
    }
}
