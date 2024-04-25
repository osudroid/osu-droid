package com.deltaflyer.osu

import ru.nsu.ccfit.zuev.osu.Config

object BlockAreaManager {
    private const val TAG = "BlockAreaManager"

    private lateinit var config: BlockAreaConfig
    private lateinit var configString: String

    fun reload(newConfig: String) {
        configString = newConfig
        config = BlockAreaConfig.fromJson(newConfig)
    }

    fun needBlock(x: Float, y: Float): Boolean {
        if (!config.activated) {
            return false
        }

        val relativeX = x / Config.getRES_WIDTH()
        val relativeY = y / Config.getRES_HEIGHT()

        for (rect in config.rects) {
            if (relativeY >= rect.top && relativeY <= rect.bottom && relativeX >= rect.left && relativeX <= rect.right) {
                return true
            }
        }
        return false
    }
}
