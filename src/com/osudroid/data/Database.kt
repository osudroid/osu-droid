package com.osudroid.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.*
import com.osudroid.mods.LegacyModConverter
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.mods.ModReplayV6
import com.osudroid.scoring.LegacyScoreMultiplierCalculator
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.mainThread
import com.reco1l.osu.ui.MessageDialog
import com.reco1l.toolkt.data.iterator
import org.apache.commons.io.FilenameUtils
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.sql.DBOpenHelper
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import kotlin.math.roundToLong
import ru.nsu.ccfit.zuev.osu.scoring.Replay


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

    /**
     * Get the mod preset table DAO.
     */
    @JvmStatic
    val modPresetTable
        get() = database.getModPresetTable()

    /**
     * The path to the database file.
     */
    @JvmStatic
    val databasePath: String
        get() = "${Config.getCorePath()}databases/room-${BuildConfig.BUILD_TYPE}.db"

    private lateinit var database: DroidDatabase

    /**
     * `true` if the database was created by a newer, incompatible version of the app and had to be reset.
     *
     * When this happens, the previous database file is kept as a backup alongside the new one.
     */
    @JvmStatic
    var wasResetDueToDowngrade = false
        private set


    @JvmStatic
    fun load(context: Context) {

        wasResetDueToDowngrade = backUpDatabaseIfDowngraded()

        // Be careful when changing the database name, it may cause data loss.
        database = Room.databaseBuilder(context, DroidDatabase::class.java, databasePath)
            .addMigrations(*ALL_MIGRATIONS)
            // No downgrade path is maintained for the database, so if it was created by a newer version of
            // the game, it will be reset. backUpDatabaseIfDowngraded() keeps a copy of it beforehand.
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .allowMainThreadQueries()
            .build()

        if (!BuildConfig.DEBUG) {
            loadLegacyMigrations(context)
        }
    }

    /**
     * Shows a dialog notifying the user that their local database was reset, if applicable.
     *
     * Must be called after the main scene has finished loading.
     */
    @JvmStatic
    fun notifyIfResetDueToDowngrade() {
        if (!wasResetDueToDowngrade) {
            return
        }

        mainThread {
            MessageDialog()
                .setTitle("Database reset")
                .setMessage(
                    "Your local database was created by a newer version of the game and could not be read, " +
                        "so it was reset. This means locally stored scores, favorites, and collections were " +
                        "lost.\n\nA backup of the previous database was saved."
                )
                .addButton("OK", clickListener = MessageDialog::dismiss)
                .show()
        }
    }

    /**
     * Backs up the current database file if it was created by a version of the game newer than this one,
     * since Room cannot open it and will destructively reset it.
     *
     * @return `true` if the database was backed up, `false` otherwise.
     */
    private fun backUpDatabaseIfDowngraded(): Boolean {
        val dbFile = File(databasePath)

        if (!dbFile.exists()) {
            return false
        }

        val fileVersion = try {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Failed to read database version prior to opening", e)
            return false
        }

        val appVersion = DroidDatabase::class.java.getAnnotation(Database::class.java)!!.version

        if (fileVersion <= appVersion) {
            return false
        }

        Log.w(
            "DatabaseManager",
            "Database was created by a newer version of the game (file version $fileVersion, game version " +
                "$appVersion) and will be reset. The previous database will be backed up."
        )

        try {
            // The WAL file may hold committed data that hasn't been written back to the main database file
            // yet, so it needs to be checkpointed before the file is copied.
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor -> cursor.moveToFirst() }
            }

            dbFile.copyTo(File(dbFile.parentFile, "${dbFile.name}.v$fileVersion.bak"), true)
        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to back up database before reset", e)
        }

        return true
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

                        val beatmapOptions = mutableListOf<BeatmapOptions>()

                        // Ignoring first object which is intended to be the version.
                        ois.readObject()

                        for ((path, properties) in ois.readObject() as Map<String, BeatmapProperties>) {
                            beatmapOptions += BeatmapOptions(
                                setDirectory = FilenameUtils.getName(FilenameUtils.normalizeNoEndSeparator(path)),
                                isFavorite = properties.favorite,
                                // Offset is flipped in version 1.8, so we need to negate it.
                                offset = -properties.offset
                            )
                        }

                        beatmapOptionsTable.insertAll(beatmapOptions)
                    }
                }

                oldPropertiesFile.renameTo(File(context.filesDir, "properties_old"))
            }

        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to migrate legacy beatmap properties", e)
        }

        // BeatmapCollections
        try {
            val oldFavoritesFile = File(Config.getCorePath(), "json/favorite.json")

            if (oldFavoritesFile.exists()) {
                GlobalManager.getInstance().info = "Migrating beatmap collections..."

                val json = JSONObject(oldFavoritesFile.readText())

                for (collectionName in json.keys()) {
                    beatmapCollectionsTable.insertCollection(collectionName)

                    for (beatmapPath in json.getJSONArray(collectionName)) {

                        beatmapCollectionsTable.addBeatmap(
                            collectionName = collectionName,
                            setDirectory = FilenameUtils.getName(FilenameUtils.normalizeNoEndSeparator(beatmapPath.toString()))
                        )
                    }
                }

                oldFavoritesFile.renameTo(File(Config.getCorePath(), "json/favorite_old.json"))
            }

        } catch (e: Exception) {
            Log.e("DatabaseManager", "Failed to migrate legacy beatmap collections", e)
        }

        // ScoreInfo
        try {
            val oldDatabaseFile = File(Config.getCorePath(), "databases/osudroid_test.db")

            if (oldDatabaseFile.exists()) {
                GlobalManager.getInstance().info = "Migrating score table..."

                DBOpenHelper.getOrCreate(context).writableDatabase.use { db ->

                    db.rawQuery("SELECT * FROM scores", null).use {

                        var pendingScores = it.count
                        val scoreMultiplierCalculator = LegacyScoreMultiplierCalculator()

                        val scoreInfos = mutableListOf<ScoreInfo>()
                        while (it.moveToNext()) {

                            try {
                                val id = it.getInt(it.getColumnIndexOrThrow("id")).toLong()

                                if (scoreInfoTable.scoreExists(id)) {
                                    pendingScores--
                                    continue
                                }

                                val replayFilePath = it.getString(it.getColumnIndexOrThrow("replayfile"))
                                val replay = Replay()

                                if (!replay.load(replayFilePath, false)) {
                                    Log.e("ScoreLibrary", "Failed to import score from old database. Replay file not found.")
                                    pendingScores--
                                    continue
                                }

                                val legacyModString = it.getString(it.getColumnIndexOrThrow("mode"))

                                val legacyMods = try {
                                    LegacyModConverter.convert(legacyModString)
                                } catch (_: Exception) {
                                    ModHashMap()
                                }.apply { put(ModReplayV6()) }

                                val legacyScore = it.getInt(it.getColumnIndexOrThrow("score")).toLong()
                                val needsMigration = legacyMods.values.any { mod -> mod is ModDifficultyAdjust }

                                val rawScore =
                                    if (needsMigration) legacyScore
                                    else (legacyScore / scoreMultiplierCalculator.calculateFor(legacyMods.values)).roundToLong()

                                scoreInfos += ScoreInfo(
                                    id = id,
                                    beatmapMD5 = replay.md5,
                                    playerName = it.getString(it.getColumnIndexOrThrow("playername")),
                                    replayFilename = FilenameUtils.getName(replayFilePath),
                                    mods = legacyMods.serializeMods(),
                                    score = rawScore,
                                    maxCombo = it.getInt(it.getColumnIndexOrThrow("combo")),
                                    mark = it.getString(it.getColumnIndexOrThrow("mark")),
                                    hit300k = it.getInt(it.getColumnIndexOrThrow("h300k")),
                                    hit300 = it.getInt(it.getColumnIndexOrThrow("h300")),
                                    hit100k = it.getInt(it.getColumnIndexOrThrow("h100k")),
                                    hit100 = it.getInt(it.getColumnIndexOrThrow("h100")),
                                    hit50 = it.getInt(it.getColumnIndexOrThrow("h50")),
                                    misses = it.getInt(it.getColumnIndexOrThrow("misses")),
                                    time = it.getLong(it.getColumnIndexOrThrow("time")),
                                    sliderHeadHits = null,
                                    sliderTickHits = null,
                                    sliderRepeatHits = null,
                                    sliderEndHits = null,
                                    needsScoreMigration = needsMigration
                                )

                                pendingScores--

                            } catch (e: Exception) {
                                Log.e("ScoreLibrary", "Failed to import score from old database.", e)
                            }
                        }

                        scoreInfoTable.insertScores(scoreInfos)

                        if (pendingScores <= 0) {
                            oldDatabaseFile.renameTo(File(Config.getCorePath(), "databases/osudroid_old.db"))
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
    version = 5,
    entities = [
        BeatmapInfo::class,
        BeatmapOptions::class,
        ScoreInfo::class,
        BeatmapSetCollection::class,
        BeatmapSetCollection_BeatmapSetInfo::class,
        BlockArea::class,
        ModPreset::class
    ]
)
abstract class DroidDatabase : RoomDatabase() {

    abstract fun getBeatmapInfoTable(): IBeatmapInfoDAO

    abstract fun getBeatmapOptionsTable(): IBeatmapOptionsDAO

    abstract fun getBeatmapCollectionsTable(): IBeatmapCollectionsDAO

    abstract fun getScoreInfoTable(): IScoreInfoDAO

    abstract fun getBlockAreaTable(): IBlockAreaDAO

    abstract fun getModPresetTable(): IModPresetDAO
}