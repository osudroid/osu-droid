package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.MultiplayerClientEvent
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The amount of time, in milliseconds, to wait for the server to acknowledge an event before giving up on it.
 */
const val ACK_TIMEOUT_MS = 5_000L

/**
 * The reason a [MultiplayerClientEvent] sent with [MultiplayerOutbox.sendOnce] was not delivered.
 */
enum class MultiplayerUndeliveredReason {
    /**
     * The session was not established when the event was sent, so it was never put on the wire. This includes the
     * transport refusing the event synchronously while it was being sent.
     */
    NotSent,

    /**
     * The event was sent, but the connection dropped before an acknowledgement arrived.
     */
    ConnectionLost,

    /**
     * The event was sent and the connection stayed up, but no acknowledgement arrived in time.
     */
    NoResponse
}

/**
 * The server's response to an event.
 *
 * @param ok Whether the server accepted the event.
 * @param reason The reason the server rejected the event, if any.
 */
data class MultiplayerAckResult(val ok: Boolean, val reason: String?)

/**
 * The socket the [MultiplayerOutbox] sends through.
 */
interface MultiplayerOutboxTransport {
    /**
     * Whether the session is established, that is, `initialConnection` has been received on the current socket.
     */
    val isReady: Boolean

    /**
     * Emits an event.
     *
     * @param event The event to send.
     * @param args The arguments of the event.
     * @param timeoutMs The amount of time, in milliseconds, to wait for an acknowledgement.
     * @param onAck Called with the server's response, or with `null` if no response arrived within [timeoutMs]. May be
     * called from any thread.
     */
    fun emit(event: MultiplayerClientEvent, args: Array<Any?>, timeoutMs: Long, onAck: OnMultiplayerEventAck)
}

/**
 * Callback for the server's acknowledgement of a [MultiplayerClientEvent] sent by the client.
 */
fun interface OnMultiplayerEventAck {
    /**
     * Called with the server's acknowledgement of a [MultiplayerClientEvent], or with `null` if none arrived.
     *
     * @param result The acknowledgement, or `null` if the [MultiplayerClientEvent] was not sent or the server did not
     * respond in time.
     */
    operator fun invoke(result: MultiplayerAckResult?)
}

/**
 * Sends events to the server and keeps track of their delivery across reconnections.
 *
 * This class is thread-safe.
 *
 * @property transport The transport to send events through.
 * @property ackTimeoutMs The amount of time, in milliseconds, to wait for an acknowledgement.
 */
