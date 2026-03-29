@file:JvmName("Execution")
@file:Suppress("OPT_IN_USAGE")

package com.osudroid.utils

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.GlobalManager

/**
 * A [Runnable] intended specifically for Java interoperability with Kotlin coroutines.
 */
interface CoroutineRunnable {
    operator fun invoke(scope: CoroutineScope)
}

// Lots of overloads here, but they are necessary for Java interoperability...

/**
 * Run a task on asynchronous using global scope.
 */
fun async(block: Runnable) = GlobalScope.launch { block.run() }

/**
 * Run a task on asynchronous using global scope.
 */
fun async(block: CoroutineScope.() -> Unit) = GlobalScope.launch(block = block)

/**
 * Run a task on asynchronous using global scope.
 */
fun async(block: CoroutineRunnable) = GlobalScope.launch { block(this) }


/**
 * Run a block of code ignoring any exceptions.
 */
fun runSafe(block: Runnable) = try {
    block.run()
} catch (e: Exception) {
    e.printStackTrace()
}


/**
 * Run a delayed task on asynchronous using global scope.
 */
fun delayed(time: Long, block: Runnable) = GlobalScope.launch {
    delay(time)
    block.run()
}

/**
 * Run a delayed task on asynchronous using global scope.
 */
fun delayed(time: Long, block: CoroutineScope.() -> Unit) = GlobalScope.launch {
    delay(time)
    block()
}

/**
 * Run a delayed task on asynchronous using global scope.
 */
fun delayed(time: Long, block: CoroutineRunnable) = GlobalScope.launch {
    delay(time)
    block(this)
}

/**
 * Run a task on the main thread.
 */
fun mainThread(block: Runnable) = GlobalManager.getInstance().mainActivity.runOnUiThread(block)

/**
 * Run a task on the update thread.
 */
fun updateThread(block: Runnable) = GlobalManager.getInstance().engine.runOnUpdateThread(block)

/**
 * Wraps a [Job.cancelAndJoin] call inside a coroutine. Useful for asynchronously canceling and joining a [Job] with
 * Java interoperability in mind.
 *
 * Returns a [CompletableFuture] that completes when the job is canceled and joined.
 */
fun Job?.stopAsync(): CompletableFuture<Unit> =
    if (this?.isCompleted == false) CoroutineScope(EmptyCoroutineContext).future { this@stopAsync.cancelAndJoin() }
    else CompletableFuture.completedFuture(Unit)
