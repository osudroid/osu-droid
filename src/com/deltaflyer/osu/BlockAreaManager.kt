package com.deltaflyer.osu

import android.content.Context
import ru.nsu.ccfit.zuev.osu.Config

object BlockAreaManager {
    private const val PREFS_NAME = "blockAreaPreferences"
    private const val KEY_BLOCK_AREA_CONFIG = "blockAreaConfig"

    private lateinit var config: BlockAreaConfig

    fun init(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val configString = sharedPreferences.getString(KEY_BLOCK_AREA_CONFIG, "null")
        config = BlockAreaConfig.fromJson(configString)
    }

    fun reload(newConfig: String) {
        config = BlockAreaConfig.fromJson(newConfig)
    }

    fun needBlock(x: Int, y: Int): Boolean {
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
