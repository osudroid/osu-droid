package com.reco1l.framework.net

import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.IOException
import java.util.*

open class WebRequest(private var url: HttpUrl) : AutoCloseable {

    constructor(url: String): this(url.toHttpUrl())


    /**
     * The response got upon request, this will not be initialized until [execute] has been
     * successfully called.
     */
    lateinit var response: Response
        protected set

    /**
     * The response body got upon request, this will not be initialized until [execute] has been
     * successfully called.
     */
    lateinit var responseBody: ResponseBody
        protected set

    /**
     * The call object used to execute the request.
     * It will be initialized upon [execute] call.
     */
    lateinit var call: Call
        protected set


    /**
     * The client to be used for this request.
     *
     * By default the [global client][globalClient] is used with default settings, if you want to
     * change specific settings is better to build a new one upon it using [OkHttpClient.newBuilder].
     */
    var client = globalClient


    private var request = Request.Builder().url(url).build()


    /**
     * Builds a new request upon the current one.
     */
    fun buildRequest(block: Request.Builder.() -> Unit) {

        request = request.newBuilder().apply(block).build()
    }

    /**
     * Builds a JSON request body with data to be sent to server.
     */
    fun buildRequestBody(block: JSONObject.() -> Unit) {

        val json = JSONObject()
        json.block()

        buildRequest {
            post(json.toString().toRequestBody(JSON_UTF_8))
        }
    }

    /**
     * Builds an HTTP URL upon the current one.
     */
    fun buildUrl(block: HttpUrl.Builder.() -> Unit) {

        url = url.newBuilder().apply(block).build()

        // Rebuild the request to apply the URL change.
        buildRequest {
            url(url)
        }
    }


    /**
     * Called as soon the response and response body is created.
     */
    protected open fun onResponse(response: Response) = Unit

    /**
     * Called when response code isn't 200-299, this can also be called from inherited
     * [onResponseSuccess] exception thrown.
     */
    protected open fun onResponseFail(exception: Exception): Unit = throw exception

    /**
     * Called when response code is 200-299.
     */
    protected open fun onResponseSuccess(response: Response) = Unit


    override fun close() {

        if (::response.isInitialized) {
            response.close()
        }

        if (::responseBody.isInitialized) {
            responseBody.close()
        }

        if (::call.isInitialized) {
            call.cancel()
        }
    }


    /**
     * Cancels the current request.
     * This will have no effect if [execute] wasn't called or the request has already finished.
     */
    open fun cancel() {
        if (::call.isInitialized) {
            call.cancel()
        }
    }


    /**
     * Executes the request.
     *
     * Make sure to call before call close() or inside the try-with-resources statement.
     */
    @Throws(Exception::class)
    open fun execute(): WebRequest {

        try {
            call = client.newCall(request)

            if (BuildConfig.DEBUG) {
                Log.i("WebRequest", "Request to: " + request.url)
            }

            response = call.execute()
            responseBody = response.body!!

            onResponse(response)

            if (response.isSuccessful) {
                onResponseSuccess(response)
            } else {
                throw UnsuccessfulResponseException(response)
            }

        } catch (e: Exception) {
            onResponseFail(e)
        }

        return this
    }


    companion object {

        /**
         * The Json UTF-8 media type specification.
         */
        val JSON_UTF_8 = "application/json; charset=utf-8".toMediaTypeOrNull()

        /**
         * The global Okio HTTP client.
         *
         * This client uses by default a 10 seconds timeout configuration.
         */
        val globalClient = OkHttpClient()

    }
}


/**
 * Denotes a response that is not successful.
 */
class UnsuccessfulResponseException(response: Response) : IOException(response.message) {

    /**
     * The response code.
     */
    val code: Int = response.code

}
