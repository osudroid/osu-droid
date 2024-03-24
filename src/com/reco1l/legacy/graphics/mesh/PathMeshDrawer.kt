package com.reco1l.legacy.graphics.mesh

import com.edlplan.framework.math.FMath
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.math.line.LinePath
import com.reco1l.legacy.math.Vectors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

class PathMeshDrawer
{

    private var triangles = FloatArray(4)

    private var innerColor = 0f

    private var outerColor = 0f

    private var radius = 0f

    private var index = 0

    private var flat = false


    private fun addVertex(
        position: Vec2,
        depth: Float,
        color: Float
    )
    {
        if (index + (if (flat) 3 else 4) >= triangles.size)
            triangles = triangles.copyOf(triangles.size * 3 / 2 + (if (flat) 3 else 4))

        triangles[index++] = position.x
        triangles[index++] = position.y

        if (!flat)
            triangles[index++] = depth

        triangles[index++] = color
    }

    private fun addSegmentQuads(
        start: Vec2,
        end: Vec2,
        segmentLeftStart: Vec2,
        segmentLeftEnd: Vec2,
        segmentRightStart: Vec2,
        segmentRightEnd: Vec2
    )
    {
        // Each segment of the path is actually rendered as 2 quads, being split in half along the
        // approximating line. Each of the quads is rendered as 2 triangles.

        // Some vertices have a depth of 1 instead of 0, which is done in order to properly handle
        // self-overlap using the depth buffer.

        // Outer quad, triangle 1
        addVertex(
            position = segmentRightEnd, depth = 0f,
            color = outerColor
        )
        addVertex(
            position = segmentRightStart, depth = 0f,
            color = outerColor
        )
        addVertex(
            position = start, depth = 1f,
            color = innerColor
        )

        // Outer quad, triangle 2
        addVertex(
            position = start, depth = 1f,
            color = innerColor
        )
        addVertex(
            position = end, depth = 1f,
            color = innerColor
        )
        addVertex(
            position = segmentRightEnd, depth = 0f,
            color = outerColor
        )

        // Inner quad, triangle 1
        addVertex(
            position = start, depth = 1f,
            color = innerColor
        )
        addVertex(
            position = end, depth = 1f,
            color = innerColor
        )
        addVertex(
            position = segmentLeftEnd, depth = 0f,
            color = outerColor
        )

        // Inner quad, triangle 2
        addVertex(
            position = segmentLeftEnd, depth = 0f,
            color = outerColor
        )
        addVertex(
            position = segmentLeftStart, depth = 0f,
            color = outerColor
        )
        addVertex(
            position = start, depth = 1f,
            color = innerColor
        )
    }

    private fun addSegmentCaps(
        rawThetaDifference: Float,
        segmentLeftStart: Vec2,
        segmentRightStart: Vec2,
        previousSegmentLeftEnd: Vec2,
        previousSegmentRightEnd: Vec2,
    )
    {
        val thetaDifference = if (abs(rawThetaDifference) > FMath.Pi)
            -sign(rawThetaDifference) * 2f * FMath.Pi + rawThetaDifference
        else
            rawThetaDifference

        if (thetaDifference == 0f)
            return

        val origin = Vec2(
            (segmentLeftStart.x + segmentRightStart.x) / 2f,
            (segmentLeftStart.y + segmentRightStart.y) / 2f
        )

        // Use segment end points instead of calculating start/end via theta to guarantee that the
        // vertices have the exact same position as the quads, which prevents possible pixel gaps.
        var current = if (thetaDifference > 0f)
            previousSegmentRightEnd
        else
            previousSegmentLeftEnd


        val end = if (thetaDifference > 0f)
            segmentRightStart
        else
            segmentLeftStart


        val initialTheta = if (thetaDifference > 0f)
            Vectors.getTheta(previousSegmentLeftEnd, previousSegmentRightEnd)
        else
            Vectors.getTheta(previousSegmentRightEnd, previousSegmentLeftEnd)


        val thetaStep = sign(thetaDifference) * FMath.Pi / 24f // MAX_RES
        val stepCount = ceil(thetaDifference / thetaStep).toInt()


        for (i in 1 .. stepCount)
        {
            // Center point
            addVertex(
                position = origin, depth = 1f,
                color = innerColor
            )

            // First outer point
            addVertex(
                position = current, depth = 0f,
                color = outerColor
            )

            current = if (i < stepCount)
            {
                // Point on circle: cos(x) and sin(x).
                val angle = initialTheta + i * thetaStep

                Vec2(
                    origin.x + cos(angle) * radius,
                    origin.y + sin(angle) * radius
                )
            }
            else end

            // Second outer point
            addVertex(
                position = current, depth = 0f,
                color = outerColor
            )
        }
    }


