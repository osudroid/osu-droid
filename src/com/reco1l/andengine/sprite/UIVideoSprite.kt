package com.reco1l.andengine.sprite

import android.opengl.GLES32
import android.os.Handler
import android.os.Looper
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn
import com.osudroid.utils.updateThread
import com.reco1l.andengine.texture.*
import com.acivev.andengine.opengl.ExternalOESShaderProgram
import org.andengine.engine.Engine
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.texture.region.*
import org.andengine.opengl.util.GLState

@OptIn(UnstableApi::class)
class UIVideoSprite(source: String, private val engine: Engine) : UISprite() {

    private val texture = VideoTexture(source)

    init {
        val w = texture.videoWidth.toFloat()
        val h = texture.videoHeight.toFloat()
        textureRegion = TextureRegion(texture, 0f, 0f, w.coerceAtLeast(1f), h.coerceAtLeast(1f))
        engine.textureManager.loadTexture(texture)

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
    // Custom shader binding — OES external texture + ST-transform matrix
    // -----------------------------------------------------------------------

    override fun beginDraw(pGLState: GLState) {
        // Latch the latest video frame and update the ST matrix BEFORE onBindShader reads it.
        texture.latch()
        super.beginDraw(pGLState)
    }

    override fun onBindShader(pGLState: GLState) {
        val shader = ExternalOESShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        if (shader.uniformMVPMatrixLocation >= 0) {
            GLES32.glUniformMatrix4fv(
                shader.uniformMVPMatrixLocation,
                1, false, pGLState.modelViewProjectionGLMatrix, 0
            )
        }

        // ST-transform: corrects Y-flip and any other orientation from the hardware decoder.
        if (shader.uniformSTMatrixLocation >= 0) {
            GLES32.glUniformMatrix4fv(
                shader.uniformSTMatrixLocation,
                1, false, texture.getTransformMatrix(), 0
            )
        }

        if (shader.uniformTexture0Location >= 0) {
            GLES32.glUniform1i(shader.uniformTexture0Location, 0)
        }

        if (shader.uniformColorLocation >= 0) {
            GLES32.glUniform4f(
                shader.uniformColorLocation,
                drawRed, drawGreen, drawBlue, drawAlpha
            )
        }

        GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)

        // UV attribute (attribute location 3).
        uvBuffer.beginDraw(pGLState)
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

                // RectangularShape initializes mScaleCenterX/Y from constructor dimensions (0×0 here).
                // setSize() updates mWidth/mHeight but not the scale center, so we must fix it manually.
                // Without this, applyBackground's centering formula — which assumes scale is applied around
                // the entity center — places the video at the wrong screen position.
                setScaleCenter(w / 2f, h / 2f)

                mainHandler.post(callback)
            }
        }
    }

    fun release() {
        // Unloading the texture triggers unloadFromHardware, which posts a single ordered block
        // to the main handler: setVideoSurface(null) --> surface teardown --> player.release().
        engine.textureManager.unloadTexture(texture)
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
    }
}
