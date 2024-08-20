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
     * Get the beatmaps table.
     */
    @JvmStatic
    val beatmapTable
        get() = database.getBeatmapTable()


    private lateinit var database: DroidDatabase


    @JvmStatic
    fun load(context: Context) {

        context.deleteDatabase(DroidDatabase::class.simpleName)

        // Be careful when changing the database name, it may cause data loss.
        database = Room.databaseBuilder(context, DroidDatabase::class.java, DroidDatabase::class.simpleName)
            // Is preferable to support migrations, otherwise destructive migration will run forcing
            // tables to recreate (in case of beatmaps table it'll re-import all beatmaps).
            // See https://developer.android.com/training/data-storage/room/migrating-db-versions.
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }

}

/**
 * The osu!droid database object class, this should be unique per instance.
 */
@Database(
    version = 3,
    entities = [
        BeatmapInfo::class,
    ]
)
abstract class DroidDatabase : RoomDatabase() {

    abstract fun getBeatmapTable(): IBeatmapDAO

}