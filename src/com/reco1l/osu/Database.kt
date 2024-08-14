package com.reco1l.osu

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger

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

        // Be careful when changing the database name, it may cause data loss.
        database = Room.databaseBuilder(context, DroidDatabase::class.java, "DroidDatabase")
            // If you want to support migrations, you should add them here. And it's preferable than
            // destructive migrations where there's data loss.
            // See https://developer.android.com/training/data-storage/room/migrating-db-versions.
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

}

/**
 * The osu!droid database object class, this should be unique per instance.
 */
@Database(
    version = 2,
    entities = [
        BeatmapInfo::class,
    ]
)
abstract class DroidDatabase : RoomDatabase() {

    abstract fun getBeatmapTable(): IBeatmapDAO

}