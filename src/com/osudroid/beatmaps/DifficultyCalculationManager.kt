package com.osudroid.beatmaps

import android.util.Log
import androidx.preference.PreferenceManager
import com.osudroid.ui.v1.LoadingBadgeFragment
import com.osudroid.utils.async
import com.osudroid.data.DatabaseManager
import com.osudroid.utils.mainThread
import com.osudroid.utils.stopAsync
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.beatmap.parser.BeatmapParser
import com.rian.osu.difficulty.BeatmapDifficultyCalculator
import com.rian.osu.difficulty.calculator.DroidDifficultyCalculator
import com.rian.osu.difficulty.calculator.StandardDifficultyCalculator
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

object DifficultyCalculationManager {


    private val mainActivity = GlobalManager.getInstance().mainActivity

    private val preferences
        get() = PreferenceManager.getDefaultSharedPreferences(mainActivity)


    private var job: Job? = null

    private var badge: LoadingBadgeFragment? = null


    @JvmStatic
    fun checkForOutdatedStarRatings(): CompletableFuture<Unit> = stopCalculation().thenApply {
        preferences.apply {
            val editor = edit()

            // Before per-star rating versioning, the version was stored in this key.
            // If both star ratings have been updated after this, this key is no longer needed and can be removed.
            val oldStarRatingVersion = getLong("starRatingVersion", 0)

            if (getLong("droidStarRatingVersion", oldStarRatingVersion) < DroidDifficultyCalculator.VERSION) {
                DatabaseManager.beatmapInfoTable.resetDroidStarRatings()
                editor.putLong("droidStarRatingVersion", DroidDifficultyCalculator.VERSION)
            }

            if (getLong("standardStarRatingVersion", oldStarRatingVersion) < StandardDifficultyCalculator.VERSION) {
                DatabaseManager.beatmapInfoTable.resetStandardStarRatings()
                editor.putLong("standardStarRatingVersion", StandardDifficultyCalculator.VERSION)
            }

            editor.apply()
        }
    }


    @JvmStatic
    fun calculateDifficulties() {
        if (job?.isActive == true) {
            return
        }

        val totalBeatmaps = LibraryManager.getLibrary().sumOf { it.beatmaps.size }
        val pendingBeatmaps = LibraryManager.getLibrary().flatMap { set -> set.beatmaps.filter { it.needsDifficultyCalculation } }

        if (pendingBeatmaps.isEmpty()) {
            return
        }

        mainThread {
            badge = LoadingBadgeFragment().apply {
                header = "Calculating beatmap difficulties..."
                message = "During this process, the game may suffer performance degradation."
                isIndeterminate = true
                show()
            }
        }

        job = async {
            val threadCount = ceil(Runtime.getRuntime().availableProcessors() / 2f).toInt()
            val threadPool = Executors.newFixedThreadPool(threadCount)

            var calculated = totalBeatmaps - pendingBeatmaps.size

            pendingBeatmaps.chunked(max(pendingBeatmaps.size / threadCount, 1))
                .fastForEach { chunk ->
                    ensureActive()

                    threadPool.submit {

                        // .indices to avoid creating an iterator.
                        for (i in chunk.indices) {
                            ensureActive()

                            val beatmapInfo = chunk[i]

                            try {
                                // The beatmap may already be calculated when the player selects the beatmap in song select.
                                if (beatmapInfo.needsDifficultyCalculation) {
                                    val msStartTime = System.currentTimeMillis()

                                    val beatmap = BeatmapParser(beatmapInfo.path, this).use { parser ->
                                        parser.parse(true)!!
                                    }

                                    beatmapInfo.apply(beatmap, this)

                                    if (beatmapInfo.droidStarRating == null) {
                                        val attributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(beatmap, scope = this)
                                        beatmapInfo.droidStarRating = attributes.starRating.toFloat()
                                    }

                                    if (beatmapInfo.standardStarRating == null) {
                                        val attributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmap, scope = this)
                                        beatmapInfo.standardStarRating = attributes.starRating.toFloat()
                                    }

                                    DatabaseManager.beatmapInfoTable.update(beatmapInfo)

                                    if (BuildConfig.DEBUG) {
                                        Log.i(
                                            "DifficultyCalculation",
                                            "Calculated difficulty for ${beatmapInfo.path}, took ${System.currentTimeMillis() - msStartTime}ms."
                                        )
                                    }
                                }

                                calculated++
                                mainThread {
                                    badge?.apply {
                                        isIndeterminate = false
                                        progress = calculated * 100 / totalBeatmaps
                                        header =
                                            "Calculating beatmap difficulties... (${progress}%)"
                                    }
                                }

                            } catch (e: Exception) {
                                if (e is CancellationException) {
                                    throw e
                                }

                                Log.e(
                                    "DifficultyCalculation",
                                    "Error while calculating difficulty.",
                                    e
                                )
                            }

                        }
                    }
                }

            threadPool.shutdown()
            try {
                threadPool.awaitTermination(1, TimeUnit.HOURS)

                mainThread {
                    badge?.dismiss()
                    badge = null
                }
                GlobalManager.getInstance().songMenu?.onDifficultyCalculationEnd()

            } catch (e: InterruptedException) {
                Log.e("DifficultyCalculation", "Failed while waiting for executor termination.", e)
            }

            job = null
        }
    }


    @JvmStatic
    fun stopCalculation(): CompletableFuture<Unit> = job.stopAsync().thenApply { job = null }
}