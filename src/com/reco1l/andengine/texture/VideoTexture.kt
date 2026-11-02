package com.reco1l.andengine.texture

import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesTextures
import android.graphics.*
import android.net.Uri
import android.opengl.*
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_NEAREST
import android.util.Log
import android.view.*
import android.os.Looper
import android.content.Context
import android.os.Handler
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.decoder.ffmpeg.ExperimentalFfmpegVideoRenderer
import org.andengine.opengl.texture.*
import org.andengine.opengl.texture.PixelFormat
import ru.nsu.ccfit.zuev.osu.GlobalManager
import java.io.*
import java.util.concurrent.atomic.AtomicReference

/**
 * A texture fed by [player] through a `SurfaceTexture`: its hardware id is a `GL_TEXTURE_EXTERNAL_OES`
 * texture, drawn through an external program of the GL core (see `BatchTexture.externalOf`).
 * Open, with the player injectable, so tests can stub the frame latch.
 */
@UnstableApi
open class VideoTexture(val source: String, val player: ExoPlayer) : Texture(
    null,
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

    // ST transform matrix provided by SurfaceTexture — applied to the UV coordinates to
    // correctly orient them (hardware decoders often flip/rotate them).
    private val stMatrix = FloatArray(16).also {
        android.opengl.Matrix.setIdentityM(it, 0)
    }

    constructor(source: String) : this(source, createPlayer())

    init {
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

    /**
     * Latches the latest decoded frame into the OES texture and updates the ST transform matrix.
     * Must be called once per frame, BEFORE [getTransformMatrix] and BEFORE [bind].
     */
    open fun latch() {
        if (isLoadedToHardware) {
            val st = surfaceTexture ?: return
            st.updateTexImage()
            // updateTexImage() binds the OES texture on the active unit without telling the cache.
            GlesBackend.state.noteTextureBound(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mHardwareTextureID)
            st.getTransformMatrix(stMatrix)
        }
    }

    /** Returns the ST transform matrix updated by the most recent [latch] call. */
    open fun getTransformMatrix(): FloatArray = stMatrix

    override fun writeTextureToHardware() {
        // Nothing – the SurfaceTexture pushes frames itself.
    }

    /**
     * Called by [TextureManager.onReload] after context loss.
     * We must release the old [Surface] and [SurfaceTexture] here because [loadToHardware] will create
     * fresh ones; leaving the old ones alive leaks their native resources. The player is kept.
     */
    override fun setNotLoadedToHardware() {
        releaseSurfaces()
        super.setNotLoadedToHardware()
    }

    private fun releaseSurfaces() {
        val s = surface ?: return
        val st = surfaceTexture
        surface = null
        surfaceTexture = null

        postDetachAndRelease(s, st, releasePlayer = false)
    }

    // The decoder may still be writing to the old Surface, so it is detached from the player first, on the
    // player's thread, and only then released (the Surface wraps the SurfaceTexture, so it goes first).
    // Releasing it while still attached abandons the decoder's output and the player stops with an error.
    // The player goes last, after the surface it was writing to is fully torn down.
    private fun postDetachAndRelease(s: Surface?, st: SurfaceTexture?, releasePlayer: Boolean) {
        mainHandler.post {
            try {
                player.setVideoSurface(null)
            } catch (_: Exception) {
                // The player may already be released.
            }

            s?.release()
            st?.release()
            if (releasePlayer) {
                player.release()
            }
        }
    }

    override fun loadToHardware() {
        // Release any lingering Surface and SurfaceTexture (safety net — normally cleared by
        // setNotLoadedToHardware, but guard against direct loadToHardware calls too).
        releaseSurfaces()

        mHardwareTextureID = GlesTextures.generate()
        // OES external textures must be bound to the OES target, with the filters they require.
        GlesTextures.setUpExternal(mHardwareTextureID)

        surfaceTexture = SurfaceTexture(mHardwareTextureID)

        // SurfaceTexture must be created on the GL thread (tied to the GL texture ID),
        // but setVideoSurface must be called on ExoPlayer's main-thread looper.
        // The Surface is stored as a field — releasing it immediately would free the native
        // window before ExoPlayer's playback thread has a chance to configure MediaCodec against it.
        surface = Surface(surfaceTexture)

        mainHandler.post { player.setVideoSurface(surface) }

        mUpdateOnHardwareNeeded = false
    }

    /**
     * A reload on a live context (e.g. returning to gameplay) keeps the SurfaceTexture: its frames come
     * from the player and stay valid. Going through [unloadFromHardware] would release the player.
     */
    override fun reloadToHardware() {
        if (isLoadedToHardware) {
            mUpdateOnHardwareNeeded = false
        } else {
            loadToHardware()
        }
    }

    override fun unloadFromHardware() {

        val st = surfaceTexture
        val s = surface

        surfaceTexture = null
        surface = null

        postDetachAndRelease(s, st, releasePlayer = true)

        super.unloadFromHardware()
    }

    /**
     * Called by [TextureManager] when it drops this texture without [unloadFromHardware]: the context was lost
     * while it waited to be unloaded, or it is released before its upload. The player is released here. The
     * surfaces, if any, are already detached and released by [setNotLoadedToHardware].
     */
    override fun onDroppedWithoutUnload() {
        postDetachAndRelease(null, null, releasePlayer = true)
    }

    /** Binds the OES texture. Call [latch] first to update the frame and ST matrix. */
    override fun bind() {
        if (isLoadedToHardware) {
            GlesBackend.state.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mHardwareTextureID)
        }
    }

    override fun getWidth() = videoWidth
    override fun getHeight() = videoHeight

    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())

        private fun createPlayer(): ExoPlayer {
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
            return ExoPlayer.Builder(context, renderersFactory)
                .setLooper(Looper.getMainLooper())
                .build()
        }
    }
}
