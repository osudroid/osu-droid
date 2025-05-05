package com.osudroid.beatmaps

import android.util.Log
import androidx.preference.PreferenceManager
import com.osudroid.ui.v1.LoadingBadgeFragment
import com.osudroid.utils.async
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.utils.mainThread
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.beatmap.parser.BeatmapParser
import com.rian.osu.difficulty.calculator.DifficultyCalculator
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
    fun checkForOutdatedStarRatings() {
        stopCalculation()

        preferences.apply {
            if (getLong("starRatingVersion", 0) >= DifficultyCalculator.Companion.VERSION) {
                return
            }

            DatabaseManager.beatmapInfoTable.resetStarRatings()
            edit().putLong("starRatingVersion", DifficultyCalculator.Companion.VERSION).apply()
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
                                val msStartTime = System.currentTimeMillis()

                                BeatmapParser(beatmapInfo.path, this).use { parser ->

                                    val data = parser.parse(true)!!
                                    val newInfo =
                                        BeatmapInfo(data, beatmapInfo.dateImported, true, this)
                                    beatmapInfo.apply(newInfo)
                                    DatabaseManager.beatmapInfoTable.update(newInfo)
                                }

                                if (BuildConfig.DEBUG) {
                                    Log.i(
                                        "DifficultyCalculation",
                                        "Calculated difficulty for ${beatmapInfo.path}, took ${System.currentTimeMillis() - msStartTime}ms."
                                    )
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
    fun stopCalculation() {
        job?.cancel()
        job = null
    }

}