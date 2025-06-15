@file:JvmName("Execution")

package com.osudroid.utils

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.future.future
import ru.nsu.ccfit.zuev.osu.GlobalManager
import com.reco1l.toolkt.kotlin.async as toolktAsync
import com.reco1l.toolkt.kotlin.delayed as toolktDelayed
import com.reco1l.toolkt.kotlin.runSafe as toolktRunSafe

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
fun async(block: Runnable) = toolktAsync { block.run() }

/**
 * Run a task on asynchronous using global scope.
 */
fun async(block: CoroutineScope.() -> Unit) = toolktAsync(block)

/**
 * Run a task on asynchronous using global scope.
 */
fun async(block: CoroutineRunnable) = toolktAsync { block(this) }


/**
 * Run a block of code ignoring any exceptions.
 */
fun runSafe(block: Runnable) = toolktRunSafe { block.run() }


/**
 * Run a delayed task on asynchronous using global scope.
 */
fun delayed(time: Long, block: Runnable) = toolktDelayed(time) { block.run() }

/**
 * Run a delayed task on asynchronous using global scope.
 */
fun delayed(time: Long, block: CoroutineScope.() -> Unit) = toolktDelayed(time) { block() }

/**
 * Run a delayed task on asynchronous using global scope.
 */
fun delayed(time: Long, block: CoroutineRunnable) = toolktDelayed(time) { block(this) }


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
