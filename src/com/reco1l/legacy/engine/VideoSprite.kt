package com.reco1l.legacy.engine

import android.graphics.Point
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.opengl.GLES10
import android.os.Build
import android.view.Surface
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.opengl.util.GLHelper
import ru.nsu.ccfit.zuev.osu.Config
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToLong

fun getVideoSize(videoPath: String): Point
{
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(videoPath)

    val widthString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
    val heightString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)

    val width = widthString?.toIntOrNull() ?: 0
    val height = heightString?.toIntOrNull() ?: 0

    return Point(width, height)
}

class VideoSprite(source: String) : Rectangle(0f, 0f, 0f, 0f), MediaPlayer.OnVideoSizeChangedListener
{

    val isPlaying
        get() = player.isPlaying


    private val player = MediaPlayer().apply {

        setOnVideoSizeChangedListener(this@VideoSprite)
        setDataSource(source)
        prepare()
        setVolume(0f, 0f)

    }

    private var surfaceTexture: SurfaceTexture? = null

    private var texture: Int? = null



    fun play() = player.start()

    fun pause() = player.pause()

    fun stop() = player.stop()

    fun release()
    {
        player.release()

        if (texture != null)
            GLES10.glDeleteTextures(1, intArrayOf(texture!!), 0)
    }

    fun setPlaybackSpeed(speed: Float)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            player.playbackParams = PlaybackParams().setSpeed(speed)
        }
    }

    fun seekTo(seconds: Double)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            player.seekTo((seconds * 1000).roundToLong(), MediaPlayer.SEEK_CLOSEST_SYNC)
        }
    }


    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int)
    {
        setSize(width.toFloat(), height.toFloat())
        setPosition((Config.getRES_WIDTH() - width) / 2f, (Config.getRES_HEIGHT() - height) / 2f)
    }

    override fun onInitDraw(graphics: GL10?)
    {
        GLHelper.setColor(graphics, mRed, mGreen, mBlue, mAlpha)
        GLHelper.blendFunction(graphics, mSourceBlendFunction, mDestinationBlendFunction)

        GLHelper.enableTextures(graphics)
        GLHelper.enableVertexArray(graphics)
        GLHelper.enableTexCoordArray(graphics)
    }

    override fun doDraw(graphics: GL10, camera: Camera?)
    {
        graphics.glEnable(GL_TEXTURE_EXTERNAL_OES)

        if (texture == null)
        {
            val textures = IntArray(1)
            graphics.glGenTextures(1, textures, 0)
            texture = textures[0]

            graphics.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture!!)
            graphics.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat())
            graphics.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())

            surfaceTexture = SurfaceTexture(texture!!)
            surfaceTexture!!.setDefaultBufferSize(Config.getRES_WIDTH(), Config.getRES_HEIGHT())

            Surface(surfaceTexture).also {

                player.setSurface(it)
                it.release()
            }
        }
        onInitDraw(graphics)

        graphics.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture!!)
        surfaceTexture!!.updateTexImage()

        onApplyVertices(graphics)
        drawVertices(graphics, camera)
        graphics.glDisable(GL_TEXTURE_EXTERNAL_OES)
    }

    companion object
    {
        const val GL_TEXTURE_EXTERNAL_OES = 0x8D65
    }
}