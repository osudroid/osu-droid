package com.reco1l.framework.net

import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.logIfDebug
import okhttp3.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Reco1l
 */
open class Requester(url: String) : AutoCloseable
{

    /**
     * Indicates if the requester should log responses.
     */
    var log = true

    /**
     * The request query.
     */
    var query: QueryContent? = null

    /**
     * The call executed by the client, don't use it before calling [execute].
     */
    lateinit var call: Call

    /**
     * The response get, don't use it before calling [execute].
     */
    lateinit var response: Response

    /**
     * The response body, don't use it before calling [execute].
     */
    lateinit var responseBody: ResponseBody



    private var client: OkHttpClient? = DEFAULT_CLIENT

    private var request: Request? = defaultRequest(url)



    fun buildClient(unit: (OkHttpClient.Builder) -> Unit)
    {
        val builder = client?.newBuilder() ?: OkHttpClient.Builder()

        unit(builder)
        client = builder.build()
    }

    fun buildRequest(unit: (Request.Builder) -> Unit)
    {
        val builder = request?.newBuilder() ?: Request.Builder()

        unit(builder)
        request = builder.build()
    }



    /**
     * Called as soon the response and response body is created.
     */
    @Throws(Exception::class)
    protected open fun onResponse(response: Response?)
    {
    }

    /**
     * Called when response code isn't 200-299, this can also be called from inherited [onResponseSuccess] exception thrown.
     */
    @Throws(Exception::class)
    protected fun onResponseFail(exception: Exception)
    {
        throw exception
    }

    /**
     * Called when response code is 200-299.
     */
    @Throws(Exception::class)
    protected open fun onResponseSuccess(response: Response?)
    {
    }



    override fun close()
    {
        if (::response.isInitialized)
        {
            response.close()
        }
        if (::responseBody.isInitialized)
        {
            responseBody.close()
        }
    }



    /**
     * Make sure to call before call close() or inside the try-with-resources statement.
     */
    @Throws(Exception::class)
    open fun execute(): Requester
    {
        requireNotNull(client)
        requireNotNull(request)

        if (query != null)
        {
            request = request!!.newBuilder().url("${request!!.url}$query").build()
        }

        call = client!!.newCall(request!!)

        try
        {
            if (log)
                "Inserted url: ${request!!.url}".logIfDebug(className)

            response = call.execute()
            responseBody = response.body!!

            onResponse(response)

            if (response.isSuccessful)
            {
                onResponseSuccess(response)
            }
            else
            {
                throw ResponseException(response)
            }
        }
        catch (e: Exception)
        {
            onResponseFail(e)
        }
        return this
    }

    /**
     * Make sure to call before call [close] or inside the try-with-resources statement.
     *
     * @return The response body.
     */
    @Throws(Exception::class)
    fun executeAndGetBody(): ResponseBody = execute().responseBody



    companion object
    {
        const val DEFAULT_TIMEOUT = 10000

        val DEFAULT_CLIENT = OkHttpClient
                .Builder()
                .connectTimeout(DEFAULT_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .build()

        fun defaultRequest(url: String) = Request
                .Builder()
                .url(url)
                .build()
    }
}
