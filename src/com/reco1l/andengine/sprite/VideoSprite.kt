package com.reco1l.andengine.sprite

import android.media.*
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.os.*
import com.reco1l.andengine.texture.*
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.sprite.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

class VideoSprite(source: String, private val engine: Engine) : Sprite(0f, 0f, VideoTexture(source).let {
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


    fun release() {
        texture.player.release()
        engine.textureManager.unloadTexture(texture)
    }

    fun play() {
        texture.player.start()
    }

    fun pause() {
        texture.player.pause()
    }

    fun seekTo(ms: Int) {
        // Unfortunately in old versions we can't seek at closest frame from the desired position.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            texture.player.seekTo(ms.toLong(), MediaPlayer.SEEK_CLOSEST)
        } else {
            texture.player.seekTo(ms)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        texture.player.playbackParams = texture.player.playbackParams.setSpeed(speed)
    }


    override fun finalize() {
        release()
        super.finalize()
    }
}


