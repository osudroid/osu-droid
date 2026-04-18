package com.edlplan.osu.support.slider;

import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.AbstractPath;
import com.edlplan.framework.math.line.LinePath;
import com.reco1l.andengine.component.ClearInfo;
import com.reco1l.andengine.component.DepthInfo;
import com.reco1l.andengine.shape.UITriangleMesh;
import com.reco1l.andengine.container.UIContainer;
import com.reco1l.framework.Color4;
import com.rian.osu.math.Vector2;

import java.util.Arrays;

public class SliderBody extends UIContainer {

    private RenderPathCache cache;
    private static final float lengthUpdateEpsilon = 0.001f;
    private static final float pointMergeDistance = 0.01f;

    private float maxPathLength;

    private final UITriangleMesh background;

    private final UITriangleMesh border;

    private final UITriangleMesh hint;

    private float backgroundWidth;

    private float borderWidth;

    private float hintWidth;

    private float startLength = 0;

    private float endLength = 0;

    private boolean shouldRebuildVertices = true;

    // Reused temporary vectors for the end-only hybrid fast path.
    private final Vec2 tmpOthExpand = new Vec2();
    private final Vec2 tmpStartL = new Vec2();
    private final Vec2 tmpStartR = new Vec2();
    private final Vec2 tmpEndL = new Vec2();
    private final Vec2 tmpEndR = new Vec2();
    private final Vec2 tmpCapCurrent = new Vec2();
    private final Vec2 tmpCapCurrent2 = new Vec2();
    private final Vec2 tmpCutPoint = new Vec2();

    // Tracks monotonic boundary movement during snaking to reduce segment lookups.
    private int lastFastEndSegmentIndex = -1;
    private int lastFastStartSegmentIndex = -1;
    private float lastFastEndLength = Float.NaN;
    private float lastFastStartLength = Float.NaN;

    public SliderBody(boolean allowHint) {

        if (allowHint) {
            hint = new UITriangleMesh();
            hint.setVisible(false);
            hint.setClearInfo(ClearInfo.ClearDepthBuffer);
            hint.setDepthInfo(DepthInfo.Less);
        } else {
            hint = null;
        }

        border = new UITriangleMesh();
        border.setDepthInfo(DepthInfo.Default);
        attachChild(border, 0);

        background = new UITriangleMesh();
        background.setDepthInfo(DepthInfo.Default);
        attachChild(background, 0);

        if (hint != null) {
            attachChild(hint, 0);
        }

        setHintVisible(false);
    }

    public void init(boolean beginEmpty, Vector2 position, RenderPathCache cache) {
        this.cache = cache;

        maxPathLength = cache.sourcePath.getMeasurer().maxLength();
        startLength = 0;

        if (beginEmpty) {
            endLength = 0;
        } else {
            endLength = maxPathLength;
        }

        shouldRebuildVertices = true;
        lastFastEndSegmentIndex = -1;
        lastFastStartSegmentIndex = -1;
        lastFastEndLength = Float.NaN;
        lastFastStartLength = Float.NaN;
        setPosition(position.x, position.y);
    }


    public void setBackgroundWidth(float value) {
        backgroundWidth = value;
    }

    public void setBackgroundColor(float r, float g, float b, float a) {
        background.setColor(r, g, b, a);
    }

    public void setBackgroundColor(Color4 color, float alpha) {
        background.setColor(color);
        background.setAlpha(alpha);
    }


    public void setHintVisible(boolean visible) {
        if (hint != null) {
            hint.setVisible(visible);
            background.setClearInfo(visible ? ClearInfo.None : ClearInfo.ClearDepthBuffer);
            background.setDepthInfo(visible ? DepthInfo.Default : DepthInfo.Less);
        } else {
            background.setClearInfo(ClearInfo.ClearDepthBuffer);
            background.setDepthInfo(DepthInfo.Less);
        }
    }

    public void setHintWidth(float value) {
        hintWidth = value;
    }

    public void setHintColor(float r, float g, float b, float a) {
        if (hint != null) {
            hint.setColor(r, g, b, a);
        }
    }

