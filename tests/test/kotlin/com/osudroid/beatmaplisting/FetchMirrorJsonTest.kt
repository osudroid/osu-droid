package com.osudroid.beatmaplisting

import com.reco1l.framework.net.UnsuccessfulResponseException
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FetchMirrorJsonTest {
    private val url = "https://example.com/api/v2/b/1".toHttpUrl()

    private fun client(code: Int, body: String, mediaType: String, onRequest: (okhttp3.Request) -> Unit = {}) =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                onRequest(chain.request())

                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("Canned")
                    .body(body.toResponseBody(mediaType.toMediaType()))
                    .build()
            })
            .build()

    @Test
    fun `Plain text 404 throws UnsuccessfulResponseException`() {
        try {
            fetchMirrorJson(url, client(404, "this content could not be found", "text/plain"))
            fail("Expected UnsuccessfulResponseException")
        } catch (e: UnsuccessfulResponseException) {
            assertEquals(404, e.code)
        }
    }

    @Test
    fun `JSON 404 throws UnsuccessfulResponseException`() {
        try {
            fetchMirrorJson(url, client(404, """{"code":404,"message":"Beatmap not found."}""", "application/json"))
            fail("Expected UnsuccessfulResponseException")
        } catch (e: UnsuccessfulResponseException) {
            assertEquals(404, e.code)
        }
    }

    @Test
    fun `200 JSON object is returned`() {
        var userAgent: String? = null

        val json = fetchMirrorJson(url, client(200, """{"id":1}""", "application/json") {
            userAgent = it.header("User-Agent")
        })

        assertEquals(1, json.getInt("id"))
        assertEquals("Chrome/Android", userAgent)
    }

    @Test(expected = JSONException::class)
    fun `200 non-JSON body throws JSONException`() {
        fetchMirrorJson(url, client(200, "oops", "text/plain"))
    }
}
