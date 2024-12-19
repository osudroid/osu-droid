package com.rian.spectator

import android.util.Log
import com.reco1l.ibancho.RoomAPI
import com.rian.osu.beatmap.hitobject.HitCircle
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osu.scoring.TouchType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import ru.nsu.ccfit.zuev.osu.game.GameHelper

/**
 * Holds spectator data that will be sent to the server periodically.
 */
class SpectatorDataManager(
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

    private var beginningCursorMoveIndexes = IntArray(GameScene.getCursorCount())
    private val endCursorMoveIndexes = IntArray(GameScene.getCursorCount())

    private val submissionTimer = Timer()
    private val submissionPeriod = 5000L
    private var isPaused = false
    private var gameEnded = false

    private val task = object : TimerTask() {
        override fun run() {
            val secPassed = gameScene.elapsedTime
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

                    val submitted = RoomAPI.submitSpectatorData(byteArrayOutputStream.toByteArray())

                    close()
                    byteArrayOutputStream.close()

                    postDataSend(submitted, gameHasEnded)
                }
            } catch (e: IOException) {
                Log.e("SpectatorDataManager", "IOException: " + e.message, e)
                postDataSend(false)
            } catch (e: OnlineManagerException) {
                Log.e("SpectatorDataManager", "OnlineManagerException: " + e.message, e)
                postDataSend(false)
            } finally {
                try {
                    byteArrayOutputStream.flush()
                    byteArrayOutputStream.close()
                } catch (e: IOException) {
                    Log.e("SpectatorDataManager", "IOException: " + e.message, e)
                }
            }
        }

        private fun postDataSend(submitSuccess: Boolean, gameHasEnded: Boolean = false) {
            if (gameHasEnded) {
                cancel()
                gameScene.stopSpectatorDataSubmission()
                return
            }

            if (submitSuccess) {
                beginningCursorMoveIndexes = endCursorMoveIndexes.clone()
                beginningObjectDataIndex = endObjectDataIndex
                beginningEventIndex = endEventIndex
            }
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
        val obj = gameScene.beatmap.hitObjects.objects[objectId]
        val replayData = replay.objectData[objectId]
        var time = obj.endTime

        if (obj is HitCircle) {
            // Special handling for circles that are not tapped.
            time += (
                if (replayData.accuracy == 10000.toShort())
                    GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty()) * 1000
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
        events.add(SpectatorEvent(gameScene.elapsedTime * 1000, stat.totalScoreWithMultiplier, stat.combo, stat.accuracy))

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
}