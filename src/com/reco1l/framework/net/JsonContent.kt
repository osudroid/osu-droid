package com.reco1l.framework.net

// import com.reco1l.framework.extensions.iterator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class JsonContent : JSONObject()
{

    /**
     * This can only be `true` if it was created from a String, and it was a [JSONArray].
     */
    var isConvertedArray = false
        private set

    //----------------------------------------------------------------------------------------------------------------//

    // Disabled for osu!droid.
/*   /**
     * This constructor parses objects and arrays into a [JSONObject].
     * In the case of arrays it puts every array entry with the index as it name.
     */
    constructor(source: String) : super()
    {
        val t = JSONTokener(source)
        val o = t.nextValue()

        if (o is JSONObject)
        {
            val names = o.names()

            if (names != null)
            {
                for (name in names)
                {
                    put(name as String, o.get(name))
                }
            }
            return
        }
        else if (o is JSONArray)
        {
            isConvertedArray = true

            for (i in 0 until o.length())
            {
                put(i.toString(), o[i])
            }
            return
        }
        throw JSONException("Not a JSON:\n$source")
    }*/

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Puts inside a new [JsonContent] and returns it.
     */
    fun putGroup(header: String): JsonContent
    {
        val group = JsonContent()
        put(header, group)
        return group
    }

    /**
     * If this content is a [JSONArray] converted it'll will return the object by its index.
     *
     * @throws JSONException If this content is not an array converted.
     */
    fun get(index: Int): Any
    {
        if (!isConvertedArray)
        {
            throw JSONException("This object is not an array converted")
        }
        if (index > length())
        {
            throw IndexOutOfBoundsException()
        }
        return get(index.toString())
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * If this content correspond to an array converted it'll convert it back to [JSONArray].
     *
     * @see isConvertedArray
     */
    fun toArray(): JSONArray?
    {
        if (isConvertedArray)
        {
            return toJSONArray(names())
        }
        throw JSONException("This object is not an array converted")
    }
}
