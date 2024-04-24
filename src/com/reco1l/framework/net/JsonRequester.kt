package com.reco1l.framework.net

import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.logIfDebug
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class JsonRequester(url: String) : Requester(url)
{

    /**
     * Whether response JSON should be logged.
     */
    var logResponse = false

    /**
     * Validate the received Json from the server, this validator will only be used if the response was successful.
     */
    var jsonValidator: ((JsonContent) -> Unit)? = null

    /**
     * Data insertion for POST requests.
     */
    var jsonInsertion: JSONObject? = null
        set(value)
        {
            if (value != null)
            {
                buildRequest {

                    "Inserted JSON: ${value.toString(4)}".logIfDebug(className)

                    it.post(value.toString().toRequestBody(JSON_UTF8))
                }
            }
            field = value
        }

    /**
     * The response JSON.
     */
    lateinit var json: JsonContent
        private set



    @Throws(Exception::class)
    override fun onResponse(response: Response?)
    {
        json = JsonContent(responseBody.string())

        if (logResponse)
            "Received JSON: ${json.toString(4)}".logIfDebug(className)
    }

    @Throws(Exception::class)
    override fun onResponseSuccess(response: Response?)
    {
        super.onResponseSuccess(response)

        jsonValidator?.invoke(json)
    }

    @Throws(Exception::class)
    override fun execute() = super.execute() as JsonRequester

    @Throws(Exception::class)
    fun executeAndGetJson(): JsonContent = execute().use { requester -> return requester.json }



    companion object
    {
        var JSON_UTF8 = "application/json; charset=utf-8".toMediaTypeOrNull()
    }
}
