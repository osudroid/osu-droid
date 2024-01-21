package com.reco1l.api.chimu

import com.reco1l.framework.extensions.mapInto
import com.reco1l.framework.extensions.mapIntoListOf
import com.reco1l.framework.net.JsonRequester
import com.reco1l.framework.net.QueryContent

/**
 * Kotlin's bindings for Chimu Cheesegull API.
 *
 * [Chimu docs...](https://chimu.moe/docs)
 */
@Suppress("FunctionName")
object CheesegullAPI
{

    /**
     * The hostname.
     */
    const val HOST = "https://api.chimu.moe"

    /**
     * The endpoint to search with a query.
     */
    const val SEARCH = "$HOST/cheesegull/search"

    /**
     * The endpoint to get a beatmap by its ID.
     */
    const val BEATMAP_BY_ID = "$HOST/cheesegull/b/"

    /**
     * The endpoint to get a beatmap by its MD5 checksum.
     */
    const val BEATMAP_BY_MD5 = "$HOST/cheesegull/md5/"

    /**
     * The endpoint to get a beatmap set by its ID.
     */
    const val BEATMAP_SET_BY_ID = "$HOST/cheesegull/s/"

    /**
     * The endpoint to download a beatmap set by its ID.
     */
    const val DOWNLOAD = "$HOST/v1/download"


    /**
     * Get beatmap information from its MD5.
     */
    inline fun <reified T : Any> GET_beatmap(md5: String): T? = JsonRequester(BEATMAP_BY_MD5 + md5).use {

        return it.executeAndGetJson().mapInto<T>()
    }

    /**
     * Get beatmap information from its ID.
     */
    inline fun <reified T : Any> GET_beatmap(id: Long): T? = JsonRequester(BEATMAP_BY_ID + id).use {

        return it.executeAndGetJson().mapInto<T>()
    }



    /**
     * Get beatmap set information from its ID.
     */
    inline fun <reified T : Any> GET_beatmapSet(id: Long): T? = JsonRequester(BEATMAP_SET_BY_ID + id).use {

        return it.executeAndGetJson().mapInto<T>()
    }



    /**
     * Search in the database with a query.
     */
    inline fun <reified T : Any> GET_search(query: QueryContent?): MutableList<T>? = JsonRequester(SEARCH).use {

        it.query = query
        return it.executeAndGetJson().toArray()?.mapIntoListOf<T>()
    }
}
