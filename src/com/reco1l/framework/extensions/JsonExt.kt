@file:JvmName("JsonUtil")
/*
 * @author Reco1l
 */

package com.reco1l.framework.extensions

import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.full.primaryConstructor


/**
 * Iterate over all objects on a [JSONArray].
 *
 * Note: Objects of type [JSONArray] wrapped on the array will be converted into a [JSONObject]
 */
inline fun JSONArray.forEach(action: (JSONObject) -> Unit)
{
    for (i in 0 until length())
    {
        // Handling wrapped JSONArray converting them into a JSONObject.
        val o = optJSONObject(i) ?: toJSONObject(optJSONArray(i) ?: continue)
        action(o)
    }
}


/**
 * Map a JSONObject into a data class.
 *
 * It'll only map arguments from the constructor of the class, the arguments name should match with the JSON keys.
 * Extremely recommended to use nullable arguments to handle not found keys in a JSON.
 *
 * @throws Exception Advise to catch any conversion error.
 */
inline fun <reified T : Any> JSONObject.mapInto(): T?
{
    val constructor = T::class.primaryConstructor ?: return null
    val parameters = constructor.parameters

    val arguments = parameters.associateWith { opt(it.name) }

    return constructor.callBy(arguments)
}

/**
 * Map a JSONArray into a MutableList containing mapped data classes for every JSONObject inside.
 *
 * @see [JSONObject.mapInto]
 * @throws Exception Advise to catch any conversion error.
 */
inline fun <reified T : Any> JSONArray.mapIntoListOf(): MutableList<T>?
{
    val list = mutableListOf<T>()

    forEach { list.add(it.mapInto<T>() ?: return null) }

    return list.takeUnless { it.isEmpty() }
}


operator fun JSONArray.iterator(): Iterator<Any> = object : Iterator<Any>
{
    private var index = 0

    override fun hasNext() = index < length()

    override fun next(): Any
    {
        val element = get(index)
        index++
        return element
    }
}
