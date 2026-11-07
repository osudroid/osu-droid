package com.osudroid.game.replay.analysis

import com.osudroid.utils.lastIndexAtOrBefore
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

/**
 * The recorded input events of a single pointer slot, stored as parallel arrays in ascending time order.
 *
 * A pointer slot is not a physical finger: Android reuses the lowest free pointer ID, so one slot may hold
 * several unrelated touch sequences, separated by [TouchType.UP] events.
 */
class ReplayAnalysisTrack(
    /**
     * The time of each event, in milliseconds.
     */
    val times: IntArray,

    /**
     * The x position of each event, in screen coordinates. Unused for [TouchType.UP] events.
     */
    val xs: FloatArray,

    /**
     * The y position of each event, in screen coordinates. Unused for [TouchType.UP] events.
     */
    val ys: FloatArray,

    /**
     * The [TouchType] ID of each event.
     */
    val types: ByteArray
) {
    /**
     * The number of events in this [ReplayAnalysisTrack].
     */
    val size
        get() = times.size
}

/**
 * Input data of a replay, pre-processed for replay analysis.
 */
class ReplayAnalysisData(
    /**
     * The [ReplayAnalysisTrack]s of this replay, one per pointer slot.
     */
    val tracks: List<ReplayAnalysisTrack>
) {
    /**
     * The time of each tap, in milliseconds, in ascending order.
     */
    val tapTimes: IntArray

    /**
     * The X position of each tap, in screen coordinates.
     */
    val tapXs: FloatArray

    /**
     * The Y position of each tap, in screen coordinates.
     */
    val tapYs: FloatArray

    /**
     * Whether each tap is direct (a pointer went down at the tap's position) or induced (another pointer was held
     * at the tap's position while a pointer went down).
     */
    val tapIsDirect: BooleanArray

    /**
     * The number of taps in this replay.
     */
    val tapCount
        get() = tapTimes.size

    init {
        val taps = mutableListOf<Tap>()

        for (i in tracks.indices) {
            val track = tracks[i]

            for (k in 0 until track.size) {
                if (track.types[k] != TouchType.DOWN.id) {
                    continue
                }

                val time = track.times[k]
                taps += Tap(time, track.xs[k], track.ys[k], true)

                // A pointer going down also hits at the position of every other held pointer. This mirrors
                // GameObject.getHittingCursor, which checks each other cursor's latest event at or before the
                // down event's time and skips it if that event is an UP.
                for (j in tracks.indices) {
                    if (i == j) {
                        continue
                    }

                    val other = tracks[j]
                    val index = other.times.lastIndexAtOrBefore(time)

                    if (index >= 0 && other.types[index] != TouchType.UP.id) {
                        taps += Tap(time, other.xs[index], other.ys[index], false)
                    }
                }
            }
        }

        // sortedBy is stable, so taps at the same time keep their pointer order.
        val sortedTaps = taps.sortedBy { it.time }

        tapTimes = IntArray(sortedTaps.size) { sortedTaps[it].time }
        tapXs = FloatArray(sortedTaps.size) { sortedTaps[it].x }
        tapYs = FloatArray(sortedTaps.size) { sortedTaps[it].y }
        tapIsDirect = BooleanArray(sortedTaps.size) { sortedTaps[it].isDirect }
    }

    private data class Tap(val time: Int, val x: Float, val y: Float, val isDirect: Boolean)

    companion object {
        /**
         * Creates a [ReplayAnalysisData] from the cursor movements of a replay.
         *
         * @param moveArrays The [Replay.MoveArray]s of the replay, one per pointer slot.
         * @return The created [ReplayAnalysisData].
         */
        @JvmStatic
        fun fromMoveArrays(moveArrays: List<Replay.MoveArray>) = ReplayAnalysisData(moveArrays.map { moves ->
            val size = moves.size
            val movements = moves.movements

            ReplayAnalysisTrack(
                IntArray(size) { movements[it].time },
                FloatArray(size) { movements[it].x },
                FloatArray(size) { movements[it].y },
                ByteArray(size) { movements[it].touchType.id }
            )
        })
    }
}
