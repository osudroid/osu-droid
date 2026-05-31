package com.reco1l.andengine.texture

import android.graphics.*
import android.net.Uri
import android.opengl.*
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import android.content.Context
import android.os.Handler
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.decoder.ffmpeg.ExperimentalFfmpegVideoRenderer
import org.anddev.andengine.opengl.texture.*
import ru.nsu.ccfit.zuev.osu.GlobalManager
import java.io.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*

@UnstableApi
class VideoTexture(val source: String) : Texture(
    PixelFormat.UNDEFINED,
    TextureOptions(
        GL_NEAREST,
        GL_LINEAR,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
        false
    ),
    null
) {

    val player: ExoPlayer

    /**
     * The callback that is invoked when the video dimensions are available. This is necessary because the dimensions
     * are not known until the video is prepared, which happens asynchronously.
     *
     * The callback will be invoked on the main thread.
     */
    @Volatile
    var onReady: Runnable? = null
        set(value) {
            field = value

            // Handle the race where dimensions arrived before the callback was registered.
            if (videoWidth > 0) {
                value?.run()
            }
        }

    @Volatile
    var videoWidth = 0
        private set

    @Volatile
    var videoHeight = 0
        private set

    private var surfaceTexture: SurfaceTexture? = null


    init {
        val context = GlobalManager.getInstance().mainActivity.applicationContext

        // DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON auto-discovers FfmpegAudioRenderer via reflection.
        // ExperimentalFfmpegVideoRenderer must be appended manually because DefaultRenderersFactory looks for the old
        // name "FfmpegVideoRenderer" (since renamed).
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>
            ) {
                super.buildVideoRenderers(context, extensionRendererMode, mediaCodecSelector,
                    enableDecoderFallback, eventHandler, eventListener, allowedVideoJoiningTimeMs, out)

                out.add(ExperimentalFfmpegVideoRenderer(
                    allowedVideoJoiningTimeMs, eventHandler, eventListener, 50))
            }
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        player = ExoPlayer.Builder(context, renderersFactory)
            .setLooper(Looper.getMainLooper())
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        videoWidth = videoSize.width
                        videoHeight = videoSize.height
                        onReady?.run()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("VideoTexture", "ExoPlayer error occurred", error)
                    }
                })
                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(source))))
                prepare()
            }
    }

    override fun writeTextureToHardware(pGL: GL10) {
        // Nothing to write, the texture is handled externally by the SurfaceTexture.
    }

    override fun bindTextureOnHardware(pGL: GL10) {

        pGL.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mHardwareTextureID)

        surfaceTexture = SurfaceTexture(mHardwareTextureID)

        val surface = Surface(surfaceTexture)
        player.setVideoSurface(surface)
        surface.release()
    }

    override fun deleteTextureOnHardware(pGL: GL10?) {

        surfaceTexture?.release()
        surfaceTexture = null

        super.deleteTextureOnHardware(pGL)
    }


    override fun bind(pGL: GL10) {
        if (isLoadedToHardware) {
            surfaceTexture?.updateTexImage()
        }
    }


    override fun getWidth() = videoWidth
    override fun getHeight() = videoHeight
}