    public void setHintColor(Color4 color, float alpha) {
        if (hint != null) {
            hint.setColor(color);
            hint.setAlpha(alpha);
        }
    }


    public void setBorderWidth(float value) {
        borderWidth = value;
    }

    public void setBorderColor(Color4 color) {
        border.setColor(color);
    }

    private LinePath fitToCachedPath(AbstractPath source, LinePath cache) {
        cache.clear();

        int sourceSize = source.size();
        Vec2 previousPoint = null;

        for (int i = 0; i < sourceSize; i++) {
            Vec2 currentPoint = source.get(i);

            if (previousPoint == null || Vec2.length(previousPoint, currentPoint) > pointMergeDistance) {
                cache.add(currentPoint);
                previousPoint = currentPoint;
            }
        }

        return cache;
    }

    private void buildVertices(LinePath subPath) {

        TriangleBuilder builder = cache.triangleBuilder;

        if (hint != null && hint.isVisible()) {
            cache.drawLinePath
                    .reset(subPath, Math.min(hintWidth, backgroundWidth - borderWidth))
                    .computeTriangles(builder)
                    .applyVertices(hint.getVertices());

            hint.setContentSize(builder.maxX, builder.maxY);
        }

        cache.drawLinePath
                .reset(subPath, backgroundWidth - borderWidth)
                .computeTriangles(builder)
                .applyVertices(background.getVertices());

        background.setContentSize(builder.maxX, builder.maxY);

        cache.drawLinePath
                .reset(subPath, backgroundWidth)
                .computeTriangles(builder)
                .applyVertices(border.getVertices());

        border.setContentSize(builder.maxX, builder.maxY);
    }


    @Override
    protected void onManagedUpdate(float deltaTimeSec) {

        if (cache != null && shouldRebuildVertices) {
            shouldRebuildVertices = false;

            float clampedStartLength = FMath.clamp(startLength, 0, maxPathLength);
            float clampedEndLength = FMath.clamp(endLength, clampedStartLength, maxPathLength);

            // Full-length path can be built directly without slicing/allocating an intermediate path.
            if (clampedStartLength <= lengthUpdateEpsilon &&
                Math.abs(clampedEndLength - maxPathLength) <= lengthUpdateEpsilon) {
                buildVertices(cache.sourcePath);
            } else {
                // Hybrid reuse path is only valid when one edge is anchored to an endpoint.
                // Otherwise, fallback to exact cutPath triangulation.
                boolean isBuiltByFastPath =
                    (clampedStartLength <= lengthUpdateEpsilon && tryBuildFastEndLength(clampedEndLength)) ||
                    (Math.abs(clampedEndLength - maxPathLength) <= lengthUpdateEpsilon && tryBuildFastStartLength(clampedStartLength));

                if (!isBuiltByFastPath) {
                    var cutPath = cache.sourcePath.cutPath(clampedStartLength, clampedEndLength);
                    buildVertices(fitToCachedPath(cutPath, cache.path));
                }
            }
        }

        super.onManagedUpdate(deltaTimeSec);
    }

    public void setStartLength(float length) {
        float clampedLength = Math.max(0, Math.min(length, maxPathLength));

        if (Math.abs(clampedLength - startLength) <= lengthUpdateEpsilon) {
            return;
        }

        startLength = clampedLength;
        shouldRebuildVertices = true;
    }

    public void setEndLength(float length) {
        float clampedLength = FMath.clamp(length, 0, maxPathLength);

        if (Math.abs(clampedLength - endLength) <= lengthUpdateEpsilon) {
            return;
        }

        endLength = clampedLength;
        shouldRebuildVertices = true;
    }


