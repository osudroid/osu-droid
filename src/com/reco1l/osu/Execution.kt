@file:JvmName("Execution")

package com.reco1l.osu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import ru.nsu.ccfit.zuev.osu.GlobalManager

/**
 * A [Runnable] intended specifically for Java interoperability with Kotlin coroutines.
 */
interface CoroutineRunnable {
    operator fun invoke(scope: CoroutineScope)
}

// Lots of overloads here, but they are necessary for Java interoperability...

/**
 * @see [com.reco1l.toolkt.kotlin.async]
 */
fun async(block: Runnable) = com.reco1l.toolkt.kotlin.async { block.run() }

/**
 * @see [com.reco1l.toolkt.kotlin.async]
 */
fun async(block: CoroutineScope.() -> Unit) = com.reco1l.toolkt.kotlin.async { block() }

/**
 * @see [com.reco1l.toolkt.kotlin.async]
 */
fun async(block: CoroutineRunnable) = com.reco1l.toolkt.kotlin.async { block(this) }

/**
 * @see [com.reco1l.toolkt.kotlin.runSafe]
 */
fun runSafe(block: Runnable) = com.reco1l.toolkt.kotlin.runSafe { block.run() }

/**
 * @see [com.reco1l.toolkt.kotlin.delayed]
 */
fun delayed(time: Long, block: Runnable) = com.reco1l.toolkt.kotlin.delayed(time) { block.run() }

/**
 * @see [com.reco1l.toolkt.kotlin.delayed]
 */
fun delayed(time: Long, block: CoroutineScope.() -> Unit) = com.reco1l.toolkt.kotlin.delayed(time) { block() }

/**
 * @see [com.reco1l.toolkt.kotlin.delayed]
 */
fun delayed(time: Long, block: CoroutineRunnable) = com.reco1l.toolkt.kotlin.delayed(time) { block(this) }


fun mainThread(block: Runnable) = GlobalManager.getInstance().mainActivity.runOnUiThread(block)

fun updateThread(block: Runnable) = GlobalManager.getInstance().engine.runOnUpdateThread(block)

