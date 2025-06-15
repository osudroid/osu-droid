package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Vector2
import kotlin.math.max
import org.junit.Assert
import org.junit.Test

class SliderPathTest {
    @Test
    fun `Test bezier path without stopping points`() {
        parsePath("387,350,73381,6,0,B|262:394|299:299|153:344,1,215.999993408203,10|0,2:0|3:0,2:0:0:0:").apply {
            Assert.assertEquals(cumulativeLength.last(), expectedDistance, 1e-2)
            testPathValidity(this)
        }
    }

    @Test
    fun `Test bezier path with stopping points`() {
        parsePath("45,352,22787,6,0,B|-27:363|-27:363|3:269,1,170.999994781494,4|10,1:2|2:0,2:0:0:0:").apply {
            Assert.assertEquals(cumulativeLength.last(), expectedDistance, 1e-2)
            Assert.assertTrue(Vector2(-72, 11) in calculatedPath)
            testPathValidity(this)
        }
    }

    @Test
    fun `Test linear path`() {
        parsePath("36,53,24587,2,0,L|102:42,1,56.9999982604981,10|0,1:2|2:0,2:0:0:0:").apply {
            Assert.assertEquals(cumulativeLength.last(), expectedDistance, 1e-2)
            testPathValidity(this)
        }
    }

    @Test
    fun `Test perfect curve path with 3 anchor points`() {
        parsePath("117,124,25187,6,0,P|167:148|196:196,1,113.999996520996,4|2,1:2|2:0,2:0:0:0:").apply {
            Assert.assertEquals(cumulativeLength.last(), expectedDistance, 1e-2)
            testPathValidity(this)
        }
    }

    @Test
    fun `Test perfect curve path with more than 3 anchor points`() {
        parsePath("117,124,25187,6,0,P|167:148|196:196|225:225,1,113.999996520996,4|2,1:2|2:0,2:0:0:0:").apply {
            Assert.assertEquals(cumulativeLength.last(), expectedDistance, 1e-2)
            testPathValidity(this)
        }
    }

    @Test
    fun `Test catmull path`() {
        parsePath("416,320,11119,6,0,C|416:320|128:320,1,280").apply {
            Assert.assertEquals(cumulativeLength.last(), expectedDistance, 1e-2)
            testPathValidity(this)
        }
    }

    @Test
    fun `Test catmull path with two equal last anchor points`() {
        parsePath("416,320,11119,6,0,C|416:320|128:320|128:320,1,300").apply {
            Assert.assertEquals(cumulativeLength.last(), 288.0, 1e-2)
            testPathValidity(this)
        }
    }

    @Test
    fun `Test negative length path`() {
        SliderPath(SliderPathType.Linear, listOf(Vector2(0), Vector2(0)), -1.0).apply {
            Assert.assertEquals(calculatedPath.size, 1)
            testPathValidity(this)
        }
    }

    @Test
    fun `Test positionAt without control points`() {
        SliderPath(SliderPathType.Linear, listOf(), 0.0).apply {
            Assert.assertEquals(positionAt(0.0), Vector2(0))
        }
    }

    @Test
    fun `Test positionAt with control points`() {
        SliderPath(SliderPathType.Linear, listOf(Vector2(0), Vector2(100, 0)), 100.0).apply {
            Assert.assertEquals(positionAt(0.0).x, 0f, 1e-2f)
            Assert.assertEquals(positionAt(0.1).x, 10f, 1e-2f)
            Assert.assertEquals(positionAt(0.2).x, 20f, 1e-2f)
            Assert.assertEquals(positionAt(0.25).x, 25f, 1e-2f)
            Assert.assertEquals(positionAt(0.5).x, 50f, 1e-2f)
            Assert.assertEquals(positionAt(0.75).x, 75f, 1e-2f)
            Assert.assertEquals(positionAt(1.0).x, 100f, 1e-2f)
        }
    }

    @Test
    fun `Test positionAt with extremely close control points`() {
        SliderPath(SliderPathType.Linear, listOf(Vector2(0), Vector2(1e-3f, 0f)), 1e-3).apply {
            Assert.assertEquals(positionAt(0.0).x, 0f, 1e-2f)
            Assert.assertEquals(positionAt(0.5).x, 0.0005f, 1e-2f)
            Assert.assertEquals(positionAt(1.0).x, 0.001f, 1e-2f)
        }
    }

    private fun parsePath(str: String): SliderPath {
        val parts = str.split(',')
        val position = Vector2(parts[0].toFloat(), parts[1].toFloat())
        val anchorPoints = parts[5].split('|')

        val pathType = when (anchorPoints[0]) {
            "B" -> SliderPathType.Bezier
            "L" -> SliderPathType.Linear
            "P" -> SliderPathType.PerfectCurve
            "C" -> SliderPathType.Catmull
            else -> throw IllegalArgumentException("Unknown path type")
        }

        val controlPoints = mutableListOf(Vector2(0))

        for (point in anchorPoints.slice(1 until anchorPoints.size)) {
            val coords = point.split(':')
            controlPoints.add(Vector2(coords[0].toFloat(), coords[1].toFloat()) - position)
        }

        // A special case for old beatmaps where the first
        // control point is in the position of the slider.
        if (controlPoints.size >= 2 && controlPoints[0] == controlPoints[1]) {
            controlPoints.removeAt(0)
        }

        return SliderPath(pathType, controlPoints, max(0.0, parts[7].toDouble()))
    }

    private fun testPathValidity(path: SliderPath) {
        Assert.assertEquals(path.calculatedPath.size, path.cumulativeLength.size)
        Assert.assertFalse(path.expectedDistance.isNaN())

        for (i in path.calculatedPath.indices) {
            val point = path.calculatedPath[i]
            val length = path.cumulativeLength[i]

            Assert.assertFalse(point.x.isNaN())
            Assert.assertFalse(point.y.isNaN())
            Assert.assertTrue(point.x.isFinite())
            Assert.assertTrue(point.y.isFinite())

            Assert.assertFalse(length.isNaN())
        }
    }
}