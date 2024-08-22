package com.reco1l.osu

import android.util.Log
import com.reco1l.osu.data.BeatmapInfo
import com.reco1l.osu.data.DatabaseManager
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.beatmap.parser.BeatmapParser
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

object DifficultyCalculationManager {


    private var isRunning = false


    @JvmStatic
    fun calculateDifficulties() {

        if (isRunning) {
            return
        }

        val beatmaps = LibraryManager.getLibrary().flatMap { set -> set.beatmaps.filter { it.needsDifficultyCalculation } }
        if (beatmaps.isEmpty()) {
            return
        }

        isRunning = true
        ToastLogger.showText("Running background difficulty calculation. Song select menu's sort order may not be accurate during this process.", true)

        object : Thread() {

            override fun run() {

                val threadCount = Runtime.getRuntime().availableProcessors()
                val threadPool = Executors.newFixedThreadPool(threadCount)

                beatmaps.chunked(max(beatmaps.size / threadCount, 1)).fastForEach { chunk ->

                    threadPool.submit {

                        chunk.fastForEach { beatmapInfo ->

                            try {
                                val msStartTime = System.currentTimeMillis()

                                BeatmapParser(beatmapInfo.path).use { parser ->

                                    val data = parser.parse(true)!!
                                    val newInfo = BeatmapInfo(data, beatmapInfo.parentPath, beatmapInfo.dateImported, beatmapInfo.path, true)
                                    beatmapInfo.apply(newInfo)

                                    DatabaseManager.beatmapInfoTable.update(newInfo)
                                }

                                if (BuildConfig.DEBUG) {
                                    Log.i("DifficultyCalculation", "Calculated difficulty for ${beatmapInfo.path}, took ${System.currentTimeMillis() - msStartTime}ms.")
                                }

                            } catch (e: Exception) {
                                Log.e("DifficultyCalculation", "Error while calculating difficulty.", e)
                            }

                        }
                    }
                }

                threadPool.shutdown()
                try {

                    if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
                        ToastLogger.showText("Background difficulty calculation has finished successfully.", true)
                    } else {
                        ToastLogger.showText("Something went wrong during background difficulty calculation.", true)
                    }

                    GlobalManager.getInstance().songMenu?.onDifficultyCalculationEnd()

                } catch (e: InterruptedException) {
                    Log.e("DifficultyCalculation", "Failed while waiting for executor termination.", e)
                }

                isRunning = false
            }

        }.start()
    }

}