    public static RenderPathCache createCache(LinePath renderPath, float backgroundWidth, float borderWidth,
                                              boolean isHintVisible, float hintWidth) {
        RenderPathCache cache = new RenderPathCache();

        if (renderPath == null || renderPath.size() == 0) {
            return cache;
        }

        float clampedBackgroundWidth = Math.max(0, backgroundWidth);
        float clampedBorderWidth = FMath.clamp(borderWidth, 0, clampedBackgroundWidth);
        float playableWidth = clampedBackgroundWidth - clampedBorderWidth;
        float clampedHintWidth = FMath.clamp(hintWidth, 0, playableWidth);

        cache.sourcePath = renderPath;
        buildPointLengthCache(renderPath, cache);
        buildSegmentThetaCache(renderPath, cache);

        for (int i = 0, size = renderPath.size(); i < size; i++) {
            cache.path.add(renderPath.get(i));
        }

        cache.path.measure();

        TriangleBuilder builder = cache.triangleBuilder;

        // Build once to capture per-segment quad offsets used by hybrid prefix/suffix reuse.
        float offsetProbeWidth = playableWidth > 0 ? playableWidth : clampedBackgroundWidth;

        if (offsetProbeWidth > 0) {
            cache.drawLinePath.reset(renderPath, offsetProbeWidth).computeTriangles(builder);
            cache.segmentCount = cache.drawLinePath.getSegmentQuadStartCount();

            if (cache.segmentQuadStartOffsets.length < cache.segmentCount) {
                cache.segmentQuadStartOffsets = new int[cache.segmentCount];
            }

            for (int i = 0; i < cache.segmentCount; i++) {
                cache.segmentQuadStartOffsets[i] = cache.drawLinePath.getSegmentQuadStartOffset(i);
            }
        }

        if (isHintVisible && clampedHintWidth > 0) {
            cache.drawLinePath.reset(renderPath, clampedHintWidth).computeTriangles(builder);
            cache.hint.setCache(builder);
        }

        if (playableWidth > 0) {
            cache.drawLinePath.reset(renderPath, playableWidth).computeTriangles(builder);
            cache.background.setCache(builder);
        }

        if (clampedBackgroundWidth > 0) {
            cache.drawLinePath.reset(renderPath, clampedBackgroundWidth).computeTriangles(builder);
            cache.border.setCache(builder);
        }

        return cache;
    }

    private static void buildPointLengthCache(LinePath path, RenderPathCache cache) {
        int size = path.size();

        if (cache.pointLengths.length < size) {
            cache.pointLengths = new float[size];
        }

        float length = 0;
        cache.pointLengths[0] = 0;

        for (int i = 1; i < size; i++) {
            length += Vec2.length(path.get(i - 1), path.get(i));
            cache.pointLengths[i] = length;
        }

        cache.pointCount = size;
    }

    private static void buildSegmentThetaCache(LinePath path, RenderPathCache cache) {
        int segmentCount = Math.max(0, path.size() - 1);

        if (cache.segmentThetas.length < segmentCount) {
            cache.segmentThetas = new float[segmentCount];
        }

        for (int i = 0; i < segmentCount; i++) {
            cache.segmentThetas[i] = Vec2.calTheta(path.get(i), path.get(i + 1));
        }
    }

    private boolean tryBuildFastEndLength(float endLength) {
        // End-moving fast path: reuse full-path prefix and rebuild only the boundary segment + end cap.
        if (cache == null ||
            cache.pointCount < 2 ||
            cache.segmentCount < 1) {
            return false;
        }

        float clampedEnd = FMath.clamp(endLength, 0, maxPathLength);

        if (clampedEnd <= lengthUpdateEpsilon || clampedEnd >= maxPathLength - lengthUpdateEpsilon) {
            return false;
        }

        int segmentIndex = getSegmentIndexForEndLength(clampedEnd);

        if (segmentIndex < 0 || segmentIndex >= cache.segmentCount) {
            return false;
        }

        Vec2 segmentStart = cache.sourcePath.get(segmentIndex);
        Vec2 cutPoint = interpolatePointAtLength(clampedEnd, segmentIndex, tmpCutPoint);
        float segmentTheta = cache.segmentThetas[segmentIndex];

        if (hint != null && hint.isVisible()) {
            float width = Math.min(hintWidth, backgroundWidth - borderWidth);

            if (width <= 0) {
                clearLayer(hint);
            } else if (!buildFastLayer(hint, cache.hint, width, segmentIndex, segmentStart, cutPoint,
                    segmentTheta)) {
                return false;
            }
        }

        if (!buildFastLayer(background, cache.background, backgroundWidth - borderWidth,
            segmentIndex, segmentStart, cutPoint, segmentTheta)) {
            return false;
        }

        return buildFastLayer(border, cache.border, backgroundWidth,
            segmentIndex, segmentStart, cutPoint, segmentTheta);
    }

