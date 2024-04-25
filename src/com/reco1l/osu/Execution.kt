/*
 * @author Reco1l
 */

@file:OptIn(DelicateCoroutinesApi::class)
@file:JvmName("Execution")

package com.reco1l.osu

import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

/**
 * Run a task on asynchronous using Kotlin Coroutines API.
 */
fun async(block: Runnable) = GlobalScope.launch {
    block.run()
}

/**
 * Run a task ignoring exceptions on asynchronous using Kotlin Coroutines API.
 */
fun asyncIgnoreExceptions(block: Runnable) = GlobalScope.launch {
    try { block.run() } catch (e: Exception) { e.printStackTrace() }
}

/**
 * Run a delayed task on asynchronous using Kotlin Coroutines API.
 */
fun delayed(time: Long, block: Runnable) = GlobalScope.launch {
    delay(time)
    block.run()
}


// Exclusive osu!droid

fun mainThread(block: Runnable) = getGlobal().mainActivity.runOnUiThread(block)

fun updateThread(block: Runnable) = getGlobal().engine.runOnUpdateThread(block)

