package com.edlplan.osu.support.slider;

import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.AbstractPath;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.framework.utils.FloatArraySlice;
import com.reco1l.andengine.shape.UITriangleMesh;
import com.reco1l.andengine.container.UIContainer;
import com.reco1l.framework.Color4;
import com.osudroid.math.Vector2;

import org.andengine.engine.camera.Camera;
import org.andengine.graphics.BlendMode;
import org.andengine.graphics.DepthState;
import org.andengine.graphics.DrawContext;
import org.andengine.graphics.RenderTarget;
import org.andengine.graphics.batch.QuadBatcher;
import org.andengine.graphics.offscreen.OffscreenRenderer;
import org.andengine.graphics.offscreen.RenderTargetLease;
import org.andengine.graphics.offscreen.RenderTargetPool;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * The body of a slider as legacy skins draw it: shadow, border, a body with a gradient and the optional hint. The
 * layers are drawn into an offscreen texture without blending, so where the slider overlaps itself every pixel holds
 * the part nearest to its own center line; the texture is then drawn with the alpha of this entity. The texture is
 * only drawn again when the slider changes, at the start of the frame (see {@link OffscreenRenderer}).
 */
public class SliderBody extends UIContainer {

    private RenderPathCache cache;
    private static final float lengthUpdateEpsilon = 0.001f;
    private static final float pointMergeDistance = 0.01f;

    // Gradient of the legacy slider body, see setBackgroundColor().
    private static final float OUTER_COLOR_SCALE = 1 / 1.1f;
    private static final float INNER_COLOR_SCALE = 1.125f;
    private static final float INNER_COLOR_ADD = 0.25f;

    // Depths in normalized device coordinates (-1 nearest). Body and shadow are cones, nearest on the center line,
    // so where a slider overlaps itself the part nearer to its own center line wins. Hint < body < border < shadow.
    private static final float HINT_DEPTH = -0.95f;
    private static final float BODY_CENTER_DEPTH = -0.9f;
    private static final float BODY_EDGE_DEPTH = -0.5f;
    private static final float BORDER_DEPTH = 0f;
    private static final float SHADOW_CENTER_DEPTH = 0.5f;
    private static final float SHADOW_EDGE_DEPTH = 0.9f;

    // The shadow around the border of legacy skins: the border's outer edge lies at the radius of a legacy hit
    // circle (64 - 5), the shadow fades out from there to the object radius (64).
    private static final float SHADOW_WIDTH_SCALE = 64f / 59f;
    private static final float SHADOW_ALPHA = 0.25f;

    // Change of the local to surface scale that makes the texture be drawn again.
    private static final float SCALE_EPSILON = 1e-4f;

    private float maxPathLength;

    private final UITriangleMesh background;

    private final UITriangleMesh border;

    private final UITriangleMesh shadow;

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
    private final Vec2 tmpPreviousEndL = new Vec2();
    private final Vec2 tmpPreviousEndR = new Vec2();
    private Vec2 tmpQuadStart;
    private Vec2 tmpQuadEnd;
    private final Vec2 tmpCapCurrent = new Vec2();
    private final Vec2 tmpCapCurrent2 = new Vec2();
    private final Vec2 tmpCutPoint = new Vec2();

    // Tracks monotonic boundary movement during snaking to reduce segment lookups.
    private int lastFastEndSegmentIndex = -1;
    private int lastFastStartSegmentIndex = -1;
    private float lastFastEndLength = Float.NaN;
    private float lastFastStartLength = Float.NaN;

    // The offscreen texture and the part of it the layers were drawn into, in local coordinates and texture
    // coordinates; null and empty until the first draw.
    private RenderTargetLease lease;
    private boolean hasContent;
    private float regionLeft;
    private float regionTop;
    private float regionRight;
    private float regionBottom;
    private int regionWidth;
    private int regionHeight;

    // What the content was drawn for: the local to surface scale and the content generation.
    private float renderedScaleX;
    private float renderedScaleY;
    private int renderedGeneration;
    private boolean contentDirty = true;

    // Local coordinates map onto surface pixels as origin + scale * local, axis by axis; see measureSurface().
    private float surfaceOriginX;
    private float surfaceOriginY;
    private float surfaceScaleX;
    private float surfaceScaleY;

