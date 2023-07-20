package com.reco1l.framework.net

/**
 * @author Reco1l
 */
class QueryContent
{

    private val builder = StringBuilder("?")

    private var useAmpersand = false


    fun put(key: String, value: Any)
    {
        val string = value.toString().takeUnless { it.isEmpty() } ?: return

        if (useAmpersand)
        {
            builder.append('&')
        }
        else
        {
            useAmpersand = true
        }
        builder.append(key).append('=').append(string)
    }



    override fun toString() = builder.toString()
}
