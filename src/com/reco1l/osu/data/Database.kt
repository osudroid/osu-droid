package com.reco1l.osu.data

import android.content.Context
import android.util.Log
import androidx.room.*
import ru.nsu.ccfit.zuev.osu.BeatmapProperties
import ru.nsu.ccfit.zuev.osu.GlobalManager
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream


// Ported from rimu! project

/**
 * The osu!droid database manager.
 */
object DatabaseManager {

    /**
     * Get the beatmaps table DAO.
     */
    @JvmStatic
    val beatmapInfoTable
        get() = database.getBeatmapInfoTable()

    /**
     * Get the beatmap options table DAO.
     */
    @JvmStatic
    val beatmapOptionsTable
        get() = database.getBeatmapOptionsTable()


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

        loadLegacyMigrations(context)
    }

    @Suppress("UNCHECKED_CAST")
    fun loadLegacyMigrations(context: Context) {

        // BeatmapOptions
        try {
            File(context.filesDir, "properties").takeIf { it.exists() }?.inputStream()?.use { fis ->

                GlobalManager.getInstance().info = "Migrating beatmap properties..."

                ObjectInputStream(fis).use { ois ->

                    // Ignoring first object which is intended to be the version.
                    ois.readObject()

                    val options = (ois.readObject() as Map<String, BeatmapProperties>).map { (path, props) ->

                        BeatmapOptions(path, props.isFavorite, props.offset)
                    }

                    beatmapOptionsTable.addAll(options)
                }

            }
        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to migrate legacy beatmap properties", e)
        }

    }

}

@Database(
    version = 1,
    entities = [
        BeatmapInfo::class,
        BeatmapOptions::class,
    ]
)
abstract class DroidDatabase : RoomDatabase() {

    abstract fun getBeatmapInfoTable(): IBeatmapInfoDAO

    abstract fun getBeatmapOptionsTable(): IBeatmapOptionsDAO

}