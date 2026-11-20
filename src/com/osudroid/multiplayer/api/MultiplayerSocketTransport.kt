package com.osudroid.multiplayer.api

import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.session.MultiplayerAckResult
import com.osudroid.multiplayer.session.MultiplayerOutbox
import com.osudroid.multiplayer.session.MultiplayerOutboxTransport
import com.osudroid.multiplayer.session.OnMultiplayerEventAck
import io.socket.client.AckWithTimeout
import io.socket.client.Socket
import org.json.JSONObject

/**
 * A [MultiplayerOutboxTransport] that sends [MultiplayerClientEvent]s through the current multiplayer [Socket].
 *
 * [MultiplayerClientEvent]s are only sent while the session is established on the current [Socket] and that [Socket]
 * is connected. Otherwise, [emit] reports that no acknowledgement arrived without sending anything, so that the outbox
 * holds the [MultiplayerClientEvent] until the next connection instead of letting the [Socket] buffer it and deliver it late.
 *
 * @property socketProvider Provides the current [Socket], or `null` if there is none.
 */
internal class MultiplayerSocketTransport(private val socketProvider: MultiplayerSocketProvider) : MultiplayerOutboxTransport {
    /**
     * The [Socket] `initialConnection` was received and processed on, or `null` if there is none. A session
     * established on a socket that was since replaced does not make the current socket ready.
     */
    @Volatile
    var establishedSocket: Socket? = null

    override val isReady: Boolean
        get() = isReady(socketProvider())

    private fun isReady(socket: Socket?) = socket != null && socket === establishedSocket && socket.connected()

    override fun emit(
        event: MultiplayerClientEvent,
        args: Array<Any?>,
        timeoutMs: Long,
        onAck: OnMultiplayerEventAck,
    ) {
        val socket = socketProvider()

        // The outbox treats a null acknowledgement as "not sent" and re-reads isReady, so this must refuse exactly when
        // isReady is false. Refusing in any other case would make the outbox resend into the refusal without end.
        if (socket == null || !isReady(socket)) {
            Multiplayer.log("NOT EMITTED: ${event.eventName} (session not established)")
            onAck(null)
            return
        }

        // Only log event name. Arguments may contain sensitive information (e.g. chat content).
        Multiplayer.log("EMITTED: ${event.eventName}")

        socket.emit(event.eventName, args, object : AckWithTimeout(timeoutMs) {
            override fun onSuccess(vararg args: Any?) {
                val result = parseMultiplayerAckResult(args)

                if (!result.ok) {
                    Multiplayer.log("REJECTED: ${event.eventName} -> ${result.reason}")
                }

                onAck(result)
            }

            override fun onTimeout() {
                // The socket also calls this when it clears its pending acknowledgements, without cancelling the timer,
                // which would otherwise fire again later.
                cancelTimer()
                Multiplayer.log("NO ACKNOWLEDGEMENT: ${event.eventName}")
                onAck(null)
            }
        })
    }
}

/**
 * Provides the [Socket] a [MultiplayerSocketTransport] sends through.
 */
internal fun interface MultiplayerSocketProvider {
    /**
     * Gets the current [Socket], or `null` if there is none.
     */
    operator fun invoke(): Socket?
}

/**
 * Parses the acknowledgement the server sends for a [MultiplayerClientEvent].
 *
 * A malformed acknowledgement is a protocol violation, but the server did respond, so it is reported as a rejection
 * rather than as a missing acknowledgement, which would make [MultiplayerOutbox] resend the [MultiplayerClientEvent].
 *
 * @param args The arguments of the acknowledgement.
 */
internal fun parseMultiplayerAckResult(args: Array<out Any?>): MultiplayerAckResult =
    try {
        val json =
            args.getOrNull(0) as? JSONObject ?: throw IllegalArgumentException("Acknowledgement is not an object")

        MultiplayerAckResult(json.getBoolean("ok"), if (json.isNull("reason")) null else json.getString("reason"))
    } catch (_: Exception) {
        Multiplayer.log("WARNING: Malformed acknowledgement -> ${args.contentToString()}")

        MultiplayerAckResult(false, "Malformed acknowledgement")
    }