    fun drawToBuffer(

        path: LinePath,
        width: Float,
        inColor: Float,
        outColor: Float,
        asFlat: Boolean,

    ): FloatArray
    {
        index = 0
        flat = asFlat
        radius = width
        innerColor = inColor
        outerColor = outColor

        // The coordinate system here is flipped, "left" corresponds to positive angles (anti-clockwise)
        // and "right" corresponds to negative angles (clockwise).

        var previousSegmentLeftEnd: Vec2? = null
        var previousSegmentRightEnd: Vec2? = null

        for (i in 0 until path.size() - 1)
        {
            val segmentStart = path.get(i)
            val segmentEnd = path.get(i + 1)

            val orthogonal = Vectors.getOrthogonalDirection(segmentStart, segmentEnd)

            if (orthogonal.x.isNaN() || orthogonal.y.isNaN())
            {
                orthogonal.x = 0f
                orthogonal.y = 1f
            }
            orthogonal.x *= radius
            orthogonal.y *= radius

            val segmentLeftStart = Vec2(
                segmentStart.x + orthogonal.x,
                segmentStart.y + orthogonal.y
            )

            val segmentLeftEnd = Vec2(
                segmentEnd.x + orthogonal.x,
                segmentEnd.y + orthogonal.y
            )

            val segmentRightStart = Vec2(
                segmentStart.x - orthogonal.x,
                segmentStart.y - orthogonal.y
            )

            val segmentRightEnd = Vec2(
                segmentEnd.x - orthogonal.x,
                segmentEnd.y - orthogonal.y
            )

            addSegmentQuads(
                segmentStart,
                segmentEnd,
                segmentLeftStart,
                segmentLeftEnd,
                segmentRightStart,
                segmentRightEnd
            )

            if (previousSegmentLeftEnd != null && previousSegmentRightEnd != null)
            {
                val previousStart = path.get(i - 1)
                val previousEnd = path.get(i)

                // Connection/filler caps between segment quads
                val thetaDifference = Vectors.getTheta(segmentStart, segmentEnd) - Vectors.getTheta(previousStart, previousEnd)

                addSegmentCaps(
                    thetaDifference,
                    segmentLeftStart,
                    segmentRightStart,
                    previousSegmentLeftEnd,
                    previousSegmentRightEnd
                )
            }

            // Semi-circles are essentially 180 degree caps. So to create these caps, we can simply
            // "fake" a segment that's 180 degrees flipped. This works because we are taking advantage
            // of the fact that a path which makes a 180 degree bend would have a semi-circle cap.
            if (i == 0)
            {
                addSegmentCaps(
                    FMath.Pi,

                    segmentLeftStart,
                    segmentRightStart,

                    // Path start cap (semi-circle), flipped segment.
                    segmentRightStart,
                    segmentLeftStart,
                )
            }

            // When is the last segment.
            if (i == path.size() - 2)
            {
                addSegmentCaps(
                    FMath.Pi,

                    // Path end cap (semi-circle), flipped segment.
                    segmentRightEnd,
                    segmentLeftEnd,

                    segmentLeftEnd,
                    segmentRightEnd
                )
            }

            previousSegmentLeftEnd = segmentLeftEnd
            previousSegmentRightEnd = segmentRightEnd
        }

        return triangles.copyOf(index + 1)
    }

}
