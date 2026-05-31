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
import java.util.concurrent.atomic.AtomicReference
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

    private val onReadyRef = AtomicReference<Runnable?>()

    /**
     * The callback that is invoked when the video dimensions are available. This is necessary because the dimensions
     * are not known until the video is prepared, which happens asynchronously.
     *
     * The callback will be invoked on the main thread exactly once.
     */
    var onReady
        get() = onReadyRef.get()
        set(value) {
            onReadyRef.set(value)

            // Handle the race where dimensions arrived before the callback was registered.
            // Uses getAndSet(null) so only one caller (this path or onVideoSizeChanged) wins.
            if (videoWidth > 0) {
                mainHandler.post { onReadyRef.getAndSet(null)?.run() }
            }
        }

    @Volatile
    var videoWidth = 0
        private set

    @Volatile
    var videoHeight = 0
        private set

    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null

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

        // ExoPlayer's builder is thread-safe, but subsequent API calls are not.
        // ExoPlayer enforces that they run on its designated looper (main thread here).
        player = ExoPlayer.Builder(context, renderersFactory)
            .setLooper(Looper.getMainLooper())
            .build()

        mainHandler.post {
            player.volume = 0f
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width == 0 || videoSize.height == 0) {
                        return
                    }

                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                    onReadyRef.getAndSet(null)?.run()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("VideoTexture", "ExoPlayer error occurred", error)
                }
            })
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(source))))
            player.prepare()
        }
    }

    override fun writeTextureToHardware(pGL: GL10) {
        // Nothing to write, the texture is handled externally by the SurfaceTexture.
    }

    override fun bindTextureOnHardware(pGL: GL10) {

        pGL.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mHardwareTextureID)

        surfaceTexture = SurfaceTexture(mHardwareTextureID)

        // SurfaceTexture must be created on the GL thread (tied to the GL texture ID),
        // but setVideoSurface must be called on ExoPlayer's main-thread looper.
        // The Surface is stored as a field — releasing it immediately would free the native
        // window before ExoPlayer's playback thread has a chance to configure MediaCodec against it.
        surface = Surface(surfaceTexture)

        mainHandler.post { player.setVideoSurface(surface) }
    }

    override fun deleteTextureOnHardware(pGL: GL10?) {

        val st = surfaceTexture
        val s = surface

        surfaceTexture = null
        surface = null

        mainHandler.post {
            // Order matters:
            // 1. Clear ExoPlayer's surface so the codec thread stops writing to the native window.
            // 2. Release the Java Surface wrapper.
            // 3. Release the SurfaceTexture.
            // 4. Release the player last, after the surface it was writing to is fully torn down.
            try {
                player.setVideoSurface(null)
            } catch (_: Exception) {}

            s?.release()
            st?.release()
            player.release()
        }

        super.deleteTextureOnHardware(pGL)
    }


    override fun bind(pGL: GL10) {
        if (isLoadedToHardware) {
            surfaceTexture?.updateTexImage()
        }
    }


    override fun getWidth() = videoWidth
    override fun getHeight() = videoHeight

    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