    private boolean tryBuildFastStartLength(float startLength) {
        // Start-moving fast path: rebuild start boundary and append cached suffix.
        if (cache == null ||
            cache.pointCount < 2 ||
            cache.segmentCount < 1) {
            return false;
        }

        float clampedStart = FMath.clamp(startLength, 0, maxPathLength);

        if (clampedStart <= lengthUpdateEpsilon || clampedStart >= maxPathLength - lengthUpdateEpsilon) {
            return false;
        }

        int segmentIndex = getSegmentIndexForStartLength(clampedStart);

        if (segmentIndex < 0 || segmentIndex >= cache.segmentCount) {
            return false;
        }

        Vec2 segmentEnd = cache.sourcePath.get(segmentIndex + 1);
        Vec2 cutPoint = interpolatePointAtLength(clampedStart, segmentIndex, tmpCutPoint);
        float segmentTheta = cache.segmentThetas[segmentIndex];

        if (hint != null && hint.isVisible()) {
            float width = Math.min(hintWidth, backgroundWidth - borderWidth);

            if (width <= 0) {
                clearLayer(hint);
            } else if (!buildFastLayerFromStart(hint, cache.hint, width, segmentIndex, segmentEnd, cutPoint,
                    segmentTheta)) {
                return false;
            }
        }

        if (!buildFastLayerFromStart(background, cache.background, backgroundWidth - borderWidth,
            segmentIndex, segmentEnd, cutPoint, segmentTheta)) {
            return false;
        }

        return buildFastLayerFromStart(border, cache.border, backgroundWidth,
            segmentIndex, segmentEnd, cutPoint, segmentTheta);
    }

    private boolean buildFastLayer(UITriangleMesh target,
                                   RenderPathCache.ComponentCache cache,
                                   float width,
                                   int segmentIndex,
                                   Vec2 segmentStart,
                                   Vec2 cutPoint,
                                   float segmentTheta) {
        // Prefix reuse layout: [cached triangles before boundary] + [rebuilt boundary].
        if (width <= 0 || !cache.hasCache || cache.vertexLength <= 0 || segmentIndex >= this.cache.segmentCount) {
            return false;
        }

        int prefixLength = this.cache.segmentQuadStartOffsets[segmentIndex];
        TriangleBuilder builder = this.cache.triangleBuilder;

        if (builder.ary.length < cache.vertexLength + 128) {
            builder.ary = Arrays.copyOf(builder.ary, cache.vertexLength + 128);
        }

        System.arraycopy(cache.vertices, 0, builder.ary, 0, prefixLength);
        builder.length = prefixLength;
        builder.maxX = cache.maxX;
        builder.maxY = cache.maxY;

        if (Vec2.length(segmentStart, cutPoint) > lengthUpdateEpsilon) {
            addLineQuad(builder, segmentStart, cutPoint, width);
            addEndCap(builder, cutPoint, segmentTheta - FMath.PiHalf, width);
        }

        builder.applyVertices(target.getVertices());
        target.setContentSize(builder.maxX, builder.maxY);
        return true;
    }

