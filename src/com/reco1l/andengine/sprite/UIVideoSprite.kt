package com.reco1l.andengine.sprite

import android.os.Handler
import android.os.Looper
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn
import com.osudroid.utils.updateThread
import com.reco1l.andengine.texture.*
import org.andengine.engine.Engine
import org.andengine.graphics.batch.BatchTexture
import org.andengine.graphics.batch.MeshProgram
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.region.*
import org.andengine.graphics.DrawContext

@OptIn(UnstableApi::class)
class UIVideoSprite(
    private val texture: VideoTexture,
    private val textureManager: TextureManager
) : UISprite() {

    constructor(source: String, engine: Engine) : this(VideoTexture(source), engine.textureManager)

    // External (OES) texture drawn through its own program, never through the quad batch.
    override val isBatchable: Boolean get() = false

    // The external view of the texture; the same instance for as long as the texture lives.
    private val externalView = BatchTexture.externalOf(texture)

    init {
        val w = texture.videoWidth.toFloat()
        val h = texture.videoHeight.toFloat()
        textureRegion = TextureRegion(texture, 0f, 0f, w.coerceAtLeast(1f), h.coerceAtLeast(1f))
        textureManager.loadTexture(texture)

        // Some hardware decoders report 0×0 from prepare() and only provide dimensions
        // on the first decoded frame. Register a listener to fix the TextureRegion then.
        if (w <= 0f || h <= 0f) {
            texture.onReady = Runnable {
                updateThread {
                    textureRegion?.setTextureWidth(texture.videoWidth.toFloat())
                    textureRegion?.setTextureHeight(texture.videoHeight.toFloat())
                    setSize(texture.videoWidth.toFloat(), texture.videoHeight.toFloat())
                }
            }
        }

        // SurfaceTexture/OES textures have their V axis inverted relative to UISprite's UV
        // convention — flip it so the video appears right-side up regardless of whether the
        // device's ST matrix also applies a V-flip (both cases work out correctly).
        flippedVertical = true
    }

    // -----------------------------------------------------------------------
    // Drawing — OES external texture + ST-transform matrix
    // -----------------------------------------------------------------------

    override fun onDrawBuffer(gl: DrawContext) {
        val positions = buffer?.data ?: return
        val view = externalView
        if (!BatchTexture.isLoaded(view)) return

        // Latch the latest video frame and update the ST matrix before the draw reads it.
        texture.latch()
        val st = texture.getTransformMatrix()
        val uvs = uvBuffer.data

        // The ST transform corrects Y-flip and any other orientation from the hardware decoder:
        // (st * vec4(u, v, 0, 1)).xy with the column-major matrix of the SurfaceTexture.
        val transformed = uvScratch
        for (i in 0 until QUAD_VERTICES) {
            val u = uvs[i * 2]
            val v = uvs[i * 2 + 1]
            transformed[i * 2] = st[0] * u + st[4] * v + st[12]
            transformed[i * 2 + 1] = st[1] * u + st[5] * v + st[13]
        }

        MeshRenderer.drawProgramQuad(
            program, positions, transformed,
            gl.modelViewGLMatrix, gl.projectionGLMatrix,
            view,
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha),
            meshBlendMode()
        )
    }

    // -----------------------------------------------------------------------
    // Playback controls
    // -----------------------------------------------------------------------

    fun setOnReady(callback: Runnable) {
        texture.onReady = Runnable {
            updateThread {
                // Update the TextureRegion UV extents and Sprite vertex dimensions now that the actual video resolution is
                // known.
                // Both were constructed as 0×0 because ExoPlayer reports dimensions asynchronously via onVideoSizeChanged.
                val w = texture.videoWidth.toFloat()
                val h = texture.videoHeight.toFloat()
                textureRegion?.setTextureWidth(w)
                textureRegion?.setTextureHeight(h)
                setSize(w, h)

                // applyBackground's centering formula assumes that the scale is applied around the
                // center of the entity. The scale center of a UIComponent is relative to its size
                // rather than in pixels, so passing half of the video size here would move the
                // video far off the screen.
                setScaleCenter(0.5f, 0.5f)

                mainHandler.post(callback)
            }
        }
    }

    fun release() {
        // Unloading the texture triggers unloadFromHardware, which posts a single ordered block
        // to the main handler: setVideoSurface(null) --> surface teardown --> player.release().
        textureManager.unloadTexture(texture)
    }

    fun play() {
        mainHandler.post { texture.player.play() }
    }

    fun pause() {
        mainHandler.post { texture.player.pause() }
    }

    fun seekTo(ms: Int) {
        mainHandler.post { texture.player.seekTo(ms.toLong()) }
    }

    fun setPlaybackSpeed(speed: Float) {
        mainHandler.post { texture.player.playbackParameters = PlaybackParameters(speed) }
    }

    override fun finalize() {
        // Guard against a partially-constructed object: if VideoTexture's init threw,
        // texture.player is null at the bytecode level despite Kotlin's non-null type.
        try {
            release()
        } catch (_: Throwable) {}

        super.finalize()
    }

    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())

        private const val QUAD_VERTICES = 4

        // The transformed u, v pairs in sprite buffer order; GL thread only, shared by all video sprites.
        private val uvScratch = FloatArray(QUAD_VERTICES * 2)

        // The frame tinted by the draw color, straight alpha (blended as the texture options ask).
        private const val FRAGMENT_SHADER = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform samplerExternalOES uTexture;
in vec2 vTexCoord;
in vec4 vColor;
out vec4 fragColor;
void main() {
    fragColor = vColor * texture(uTexture, vTexCoord);
}
"""

        private var programInstance: MeshProgram? = null

        // Shared by all video sprites; created at the first draw, the program restores itself after a context loss.
        private val program: MeshProgram
            get() = programInstance ?: MeshProgram("video", FRAGMENT_SHADER, emptyList(), external = true).also { programInstance = it }

        /** Disposes the GL program; the next draw creates it again. For shutdown and tests. */
        @JvmStatic
        fun releaseProgram() {
            programInstance?.releaseResources()
            programInstance = null
        }
    }
}
