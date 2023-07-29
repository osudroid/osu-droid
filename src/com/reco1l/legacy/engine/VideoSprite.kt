package com.reco1l.legacy.engine

import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.sprite.Sprite
import javax.microedition.khronos.opengles.GL10

class VideoSprite(source: String) : Sprite(0f, 0f, VideoTexture(source).toRegion())
{

    val texture = textureRegion.texture as VideoTexture

    val isPlaying
        get() = texture.isPlaying


    fun play() = texture.play()

    fun pause() = texture.pause()

    fun stop() = texture.stop()

    fun release() = texture.release()


    fun seekTo(seconds: Double) = texture.seekTo(seconds)

    fun setPlaybackSpeed(speed: Float) = texture.setPlaybackSpeed(speed)


    override fun doDraw(pGL: GL10, pCamera: Camera?)
    {
        onInitDraw(pGL)
        pGL.glEnable(VideoTexture.GL_TEXTURE_EXTERNAL_OES)

        textureRegion.onApply(pGL)

        onApplyVertices(pGL)
        drawVertices(pGL, pCamera)

        pGL.glDisable(VideoTexture.GL_TEXTURE_EXTERNAL_OES)
    }
}