    private final float[] tmpSurfacePoint = new float[2];

    private final OffscreenRenderer.Job renderJob = this::renderOffscreen;

    private final OffscreenRenderer.Job releaseJob = (drawContext, camera) -> releaseTarget();

    // What drawLayers draws with; only set during renderOffscreen(), so no lambda is created per render.
    private DrawContext layerDrawContext;
    private Camera layerCamera;

    private final Runnable drawLayers = this::drawLayers;

    public SliderBody(boolean allowHint) {

        // The layers are not attached: they are drawn into the offscreen texture only, with their own colors.
        if (allowHint) {
            hint = new UITriangleMesh();
            hint.setHasVertexWeights(true);
            hint.setDepthRange(HINT_DEPTH, HINT_DEPTH);
            hint.setVisible(false);
            // Over the body, keeping the coverage in the texture's alpha.
            hint.setBlendMode(BlendMode.Alpha);
            hint.setDepthState(DepthState.Less);
        } else {
            hint = null;
        }

        background = new UITriangleMesh();
        background.setHasVertexWeights(true);
        background.setDepthRange(BODY_EDGE_DEPTH, BODY_CENTER_DEPTH);
        background.setBlendMode(BlendMode.Opaque);
        background.setDepthState(DepthState.Less);

        border = new UITriangleMesh();
        border.setHasVertexWeights(true);
        border.setDepthRange(BORDER_DEPTH, BORDER_DEPTH);
        border.setBlendMode(BlendMode.Opaque);
        border.setDepthState(DepthState.Less);

        shadow = new UITriangleMesh();
        shadow.setHasVertexWeights(true);
        shadow.setDepthRange(SHADOW_EDGE_DEPTH, SHADOW_CENTER_DEPTH);
        shadow.setBlendMode(BlendMode.Opaque);
        shadow.setDepthState(DepthState.Less);
        // Black from transparent on its outline to SHADOW_ALPHA at the border's outer edge.
        shadow.setGradient(0, 0, 0, 0, 0, 0, 0, SHADOW_ALPHA);
        shadow.setGradientRange(0, 1 - 1 / SHADOW_WIDTH_SCALE);
    }

