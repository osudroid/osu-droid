package com.reco1l.osu

import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.edlplan.ui.fragment.BaseFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.reco1l.osu.data.BeatmapInfo
import com.reco1l.osu.data.DatabaseManager
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.beatmap.parser.BeatmapParser
import com.rian.osu.difficulty.calculator.DifficultyCalculator
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import ru.nsu.ccfit.zuev.osuplus.R
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlinx.coroutines.*

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
            if (getLong("starRatingVersion", 0) >= DifficultyCalculator.VERSION) {
                return
            }

            DatabaseManager.beatmapInfoTable.resetStarRatings()
            edit().putLong("starRatingVersion", DifficultyCalculator.VERSION).apply()
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

            pendingBeatmaps.chunked(max(pendingBeatmaps.size / threadCount, 1)).fastForEach { chunk ->
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
                                val newInfo = BeatmapInfo(data, beatmapInfo.dateImported, true, this)
                                beatmapInfo.apply(newInfo)
                                DatabaseManager.beatmapInfoTable.update(newInfo)
                            }

                            if (BuildConfig.DEBUG) {
                                Log.i("DifficultyCalculation", "Calculated difficulty for ${beatmapInfo.path}, took ${System.currentTimeMillis() - msStartTime}ms.")
                            }

                            calculated++
                            mainThread {
                                badge?.apply {
                                    isIndeterminate = false
                                    progress = calculated * 100 / totalBeatmaps
                                    header = "Calculating beatmap difficulties... (${progress}%)"
                                }
                            }

                        } catch (e: Exception) {
                            if (e is CancellationException) {
                                throw e
                            }

                            Log.e("DifficultyCalculation", "Error while calculating difficulty.", e)
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


class LoadingBadgeFragment : BaseFragment() {

    override val layoutID = R.layout.loading_badge_fragment


    var progress = 0
        set(value) {
            field = value
            if (::progressView.isInitialized) {
                progressView.progress = value
            }
        }

    var isIndeterminate = true
        set(value) {
            field = value
            if (::progressView.isInitialized) {
                progressView.isIndeterminate = value
            }
        }

    var header = "Loading..."
        set(value) {
            field = value
            if (::textView.isInitialized) {
                textView.text = value
            }
        }

    var message = "Please wait..."
        set(value) {
            field = value
            if (::messageView.isInitialized) {
                messageView.text = value
                messageView.visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
            }
        }


    private lateinit var textView: TextView

    private lateinit var messageView: TextView

    private lateinit var progressView: CircularProgressIndicator


    override fun onLoadView() {
        progressView = findViewById(R.id.progress)!!
        messageView = findViewById(R.id.message)!!
        textView = findViewById(R.id.text)!!

        progressView.isIndeterminate = isIndeterminate
        progressView.progress = progress
        textView.text = header
    }

}