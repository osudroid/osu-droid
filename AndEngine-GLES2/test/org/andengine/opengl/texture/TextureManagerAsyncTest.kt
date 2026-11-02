package org.andengine.opengl.texture

import android.graphics.Bitmap
import android.opengl.GLES20
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.texture.Pixels
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource
import org.andengine.opengl.texture.bitmap.BitmapTextureFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

@RunWith(RobolectricTestRunner::class)
class TextureManagerAsyncTest {

    /** Runs submitted preparations only when the test says so, on the test thread. */
    private class ManualExecutor : Executor {
        val tasks = ArrayList<Runnable>()
        var reject = false

        override fun execute(command: Runnable) {
            if (reject) throw RejectedExecutionException()
            tasks += command
        }

        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeAt(0).run()
        }
    }

    /** A 2x2 pixel source; [failures] loads throw first, [nullPixels] makes it ask for the bitmap path. */
    private class FakePixelSource(
        val seed: Int = 1,
        var failures: Int = 0,
        var nullPixels: Boolean = false,
        var error: Error? = null,
        val premultipliedPixels: Boolean = false
    ) : BaseTextureAtlasSource(0, 0, 2, 2), IPixelTextureAtlasSource {
        val bytes = ByteArray(2 * 2 * 4) { (it * 7 + seed).toByte() }
        var pixelLoads = 0
        var bitmapLoads = 0
        val requestedVariants = ArrayList<Boolean>()

        override fun canLoadPixels(premultiplied: Boolean) = !premultiplied || premultipliedPixels

        override fun onLoadPixels(): Pixels? = onLoadPixels(false)

        override fun onLoadPixels(premultiplied: Boolean): Pixels? {
            pixelLoads++
            requestedVariants += premultiplied
            error?.let { error = null; throw it }
            if (failures > 0) {
                failures--
                throw IllegalStateException("decode failed")
            }
            if (nullPixels) return null
            val data = ByteBuffer.allocateDirect(bytes.size)
            data.put(bytes).flip()
            return Pixels(2, 2, data, premultiplied)
        }

        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap {
            bitmapLoads++
            return Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        }

        override fun deepCopy() = FakePixelSource(seed)
    }

    private class FakeBitmapSource : BaseTextureAtlasSource(0, 0, 2, 2), IBitmapTextureAtlasSource {
        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        override fun deepCopy() = FakeBitmapSource()
    }

    private lateinit var gl: RecordingGL
    private val executor = ManualExecutor()

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        GlesBackend.onContextCreatedForTest(gl)
        gl.calls.clear()
        gl.texSubImage2DBytes.clear()
    }

    private fun manager(uploadBytesPerFrame: Long = 1L shl 30, bytesInFlight: Long = 1L shl 30) =
        TextureManager(executor, uploadBytesPerFrame, bytesInFlight)

    private fun atlas(manager: TextureManager, vararg sources: IBitmapTextureAtlasSource, options: TextureOptions = TextureOptions.BILINEAR, size: Int = 16) =
        BitmapTextureAtlas(manager, size, size, BitmapTextureFormat.RGBA_8888, options).also { atlas ->
            sources.forEachIndexed { i, source -> atlas.addTextureAtlasSource(source, i * 4, 0) }
        }

    private fun pixelUploads() = gl.calls.count { it.startsWith("glTexSubImage2D(") && it.endsWith(",buf)") }

    /** The bitmap uploads (converted by getPixels) as `bitmap(x,y,width,height)`. */
    private fun bitmapUploads() = gl.calls.filter { it.startsWith("glTexSubImage2D(") && it.endsWith(",ints)") }.map { call ->
        val args = call.removePrefix("glTexSubImage2D(").removeSuffix(")").split(",")
        "bitmap(${args[2]},${args[3]},${args[4]},${args[5]})"
    }


    @Test
    fun asyncPrepare_uploadsOnlyWhenReady() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        manager.loadTexture(texture)

        manager.updateTextures()

        assertFalse("not uploaded before the pixels are prepared", texture.isLoadedToHardware)
        assertTrue(manager.isTextureLoadPending(texture))
        assertEquals("nothing decoded on the GL thread", 0, source.pixelLoads)
        assertEquals(1, executor.tasks.size)

        // Still pending: further frames neither upload nor submit again.
        manager.updateTextures()
        assertFalse(texture.isLoadedToHardware)
        assertEquals(1, executor.tasks.size)

        executor.runAll()
        assertEquals(1, source.pixelLoads)
        assertFalse("uploaded only by the GL thread", texture.isLoadedToHardware)

        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertFalse(manager.isTextureLoadPending(texture))
        assertEquals("the GL thread only uploaded the prepared pixels", 1, source.pixelLoads)
        assertEquals(1, pixelUploads())
        assertArrayEquals(source.bytes, gl.texSubImage2DBytes.single())
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun asyncPrepare_failureFallsBackToSync() {
        val manager = manager()
        val source = FakePixelSource(failures = 1)
        val texture = atlas(manager, source)
        manager.loadTexture(texture)

        manager.updateTextures()
        executor.runAll()
        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertEquals("decoded again on the synchronous path", 2, source.pixelLoads)
        assertEquals(1, pixelUploads())
        assertArrayEquals(source.bytes, gl.texSubImage2DBytes.single())
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun asyncPrepare_errorFallsBackToSync() {
        val manager = manager()
        val source = FakePixelSource(error = ExceptionInInitializerError("decoder class failed"))
        val texture = atlas(manager, source)
        manager.loadTexture(texture)

        manager.updateTextures()
        executor.runAll()
        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertEquals("decoded again on the synchronous path", 2, source.pixelLoads)
        assertArrayEquals(source.bytes, gl.texSubImage2DBytes.single())
        assertEquals("in-flight memory freed", 0, manager.preparationsInFlight)
        assertFalse(manager.hasPendingTextureLoads())
    }

    @Test
    fun onDestroy_cancelsPreparations() {
        val manager = manager()
        val pending = FakePixelSource(seed = 1)
        val ready = FakePixelSource(seed = 2)
        val pendingTexture = atlas(manager, pending)
        val readyTexture = atlas(manager, ready)
        manager.loadTexture(readyTexture)
        manager.updateTextures()
        executor.runAll()
        manager.loadTexture(pendingTexture)
        manager.updateTextures()
        assertEquals(1, executor.tasks.size)

        manager.onDestroy()

        assertFalse(manager.hasPendingTextureLoads())
        assertEquals("the queued preparation still has to run", 1, manager.preparationsInFlight)
        executor.runAll()
        assertEquals("cancelled before it started", 0, pending.pixelLoads)
        assertEquals(0, manager.preparationsInFlight)
        assertFalse(pendingTexture.isLoadedToHardware)
    }

    @Test
    fun awaitPendingTextureLoads_returnsOnceTheQueueIsEmpty() {
        val manager = manager()
        val texture = atlas(manager, FakePixelSource())
        manager.loadTexture(texture)
        manager.updateTextures()

        assertFalse("times out while pending", manager.awaitPendingTextureLoads(10))

        executor.runAll()
        val gl = Thread {
            Thread.sleep(50)
            manager.updateTextures()
        }
        gl.start()
        assertTrue(manager.awaitPendingTextureLoads(5000))
        gl.join()
        assertTrue(texture.isLoadedToHardware)
    }

    @Test
    fun awaitPendingTextureLoads_wakesOnDirectLoadOfTheLastTexture() {
        val manager = manager()
        val texture = atlas(manager, FakePixelSource())
        manager.loadTexture(texture)

        var result = false
        var elapsed = 0L
        val waiter = Thread {
            val start = System.nanoTime()
            result = manager.awaitPendingTextureLoads(10000)
            elapsed = (System.nanoTime() - start) / 1000000L
        }
        waiter.start()
        Thread.sleep(100)
        manager.loadTextureNow(texture)
        waiter.join(5000)

        assertTrue(result)
        assertTrue("woken early, took $elapsed ms", elapsed < 5000)
    }

    @Test
    fun onDestroy_keepsInjectedExecutorAndOwnWorkerIsRecreated() {
        val manager = manager()
        manager.onDestroy()
        manager.loadTexture(atlas(manager, FakePixelSource()))
        manager.updateTextures()
        assertEquals(1, executor.tasks.size)

        val own = TextureManager(null, 1L shl 30, 1L shl 30)
        own.onDestroy()
        val texture = atlas(own, FakePixelSource())
        own.loadTexture(texture)
        own.updateTextures()
        own.onDestroy()
        own.loadTexture(texture)
        own.updateTextures()
        own.onDestroy()
    }

    @Test
    fun asyncPrepare_rejectedSubmissionLoadsSync() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        manager.loadTexture(texture)
        executor.reject = true

        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertEquals(1, source.pixelLoads)
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun unloadBeforeReady_dropsPreparedPixels() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        manager.loadTexture(texture)
        manager.updateTextures()
        executor.runAll()

        manager.unloadTexture(texture)
        manager.updateTextures()

        assertFalse(texture.isLoadedToHardware)
        assertEquals(0, pixelUploads())
        assertEquals("prepared pixels released", 0, manager.preparationsInFlight)
        assertFalse(manager.isTextureLoadPending(texture))

        // Loading it again prepares anew instead of using the dropped pixels.
        manager.loadTexture(texture)
        manager.updateTextures()
        assertFalse(texture.isLoadedToHardware)
        executor.runAll()
        manager.updateTextures()
        assertTrue(texture.isLoadedToHardware)
        assertEquals(2, source.pixelLoads)
    }

    @Test
    fun unloadBeforeStart_skipsPreparation() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        manager.loadTexture(texture)
        manager.updateTextures()

        manager.unloadTexture(texture)
        assertEquals("a cancelled preparation still counts until it ran", 1, manager.preparationsInFlight)
        executor.runAll()
        manager.updateTextures()

        assertEquals("cancelled before it started", 0, source.pixelLoads)
        assertFalse(texture.isLoadedToHardware)
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun contextLoss_preparesAgain() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        manager.loadTexture(texture)
        manager.updateTextures()
        executor.runAll()
        manager.updateTextures()
        assertTrue(texture.isLoadedToHardware)

        manager.onReload()
        manager.updateTextures()

        assertFalse("waits for the new preparation", texture.isLoadedToHardware)
        assertEquals(1, source.pixelLoads)
        executor.runAll()
        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertEquals(2, source.pixelLoads)
        assertEquals(2, pixelUploads())
        assertArrayEquals(source.bytes, gl.texSubImage2DBytes[1])
    }

    @Test
    fun contextLossWhilePreparing_dropsTheOldResult() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        manager.loadTexture(texture)
        manager.updateTextures()

        manager.onReload()
        executor.runAll()
        manager.updateTextures()

        assertFalse(texture.isLoadedToHardware)
        assertEquals(1, executor.tasks.size)
        executor.runAll()
        manager.updateTextures()
        assertTrue(texture.isLoadedToHardware)
        assertEquals(1, pixelUploads())
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun nonPixelAtlases_loadInTheSameFrame() {
        val manager = manager()
        val bitmapAtlas = atlas(manager, FakeBitmapSource())
        val mixedAtlas = atlas(manager, FakePixelSource(), FakeBitmapSource())
        val premultiplied = atlas(manager, FakePixelSource(), options = TextureOptions.BILINEAR_PREMULTIPLYALPHA)
        manager.loadTexture(bitmapAtlas)
        manager.loadTexture(mixedAtlas)
        manager.loadTexture(premultiplied)

        manager.updateTextures()

        assertTrue(bitmapAtlas.isLoadedToHardware)
        assertTrue(mixedAtlas.isLoadedToHardware)
        assertTrue(premultiplied.isLoadedToHardware)
        assertTrue(executor.tasks.isEmpty())
    }

    @Test
    fun premultipliedAtlas_preparedInBackground() {
        val manager = manager()
        val source = FakePixelSource(premultipliedPixels = true)
        val texture = atlas(manager, source, options = TextureOptions.BILINEAR_PREMULTIPLYALPHA)
        manager.loadTexture(texture)

        manager.updateTextures()
        assertFalse(texture.isLoadedToHardware)
        assertEquals("nothing decoded on the GL thread", 0, source.pixelLoads)
        assertEquals(1, executor.tasks.size)

        executor.runAll()
        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertEquals(listOf(true), source.requestedVariants)
        assertEquals(0, source.bitmapLoads)
        assertEquals(1, pixelUploads())
        assertArrayEquals(source.bytes, gl.texSubImage2DBytes.single())
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun nullPreparedPixels_useTheBitmapPath() {
        val manager = manager()
        val source = FakePixelSource(nullPixels = true)
        val texture = atlas(manager, source)
        manager.loadTexture(texture)
        manager.updateTextures()
        executor.runAll()
        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertEquals("pixels not asked for again", 1, source.pixelLoads)
        assertEquals(1, source.bitmapLoads)
        assertEquals(1, bitmapUploads().size)
    }

    @Test
    fun sourcesChangedAfterPreparation_loadSync() {
        val manager = manager()
        val first = FakePixelSource(seed = 1)
        val second = FakePixelSource(seed = 2)
        val texture = atlas(manager, first)
        manager.loadTexture(texture)
        manager.updateTextures()
        executor.runAll()

        texture.addTextureAtlasSource(second, 8, 8)
        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertEquals("stale preparation ignored, decoded on the GL thread", 2, first.pixelLoads)
        assertEquals(1, second.pixelLoads)
        assertEquals(2, pixelUploads())
    }

    @Test
    fun immediateLoad_cancelsPreparation() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        manager.loadTexture(texture)
        manager.updateTextures()

        manager.loadTextureNow(texture)
        executor.runAll()
        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertEquals(1, source.pixelLoads)
        assertEquals(1, pixelUploads())
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun uploadBudget_limitsUploadsPerFrame() {
        // 16 bytes per texture; a budget of 20 bytes fits one upload per frame.
        val manager = manager(uploadBytesPerFrame = 20)
        val textures = List(3) { atlas(manager, FakePixelSource(seed = it)) }
        textures.forEach { manager.loadTexture(it) }
        manager.updateTextures()
        executor.runAll()

        manager.updateTextures()
        assertEquals(listOf(true, false, false), textures.map { it.isLoadedToHardware })
        manager.updateTextures()
        assertEquals(listOf(true, true, false), textures.map { it.isLoadedToHardware })
        manager.updateTextures()
        assertEquals(listOf(true, true, true), textures.map { it.isLoadedToHardware })
    }

    @Test
    fun uploadBudget_alwaysUploadsOneTexture() {
        val manager = manager(uploadBytesPerFrame = 1)
        val texture = atlas(manager, FakePixelSource())
        manager.loadTexture(texture)
        manager.updateTextures()
        executor.runAll()

        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
    }

    @Test
    fun bytesInFlight_limitsPreparations() {
        // Each 16x16 atlas is estimated at 1024 bytes; the limit allows one at a time (at least one always).
        val manager = manager(bytesInFlight = 1500)
        val textures = List(3) { atlas(manager, FakePixelSource(seed = it)) }
        textures.forEach { manager.loadTexture(it) }

        manager.updateTextures()
        assertEquals(1, executor.tasks.size)
        assertEquals(1, manager.preparationsInFlight)

        executor.runAll()
        manager.updateTextures()
        assertEquals("first uploaded, second submitted", 1, textures.count { it.isLoadedToHardware })
        assertEquals(1, executor.tasks.size)

        executor.runAll()
        manager.updateTextures()
        executor.runAll()
        manager.updateTextures()
        assertTrue(textures.all { it.isLoadedToHardware })
        assertEquals(0, manager.preparationsInFlight)
    }

    /** Loads [textures] through the background path until all of them are on the GPU. */
    private fun loadAll(manager: TextureManager, vararg textures: ITexture) {
        textures.forEach { manager.loadTexture(it) }
        while (textures.any { !it.isLoadedToHardware }) {
            manager.updateTextures()
            executor.runAll()
        }
    }

    @Test
    fun reloadLoaded_keepsOldTextureUntilPrepared() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        loadAll(manager, texture)
        val oldName = texture.hardwareTextureID
        assertEquals(1, pixelUploads())

        manager.reloadLoadedTextures()
        manager.updateTextures()

        assertTrue("the old texture stays loaded and drawn", texture.isLoadedToHardware)
        assertEquals(oldName, texture.hardwareTextureID)
        assertTrue(texture.isUpdateOnHardwareNeeded)
        assertEquals("nothing decoded on the GL thread", 1, source.pixelLoads)
        assertEquals(1, pixelUploads())
        assertEquals(1, executor.tasks.size)
        assertTrue(manager.hasPendingTextureReloads())
        assertFalse("a reload is not a pending load", manager.hasPendingTextureLoads())
        assertFalse(manager.isTextureLoadPending(texture))

        // Still preparing: further frames neither upload nor submit again.
        manager.updateTextures()
        assertEquals(oldName, texture.hardwareTextureID)
        assertEquals(1, executor.tasks.size)

        executor.runAll()
        assertEquals(2, source.pixelLoads)
        assertEquals("uploaded only by the GL thread", oldName, texture.hardwareTextureID)

        manager.updateTextures()

        assertTrue(texture.isLoadedToHardware)
        assertTrue("uploaded into a new texture name", texture.hardwareTextureID != oldName)
        assertFalse(texture.isUpdateOnHardwareNeeded)
        assertEquals("the GL thread only uploaded the prepared pixels", 2, source.pixelLoads)
        assertEquals(2, pixelUploads())
        assertArrayEquals(source.bytes, gl.texSubImage2DBytes[1])
        assertEquals(0, manager.preparationsInFlight)
        assertFalse(manager.hasPendingTextureReloads())
    }

    @Test
    fun reloadLoaded_uploadsWithinBudget() {
        // 16 bytes per texture; a budget of 20 bytes fits one upload per frame.
        val manager = manager(uploadBytesPerFrame = 20)
        val textures = List(3) { atlas(manager, FakePixelSource(seed = it)) }
        loadAll(manager, *textures.toTypedArray())
        val oldNames = textures.map { it.hardwareTextureID }

        manager.reloadLoadedTextures()
        manager.updateTextures()
        assertEquals(3, executor.tasks.size)
        executor.runAll()

        fun reloaded() = textures.mapIndexed { i, texture -> texture.hardwareTextureID != oldNames[i] }.count { it }
        manager.updateTextures()
        assertEquals(1, reloaded())
        manager.updateTextures()
        assertEquals(2, reloaded())
        manager.updateTextures()
        assertEquals(3, reloaded())
        assertTrue(textures.all { it.isLoadedToHardware && !it.isUpdateOnHardwareNeeded })
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun reloadLoaded_budgetIsSharedWithLoads() {
        val manager = manager(uploadBytesPerFrame = 20)
        val reloaded = atlas(manager, FakePixelSource(seed = 1))
        loadAll(manager, reloaded)
        val oldName = reloaded.hardwareTextureID

        manager.reloadLoadedTextures()
        val loaded = atlas(manager, FakePixelSource(seed = 2))
        manager.loadTexture(loaded)
        manager.updateTextures()
        executor.runAll()

        manager.updateTextures()
        assertEquals("one upload in this frame", 1, listOf(reloaded.hardwareTextureID != oldName, loaded.isLoadedToHardware).count { it })
        manager.updateTextures()
        assertTrue(reloaded.hardwareTextureID != oldName)
        assertTrue(loaded.isLoadedToHardware)
    }

    @Test
    fun reloadLoaded_bytesInFlightLimitsPreparations() {
        // Each 16x16 atlas is estimated at 1024 bytes; the limit allows one at a time.
        val manager = manager(bytesInFlight = 1500)
        val textures = List(2) { atlas(manager, FakePixelSource(seed = it)) }
        loadAll(manager, *textures.toTypedArray())

        manager.reloadLoadedTextures()
        manager.updateTextures()
        assertEquals(1, executor.tasks.size)
        assertEquals(1, manager.preparationsInFlight)

        executor.runAll()
        manager.updateTextures()
        assertEquals("first uploaded, second submitted", 1, textures.count { !it.isUpdateOnHardwareNeeded })
        assertEquals(1, executor.tasks.size)

        executor.runAll()
        manager.updateTextures()
        assertTrue(textures.none { it.isUpdateOnHardwareNeeded })
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun reloadPreparation_cancelledOnUnloadAndContextLoss() {
        val manager = manager()

        // Unload while the re-preparation is queued: it never runs and nothing is uploaded.
        val unloadedSource = FakePixelSource(seed = 1)
        val unloaded = atlas(manager, unloadedSource)
        loadAll(manager, unloaded)
        manager.reloadLoadedTextures()
        manager.updateTextures()
        manager.unloadTexture(unloaded)
        executor.runAll()
        manager.updateTextures()
        assertFalse(manager.hasPendingTextureReloads())

        assertFalse(unloaded.isLoadedToHardware)
        assertEquals("cancelled before it started", 1, unloadedSource.pixelLoads)
        assertEquals(1, pixelUploads())
        assertEquals(0, manager.preparationsInFlight)

        // Context loss with prepared pixels waiting: they are dropped and the texture prepares anew.
        val lostSource = FakePixelSource(seed = 2)
        val lost = atlas(manager, lostSource)
        loadAll(manager, lost)
        assertEquals(2, pixelUploads())
        manager.reloadLoadedTextures()
        manager.updateTextures()
        executor.runAll()
        assertEquals(2, lostSource.pixelLoads)

        manager.onReload()
        assertFalse(manager.hasPendingTextureReloads())
        manager.updateTextures()

        assertFalse("waits for the new preparation", lost.isLoadedToHardware)
        assertEquals(2, pixelUploads())
        assertEquals(1, executor.tasks.size)
        executor.runAll()
        manager.updateTextures()
        assertTrue(lost.isLoadedToHardware)
        assertFalse(lost.isUpdateOnHardwareNeeded)
        assertEquals(3, lostSource.pixelLoads)
        assertEquals(3, pixelUploads())
        assertEquals(0, manager.preparationsInFlight)

        // Destroy while re-preparing.
        val destroyedSource = FakePixelSource(seed = 3)
        val destroyed = atlas(manager, destroyedSource)
        loadAll(manager, destroyed)
        manager.reloadLoadedTextures()
        manager.updateTextures()
        manager.onDestroy()
        assertFalse(manager.hasPendingTextureReloads())
        executor.runAll()
        assertEquals(1, destroyedSource.pixelLoads)
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun reloadLoaded_nonPreparableStaysSync() {
        val manager = manager()
        val bitmapAtlas = atlas(manager, FakeBitmapSource())
        val mixedAtlas = atlas(manager, FakePixelSource(), FakeBitmapSource())
        loadAll(manager, bitmapAtlas, mixedAtlas)
        val oldNames = listOf(bitmapAtlas.hardwareTextureID, mixedAtlas.hardwareTextureID)

        manager.reloadLoadedTextures()
        manager.updateTextures()

        assertTrue(bitmapAtlas.hardwareTextureID != oldNames[0])
        assertTrue(mixedAtlas.hardwareTextureID != oldNames[1])
        assertFalse(bitmapAtlas.isUpdateOnHardwareNeeded)
        assertFalse(mixedAtlas.isUpdateOnHardwareNeeded)
        assertTrue(executor.tasks.isEmpty())
        assertFalse(manager.hasPendingTextureReloads())
    }

    @Test
    fun reloadLoaded_secondRequestRestartsPreparation() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        loadAll(manager, texture)

        manager.reloadLoadedTextures()
        manager.updateTextures()
        manager.reloadLoadedTextures()
        manager.updateTextures()
        assertEquals("the first preparation is cancelled, a new one submitted", 2, executor.tasks.size)

        executor.runAll()
        assertEquals("only the new preparation loaded pixels", 2, source.pixelLoads)
        manager.updateTextures()

        assertFalse(texture.isUpdateOnHardwareNeeded)
        assertEquals("uploaded once", 2, pixelUploads())
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun reloadLoaded_immediateLoadReloadsSync() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        loadAll(manager, texture)
        val oldName = texture.hardwareTextureID

        manager.reloadLoadedTextures()
        manager.updateTextures()
        manager.loadTextureNow(texture)

        assertTrue(texture.hardwareTextureID != oldName)
        assertFalse(texture.isUpdateOnHardwareNeeded)
        assertFalse(manager.hasPendingTextureReloads())
        executor.runAll()
        manager.updateTextures()
        assertEquals("cancelled before it started", 2, source.pixelLoads)
        assertEquals(2, pixelUploads())
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun reloadLoaded_sourceAddedWhilePreparingReloadsSync() {
        val manager = manager()
        val first = FakePixelSource(seed = 1)
        val second = FakePixelSource(seed = 2)
        val texture = atlas(manager, first)
        loadAll(manager, texture)
        val oldName = texture.hardwareTextureID

        manager.reloadLoadedTextures()
        manager.updateTextures()
        texture.addTextureAtlasSource(second, 8, 8)
        manager.updateTextures()

        assertTrue("the new source is on the GPU right away", texture.hardwareTextureID != oldName)
        assertFalse(texture.isUpdateOnHardwareNeeded)
        assertEquals(1, second.pixelLoads)
        executor.runAll()
        assertEquals(0, manager.preparationsInFlight)
        assertFalse(manager.hasPendingTextureReloads())
    }

    @Test
    fun updateFlagWithoutReloadRequest_staysSync() {
        val manager = manager()
        val texture = atlas(manager, FakePixelSource())
        loadAll(manager, texture)
        val oldName = texture.hardwareTextureID

        texture.isUpdateOnHardwareNeeded = true
        manager.updateTextures()

        assertTrue(texture.hardwareTextureID != oldName)
        assertTrue(executor.tasks.isEmpty())
    }

    @Test
    fun queuedLoad_isPreparedAndUploadedBeforeReload() {
        // Each 16x16 atlas is estimated at 1024 bytes; the limit allows one preparation at a time.
        val manager = manager(bytesInFlight = 1500)
        val reloaded = atlas(manager, FakePixelSource(seed = 1))
        loadAll(manager, reloaded)
        val oldName = reloaded.hardwareTextureID

        manager.reloadLoadedTextures()
        val loaded = atlas(manager, FakePixelSource(seed = 2))
        manager.loadTexture(loaded)
        manager.updateTextures()
        assertEquals("only the load is prepared", 1, executor.tasks.size)
        assertEquals(1, manager.preparationsInFlight)

        executor.runAll()
        manager.updateTextures()
        assertTrue("the load is uploaded first", loaded.isLoadedToHardware)
        assertEquals("the reload still shows the old texture", oldName, reloaded.hardwareTextureID)
        assertEquals("the reload is prepared next", 1, executor.tasks.size)

        executor.runAll()
        manager.updateTextures()
        assertTrue(reloaded.hardwareTextureID != oldName)
        assertFalse(reloaded.isUpdateOnHardwareNeeded)
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun reloadPreparation_notStartedWhileLoadsWait() {
        // 1024 bytes per 16x16 atlas, 4096 per 32x32: the large load does not fit next to the small one; a small
        // re-preparation would, but must not take the room the waiting load needs.
        val manager = manager(bytesInFlight = 5000)
        val reloaded = atlas(manager, FakePixelSource(seed = 1))
        loadAll(manager, reloaded)
        val oldName = reloaded.hardwareTextureID

        manager.reloadLoadedTextures()
        val small = atlas(manager, FakePixelSource(seed = 2))
        val large = atlas(manager, FakePixelSource(seed = 3), size = 32)
        manager.loadTexture(small)
        manager.loadTexture(large)
        manager.updateTextures()
        assertEquals("the large load waits, no re-preparation starts", 1, executor.tasks.size)

        executor.runAll()
        manager.updateTextures()
        assertTrue(small.isLoadedToHardware)
        assertEquals(oldName, reloaded.hardwareTextureID)

        executor.runAll()
        manager.updateTextures()
        executor.runAll()
        manager.updateTextures()
        assertTrue(large.isLoadedToHardware)
        assertTrue(reloaded.hardwareTextureID != oldName)
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun reloadLoaded_nonPreparableReloadsWhileLoadsWait() {
        val manager = manager(bytesInFlight = 1500)
        val bitmapAtlas = atlas(manager, FakeBitmapSource())
        loadAll(manager, bitmapAtlas)
        val oldName = bitmapAtlas.hardwareTextureID

        manager.reloadLoadedTextures()
        List(2) { atlas(manager, FakePixelSource(seed = it)) }.forEach { manager.loadTexture(it) }
        manager.updateTextures()

        assertTrue(bitmapAtlas.hardwareTextureID != oldName)
    }

    @Test
    fun reloadPreparation_failureFallsBackToSync() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        loadAll(manager, texture)
        val oldName = texture.hardwareTextureID

        manager.reloadLoadedTextures()
        manager.updateTextures()
        source.failures = 1
        executor.runAll()
        manager.updateTextures()

        assertTrue(texture.hardwareTextureID != oldName)
        assertFalse(texture.isUpdateOnHardwareNeeded)
        assertEquals("decoded again on the synchronous path", 3, source.pixelLoads)
        assertEquals(2, pixelUploads())
        assertArrayEquals(source.bytes, gl.texSubImage2DBytes[1])
        assertEquals(0, manager.preparationsInFlight)
        assertFalse(manager.hasPendingTextureReloads())
    }

    @Test
    fun reloadLoaded_secondRequestAfterReadyDropsPixels() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        loadAll(manager, texture)
        val oldName = texture.hardwareTextureID

        manager.reloadLoadedTextures()
        manager.updateTextures()
        executor.runAll()
        assertEquals("ready, waiting for upload", 1, manager.preparationsInFlight)

        manager.reloadLoadedTextures()
        assertEquals("the ready pixels are dropped and freed", 0, manager.preparationsInFlight)
        assertEquals(oldName, texture.hardwareTextureID)

        manager.updateTextures()
        assertEquals("prepared anew", 1, executor.tasks.size)
        assertEquals(oldName, texture.hardwareTextureID)
        executor.runAll()
        manager.updateTextures()

        assertTrue(texture.hardwareTextureID != oldName)
        assertEquals(3, source.pixelLoads)
        assertEquals("uploaded once", 2, pixelUploads())
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun unloadRescuedByLoad_keepsTheRequestedReload() {
        val manager = manager()
        val source = FakePixelSource()
        val texture = atlas(manager, source)
        loadAll(manager, texture)
        val oldName = texture.hardwareTextureID

        manager.reloadLoadedTextures()
        manager.updateTextures()
        manager.unloadTexture(texture)
        manager.loadTexture(texture)
        assertTrue(manager.hasPendingTextureReloads())
        manager.updateTextures()

        assertEquals("prepared anew, the old texture stays meanwhile", oldName, texture.hardwareTextureID)
        executor.runAll()
        manager.updateTextures()
        assertTrue(texture.hardwareTextureID != oldName)
        assertEquals("the cancelled preparation never ran", 2, source.pixelLoads)
        assertEquals(0, manager.preparationsInFlight)
    }

    @Test
    fun updateTextures_reportsFramesWithUploads() {
        val manager = manager()
        val texture = atlas(manager, FakePixelSource())
        manager.loadTexture(texture)
        assertFalse("only submitted", manager.updateTextures())
        executor.runAll()
        assertTrue("uploaded prepared pixels", manager.updateTextures())
        assertFalse(manager.updateTextures())
    }
}
