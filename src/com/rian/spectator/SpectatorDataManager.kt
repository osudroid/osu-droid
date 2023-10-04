package com.rian.spectator

import android.util.Log
import com.rian.difficultycalculator.beatmap.hitobject.HitCircle
import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osu.scoring.TouchType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

/**
 * Holds spectator data that will be sent to the server periodically.
 */
class SpectatorDataManager(
    private val roomId: Long,
    private val gameScene: GameScene,
    private val replay: Replay,
    private val stat: StatisticV2
) {
    private val objectData = mutableListOf<SpectatorObjectData>()
    private var beginningObjectDataIndex = 0
    private var endObjectDataIndex = 0

    private val events = mutableListOf<SpectatorEvent>()
    private var beginningEventIndex = 0
    private var endEventIndex = 0

    private var beginningCursorMoveIndexes = IntArray(GameScene.CursorCount)
    private val endCursorMoveIndexes = IntArray(GameScene.CursorCount)

    private val submissionTimer = Timer()
    private val submissionPeriod = 5000L
    private var isPaused = false
    private var gameEnded = false

    private val task = object : TimerTask() {
        override fun run() {
            val secPassed = gameScene.secPassed
            val gameHasEnded = gameEnded
            val byteArrayOutputStream = ByteArrayOutputStream()

            try {
                DataOutputStream(byteArrayOutputStream).apply {
                    for (i in endCursorMoveIndexes.indices) {
                        endCursorMoveIndexes[i] = replay.cursorMoves[i].size
                    }

                    endObjectDataIndex = objectData.size
                    endEventIndex = events.size

                    writeFloat(secPassed)
                    writeInt(stat.totalScoreWithMultiplier)
                    writeInt(stat.combo)
                    writeFloat(stat.getAccuracy())
                    writeInt(stat.hit300)
                    writeInt(stat.hit100)
                    writeInt(stat.hit50)
                    writeInt(stat.misses)

                    writeInt(replay.cursorMoves.size)
                    for (i in beginningCursorMoveIndexes.indices) {
                        val move = replay.cursorMoves[i]
                        val beginningIndex = beginningCursorMoveIndexes[i]
                        val endIndex = endCursorMoveIndexes[i]

                        writeInt(endIndex - beginningIndex)

                        for (j in beginningIndex until endIndex) {
                            val movement = move.movements[j]
                            writeInt((movement.time shl 2) + movement.touchType.id)

                            if (movement.touchType != TouchType.UP) {
                                writeFloat(movement.point.x * Config.getTextureQuality())
                                writeFloat(movement.point.y * Config.getTextureQuality())
                            }
                        }
                    }

                    writeInt(endObjectDataIndex - beginningObjectDataIndex)
                    for (i in beginningObjectDataIndex until endObjectDataIndex) {
                        val objData = objectData[i]
                        val data = objData.data

                        writeInt(i)
                        writeDouble(objData.time)
                        writeShort(data.accuracy.toInt())

                        if (data.tickSet == null || data.tickSet.length() == 0) {
                            writeByte(0)
                        } else {
                            val bytes = ByteArray((data.tickSet.length() + 7) / 8)
                            for (j in 0 until data.tickSet.length()) {
                                if (data.tickSet[j]) {
                                    bytes[bytes.size - j / 8 - 1] =
                                        (bytes[bytes.size - j / 8 - 1].toInt() or (1 shl j % 8)).toByte()
                                }
                            }
                            writeByte(bytes.size)
                            write(bytes)
                        }

                        writeByte(data.result.toInt())
                    }

                    writeInt(endEventIndex - beginningEventIndex)
                    for (i in beginningEventIndex until endEventIndex) {
                        val event = events[i]

                        writeFloat(event.time)
                        writeInt(event.score)
                        writeInt(event.combo)
                        writeFloat(event.accuracy)
                    }

                    flush()
                    byteArrayOutputStream.flush()

                    val message = OnlineManager.getInstance().sendSpectatorData(
                        roomId,
                        byteArrayOutputStream.toByteArray()
                    )

                    close()
                    byteArrayOutputStream.close()

                    if (message == "FAILED") {
                        gameScene.stopSpectatorDataSubmission()
                        return
                    }

                    postDataSend(message == "SUCCESS", gameHasEnded)
                }
            } catch (e: IOException) {
                Log.e("SpectatorDataManager", "IOException: " + e.message, e)
                postDataSend(success = false, gameHasEnded = false)
            } catch (e: OnlineManagerException) {
                Log.e("SpectatorDataManager", "OnlineManagerException: " + e.message, e)
                postDataSend(success = false, gameHasEnded = false)
            } finally {
                try {
                    byteArrayOutputStream.flush()
                    byteArrayOutputStream.close()
                } catch (e: IOException) {
                    Log.e("SpectatorDataManager", "IOException: " + e.message, e)
                }
            }
        }

        private fun postDataSend(success: Boolean, gameHasEnded: Boolean) {
            if (!success) {
                return
            }

            if (gameHasEnded) {
                cancel()
                gameScene.stopSpectatorDataSubmission()
                return
            }

            beginningCursorMoveIndexes = endCursorMoveIndexes.clone()
            beginningObjectDataIndex = endObjectDataIndex
            beginningEventIndex = endEventIndex
        }
    }

    init {
        submissionTimer.scheduleAtFixedRate(
            task,
            submissionPeriod,
            submissionPeriod
        )
    }

    /**
     * Adds a hit object data.
     *
     * @param objectId The ID of the object.
     */
    fun addObjectData(objectId: Int) {
        val obj = gameScene.beatmapData.hitObjects.objects[objectId]
        val replayData = replay.objectData[objectId]

        var time = obj.endTime
        if (obj is HitCircle) {
            // Special handling for circles that are not tapped.
            time += (
                if (replayData.accuracy == 10000.toShort())
                    gameScene.difficultyHelper.hitWindowFor50(gameScene.overallDifficulty) * 1000
                else replayData.accuracy
            ).toDouble()
        }

        objectData.add(SpectatorObjectData(time, replayData))
    }

    /**
     * Resumes the timer after a specified delay.
     *
     * @param delay The delay.
     */
    fun resumeTimer(delay: Long) {
        if (!isPaused) {
            return
        }

        submissionTimer.scheduleAtFixedRate(task, delay, submissionPeriod)
        isPaused = false
    }

    /**
     * Adds a spectator event.
     */
    fun addEvent() =
        events.add(SpectatorEvent(gameScene.secPassed * 1000, stat.totalScoreWithMultiplier, stat.combo, stat.accuracy))

    /**
     * Pauses the timer.
     */
    fun pauseTimer() {
        if (isPaused) {
            return
        }

        submissionTimer.cancel()
        isPaused = true
    }

    fun setGameEnded(gameEnded: Boolean) {
        this.gameEnded = gameEnded
    }

    @Throws(Throwable::class)
    protected fun finalize() = pauseTimer()

    companion object {
        const val SPECTATOR_DATA_VERSION = 3
    }
}

val HitObject.endTime: Double
    get() = if (this is HitObjectWithDuration) this.endTime else this.startTime