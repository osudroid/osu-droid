package com.reco1l.osu.data

import androidx.room.ColumnInfo
import androidx.room.Relation

/**
 * Defines a beatmap set, they're virtually created by the database using DISTINCT operation. This means it doesn't have
 * a table.
 */
data class BeatmapSetInfo(

    /**
     * The ID.
     */
    @ColumnInfo(name = "parentId")
    val id: Int?,

    /**
     * This can equal to the set ID or its MD5.
     */
    @ColumnInfo(name = "parentPath")
    val path: String,

    /**
     * The list of beatmaps
     */
    @Relation(parentColumn = "parentPath", entityColumn = "parentPath")
    val beatmaps: List<BeatmapInfo>

) {

    /**
     * The beatmap set size.
     */
    val count
        get() = beatmaps.size


    /**
     * Get a beatmap from its index.
     */
    @JvmName("getBeatmap")
    operator fun get(index: Int): BeatmapInfo {
        return beatmaps[index]
    }

}