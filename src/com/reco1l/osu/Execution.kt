@file:JvmName("Execution")

package com.reco1l.osu

import kotlinx.coroutines.Runnable
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal


/**
 * @see [com.reco1l.toolkt.kotlin.async]
 */
fun async(block: Runnable) = com.reco1l.toolkt.kotlin.async { block.run() }

/**
 * @see [com.reco1l.toolkt.kotlin.runSafe]
 */
fun runSafe(block: Runnable) = com.reco1l.toolkt.kotlin.runSafe { block.run() }

/**
 * @see [com.reco1l.toolkt.kotlin.delayed]
 */
fun delayed(time: Long, block: Runnable) = com.reco1l.toolkt.kotlin.delayed(time) { block.run() }


fun mainThread(block: Runnable) = getGlobal().mainActivity.runOnUiThread(block)

fun updateThread(block: Runnable) = getGlobal().engine.runOnUpdateThread(block)

