package com.osudroid.game.cursor.trail

import android.graphics.BitmapFactory
import android.opengl.GLES32
import com.reco1l.andengine.component.UIComponent
import org.andengine.engine.camera.Camera
import org.andengine.opengl.shader.PositionTextureCoordinatesUniformColorShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

private const val MAX_PARTS = 512
private const val DISJOINT_SPAWN_INTERVAL_MS = 1000f / 60f

/**
 * Some skins ship "cursortrail.png" with large fully transparent padding around the actual visible
 * blob (e.g. glow bleed exported without cropping). Spacing trail parts by the raw canvas width
 * then places them far apart relative to what's actually visible, looking gappy even though parts
 * are technically still being interpolated. This measures the alpha-opaque contents bounding box
 * so [CursorTrail] can space parts by what's actually visible instead.
 *
 * Decodes the same file the skin system resolved for "cursortrail" (custom skin override, or the
 * bundled default asset) independently of the GPU uploaded texture, since the source bitmap isn't
 * kept around after upload. This only runs once per [CursorTrail] instance.
 */
private fun measureVisibleContentSize(fallbackSize: Float): Float {
    val bitmap = try {
        val file = File(Config.getSkinPath() + "cursortrail.png")
        if (file.exists()) {
            BitmapFactory.decodeFile(file.path)
        } else {
            GlobalManager.getInstance().mainActivity.assets.open("gfx/cursortrail.png").use {
                BitmapFactory.decodeStream(it)
            }
        }
    } catch (e: Exception) {
        null
    } ?: return fallbackSize

    try {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return fallbackSize
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                // Small threshold to ignore near invisible antialiasing/compression noise.
                if (pixels[rowOffset + x] ushr 24 > 4) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return fallbackSize
        }

        return max(maxX - minX + 1, maxY - minY + 1).toFloat()
    } finally {
        bitmap.recycle()
    }
}

/**
 * Renders the skin's "cursortrail" texture behind the moving cursor, osu! legacy skin
 * cursor trail (see `osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyCursorTrail.cs` and its
 * base `osu.Game.Rulesets.Osu/UI/Cursor/CursorTrail.cs`).
 */
