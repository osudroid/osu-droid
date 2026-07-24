@file:JvmName("Execution")

package com.osudroid.utils

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
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

/**
 * Scope used for fire-and-forget coroutines launched through [async] and [delayed].
 */
private val executionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

// Lots of overloads here, but they are necessary for Java interoperability...

/**
 * Run a task on asynchronous using [executionScope].
 */
fun async(block: Runnable) = executionScope.launch { block.run() }

/**
 * Run a task on asynchronous using [executionScope].
 */
fun async(block: CoroutineScope.() -> Unit) = executionScope.launch(block = block)

/**
 * Run a task on asynchronous using [executionScope].
 */
fun async(block: CoroutineRunnable) = executionScope.launch { block(this) }


/**
 * Run a block of code ignoring any exceptions.
 */
fun runSafe(block: Runnable) = try {
    block.run()
} catch (e: Exception) {
    e.printStackTrace()
}


/**
 * Run a delayed task on asynchronous using [executionScope].
 */
fun delayed(time: Long, block: Runnable) = executionScope.launch {
    delay(time)
    block.run()
}

/**
 * Run a delayed task on asynchronous using [executionScope].
 */
fun delayed(time: Long, block: CoroutineScope.() -> Unit) = executionScope.launch {
    delay(time)
    block()
}

/**
 * Run a delayed task on asynchronous using [executionScope].
 */
fun delayed(time: Long, block: CoroutineRunnable) = executionScope.launch {
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
