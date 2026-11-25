package ru.nsu.ccfit.zuev.osu.online

import java.io.BufferedReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PostBuilderTest {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(200, TimeUnit.MILLISECONDS)
        .build()

    @Test
    fun `Test response lines are returned`() = runTest(timeout = 5.seconds) {
        createServer().use { server ->
            acceptOne(server) { socket, _ ->
                val body = "SUCCESS\nsome data"

                socket.getOutputStream().write(
                    "HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body".toByteArray()
                )
            }

            val response = createPost().request(client, "http://127.0.0.1:${server.localPort}/")

            Assert.assertEquals(listOf("SUCCESS", "some data"), response)
        }
    }

    @Test
    fun `Test refused connection is not sent`() {
        val port = createServer().use { it.localPort }

        Assert.assertTrue(requestFailure(client, "http://127.0.0.1:$port/").wasNotSent())
    }

    @Test
    fun `Test unresolvable host is not sent`() {
        val client = client.newBuilder().dns(object : Dns {
            override fun lookup(hostname: String) = throw UnknownHostException(hostname)
        }).build()

        Assert.assertTrue(requestFailure(client, "http://unresolvable.invalid/").wasNotSent())
    }

    @Test
    fun `Test request without a response before the read timeout counts as sent`() = runTest(timeout = 5.seconds) {
        createServer().use { server ->
            val received = CompletableDeferred<Unit>()

            acceptOne(server) { _, reader ->
                received.complete(Unit)

                // Keep the connection open until the client gives up.
                runCatching {
                    while (reader.read() != -1) {
                        // Discard the request body.
                    }
                }
            }

            val exception = requestFailure(client, "http://127.0.0.1:${server.localPort}/")

            Assert.assertTrue(exception.cause.toString(), exception.cause is SocketTimeoutException)
            Assert.assertFalse(exception.wasNotSent())

            received.await()
        }
    }

    /**
     * Accepts one connection on [server] in the background and reads its request headers.
     *
     * @param afterHeaders Called with the connection and its reader once the request headers have been read.
     */
    private fun CoroutineScope.acceptOne(server: ServerSocket, afterHeaders: (Socket, BufferedReader) -> Unit) =
        launch(Dispatchers.IO) {
            server.accept().use { socket ->
                val reader = socket.getInputStream().bufferedReader()

                while (!reader.readLine().isNullOrEmpty()) {
                    // Read until the end of the request headers.
                }

                afterHeaders(socket, reader)
            }
        }

    private fun createPost() = URLEncodedPostBuilder().apply { addParam("key", "value") }

    private fun createServer() = ServerSocket(0, 1, InetAddress.getLoopbackAddress())

    private fun requestFailure(client: OkHttpClient, url: String): PostBuilder.RequestException {
        try {
            createPost().request(client, url)
        } catch (e: PostBuilder.RequestException) {
            return e
        }

        Assert.fail("The request was expected to fail")
        throw AssertionError()
    }
}
