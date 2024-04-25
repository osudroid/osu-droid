package com.reco1l.framework.net

import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

open class WebRequest(private var url: HttpUrl) : AutoCloseable {

    constructor(url: String): this(url.toHttpUrl())


    /**
     * The response got upon request, this will not be initialized until [execute] has been
     * successfully called.
     */
    lateinit var response: Response

    /**
     * The response body got upon request, this will not be initialized until [execute] has been
     * successfully called.
     */
    lateinit var responseBody: ResponseBody


    private var client = DEFAULT_CLIENT

    private var request = Request.Builder().url(url).build()


    /**
     * Builds a new Okio HTTP client upon the current one.
     */
    fun buildClient(unit: OkHttpClient.Builder.() -> Unit) {
        val builder = client.newBuilder()
        builder.unit()
        client = builder.build()
    }

    /**
     * Builds a new request upon the current one.
     */
    fun buildRequest(unit: Request.Builder.() -> Unit) {
        val builder = request.newBuilder()
        builder.unit()
        request = builder.build()
    }

    /**
     * Builds an HTTP URL upon the current one.
     */
    fun buildUrl(unit: HttpUrl.Builder.() -> Unit) {
        val builder = url.newBuilder()
        builder.unit()
        url = builder.build()
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

    }


    /**
     * Executes the request.
     *
     * Make sure to call before call close() or inside the try-with-resources statement.
     */
    @Throws(Exception::class)
    open fun execute(): WebRequest {

        val call = client.newCall(request)

        try {

            if (BuildConfig.DEBUG) {
                Log.i("WebRequest", "Request to: " + request.url)
            }

            response = call.execute()
            responseBody = response.body!!

            onResponse(response)

            if (response.isSuccessful) {
                onResponseSuccess(response)
            } else {
                throw ResponseException(response)
            }

        } catch (e: Exception) {
            onResponseFail(e)
        }

        return this
    }


    companion object {

        /**
         * The default Okio HTTP client.
         *
         * This client uses by default a 10 seconds timeout configuration.
         */
        val DEFAULT_CLIENT = OkHttpClient
            .Builder()
            .connectTimeout(10000, TimeUnit.MILLISECONDS)
            .build()

    }
}


/**
 * Denotes a response that is not successful.
 */
class ResponseException(response: Response) : IOException("Unexpected response: $response") {
    val code: Int = response.code
}
