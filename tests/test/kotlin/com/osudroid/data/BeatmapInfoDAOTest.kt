package com.osudroid.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BeatmapInfoDAOTest {
    private lateinit var database: DroidDatabase

    /**
     * The bind variable count of every `DELETE` statement executed against [database], in execution order.
     */
    private val deleteBindArgCounts = mutableListOf<Int>()

    private val dao
        get() = database.getBeatmapInfoTable()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            DroidDatabase::class.java
        ).allowMainThreadQueries().setQueryCallback(
            { sql, bindArgs ->
                if (sql.startsWith("DELETE")) {
                    synchronized(deleteBindArgCounts) { deleteBindArgCounts += bindArgs.size }
                }
            },
            // Run the callback on the calling thread so that captures are complete once a DAO call returns.
            { it.run() }
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `Test deleteAllBeatmapSets keeps every statement within the SQLite bind variable limit`() {
        // Android 11 (API 30) and older cap a single statement at 999 bind variables, which `IN (:directories)` hits
        // when many beatmap sets were removed from the file system while the app was closed.
        //
        // This is checked by counting bind variables per statement rather than by letting SQLite reject the
        // statement, because Robolectric's SQLite limit depends on the host OS (999 on Windows, 32766 elsewhere) and
        // matches neither every device nor CI. The limit is hardcoded instead of referencing the production chunk
        // size so that this test fails if the chunk size is raised past it.
        val deviceBindVariableLimit = 999
        val staleDirectories = (0 until 2_500).map { "stale-$it" }

        // Only some directories get a row. The limit applies to the bind variables in the DELETE statement, not the
        // number of rows, and spacing the rows 100 apart places at least one row in every chunk.
        val stale = staleDirectories.filterIndexed { i, _ -> i % 100 == 0 }.map { createBeatmapInfo(it) }
        val kept = (0 until 3).map { createBeatmapInfo("kept-$it") }

        dao.insertAll(stale + kept)
        dao.deleteAllBeatmapSets(staleDirectories)

        val counts = synchronized(deleteBindArgCounts) { deleteBindArgCounts.toList() }

        assertTrue("Expected the directories to be split into multiple statements, got $counts", counts.size > 1)

        assertTrue(
            "Expected every statement to have at most $deviceBindVariableLimit bind variables, got $counts",
            counts.all { it <= deviceBindVariableLimit }
        )

        assertEquals("Expected every directory to be bound exactly once", staleDirectories.size, counts.sum())

        assertEquals(kept.map { it.setDirectory }.sorted(), dao.getBeatmapSetPaths().sorted())
    }

    @Test
    fun `Test deleteAllBeatmapSets with an empty list`() {
        val beatmaps = (0 until 3).map { createBeatmapInfo("kept-$it") }

        dao.insertAll(beatmaps)

        dao.deleteAllBeatmapSets(emptyList())

        assertTrue("Expected no DELETE statement for an empty list", deleteBindArgCounts.isEmpty())
        assertEquals(beatmaps.map { it.setDirectory }.sorted(), dao.getBeatmapSetPaths().sorted())
    }

    private fun createBeatmapInfo(setDirectory: String) = BeatmapInfo(
        filename = "map.osu",
        md5 = "md5-$setDirectory",
        id = null,
        audioFilename = "audio.mp3",
        backgroundFilename = null,
        status = null,
        setDirectory = setDirectory,
        setId = null,
        title = "",
        titleUnicode = "",
        artist = "",
        artistUnicode = "",
        creator = "",
        version = "",
        tags = "",
        source = "",
        dateImported = 0L,
        approachRate = 9f,
        overallDifficulty = 8f,
        circleSize = 4f,
        hpDrainRate = 6f,
        droidStarRating = null,
        standardStarRating = null,
        bpmMax = 60f,
        bpmMin = 60f,
        mostCommonBPM = 60f,
        length = 0L,
        previewTime = 0,
        hitCircleCount = 0,
        spinnerCount = 0,
        sliderCount = 0,
        maxCombo = 0,
        epilepsyWarning = false
    )
}