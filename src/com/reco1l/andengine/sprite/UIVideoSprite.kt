package com.reco1l.andengine.sprite

import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import com.reco1l.andengine.texture.*
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.sprite.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

@UnstableApi
class UIVideoSprite(source: String, private val engine: Engine) : Sprite(0f, 0f, VideoTexture(source).let {
    TextureRegion(it, 0, 0, it.width, it.height)
}) {

    private val texture = textureRegion.texture as VideoTexture


    private var isMaliGPU: Boolean? = null


    init {
        engine.textureManager.loadTexture(texture)
    }


    override fun onInitDraw(pGL: GL10) {

        if (isMaliGPU == null) {
            isMaliGPU = pGL.glGetString(GL10.GL_RENDERER).contains("Mali", true)
        }

        super.onInitDraw(pGL)

        // Apparently there is either a bug or unintended behavior in Mali GPUs' OpenGL ES implementation.
        // Causes the wrong texture to be displayed when GL_TEXTURE_2D is enabled before enabling GL_TEXTURE_EXTERNAL_OES.
        if (isMaliGPU!!) {
            GLHelper.disableTextures(pGL)
        }

        pGL.glEnable(GL_TEXTURE_EXTERNAL_OES)
    }

    override fun drawVertices(gl: GL10, camera: Camera) {
        super.drawVertices(gl, camera)

        gl.glDisable(GL_TEXTURE_EXTERNAL_OES)
    }


    fun setOnReady(callback: Runnable) {
        texture.onReady = callback
    }

    fun release() {
        texture.player.release()
        engine.textureManager.unloadTexture(texture)
    }

    fun play() {
        texture.player.play()
    }

    fun pause() {
        texture.player.pause()
    }

    fun seekTo(ms: Int) {
        texture.player.seekTo(ms.toLong())
    }

    fun setPlaybackSpeed(speed: Float) {
        texture.player.playbackParameters = PlaybackParameters(speed)
    }


    override fun finalize() {
        release()
        super.finalize()
    }
}