    private boolean buildFastLayerFromStart(UITriangleMesh target,
                                            RenderPathCache.ComponentCache cache,
                                            float width,
                                            int segmentIndex,
                                            Vec2 segmentEnd,
                                            Vec2 cutPoint,
                                            float segmentTheta) {
        // Suffix reuse layout: [rebuilt boundary at start] + [cached triangles after boundary].
        if (width <= 0 || !cache.hasCache || cache.vertexLength <= 0 || segmentIndex >= this.cache.segmentCount) {
            return false;
        }

        TriangleBuilder builder = this.cache.triangleBuilder;

        if (builder.ary.length < cache.vertexLength + 128) {
            builder.ary = Arrays.copyOf(builder.ary, cache.vertexLength + 128);
        }

        builder.length = 0;
        builder.maxX = cache.maxX;
        builder.maxY = cache.maxY;

        addStartCap(builder, cutPoint, segmentTheta + FMath.PiHalf, width);

        if (Vec2.length(cutPoint, segmentEnd) > lengthUpdateEpsilon) {
            addLineQuad(builder, cutPoint, segmentEnd, width);
        }

        if (segmentIndex < this.cache.segmentCount - 1) {
            float nextTheta = this.cache.segmentThetas[segmentIndex + 1];
            addLineCap(builder, segmentEnd, segmentTheta - FMath.PiHalf, nextTheta - segmentTheta, width);

            int suffixOffset = this.cache.segmentQuadStartOffsets[segmentIndex + 1];
            int suffixLength = cache.vertexLength - suffixOffset;

            if (suffixLength > 0) {
                System.arraycopy(cache.vertices, suffixOffset, builder.ary, builder.length, suffixLength);
                builder.length += suffixLength;
            }
        } else {
            addEndCap(builder, segmentEnd, segmentTheta - FMath.PiHalf, width);
        }

        builder.applyVertices(target.getVertices());
        target.setContentSize(builder.maxX, builder.maxY);
        return true;
    }

    private void clearLayer(UITriangleMesh target) {
        if (target == null) {
            return;
        }

        target.getVertices().length = 0;
        target.setContentSize(0, 0);
    }

    private int getSegmentIndexForEndLength(float length) {
        int segmentIndex = getSegmentIndexForLength(length, false, lastFastEndSegmentIndex, lastFastEndLength);

        lastFastEndSegmentIndex = segmentIndex;
        lastFastEndLength = length;

        return segmentIndex;
    }

    private int getSegmentIndexForStartLength(float length) {
        int segmentIndex = getSegmentIndexForLength(length, true, lastFastStartSegmentIndex, lastFastStartLength);

        lastFastStartSegmentIndex = segmentIndex;
        lastFastStartLength = length;

        return segmentIndex;
    }

