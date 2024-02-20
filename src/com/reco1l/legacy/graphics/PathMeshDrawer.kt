package com.reco1l.legacy.graphics

import com.edlplan.framework.math.FMath
import com.edlplan.framework.math.line.LinePath
import ru.nsu.ccfit.zuev.osu.RGBColor
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


    private var segments = mutableListOf<Line>()


    private fun addVertex(position: Vector3, color: Float)
    {
        if (index + 4 >= triangles.size)
            triangles = triangles.copyOf(triangles.size * 3 / 2 + 4)

        triangles[index++] = position.x
        triangles[index++] = position.y
        triangles[index++] = position.z
        triangles[index++] = color
    }

    private fun addSegmentQuads(segment: Line, segmentLeft: Line, segmentRight: Line)
    {
        // Each segment of the path is actually rendered as 2 quads, being split in half along the
        // approximating line.

        // On this line the depth is 1 instead of 0, which is done in order to properly handle
        // self-overlap using the depth buffer.
        val firstMiddlePoint = Vector3(segment.startPoint.x, segment.startPoint.y, 1f)
        val secondMiddlePoint = Vector3(segment.endPoint.x, segment.endPoint.y, 1f)

        // Each of the quads (mentioned above) is rendered as 2 triangles:

        // Outer quad, triangle 1
        addVertex(
            position = Vector3(segmentRight.endPoint.x, segmentRight.endPoint.y, 0f),
            color = outerColor
        )
        addVertex(
            position = Vector3(segmentRight.startPoint.x, segmentRight.startPoint.y, 0f),
            color = outerColor
        )
        addVertex(
            position = firstMiddlePoint,
            color = innerColor
        )

        // Outer quad, triangle 2
        addVertex(
            position = firstMiddlePoint,
            color = innerColor
        )
        addVertex(
            position = secondMiddlePoint,
            color = innerColor
        )
        addVertex(
            position = Vector3(segmentRight.endPoint.x, segmentRight.endPoint.y, 0f),
            color = outerColor
        )

        // Inner quad, triangle 1
        addVertex(
            position = firstMiddlePoint,
            color = innerColor
        )
        addVertex(
            position = secondMiddlePoint,
            color = innerColor
        )
        addVertex(
            position = Vector3(segmentLeft.endPoint.x, segmentLeft.endPoint.y, 0f),
            color = outerColor
        )

        // Inner quad, triangle 2
        addVertex(
            position = Vector3(segmentLeft.endPoint.x, segmentLeft.endPoint.y, 0f),
            color = outerColor
        )
        addVertex(
            position = Vector3(segmentLeft.startPoint.x, segmentLeft.startPoint.y, 0f),
            color = outerColor
        )
        addVertex(
            position = firstMiddlePoint,
            color = innerColor
        )
    }

    private fun addSegmentCaps(
        rawThetaDifference: Float,
        segmentLeft: Line,
        segmentRight: Line,
        previousSegmentLeft: Line,
        previousSegmentRight: Line
    )
    {
        val thetaDifference = if (abs(rawThetaDifference) > FMath.Pi)
            -sign(rawThetaDifference) * 2f * FMath.Pi + rawThetaDifference
        else
            rawThetaDifference

        if (thetaDifference == 0f)
            return

        val origin = (segmentLeft.startPoint + segmentRight.startPoint) / 2f

        // Use segment end points instead of calculating start/end via theta to guarantee that the
        // vertices have the exact same position as the quads, which prevents possible pixel gaps
        // during rasterization.
        var current = if (thetaDifference > 0f) previousSegmentRight.endPoint else previousSegmentLeft.endPoint
        val end = if (thetaDifference > 0f) segmentRight.startPoint else segmentLeft.startPoint

        val start = if (thetaDifference > 0f)
            Line(previousSegmentLeft.endPoint, previousSegmentRight.endPoint)
        else
            Line(previousSegmentRight.endPoint, previousSegmentLeft.endPoint)

        val initialTheta = start.theta
        val thetaStep = sign(thetaDifference) * FMath.Pi / MAXRES
        val stepCount = ceil(thetaDifference / thetaStep).toInt()


        fun pointOnCircle(angle: Float) = Vector3(cos(angle), sin(angle), 0f)

        for (i in 1 .. stepCount)
        {
            // Center point
            addVertex(
                position = Vector3(origin.x, origin.y, 1f),
                color = innerColor
            )

            // First outer point
            addVertex(
                position = Vector3(current.x, current.y, 0f),
                color = outerColor
            )

            current = if (i < stepCount)
                origin + pointOnCircle(initialTheta + i * thetaStep) * (radius / 2f)
            else
                end

            // Second outer point
            addVertex(
                position = Vector3(current.x, current.y, 0f),
                color = outerColor
            )
        }
    }


    fun drawToBuffer(

        path: LinePath,
        width: Float,
        inColor: Float,
        outColor: Float,
        calculateSegments: Boolean = true

    ): FloatArray
    {
        index = 0
        radius = width * 2f
        innerColor = inColor
        outerColor = outColor

        if (calculateSegments)
        {
            segments.clear()

            for (i in 0 until path.size() - 1)
            {
                val start = path.get(i).toVector3()
                val end = path.get(i + 1).toVector3()

                segments.add(Line(start, end))
            }
        }

        // The coordinate system here is flipped, "left" corresponds to positive angles (anti-clockwise)
        // and "right" corresponds to negative angles (clockwise).

        var previousSegmentLeft: Line? = null
        var previousSegmentRight: Line? = null

        for (i in 0 until segments.size)
        {
            val segment: Line = segments[i]

            var orthogonalDirection = segment.orthogonalDirection

            if (orthogonalDirection.x.isNaN() || orthogonalDirection.y.isNaN())
                orthogonalDirection = Vector3(0f, 1f, 0f)

            val segmentLeft = Line(
                segment.startPoint + orthogonalDirection * (radius / 2f),
                segment.endPoint + orthogonalDirection * (radius / 2f)
            )

            val segmentRight = Line(
                segment.startPoint - orthogonalDirection * (radius / 2f),
                segment.endPoint - orthogonalDirection * (radius / 2f)
            )

            addSegmentQuads(segment, segmentLeft, segmentRight)

            if (previousSegmentLeft != null && previousSegmentRight != null)
            {
                // Connection/filler caps between segment quads
                val thetaDifference = segment.theta - segments[i - 1].theta

                addSegmentCaps(
                    thetaDifference,
                    segmentLeft,
                    segmentRight,
                    previousSegmentLeft,
                    previousSegmentRight
                )
            }

            // Semi-circles are essentially 180 degree caps. So to create these caps, we can simply
            // "fake" a segment that's 180 degrees flipped. This works because we are taking advantage
            // of the fact that a path which makes a 180 degree bend would have a semi-circle cap.
            if (i == 0)
            {
                // Path start cap (semi-circle);
                val flippedLeft = Line(segmentRight.endPoint, segmentRight.startPoint)
                val flippedRight = Line(segmentLeft.endPoint, segmentLeft.startPoint)

                addSegmentCaps(
                    FMath.Pi,
                    segmentLeft,
                    segmentRight,
                    flippedLeft,
                    flippedRight
                )
            }

            if (i == segments.lastIndex)
            {
                // Path end cap (semi-circle)
                val flippedLeft = Line(segmentRight.endPoint, segmentRight.startPoint)
                val flippedRight = Line(segmentLeft.endPoint, segmentLeft.startPoint)

                addSegmentCaps(FMath.Pi, flippedLeft, flippedRight, segmentLeft, segmentRight)
            }

            previousSegmentLeft = segmentLeft
            previousSegmentRight = segmentRight
        }

        return triangles.copyOf(index + 1)
    }


    companion object
    {
        private const val MAXRES = 24
    }
}