class CursorTrail(
    private val textureRegion: TextureRegion,
    private val cursorSprite: CursorSprite
) : UIComponent() {

    private val disjointTrail = !ResourceManager.getInstance().isTextureLoaded("cursormiddle")
    private val allowPartRotation = OsuSkin.get().isRotateCursorTrail
    private val fadeDurationMs = if (disjointTrail) 150f else 500f

    // Spacing is based on the texture's actual visible content, not its raw canvas size. See
    // measureVisibleContentSize's doc for why.
    private val trailContentSize = measureVisibleContentSize(textureRegion.width)

    private val partX = FloatArray(MAX_PARTS)
    private val partY = FloatArray(MAX_PARTS)
    private val partSpawnMs = FloatArray(MAX_PARTS) { -1f }
    private var writeIndex = 0

    private var spawningEnabled = false
    private var elapsedMs = 0f
    private var disjointSpawnAccumulatorMs = 0f

    private var hasPosition = false
    private var lastX = 0f
    private var lastY = 0f
    private var currentX = 0f
    private var currentY = 0f

    // Interleaved per-vertex: x, y, u, v
    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(4 /* vertices */ * 4 /* floats */ * 4 /* bytes */)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    init {
        // GameScene drives updates manually (see CursorEntity update), skip the engines automatic pass.
        isIgnoreUpdate = true
    }

    /**
     * Enables or disables spawning new trail parts, without touching parts that already exist.
     * they keep fading out on their own. Mirrors the previous particle systems
     * `setParticlesSpawnEnabled`, which let the trail taper off naturally when the cursor is
     * hidden instead of cutting it off abruptly.
     */
    fun setSpawningEnabled(enabled: Boolean) {
        spawningEnabled = enabled
    }

    /**
     * Called whenever the cursors position is updated. Potentially multiple times per rendered
     * frame, see the high frequency raw pointer sampling path in `GameScene`. Position tracking
     * always happens regardless of [setSpawningEnabled], so re-enabling spawning doesn't produce a
     * single long "catch up" part connecting stale and current positions.
     */
    fun addPosition(x: Float, y: Float) {
        currentX = x
        currentY = y

        if (!hasPosition) {
            hasPosition = true
            lastX = x
            lastY = y
            return
        }

        if (disjointTrail || !spawningEnabled) {
            // The disjoint style doesn't interpolate, parts are added purely on a timer in update().
            lastX = x
            lastY = y
            return
        }

        val baseX = lastX
        val baseY = lastY
        val diffX = x - baseX
        val diffY = y - baseY
        val distance = hypot(diffX, diffY)

        if (distance <= 0f) {
            return
        }

        val dirX = diffX / distance
        val dirY = diffY / distance

        val interval = interval()
        if (interval <= 0f) {
            return
        }

        // Leave a gap right under the cursor so the newest trail part doesn't overlap it.
        val stopAt = distance - interval

        var d = interval
        while (d < stopAt) {
            lastX = baseX + dirX * d
            lastY = baseY + dirY * d
            addPart(lastX, lastY)
            d += interval
        }
    }

    /**
     * Advances the fade clock and, for the disjoint style, spawns new parts on a fixed real time
     * interval. Called unconditionally every frame (regardless of whether the cursor is currently
     * shown) so already-spawned parts keep fading out in real time instead of freezing.
     */
    fun update(secondsElapsed: Float) {
        elapsedMs += secondsElapsed * 1000f

        if (disjointTrail && spawningEnabled && hasPosition) {
            disjointSpawnAccumulatorMs += secondsElapsed * 1000f

            while (disjointSpawnAccumulatorMs >= DISJOINT_SPAWN_INTERVAL_MS) {
                disjointSpawnAccumulatorMs -= DISJOINT_SPAWN_INTERVAL_MS
                addPart(currentX, currentY)
            }
        }
    }

    private fun interval(): Float = trailContentSize * cursorSprite.baseSize / 2.5f

    private fun addPart(x: Float, y: Float) {
        partX[writeIndex] = x
        partY[writeIndex] = y
        partSpawnMs[writeIndex] = elapsedMs
        writeIndex = (writeIndex + 1) % MAX_PARTS
    }

    override fun doDraw(pGLState: GLState, pCamera: Camera) {
        super.doDraw(pGLState, pCamera)

        val halfW = textureRegion.width * cursorSprite.scaleX / 2f
        val halfH = textureRegion.height * cursorSprite.scaleY / 2f

        val angle = if (allowPartRotation) Math.toRadians(cursorSprite.rotation.toDouble()).toFloat() else 0f
        val sin = sin(angle)
        val cos = cos(angle)

        val u = textureRegion.u
        val v = textureRegion.v
        val u2 = textureRegion.u2
        val v2 = textureRegion.v2

        val shader = PositionTextureCoordinatesUniformColorShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        GLES32.glUniformMatrix4fv(shader.uniformMVPMatrixLocation, 1, false, pGLState.modelViewProjectionGLMatrix, 0)
        GLES32.glUniform1i(shader.uniformTexture0Location, 0)

        GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0)

        textureRegion.texture.bind(pGLState)

        GLES32.glEnable(GLES32.GL_BLEND)
        // "Attached" (has cursormiddle) trails glow additively "disjoint" ones use normal alpha,
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, if (disjointTrail) GLES32.GL_ONE_MINUS_SRC_ALPHA else GLES32.GL_ONE)

        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION)
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)

        for (i in 0 until MAX_PARTS) {
            val spawnMs = partSpawnMs[i]
            if (spawnMs < 0f) {
                continue
            }

            val elapsedSincePart = elapsedMs - spawnMs
            if (elapsedSincePart >= fadeDurationMs) {
                continue
            }

            var alpha = 1f - elapsedSincePart / fadeDurationMs
            alpha = alpha.coerceIn(0f, 1f) * drawAlpha

            if (alpha <= 0f) {
                continue
            }

            val cx = partX[i]
            val cy = partY[i]

            putVertex(0, -halfW, -halfH, cx, cy, sin, cos, u, v)
            putVertex(1, -halfW, halfH, cx, cy, sin, cos, u, v2)
            putVertex(2, halfW, -halfH, cx, cy, sin, cos, u2, v)
            putVertex(3, halfW, halfH, cx, cy, sin, cos, u2, v2)

            vertexBuffer.position(0)
            GLES32.glVertexAttribPointer(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, 2, GLES32.GL_FLOAT, false, 16, vertexBuffer)
            vertexBuffer.position(2)
            GLES32.glVertexAttribPointer(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION, 2, GLES32.GL_FLOAT, false, 16, vertexBuffer)

            GLES32.glUniform4f(shader.uniformColorLocation, drawRed, drawGreen, drawBlue, alpha)

            GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4)
        }

        // Restore state expected by other renderers.
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA)
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
    }

    private fun putVertex(index: Int, dx: Float, dy: Float, cx: Float, cy: Float, sin: Float, cos: Float, texU: Float, texV: Float) {
        val x = dx * cos - dy * sin + cx
        val y = dx * sin + dy * cos + cy

        val base = index * 4
        vertexBuffer.put(base, x)
        vertexBuffer.put(base + 1, y)
        vertexBuffer.put(base + 2, texU)
        vertexBuffer.put(base + 3, texV)
    }
}
