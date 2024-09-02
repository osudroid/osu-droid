package com.reco1l.osu.data

import android.content.Context
import android.util.Log
import androidx.room.*
import com.reco1l.toolkt.data.iterator
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.*
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
     * Get the beatmap collections table DAO.
     */
    @JvmStatic
    val beatmapCollectionsTable
        get() = database.getBeatmapCollectionsTable()

    /**
     * Get the score table DAO.
     */
    @JvmStatic
    val scoreInfoTable
        get() = database.getScoreInfoTable()

    /**
     * Get the block area table DAO.
     */
    @JvmStatic
    val blockAreaTable
        get() = database.getBlockAreaTable()


    private lateinit var database: DroidDatabase


    @JvmStatic
    fun load(context: Context) {

        // Be careful when changing the database name, it may cause data loss.
        database = Room.databaseBuilder(context, DroidDatabase::class.java, "${Config.getCorePath()}databases/room.db")
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

                        for ((path, properties) in ois.readObject() as Map<String, BeatmapProperties>) {

                            beatmapOptionsTable.insert(BeatmapOptions(
                                setDirectory = path.let {
                                    if (it.endsWith('/')) {
                                        it.substring(0, it.length - 1).substringAfterLast('/')
                                    } else {
                                        it.substringAfterLast('/')
                                    }
                                },
                                isFavorite = properties.favorite,
                                offset = properties.offset
                            ))
                        }
                    }
                }

                oldPropertiesFile.renameTo(File(context.filesDir, "properties_old"))
            }

        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to migrate legacy beatmap properties", e)
        }

        // BeatmapCollections
        try {
            val oldFavoritesFile = File(Config.getCorePath(), "json/favorites.json")

            if (oldFavoritesFile.exists()) {
                GlobalManager.getInstance().info = "Migrating beatmap collections..."

                val json = JSONObject(oldFavoritesFile.readText())

                for (collectionName in json.keys()) {
                    beatmapCollectionsTable.insertCollection(collectionName)

                    for (beatmapPath in json.getJSONArray(collectionName)) {

                        beatmapCollectionsTable.addBeatmap(
                            collectionName = collectionName,
                            setDirectory = beatmapPath.toString().let {
                                if (it.endsWith('/')) {
                                    it.substring(0, it.length - 1).substringAfterLast('/')
                                } else {
                                    it.substringAfterLast('/')
                                }
                            }
                        )
                    }
                }

                oldFavoritesFile.renameTo(File(Config.getCorePath(), "json/favorites_old.json"))
            }

        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to migrate legacy beatmap properties", e)
        }

        // ScoreInfo
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
                                    val id = it.getInt(it.getColumnIndexOrThrow("id")).toLong()

                                    if (scoreInfoTable.scoreExists(id)) {
                                        pendingScores--
                                        continue
                                    }

                                    val scoreInfo = ScoreInfo(
                                        id = id,
                                        // "filename" can contain the full path, so we need to extract both filename and directory name refers
                                        // to the beatmap set directory. The pattern could be `/beatmapSetDirectory/beatmapFilename/` with or
                                        // without the trailing slash.
                                        beatmapFilename = it.getString(it.getColumnIndexOrThrow("filename")).let { result ->
                                            if (result.endsWith('/')) {
                                                result.substring(0, result.length - 1).substringAfterLast('/')
                                            } else {
                                                result.substringAfterLast('/')
                                            }
                                        },
                                        beatmapSetDirectory = it.getString(it.getColumnIndexOrThrow("filename")).let { result ->
                                            if (result.endsWith('/')) {
                                                result.substringBeforeLast('/').substringBeforeLast('/').substringAfterLast('/')
                                            } else {
                                                result.substringBeforeLast('/').substringAfterLast('/')
                                            }
                                        },
                                        playerName = it.getString(it.getColumnIndexOrThrow("playername")),
                                        replayFilename = it.getString(it.getColumnIndexOrThrow("replayfile")).let { result ->

                                            // The old format used the full path, so we need to extract the file name.
                                            if (result.endsWith('/')) {
                                                result.substring(0, result.length - 1).substringAfterLast('/')
                                            } else {
                                                result.substringAfterLast('/')
                                            }
                                        },
                                        mods = it.getString(it.getColumnIndexOrThrow("mode")),
                                        score = it.getInt(it.getColumnIndexOrThrow("score")),
                                        maxCombo = it.getInt(it.getColumnIndexOrThrow("combo")),
                                        mark = it.getString(it.getColumnIndexOrThrow("mark")),
                                        hit300k = it.getInt(it.getColumnIndexOrThrow("h300k")),
                                        hit300 = it.getInt(it.getColumnIndexOrThrow("h300")),
                                        hit100k = it.getInt(it.getColumnIndexOrThrow("h100k")),
                                        hit100 = it.getInt(it.getColumnIndexOrThrow("h100")),
                                        hit50 = it.getInt(it.getColumnIndexOrThrow("h50")),
                                        misses = it.getInt(it.getColumnIndexOrThrow("misses")),
                                        accuracy = it.getFloat(it.getColumnIndexOrThrow("accuracy")),
                                        time = it.getLong(it.getColumnIndexOrThrow("time")),
                                        isPerfect = it.getInt(it.getColumnIndexOrThrow("perfect")) == 1

                                    )

                                    scoreInfoTable.insertScore(scoreInfo)
                                    pendingScores--

                                } catch (e: Exception) {
                                    Log.e("ScoreLibrary", "Failed to import score from old database.", e)
                                }
                            }

                            if (pendingScores <= 0) {
                                oldDatabaseFile.renameTo(File(Config.getCorePath(), "databases/osudroid_old.db"))
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
        ScoreInfo::class,
        BeatmapSetCollection::class,
        BeatmapSetCollection_BeatmapSetInfo::class,
        BlockArea::class
    ]
)
abstract class DroidDatabase : RoomDatabase() {

    abstract fun getBeatmapInfoTable(): IBeatmapInfoDAO

    abstract fun getBeatmapOptionsTable(): IBeatmapOptionsDAO

    abstract fun getBeatmapCollectionsTable(): IBeatmapCollectionsDAO

    abstract fun getScoreInfoTable(): IScoreInfoDAO

    abstract fun getBlockAreaTable(): IBlockAreaDAO
}