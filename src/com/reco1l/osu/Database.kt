package com.reco1l.osu

import android.content.Context
import androidx.room.*

// Ported from rimu! project

/**
 * The ~~rimu!~~ osu!droid database manager.
 * It joins all entities DAOs in one class.
 *
 * @see IBeatmapDAO
 */
object DatabaseManager {

    /**
     * Get beatmaps table.
     */
    @JvmStatic
    val beatmapTable
        get() = database.getBeatmapTable()


    private lateinit var database: DroidDatabase


    @JvmStatic
    fun load(context: Context) {
        database = Room.databaseBuilder(context, DroidDatabase::class.java, "DroidDatabase").build()
    }

}

/**
 * The osu!droid database object class, this should be unique per instance.
 */
@Database(
    version = 1,
    entities = [
        BeatmapInfo::class,
    ]
)
abstract class DroidDatabase : RoomDatabase() {

    abstract fun getBeatmapTable(): IBeatmapDAO

}