class MultiplayerOutbox(
    private val transport: MultiplayerOutboxTransport,
    private val ackTimeoutMs: Long = ACK_TIMEOUT_MS
) {
    private class RetriedItem(val event: MultiplayerClientEvent, val roundId: Int?, val args: Array<Any?>) {
        /**
         * The attempt that is currently in flight. Callbacks from other attempts are stale.
         */
        var attempt = 0

        /**
         * Whether the current [attempt] is on the wire, as opposed to waiting to be (re)sent.
         */
        var inFlight = false
    }

    private class OnceItem(val onNotDelivered: OnEventNotDelivered)

    /**
     * An emission decided under the lock and performed outside of it.
     */
    private class Emission(val item: RetriedItem, val attempt: Int, val args: Array<Any?>)

    private val lock = Any()

    // Insertion-ordered, so that flushing preserves enqueue order.
    private val retried = mutableListOf<RetriedItem>()
    private val onceItems = mutableListOf<OnceItem>()

    /**
     * [MultiplayerClientEvent]s sent with [sendUntilAcknowledged] that were not acknowledged yet, in enqueue order.
     */
    val unacknowledged: List<MultiplayerClientEvent>
        get() = synchronized(lock) { retried.map { it.event } }

    /**
     * Sends a [MultiplayerClientEvent] that must be acknowledged by the server. It is resent until the server
     * acknowledges it, including after a reconnection, unless [pruneStale] drops it.
     *
     * The [MultiplayerClientEvent] is held until [flush] is called if the session is not established or earlier
     * [MultiplayerClientEvent]s are still waiting to be sent, so that [MultiplayerClientEvent]s always go out in the
     * order they were queued.
     *
     * @param event The [MultiplayerClientEvent] to send.
     * @param roundId The ID of the round [event] belongs to. Appended to [args] if [event] is
     * [round-scoped][MultiplayerClientEvent.isRoundScoped], unless it is `null`. Ignored otherwise.
     * @param args The arguments of [event].
     */
    fun sendUntilAcknowledged(event: MultiplayerClientEvent, roundId: Int?, args: Array<Any?>) {
        var emission: Emission? = null

        synchronized(lock) {
            val item = RetriedItem(event, roundId, args)
            val canEmit = transport.isReady && retried.none { !it.inFlight }

            retried.add(item)

            if (canEmit) {
                emission = beginAttempt(item)
            }
        }

        emission?.let(::perform)
    }

    /**
     * Sends a [MultiplayerClientEvent] once.
     *
     * @param event The [MultiplayerClientEvent] to send.
     * @param args The arguments of [event].
     * @param onNotDelivered The callback that is called at most once if the server did not acknowledge [event]. Its
     * argument tells why.
     */
    fun sendOnce(event: MultiplayerClientEvent, args: Array<Any?>, onNotDelivered: OnEventNotDelivered) {
        val item = OnceItem(onNotDelivered)
        var ready = false

        synchronized(lock) {
            ready = transport.isReady

            if (ready) {
                onceItems.add(item)
            }
        }

        if (!ready) {
            // Emitting now could make the socket buffer the event and deliver it after a reconnection, long after the
            // user's action stopped being relevant.
            onNotDelivered(MultiplayerUndeliveredReason.NotSent)
            return
        }

        // A refusal is reported synchronously, on this thread, before emit returns. A missing acknowledgement can also
        // arrive on another thread while emit is still running (e.g. a closing socket clearing its pending
        // acknowledgements), and that event was sent, so both the thread and the timing are checked.
        val emittingThread = Thread.currentThread()
        val emitting = AtomicBoolean(true)

        try {
            transport.emit(event, args, ackTimeoutMs) { result ->
                val failed = synchronized(lock) { onceItems.remove(item) }

                // A null result means no acknowledgement arrived. Any other result is a response from the server, and
                // whether the server accepted the event is up to the caller to learn through other means.
                if (failed && result == null) {
                    val reason = when {
                        // The transport refused the event before it went out.
                        emitting.get() && Thread.currentThread() === emittingThread -> MultiplayerUndeliveredReason.NotSent
                        transport.isReady -> MultiplayerUndeliveredReason.NoResponse
                        // A socket that is going away abandoned the emission.
                        else -> MultiplayerUndeliveredReason.ConnectionLost
                    }

                    item.onNotDelivered(reason)
                }
            }
        } finally {
            emitting.set(false)
        }
    }

    /**
     * Notifies this [MultiplayerOutbox] that the socket was disconnected. Queued [MultiplayerClientEvent]s from
     * [sendUntilAcknowledged] are sent again on the next [flush], and queued [MultiplayerClientEvent]s from
     * [sendOnce] are reported as not delivered.
     */
    fun onDisconnected() {
        val undelivered: List<OnceItem>

        synchronized(lock) {
            for (item in retried) {
                // Invalidate any callback of the attempt on the dead socket.
                item.attempt++
                item.inFlight = false
            }

            undelivered = onceItems.toList()
            onceItems.clear()
        }

        for (item in undelivered) {
            item.onNotDelivered(MultiplayerUndeliveredReason.ConnectionLost)
        }
    }

    /**
     * Drops [round-scoped][MultiplayerClientEvent.isRoundScoped] [MultiplayerClientEvent]s from [sendUntilAcknowledged] if
     * they belong to another round that is not [activeRoundId], or if the server has received them.
     *
     * @param activeRoundId The ID of the current round, or `null` if there is none. Specifying `null` will drop all
     * round-scoped [MultiplayerClientEvent]s regardless of round.
     * @param alreadyReceived The round-scoped [MultiplayerClientEvent]s the server is known to have received already for
     * the current round.
     */
    fun pruneStale(activeRoundId: Int?, alreadyReceived: Set<MultiplayerClientEvent>) {
        synchronized(lock) {
            retried.removeAll { item ->
                item.event.isRoundScoped && (
                    activeRoundId == null || item.roundId != activeRoundId || item.event in alreadyReceived
                )
            }
        }
    }

    /**
     * Sends queued [MultiplayerClientEvent]s from [sendUntilAcknowledged] that are not awaiting an acknowledgement, in the
     * order they were queued. This does nothing if the session is not established.
     */
    fun flush() {
        val emissions = synchronized(lock) {
            if (!transport.isReady) {
                return
            }

            retried.filter { !it.inFlight }.map(::beginAttempt)
        }

        emissions.forEach(::perform)
    }

    /**
     * Drops every queued [MultiplayerClientEvent].
     */
    fun clear() {
        synchronized(lock) {
            retried.clear()
            onceItems.clear()
        }
    }

    /**
     * Starts a new attempt for [item]. Must be called with the lock held.
     */
    private fun beginAttempt(item: RetriedItem): Emission {
        item.attempt++
        item.inFlight = true

        val args = if (item.event.isRoundScoped && item.roundId != null) item.args + item.roundId else item.args

        return Emission(item, item.attempt, args)
    }

    /**
     * Emits an [Emission]. Must be called without the lock held.
     */
    private fun perform(emission: Emission) {
        transport.emit(emission.item.event, emission.args, ackTimeoutMs) { result ->
            onRetriedAck(emission.item, emission.attempt, result)
        }
    }

    private fun onRetriedAck(item: RetriedItem, attempt: Int, result: MultiplayerAckResult?) {
        var resend: Emission? = null

        synchronized(lock) {
            // Stale callbacks come from a superseded attempt, or from an item that was acknowledged, pruned or cleared.
            if (item.attempt != attempt || !retried.contains(item)) {
                return
            }

            if (result != null) {
                // Accepted or rejected, the server has seen the event. Resending it would not change the outcome.
                retried.remove(item)
                return
            }

            // Timed out.
            if (transport.isReady) {
                resend = beginAttempt(item)
            } else {
                // Held until the next flush.
                item.attempt++
                item.inFlight = false
            }
        }

        resend?.let(::perform)
    }

    /**
     * Callback function to be invoked when a [MultiplayerClientEvent] is not delivered.
     */
    fun interface OnEventNotDelivered {
        /**
         * Called when the event was not delivered. This callback may call back into this [MultiplayerOutbox].
         *
         * @param reason Why the event was not delivered.
         */
        operator fun invoke(reason: MultiplayerUndeliveredReason)
    }
}
