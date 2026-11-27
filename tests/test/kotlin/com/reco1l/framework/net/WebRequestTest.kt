package com.reco1l.framework.net

import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebRequestTest {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(200, TimeUnit.MILLISECONDS)
        .build()

    @Test
    fun `Test cancel before execute cancels the call`() {
        createServer().use { server ->
            val request = createRequest(client, server)

            request.cancel()

            Assert.assertThrows(IOException::class.java) { request.execute() }
            Assert.assertTrue(request.call.isCanceled())
        }
    }

    @Test
    fun `Test cancel during execute aborts the call`() = runTest(timeout = 5.seconds) {
        createServer().use { server ->
            val received = CompletableDeferred<Unit>()

            launch(Dispatchers.IO) {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()

                    while (!reader.readLine().isNullOrEmpty()) {
                        // Read until the end of the request headers.
                    }

                    received.complete(Unit)

                    // Keep the connection open without responding until the client closes it.
                    runCatching {
                        while (reader.read() != -1) {
                            // Discard anything else the client sends.
                        }
                    }
                }
            }

            // Without the cancel, this request would wait for the read timeout, which is longer than the test's timeout.
            val request = createRequest(client.newBuilder().readTimeout(30, TimeUnit.SECONDS).build(), server)
            val execution = async(Dispatchers.IO) { runCatching { request.execute() } }

            received.await()
            request.cancel()

            val exception = execution.await().exceptionOrNull()

            Assert.assertTrue(exception.toString(), exception is IOException)
            Assert.assertTrue(request.call.isCanceled())
        }
    }

    private fun createRequest(client: OkHttpClient, server: ServerSocket) =
        WebRequest("http://127.0.0.1:${server.localPort}/").also { it.client = client }

    private fun createServer() = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
}
