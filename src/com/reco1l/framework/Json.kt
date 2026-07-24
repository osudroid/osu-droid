package com.reco1l.framework

import org.json.JSONArray

/**
 * Iterator for [JSONArray]
 */
operator fun JSONArray.iterator(): Iterator<Any> = object : Iterator<Any> {

    private var index = 0

    override fun hasNext() = index < length()

    override fun next(): Any {
        val element = get(index)
        index++
        return element
    }
}