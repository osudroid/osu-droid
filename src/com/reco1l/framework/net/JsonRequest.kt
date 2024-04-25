package com.reco1l.framework.net

import android.util.Log
import com.reco1l.toolkt.data.asJSONObject
import com.reco1l.toolkt.kotlin.isBetween
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osuplus.BuildConfig

sealed class JsonRequest<T : Any>(url: String) : WebRequest(url)
{

    /**
     * Whether response should be logged.
     */
    var logResponse = false

    /**
     * The response JSON.
     */
    lateinit var json: T
        private set


    fun buildRequestBody(unit: JSONObject.() -> Unit) {

        buildRequest {

            val json = JSONObject()
            json.unit()
            post(json.toString().toRequestBody(JSON_UTF_8))


            if (BuildConfig.DEBUG) {
                Log.i("JsonRequest", "Inserting JSON: " + json.toString(4))
            }
        }
    }


    protected abstract fun onCreateResponseJson(responseBody: String): T


    override fun onResponse(response: Response) {
        json = onCreateResponseJson(responseBody.string())
    }


    @Suppress("UNCHECKED_CAST")
    override fun execute() = super.execute() as JsonRequest<T>


    companion object {

        var JSON_UTF_8 = "application/json; charset=utf-8".toMediaTypeOrNull()
    }
}


class JsonObjectRequest(url: String) : JsonRequest<JSONObject>(url) {

    override fun onCreateResponseJson(responseBody: String): JSONObject {

        if (!responseBody.isBetween('{' to '}')) {
            throw JSONException("Response is not a JSON object.\n$responseBody")
        }

        val json = JSONObject(responseBody)

        if (BuildConfig.DEBUG) {
            Log.i("JsonRequester", "Received JSON: " + json.toString(4))
        }

        return json
    }

}

class JsonArrayRequest(url: String) : JsonRequest<JSONArray>(url) {

    override fun onCreateResponseJson(responseBody: String): JSONArray {

        if (!responseBody.isBetween('[' to ']')) {
            throw JSONException("Response is not a JSON array.\n$responseBody")
        }

        val json = JSONArray(responseBody)

        if (BuildConfig.DEBUG) {
            Log.i("JsonRequester", "Received JSON: " + json.toString(4))
        }

        return json
    }
}