    public void init(boolean beginEmpty, Vector2 position, RenderPathCache cache) {
        this.cache = cache;

        clearLayer(hint);
        clearLayer(background);
        clearLayer(border);
        clearLayer(shadow);
        markContentDirty();

        if (cache.sourcePath == null || cache.pointCount == 0 || cache.sourcePath.size() == 0) {
            maxPathLength = 0;
            startLength = 0;
            endLength = 0;
            shouldRebuildVertices = false;
            lastFastEndSegmentIndex = -1;
            lastFastStartSegmentIndex = -1;
            lastFastEndLength = Float.NaN;
            lastFastStartLength = Float.NaN;
            setPosition(position.x, position.y);
            return;
        }

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

    /**
     * Sets the color of the body, which is drawn with the gradient of legacy skins: from a slightly darkened
     * color next to the border to a lightened one on the center line.
     */
    public void setBackgroundColor(float r, float g, float b, float a) {
        background.setColor(1, 1, 1, a);
        background.setGradient(
            r * OUTER_COLOR_SCALE, g * OUTER_COLOR_SCALE, b * OUTER_COLOR_SCALE, 1,
            lighten(r), lighten(g), lighten(b), 1
        );
        markContentDirty();
    }

    public void setBackgroundColor(Color4 color, float alpha) {
        setBackgroundColor(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Lightening of the legacy slider renderer.
     */
    private static float lighten(float component) {
        return Math.min(1, component * INNER_COLOR_SCALE + INNER_COLOR_ADD);
    }


    public void setHintVisible(boolean visible) {
        if (hint != null) {
            hint.setVisible(visible);
            markContentDirty();
        }
    }

    public void setHintWidth(float value) {
        hintWidth = value;
    }

    public void setHintColor(float r, float g, float b, float a) {
        if (hint != null) {
            hint.setColor(r, g, b, a);
            markContentDirty();
        }
    }

    public void setHintColor(Color4 color, float alpha) {
        if (hint != null) {
            hint.setColor(color.getRed(), color.getGreen(), color.getBlue());
            hint.setAlpha(alpha);
            markContentDirty();
        }
    }


    public void setBorderWidth(float value) {
        borderWidth = value;
    }

    public void setBorderColor(Color4 color) {
        border.setColor(color.getRed(), color.getGreen(), color.getBlue());
        markContentDirty();
    }

    private LinePath fitToCachedPath(AbstractPath source, LinePath cache) {
        cache.clear();

        int sourceSize = source.size();
        Vec2 previousPoint = null;

        for (int i = 0; i < sourceSize; i++) {
            Vec2 currentPoint = source.get(i);

            if (previousPoint == null || i == sourceSize - 1 || Vec2.length(previousPoint, currentPoint) > pointMergeDistance) {
                cache.add(currentPoint);
                previousPoint = currentPoint;
            }
        }

        return cache;
    }

    private float getClampedBackgroundWidth() {
        return Math.max(0, backgroundWidth);
    }

    private float getShadowWidth() {
        return getClampedBackgroundWidth() * SHADOW_WIDTH_SCALE;
    }

    private float getPlayableWidth() {
        return getClampedBackgroundWidth() - FMath.clamp(borderWidth, 0, getClampedBackgroundWidth());
    }

    private float getClampedHintWidth() {
        return FMath.clamp(hintWidth, 0, getPlayableWidth());
    }

    private void buildVertices(LinePath subPath) {

        TriangleBuilder builder = cache.triangleBuilder;

        if (hint != null && hint.isVisible()) {
            cache.drawLinePath
                    .reset(subPath, getClampedHintWidth())
                    .computeTriangles(builder)
                    .applyVertices(hint.getVertices());

            hint.setContentSize(builder.maxX, builder.maxY);
        }

        cache.drawLinePath
                .reset(subPath, getPlayableWidth())
                .computeTriangles(builder)
                .applyVertices(background.getVertices());

        background.setContentSize(builder.maxX, builder.maxY);

        cache.drawLinePath
                .reset(subPath, getClampedBackgroundWidth())
                .computeTriangles(builder)
                .applyVertices(border.getVertices());

        border.setContentSize(builder.maxX, builder.maxY);

        cache.drawLinePath
                .reset(subPath, getShadowWidth())
                .computeTriangles(builder)
                .applyVertices(shadow.getVertices());

        shadow.setContentSize(builder.maxX, builder.maxY);
    }


    @Override
    protected void onManagedUpdate(float deltaTimeSec) {

        if (cache != null && shouldRebuildVertices) {
            shouldRebuildVertices = false;
            markContentDirty();

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

    private void markContentDirty() {
        contentDirty = true;
        OffscreenRenderer.request(renderJob);
    }

    /**
     * Measures how local coordinates map onto surface pixels, with the y axis pointing down like the local one.
     */
    private void measureSurface(Camera camera) {
        float[] point = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0, 0, tmpSurfacePoint), false);
        surfaceOriginX = point[0];
        surfaceOriginY = point[1];

        point = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(1, 1, tmpSurfacePoint), false);
        surfaceScaleX = point[0] - surfaceOriginX;
        surfaceScaleY = point[1] - surfaceOriginY;
    }

    /**
     * Computes the region of the texture for the local bounds, covering whole surface pixels so that the texture is
     * drawn back without resampling. A region larger than a texture can be holds the whole bounds at a lower
     * resolution instead.
     */
    private void computeRegion(float minX, float minY, float maxX, float maxY) {
        float pixelLeft = (float) Math.floor(Math.min(surfaceOriginX + surfaceScaleX * minX, surfaceOriginX + surfaceScaleX * maxX));
        float pixelTop = (float) Math.floor(Math.min(surfaceOriginY + surfaceScaleY * minY, surfaceOriginY + surfaceScaleY * maxY));
        float pixelRight = (float) Math.ceil(Math.max(surfaceOriginX + surfaceScaleX * minX, surfaceOriginX + surfaceScaleX * maxX));
        float pixelBottom = (float) Math.ceil(Math.max(surfaceOriginY + surfaceScaleY * minY, surfaceOriginY + surfaceScaleY * maxY));

        int maxSize = RenderTargetPool.getMaxSize();
        regionWidth = Math.max(1, (int) (pixelRight - pixelLeft));
        regionHeight = Math.max(1, (int) (pixelBottom - pixelTop));

        regionLeft = (pixelLeft - surfaceOriginX) / surfaceScaleX;
        regionRight = (pixelLeft + regionWidth - surfaceOriginX) / surfaceScaleX;
        regionTop = (pixelTop - surfaceOriginY) / surfaceScaleY;
        regionBottom = (pixelTop + regionHeight - surfaceOriginY) / surfaceScaleY;

        if (regionWidth > maxSize) {
            regionWidth = maxSize;
            regionLeft = surfaceScaleX > 0 ? minX : maxX;
            regionRight = surfaceScaleX > 0 ? maxX : minX;
        }

        if (regionHeight > maxSize) {
            regionHeight = maxSize;
            regionTop = surfaceScaleY > 0 ? minY : maxY;
            regionBottom = surfaceScaleY > 0 ? maxY : minY;
        }
    }

    private void renderOffscreen(DrawContext drawContext, Camera camera) {
        if (!contentDirty) {
            return;
        }

        // The shadow is the widest layer, so its vertices bound everything.
        FloatArraySlice vertices = shadow.getVertices();

        if (getParent() == null || vertices.length == 0) {
            releaseTarget();
            contentDirty = false;
            return;
        }

        measureSurface(camera);

        if (surfaceScaleX == 0 || surfaceScaleY == 0) {
            // Not on the surface yet.
            OffscreenRenderer.request(renderJob);
            return;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        int end = vertices.offset + vertices.length;

        for (int i = vertices.offset; i < end; i += TriangleBuilder.FLOATS_PER_VERTEX) {
            float x = vertices.ary[i];
            float y = vertices.ary[i + 1];
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        computeRegion(minX, minY, maxX, maxY);

        if (lease == null || !lease.isValid() || lease.getTarget().getWidth() < regionWidth || lease.getTarget().getHeight() < regionHeight) {
            releaseTarget();
            lease = RenderTargetPool.acquire(regionWidth, regionHeight);
        }

        layerDrawContext = drawContext;
        layerCamera = camera;
        try {
            OffscreenRenderer.draw(lease.getTarget(), regionWidth, regionHeight, regionLeft, regionTop, regionRight, regionBottom, drawContext, drawLayers);
        } finally {
            layerDrawContext = null;
            layerCamera = null;
        }

        renderedScaleX = surfaceScaleX;
        renderedScaleY = surfaceScaleY;
        renderedGeneration = OffscreenRenderer.getContentGeneration();
        lease.markUsed();
        hasContent = true;
        contentDirty = false;
    }

    private void drawLayers() {
        background.doDraw(layerDrawContext, layerCamera);

        if (hint != null && hint.isVisible()) {
            hint.doDraw(layerDrawContext, layerCamera);
        }

        border.doDraw(layerDrawContext, layerCamera);
        shadow.doDraw(layerDrawContext, layerCamera);
    }

    private void releaseTarget() {
        if (lease != null) {
            RenderTargetPool.release(lease);
            lease = null;
        }
        hasContent = false;
    }

    @Override
    public void doDraw(@NotNull DrawContext drawContext, @NotNull Camera camera) {
        super.doDraw(drawContext, camera);

        if (!hasContent) {
            return;
        }

        // The pool took the texture back after the slider was not drawn for a while.
        if (!lease.isValid()) {
            lease = null;
            hasContent = false;
            markContentDirty();
            return;
        }

        lease.markUsed();

        // Drawn again for a new scale (the texture would be resampled) or after the context was lost.
        if (!contentDirty) {
            measureSurface(camera);

            if (renderedGeneration != OffscreenRenderer.getContentGeneration() ||
                Math.abs(surfaceScaleX - renderedScaleX) > SCALE_EPSILON ||
                Math.abs(surfaceScaleY - renderedScaleY) > SCALE_EPSILON) {
                markContentDirty();
            }
        }

        RenderTarget target = lease.getTarget();

        QuadBatcher.setProjection(drawContext.getProjectionGLMatrix());
        // The texture's rows go up from v = 0, the local y axis goes down.
        QuadBatcher.add(
            target.getColor(),
            BlendMode.Alpha,
            drawContext.getModelViewGLMatrix(),
            regionLeft, regionTop, regionRight, regionBottom,
            0, (float) regionHeight / target.getHeight(), (float) regionWidth / target.getWidth(), 0,
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha)
        );
    }

    @Override
    public void onDetached() {
        super.onDetached();
        OffscreenRenderer.cancel(renderJob);
        OffscreenRenderer.request(releaseJob);
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


    public static RenderPathCache createCache(@NotNull LinePath renderPath, float backgroundWidth, float borderWidth,
                                              boolean isHintVisible, float hintWidth, boolean computeFullCache) {
        RenderPathCache cache = new RenderPathCache();

        cache.sourcePath = renderPath;

        if (renderPath.size() == 0) {
            cache.pointCount = 0;
            cache.segmentCount = 0;
            return cache;
        }

        float clampedBackgroundWidth = Math.max(0, backgroundWidth);
        float clampedBorderWidth = FMath.clamp(borderWidth, 0, clampedBackgroundWidth);
        float playableWidth = clampedBackgroundWidth - clampedBorderWidth;
        float clampedHintWidth = FMath.clamp(hintWidth, 0, playableWidth);

        buildPointLengthCache(renderPath, cache);
        buildSegmentThetaCache(renderPath, cache);

        cache.segmentCount = Math.max(0, cache.pointCount - 1);

        if (computeFullCache) {
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

                cache.drawLinePath.reset(renderPath, clampedBackgroundWidth * SHADOW_WIDTH_SCALE).computeTriangles(builder);
                cache.shadow.setCache(builder);
            }
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
            float width = getClampedHintWidth();

            if (width <= 0) {
                clearLayer(hint);
            } else if (!buildFastLayer(hint, cache.hint, width, segmentIndex, segmentStart, cutPoint,
                    segmentTheta)) {
                return false;
            }
        }

        if (!buildFastLayer(background, cache.background, getPlayableWidth(),
            segmentIndex, segmentStart, cutPoint, segmentTheta)) {
            return false;
        }

        if (!buildFastLayer(border, cache.border, getClampedBackgroundWidth(),
            segmentIndex, segmentStart, cutPoint, segmentTheta)) {
            return false;
        }

        return buildFastLayer(shadow, cache.shadow, getShadowWidth(),
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
            float width = getClampedHintWidth();

            if (width <= 0) {
                clearLayer(hint);
            } else if (!buildFastLayerFromStart(hint, cache.hint, width, segmentIndex, segmentEnd, cutPoint,
                    segmentTheta)) {
                return false;
            }
        }

        if (!buildFastLayerFromStart(background, cache.background, getPlayableWidth(),
            segmentIndex, segmentEnd, cutPoint, segmentTheta)) {
            return false;
        }

        if (!buildFastLayerFromStart(border, cache.border, getClampedBackgroundWidth(),
            segmentIndex, segmentEnd, cutPoint, segmentTheta)) {
            return false;
        }

        return buildFastLayerFromStart(shadow, cache.shadow, getShadowWidth(),
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
            // The quad starts at the corners of the cached join, so its normal is taken from the whole segment.
            computeQuadCorners(segmentStart, cutPoint, segmentStart, this.cache.sourcePath.get(segmentIndex + 1), width);
            addLineQuad(builder);
            addLineCap(builder, cutPoint, segmentTheta - FMath.PiHalf, FMath.Pi, width, tmpEndR, tmpEndL);
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

        computeQuadCorners(cutPoint, segmentEnd, this.cache.sourcePath.get(segmentIndex), segmentEnd, width);
        addLineCap(builder, cutPoint, segmentTheta + FMath.PiHalf, FMath.Pi, width, tmpStartL, tmpStartR);

        if (Vec2.length(cutPoint, segmentEnd) > lengthUpdateEpsilon) {
            addLineQuad(builder);
        }

        if (segmentIndex < this.cache.segmentCount - 1) {
            float nextTheta = this.cache.segmentThetas[segmentIndex + 1];
            Vec2 nextPoint = this.cache.sourcePath.get(segmentIndex + 2);

            // Same join as built by DrawLinePath, so it meets the cached quads of the next segment exactly.
            tmpPreviousEndL.set(tmpEndL);
            tmpPreviousEndR.set(tmpEndR);
            computeQuadCorners(segmentEnd, nextPoint, segmentEnd, nextPoint, width);

            float thetaDiff = nextTheta - segmentTheta;
            if (thetaDiff < 0) {
                addLineCap(builder, segmentEnd, segmentTheta - FMath.PiHalf, thetaDiff, width, tmpPreviousEndL, tmpStartL);
                builder.add(segmentEnd, tmpPreviousEndR, tmpStartR);
            } else {
                addLineCap(builder, segmentEnd, segmentTheta - FMath.PiHalf, thetaDiff, width, tmpPreviousEndR, tmpStartR);
                builder.add(segmentEnd, tmpPreviousEndL, tmpStartL);
            }

            int suffixOffset = this.cache.segmentQuadStartOffsets[segmentIndex + 1];
            int suffixLength = cache.vertexLength - suffixOffset;

            if (suffixLength > 0) {
                System.arraycopy(cache.vertices, suffixOffset, builder.ary, builder.length, suffixLength);
                builder.length += suffixLength;
            }
        } else {
            addLineCap(builder, segmentEnd, segmentTheta - FMath.PiHalf, FMath.Pi, width, tmpEndR, tmpEndL);
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
            }

            // If next exact point is preferred, select it if its length falls within the epsilon.
            if (preferNextOnExactPoint && segmentIndex + 1 < cache.pointCount - 1) {
                float pointLength = cache.pointLengths[segmentIndex + 1];

                if (Math.abs(pointLength - length) <= lengthUpdateEpsilon) {
                    return segmentIndex + 1;
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

    /**
     * Computes the quad corners like {@link DrawLinePath}, with the normal of {@code segmentStart} to {@code segmentEnd}.
     * The corners must match the cached ones bit for bit, otherwise hairline gaps appear between them.
     */
    private void computeQuadCorners(Vec2 pointStart, Vec2 pointEnd, Vec2 segmentStart, Vec2 segmentEnd, float width) {
        tmpOthExpand.set(Vec2.lineOthNormal(segmentStart, segmentEnd, tmpOthExpand));
        tmpOthExpand.zoom(width);

        tmpQuadStart = pointStart;
        tmpQuadEnd = pointEnd;

        tmpStartL.set(pointStart);
        tmpStartL.add(tmpOthExpand);

        tmpStartR.set(pointStart);
        tmpStartR.minus(tmpOthExpand);

        tmpEndL.set(pointEnd);
        tmpEndL.add(tmpOthExpand);

        tmpEndR.set(pointEnd);
        tmpEndR.minus(tmpOthExpand);
    }

    /**
     * Adds the quad whose corners were last computed by {@link #computeQuadCorners}.
     */
    private void addLineQuad(TriangleBuilder builder) {
        builder.add(tmpQuadStart, 1, tmpQuadEnd, 1, tmpEndL, 0);
        builder.add(tmpQuadStart, 1, tmpEndL, 0, tmpStartL, 0);
        builder.add(tmpQuadStart, 1, tmpEndR, 0, tmpQuadEnd, 1);
        builder.add(tmpQuadStart, 1, tmpStartR, 0, tmpEndR, 0);
    }

    /**
     * Adds a cap from {@code first} to {@code last}, see {@link DrawLinePath} for why they are passed in.
     */
    private void addLineCap(TriangleBuilder builder, Vec2 origin, float theta, float thetaDiff, float width, Vec2 first, Vec2 last) {
        final float step = FMath.Pi / 24;

        float dir = Math.signum(thetaDiff);
        thetaDiff *= dir;
        int amountPoints = Math.max(1, (int) Math.ceil(thetaDiff / step));

        if (dir < 0) {
            theta += FMath.Pi;
        }

        tmpCapCurrent.set(first);

        for (int i = 1; i <= amountPoints; i++) {
            if (i == amountPoints) {
                tmpCapCurrent2.set(last);
            } else {
                tmpCapCurrent2.set(Vec2.atCircle(theta + dir * i * step, tmpCapCurrent2));
                tmpCapCurrent2.zoom(width);
                tmpCapCurrent2.add(origin);
            }

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
        public ComponentCache shadow = new ComponentCache();

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
