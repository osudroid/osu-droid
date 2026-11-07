package com.osudroid.replay

import org.junit.Assert
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

class ReplayMoveArrayTest {
    @Test
    fun `Test redundant move point overwrites previous point in place`() {
        val replay = Replay()
        val array = Replay.MoveArray(10)

        array.pushBack(replay, 0, 0f, 0f, TouchType.DOWN)
        array.pushBack(replay, 10, 10f, 10f, TouchType.MOVE)

        val previous = array.movements[1]

        // (20, 20) makes (10, 10) the midpoint of the last two points, so the previous point is redundant.
        array.pushBack(replay, 20, 20f, 20f, TouchType.MOVE)

        Assert.assertEquals(2, array.size)
        Assert.assertSame(previous, array.movements[1])
        Assert.assertEquals(20, previous.time)
        Assert.assertEquals(20f, previous.x, 0f)
        Assert.assertEquals(20f, previous.y, 0f)
        Assert.assertEquals(TouchType.MOVE, previous.touchType)
    }

    @Test
    fun `Test non-redundant move point is appended`() {
        val replay = Replay()
        val array = Replay.MoveArray(10)

        array.pushBack(replay, 0, 0f, 0f, TouchType.DOWN)
        array.pushBack(replay, 10, 10f, 10f, TouchType.MOVE)
        array.pushBack(replay, 20, 100f, 0f, TouchType.MOVE)

        Assert.assertEquals(3, array.size)
        Assert.assertNotSame(array.movements[1], array.movements[2])
        Assert.assertEquals(10f, array.movements[1].x, 0f)
        Assert.assertEquals(100f, array.movements[2].x, 0f)
    }

    @Test
    fun `Test reserving movement capacity grows but never shrinks buffers`() {
        val replay = Replay(true)

        replay.reserveMovementCapacity(100f)

        Assert.assertEquals(6000, replay.cursorMoves[0].allocated)
        Assert.assertEquals(2000, replay.cursorMoves[1].allocated)
        // Other cursors keep their default capacity.
        Assert.assertEquals(15, replay.cursorMoves[2].allocated)

        replay.reserveMovementCapacity(1f)

        Assert.assertEquals(6000, replay.cursorMoves[0].allocated)
        Assert.assertEquals(2000, replay.cursorMoves[1].allocated)
    }

    @Test
    fun `Test reserving movement capacity is capped for very long durations`() {
        val replay = Replay(true)

        replay.reserveMovementCapacity(Float.MAX_VALUE)

        Assert.assertEquals(Replay.MAX_RESERVED_MOVEMENT_CAPACITY, replay.cursorMoves[0].allocated)
        Assert.assertEquals(Replay.MAX_RESERVED_MOVEMENT_CAPACITY, replay.cursorMoves[1].allocated)
    }
}
