/*
 * @author Reco1l
 */

@file:OptIn(DelicateCoroutinesApi::class)
@file:JvmName("Execution")

package com.reco1l.framework.lang

import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

/**
 * Run a task on asynchronous using Kotlin Coroutines API.
 */
fun async(block: () -> Unit) = GlobalScope.launch {
    block()
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
fun delayed(time: Long, block: () -> Unit) = GlobalScope.launch {
    delay(time)
    block()
}

// Exclusive osu!droid

fun uiThread(block: Runnable) = getGlobal().mainActivity.runOnUiThread(block)

fun glThread(block: Runnable) = getGlobal().engine.runOnUpdateThread(block)

