/*
 * @author Reco1l
 */

@file:OptIn(DelicateCoroutinesApi::class)
@file:JvmName("Execution")

package com.reco1l.framework.lang

import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.GlobalManager

/**
 * Run a task on asynchronous using Kotlin Coroutines API.
 */
fun async(block: () -> Unit) = GlobalScope.launch {
    block()
}

/**
 * Run a delayed task on asynchronous using Kotlin Coroutines API.
 */
fun delayed(time: Long, block: () -> Unit) = GlobalScope.launch {
    delay(time)
    block()
}

// Exclusive osu!droid

fun uiThread(block: () -> Unit) = GlobalManager.getInstance().mainActivity.runOnUiThread(block)

fun glThread(block: () -> Unit) = GlobalManager.getInstance().engine.runOnUpdateThread(block)

