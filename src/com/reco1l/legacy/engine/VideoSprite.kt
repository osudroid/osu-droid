package com.reco1l.legacy.engine

import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.sprite.Sprite
import javax.microedition.khronos.opengles.GL10

class VideoSprite(source: String, private val engine: Engine) : Sprite(0f, 0f, VideoTexture(source).toRegion())
{

    val texture = textureRegion.texture as VideoTexture

    val isPlaying
        get() = texture.isPlaying


    init
    {
        engine.textureManager.loadTexture(texture)
    }


    fun play() = texture.play()

    fun pause() = texture.pause()

    fun stop() = texture.stop()

    fun release()
    {
        texture.release()
        engine.textureManager.unloadTexture(texture)
    }


    fun seekTo(seconds: Int) = texture.seekTo(seconds)

    fun setPlaybackSpeed(speed: Float) = texture.setPlaybackSpeed(speed)


    override fun doDraw(pGL: GL10, pCamera: Camera?)
    {
        onInitDraw(pGL)
        pGL.glEnable(GL_TEXTURE_EXTERNAL_OES)

        textureRegion.onApply(pGL)

        onApplyVertices(pGL)
        drawVertices(pGL, pCamera)

        pGL.glDisable(GL_TEXTURE_EXTERNAL_OES)
    }

    override fun finalize()
    {
        release()
        super.finalize()
    }
}