package com.reco1l.osu

import android.util.Log
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.GameMode
import com.rian.osu.beatmap.parser.BeatmapParser
import com.rian.osu.difficulty.BeatmapDifficultyCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osuplus.BuildConfig

object DifficultyCalculationManager {


    private var isRunning = false


    @JvmStatic
    fun calculateDifficulties() {

        if (isRunning) {
            return
        }
        isRunning = true

        CoroutineScope(Dispatchers.Default).launch {

            val beatmapSets = DatabaseManager.beatmapTable.getBeatmapSetList()

            // No beatmaps difficulties pending to calculate.
            if (beatmapSets.isEmpty()) {
                isRunning = false
                return@launch
            }

            ToastLogger.showText("Running background difficulty calculation. song select menu's sort order may not be accurate during this process.", true)

            beatmapSets.fastForEach { beatmapSetInfo ->

                launch {
                    var shouldUpdate = false

                    beatmapSetInfo.beatmaps.fastForEach setLoop@{ beatmapInfo ->

                        if (beatmapInfo.droidStarRating != null && beatmapInfo.standardStarRating != null) {
                            return@setLoop
                        }
                        shouldUpdate = true

                        val startTime = System.currentTimeMillis()

                        BeatmapParser(beatmapInfo.path).use { parser ->

                            val data = parser.parse(true, GameMode.Droid)!!

                            val droidAttributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(data)
                            val standardAttributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(data)

                            beatmapInfo.droidStarRating = GameHelper.Round(droidAttributes.starRating, 2)
                            beatmapInfo.standardStarRating = GameHelper.Round(standardAttributes.starRating, 2)
                            beatmapInfo.hitCircleCount = data.hitObjects.circleCount
                            beatmapInfo.sliderCount = data.hitObjects.sliderCount
                            beatmapInfo.spinnerCount = data.hitObjects.spinnerCount
                            beatmapInfo.maxCombo = data.maxCombo
                        }

                        if (BuildConfig.DEBUG) {
                            Log.i("DifficultyCalculation", "Calculated difficulty for ${beatmapInfo.path} in ${System.currentTimeMillis() - startTime}ms.")
                        }
                    }

                    if (shouldUpdate) {
                        DatabaseManager.beatmapTable.insertAll(beatmapSetInfo.beatmaps)
                    }

                }

            }

        }.invokeOnCompletion {
            LibraryManager.loadLibrary()
            ToastLogger.showText("Background difficulty calculation has finished.", true)

            GlobalManager.getInstance().songMenu?.also {

                if (GlobalManager.getInstance().engine.scene == it.scene) {
                    it.onDifficultyCalculationEnd()
                }
            }

        }

    }

}