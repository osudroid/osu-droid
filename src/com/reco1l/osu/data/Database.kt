package com.reco1l.osu.data

import android.content.Context
import androidx.room.*


// Ported from rimu! project

/**
 * The osu!droid database manager.
 */
object DatabaseManager {

    /**
     * Get the beatmaps table DAO.
     */
    @JvmStatic
    val beatmapTable
        get() = database.getBeatmapTable()


    private lateinit var database: DroidDatabase


    @JvmStatic
    fun load(context: Context) {

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

@Database(
    version = 2,
    entities = [
        BeatmapInfo::class,
    ]
)
abstract class DroidDatabase : RoomDatabase() {

    abstract fun getBeatmapTable(): IBeatmapInfoDAO

}