    private int getSegmentIndexForLength(float length, boolean preferNextOnExactPoint,
                                         int previousSegmentIndex, float previousLength) {
        // Map a measured length to the segment containing that length.
        // For start cuts at exact points, prefer the next segment to avoid reusing removed geometry.
        int segmentCount = cache.segmentCount;

        if (!Float.isNaN(previousLength) && previousSegmentIndex >= 0 && previousSegmentIndex < segmentCount) {
            int segmentIndex = previousSegmentIndex;

            if (length >= previousLength) {
                while (segmentIndex + 1 < segmentCount && cache.pointLengths[segmentIndex + 1] < length) {
                    segmentIndex++;
                }
            } else {
                while (segmentIndex > 0 && cache.pointLengths[segmentIndex] > length) {
                    segmentIndex--;
                }

                if (preferNextOnExactPoint && segmentIndex + 1 < cache.pointCount - 1) {
                    float pointLength = cache.pointLengths[segmentIndex + 1];

                    if (Math.abs(pointLength - length) <= lengthUpdateEpsilon) {
                        return segmentIndex + 1;
                    }
                }
            }

            return segmentIndex;
        }

        int left = 0;
        int right = cache.pointCount - 1;

        while (left <= right) {
            int mid = left + ((right - left) >> 1);

            if (cache.pointLengths[mid] < length) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        int pointIndex = Math.max(1, Math.min(left, cache.pointCount - 1));
        int segmentIndex = pointIndex - 1;

        if (preferNextOnExactPoint && pointIndex < cache.pointCount - 1) {
            float pointLength = cache.pointLengths[pointIndex];

            if (Math.abs(pointLength - length) <= lengthUpdateEpsilon) {
                segmentIndex = pointIndex;
            }
        }

        return Math.max(0, Math.min(segmentIndex, cache.segmentCount - 1));
    }

    private Vec2 interpolatePointAtLength(float length, int segmentIndex, Vec2 out) {
        float startLength = cache.pointLengths[segmentIndex];
        float endLength = cache.pointLengths[segmentIndex + 1];
        float segmentLength = endLength - startLength;

        Vec2 startPoint = cache.sourcePath.get(segmentIndex);

        if (segmentLength <= lengthUpdateEpsilon) {
            out.set(startPoint);
            return out;
        }

        float t = FMath.clamp((length - startLength) / segmentLength, 0, 1);
        Vec2 endPoint = cache.sourcePath.get(segmentIndex + 1);

        out.set(
            startPoint.x + (endPoint.x - startPoint.x) * t,
            startPoint.y + (endPoint.y - startPoint.y) * t
        );

        return out;
    }

    private void addLineQuad(TriangleBuilder builder, Vec2 pointStart, Vec2 pointEnd, float width) {
        tmpOthExpand.set(Vec2.lineOthNormal(pointStart, pointEnd, tmpOthExpand));
        tmpOthExpand.zoom(width);

        tmpStartL.set(pointStart);
        tmpStartL.add(tmpOthExpand);

        tmpStartR.set(pointStart);
        tmpStartR.minus(tmpOthExpand);

        tmpEndL.set(pointEnd);
        tmpEndL.add(tmpOthExpand);

        tmpEndR.set(pointEnd);
        tmpEndR.minus(tmpOthExpand);

        builder.add(pointStart, pointEnd, tmpEndL);
        builder.add(pointStart, tmpEndL, tmpStartL);
        builder.add(pointStart, tmpEndR, pointEnd);
        builder.add(pointStart, tmpStartR, tmpEndR);
    }

    private void addEndCap(TriangleBuilder builder, Vec2 origin, float theta, float width) {
        addLineCap(builder, origin, theta, FMath.Pi, width);
    }

    private void addStartCap(TriangleBuilder builder, Vec2 origin, float theta, float width) {
        addLineCap(builder, origin, theta, FMath.Pi, width);
    }

    private void addLineCap(TriangleBuilder builder, Vec2 origin, float theta, float thetaDiff, float width) {
        final float step = FMath.Pi / 24;

        float dir = Math.signum(thetaDiff);
        thetaDiff *= dir;
        int amountPoints = (int) Math.ceil(thetaDiff / step);

        if (dir < 0) {
            theta += FMath.Pi;
        }

        tmpCapCurrent.set(Vec2.atCircle(theta, tmpCapCurrent));
        tmpCapCurrent.zoom(width);
        tmpCapCurrent.add(origin);

        for (int i = 1; i <= amountPoints; i++) {
            tmpCapCurrent2.set(Vec2.atCircle(theta + dir * Math.min(i * step, thetaDiff), tmpCapCurrent2));
            tmpCapCurrent2.zoom(width);
            tmpCapCurrent2.add(origin);

            builder.add(origin, tmpCapCurrent, tmpCapCurrent2);
            tmpCapCurrent.set(tmpCapCurrent2);
        }
    }

    public static class RenderPathCache {
        public final LinePath path = new LinePath();
        public final TriangleBuilder triangleBuilder = new TriangleBuilder();
        public final DrawLinePath drawLinePath = new DrawLinePath();

        public LinePath sourcePath;

        // Cumulative length at each path point, used to locate boundary segments without path slicing.
        public float[] pointLengths = new float[0];
        public int pointCount;

        // Quad start offsets for each segment in the precached triangulation output.
        public int[] segmentQuadStartOffsets = new int[0];
        public int segmentCount;

        // Per-segment theta values for the source path.
        public float[] segmentThetas = new float[0];

        public ComponentCache hint = new ComponentCache();
        public ComponentCache background = new ComponentCache();
        public ComponentCache border = new ComponentCache();

        // Full precached vertex buffer for a visual layer.
        public static class ComponentCache {
            public float[] vertices = new float[0];
            public int vertexLength;
            public float maxX;
            public float maxY;
            public boolean hasCache;

            public void setCache(TriangleBuilder builder) {
                vertices = Arrays.copyOf(builder.ary, builder.length);
                vertexLength = builder.length;
                maxX = builder.maxX;
                maxY = builder.maxY;
                hasCache = true;
            }
        }
    }
}
