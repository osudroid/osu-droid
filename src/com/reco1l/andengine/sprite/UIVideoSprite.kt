package com.reco1l.andengine.sprite

import android.media.*
import android.opengl.GLES20
import android.os.*
import com.acivev.andengine.opengl.ExternalOESShaderProgram
import com.reco1l.andengine.texture.*
import org.andengine.engine.Engine
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.texture.region.*
import org.andengine.opengl.util.GLState

/**
 * A sprite that renders a video file using Android's MediaPlayer and a GL_TEXTURE_EXTERNAL_OES
 * texture. Uses [com.acivev.andengine.opengl.ExternalOESShaderProgram] so that the OES sampler and SurfaceTexture
 * transform matrix are applied correctly.
 */
class UIVideoSprite(source: String, private val engine: Engine) : UISprite() {

    private val videoTexture = VideoTexture(source)

    init {
        // Build a 0→1 region covering the whole video frame.
        val w = videoTexture.width.toFloat()
        val h = videoTexture.height.toFloat()
        textureRegion = TextureRegion(videoTexture, 0f, 0f, w, h)
        engine.textureManager.loadTexture(videoTexture)

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
        videoTexture.latch()
        super.beginDraw(pGLState)
    }

    override fun onBindShader(pGLState: GLState) {
        val shader = ExternalOESShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        if (shader.uniformMVPMatrixLocation >= 0) {
            GLES20.glUniformMatrix4fv(
                shader.uniformMVPMatrixLocation,
                1, false, pGLState.modelViewProjectionGLMatrix, 0
            )
        }

        // ST-transform: corrects Y-flip and any other orientation from the hardware decoder.
        if (shader.uniformSTMatrixLocation >= 0) {
            GLES20.glUniformMatrix4fv(
                shader.uniformSTMatrixLocation,
                1, false, videoTexture.getTransformMatrix(), 0
            )
        }

        if (shader.uniformTexture0Location >= 0) {
            GLES20.glUniform1i(shader.uniformTexture0Location, 0)
        }

        if (shader.uniformColorLocation >= 0) {
            GLES20.glUniform4f(
                shader.uniformColorLocation,
                drawRed, drawGreen, drawBlue, drawAlpha
            )
        }

        GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)

        // UV attribute (attribute location 3).
        uvBuffer.beginDraw(pGLState)
    }

    // -----------------------------------------------------------------------
    // Playback controls
    // -----------------------------------------------------------------------

    fun play()  { videoTexture.player.start() }
    fun pause() { videoTexture.player.pause() }

    fun seekTo(ms: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            videoTexture.player.seekTo(ms.toLong(), MediaPlayer.SEEK_CLOSEST)
        } else {
            @Suppress("DEPRECATION")
            videoTexture.player.seekTo(ms)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        videoTexture.player.playbackParams = videoTexture.player.playbackParams.setSpeed(speed)
    }

    fun release() {
        videoTexture.player.release()
        engine.textureManager.unloadTexture(videoTexture)
    }

    override fun finalize() {
        // Do NOT call release() here — finalize() runs on a GC thread which has no active GL
        // context.  Calling unloadTexture() from a non-GL thread silently corrupts GL state.
        // Callers must call release() explicitly before dropping the last reference.
        super.finalize()
    }
}
