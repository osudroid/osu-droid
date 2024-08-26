package com.reco1l.osu.data

import android.content.Context
import android.util.Log
import androidx.room.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.helper.sql.DBOpenHelper
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

    /**
     * Get the score table DAO.
     */
    @JvmStatic
    val scoreInfoTable
        get() = database.getScoreInfoTable()


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
    private fun loadLegacyMigrations(context: Context) {

        // BeatmapOptions
        try {
            val oldPropertiesFile = File(context.filesDir, "properties")

            if (oldPropertiesFile.exists()) {

                GlobalManager.getInstance().info = "Migrating beatmap properties..."

                oldPropertiesFile.inputStream().use { fis ->

                    ObjectInputStream(fis).use { ois ->

                        // Ignoring first object which is intended to be the version.
                        ois.readObject()

                        val options = (ois.readObject() as Map<String, BeatmapProperties>).map { (path, properties) ->

                            // Old properties storage system stores the absolute path of the beatmap
                            // set so we're extracting it here to use the directory name.
                            val setDirectory = if (path.endsWith('/')) {
                                path.substring(0, path.length - 1).substringAfterLast('/')
                            } else {
                                path.substringAfterLast('/')
                            }

                            BeatmapOptions(setDirectory, properties.favorite, properties.offset)
                        }

                        beatmapOptionsTable.insertAll(options)
                    }

                }

                oldPropertiesFile.renameTo(File(context.filesDir, "properties.old"))
            }

        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to migrate legacy beatmap properties", e)
        }

        // Score table
        try {
            val oldDatabaseFile = File(Config.getCorePath(), "databases/osudroid_test.db")

            if (oldDatabaseFile.exists()) {

                GlobalManager.getInstance().info = "Migrating score table..."

                DBOpenHelper.getOrCreate(context).use { helper ->

                    helper.writableDatabase.use { db ->

                        db.rawQuery("SELECT * FROM scores", null).use {

                            var pendingScores = it.count

                            while (it.moveToNext()) {

                                try {
                                    val scoreInfo = ScoreInfo(
                                        it.getInt(it.getColumnIndexOrThrow("id")).toLong(),
                                        // "filename" can contain the full path, so we need to extract both filename and directory name
                                        // which refers to the beatmap set directory. The pattern could be `/beatmapSetDirectory/beatmapFilename/`
                                        // with or without the trailing slash.
                                        it.getString(it.getColumnIndexOrThrow("filename")).let { result ->
                                            if (result.endsWith('/')) {
                                                result.substring(0, result.length - 1).substringAfterLast('/')
                                            } else {
                                                result.substringAfterLast('/')
                                            }
                                        },
                                        it.getString(it.getColumnIndexOrThrow("filename")).let { result ->
                                            if (result.endsWith('/')) {
                                                result.substringBeforeLast('/').substringBeforeLast('/').substringAfterLast('/')
                                            } else {
                                                result.substringBeforeLast('/').substringAfterLast('/')
                                            }
                                        },
                                        it.getString(it.getColumnIndexOrThrow("playername")),
                                        it.getString(it.getColumnIndexOrThrow("replayfile")).let { result ->

                                            // The old format used the full path, so we need to extract the file name.
                                            if (result.endsWith('/')) {
                                                result.substring(0, result.length - 1).substringAfterLast('/')
                                            } else {
                                                result.substringAfterLast('/')
                                            }
                                        },
                                        it.getString(it.getColumnIndexOrThrow("mode")),
                                        it.getInt(it.getColumnIndexOrThrow("score")),
                                        it.getInt(it.getColumnIndexOrThrow("combo")),
                                        it.getString(it.getColumnIndexOrThrow("mark")),
                                        it.getInt(it.getColumnIndexOrThrow("h300k")),
                                        it.getInt(it.getColumnIndexOrThrow("h300")),
                                        it.getInt(it.getColumnIndexOrThrow("h100k")),
                                        it.getInt(it.getColumnIndexOrThrow("h100")),
                                        it.getInt(it.getColumnIndexOrThrow("h50")),
                                        it.getInt(it.getColumnIndexOrThrow("misses")),
                                        it.getFloat(it.getColumnIndexOrThrow("accuracy")),
                                        it.getLong(it.getColumnIndexOrThrow("time")),
                                        it.getInt(it.getColumnIndexOrThrow("perfect")) == 1

                                    )

                                    scoreInfoTable.insertScore(scoreInfo)
                                    db.rawQuery("DELETE FROM scores WHERE id = ${scoreInfo.id}", null)
                                    pendingScores--

                                } catch (e: Exception) {
                                    Log.e("ScoreLibrary", "Failed to import score from old database.", e)
                                }
                            }

                            if (pendingScores <= 0) {
                                oldDatabaseFile.renameTo(File(Config.getCorePath(), "databases/osudroid_test.olddb"))
                            }
                        }
                    }

                }

            }

        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to migrate legacy score table", e)
        }

    }

}

@Database(
    version = 1,
    entities = [
        BeatmapInfo::class,
        BeatmapOptions::class,
        ScoreInfo::class
    ]
)
abstract class DroidDatabase : RoomDatabase() {

    abstract fun getBeatmapInfoTable(): IBeatmapInfoDAO

    abstract fun getBeatmapOptionsTable(): IBeatmapOptionsDAO

    abstract fun getScoreInfoTable(): IScoreInfoDAO

}