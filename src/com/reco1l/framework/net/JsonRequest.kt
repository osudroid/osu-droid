package com.reco1l.framework.net

import android.util.Log
import com.reco1l.toolkt.kotlin.isBetween
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osuplus.BuildConfig

sealed class JsonRequest<T : Any>(url: HttpUrl) : WebRequest(url) {

    constructor(url: String): this(url.toHttpUrl())


    /**
     * The response JSON.
     */
    lateinit var json: T
        private set


    protected abstract fun onCreateResponseJson(responseBody: String): T


    override fun onResponse(response: Response) {
        json = onCreateResponseJson(responseBody.string())
    }


    @Suppress("UNCHECKED_CAST")
    override fun execute() = super.execute() as JsonRequest<T>

}


class JsonObjectRequest(url: HttpUrl) : JsonRequest<JSONObject>(url) {

    constructor(url: String): this(url.toHttpUrl())


    override fun onCreateResponseJson(responseBody: String): JSONObject {

        if (!responseBody.isBetween('{' to '}')) {
            throw JSONException("Response is not a JSON object:\n$responseBody")
        }

        val json = JSONObject(responseBody)

        if (BuildConfig.DEBUG) {
            Log.i("JsonRequester", "Received JSON: " + json.toString(4))
        }

        return json
    }

}

class JsonArrayRequest(url: HttpUrl) : JsonRequest<JSONArray>(url) {

    constructor(url: String): this(url.toHttpUrl())


    override fun onCreateResponseJson(responseBody: String): JSONArray {

        if (!responseBody.isBetween('[' to ']')) {
            throw JSONException("Response is not a JSON array:\n$responseBody")
        }

        val json = JSONArray(responseBody)

        if (BuildConfig.DEBUG) {
            Log.i("JsonRequester", "Received JSON: " + json.toString(4))
        }

        return json
    }
}