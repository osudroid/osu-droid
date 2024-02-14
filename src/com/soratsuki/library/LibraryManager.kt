package com.soratsuki.library

import com.reco1l.legacy.engine.VideoTexture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.osu.BeatmapInfo
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

object LibraryManager {
    private const val VERSION = "library4.2"
    private const val LIB_FILE = "library.$VERSION.dat"

    val library: MutableList<BeatmapInfo> = Collections.synchronizedList(mutableListOf())

    val sizeOfBeatmaps: Int
        get() = library.size

    private var fileCount = 0

    private var currentIndex = 0

    private val mutex = Mutex()

    private var fileCached = AtomicInteger(0)

    private inline fun <R> libraryRun(crossinline block: MutableList<BeatmapInfo>.() -> R): R {
        return runBlocking {
            mutex.withLock {
                library.block()
            }
        }
    }

    private inline fun <R> libraryLet(crossinline block: (MutableList<BeatmapInfo>) -> R): R {
        return runBlocking {
            mutex.withLock {
                block(library)
            }
        }
    }

    fun loadLibraryCache(forceUpdate: Boolean): Boolean = runBlocking {
        libraryRun { clear() }

        if (!FileUtils.canUseSD()) {
            return@runBlocking true
        }

        File(Config.getScorePath()).let {
            if (!it.exists()) {
                if (!it.mkdirs()) {
                    ToastLogger.showText(
                        StringTable.format(
                            R.string.message_error_createdir, it.path
                        ), true
                    )
                    return@runBlocking false
                }

                File(it.parentFile, ".nomedia").run {
                    if (createNewFile()) {
                        Debug.i("LibraryManager: Created .nomedia file")
                    } else {
                        Debug.e("LibraryManager: .nomedia file already exists")
                    }
                }
            }
        }

        File(Config.getBeatmapPath()).run {
            if (!exists()) return@runBlocking false
        }

        File(GlobalManager.getInstance().mainActivity.filesDir, LIB_FILE).let { lib ->
            try {
                if (lib.createNewFile()) {
                    Debug.i("LibraryManager: Library cache not found, creating new one")
                } else {
                    Debug.i("LibraryManager: Library cache found, loading")
                }
            } catch (e: Exception) {
                Debug.e("LibraryManager: Error creating library cache", e)
            }

            try {
                ObjectInputStream(FileInputStream(lib)).use { ois ->
                    var obj = ois.readObject()
                    if (obj is String) {
                        if (obj != VERSION) {
                            return@runBlocking false
                        }

                        obj = ois.readObject()
                        if (obj is Int) {
                            fileCount = obj

                            obj = ois.readObject()
                            if (obj is Collection<*>) {
                                mutex.withLock {
                                    obj.filterIsInstance(BeatmapInfo::class.java)
                                        .let { library.addAll(it) }
                                }

                                if (forceUpdate) {
                                    scanLibrary(true)
                                }

                                return@runBlocking true
                            }
                        }
                    }
                }
            } catch (_: IOException) {}

            return@runBlocking false
        }
    }

    @JvmOverloads
    fun scanLibrary(addUncachedBeatmaps: Boolean = false) {
        if (!addUncachedBeatmaps) libraryRun { clear() }

        val files = FileUtils.listFiles(File(Config.getBeatmapPath())).also { if (it.size == fileCount) return@scanLibrary }

        if (addUncachedBeatmaps) ToastLogger.showText(StringTable.get(R.string.message_lib_update), true)

        fileCount = files.size
        runBlocking {
            startCaching(files.toList(), addUncachedBeatmaps)
        }

        saveToCache()
        ToastLogger.showText(StringTable.format(R.string.message_lib_complete, fileCount), true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startCaching(files: List<File>, addUncachedBeatmaps: Boolean) {
        val coreCount = Runtime.getRuntime().availableProcessors()
        val chunks = files.chunked(files.size / coreCount)

        val scope = Dispatchers.IO.limitedParallelism(coreCount)

        runBlocking {
            chunks.map { chunk ->
                launch(scope) {
                    cacheBeatmap(chunk, addUncachedBeatmaps)
                }
            }.joinAll()
        }
    }

    fun saveToCache() {
        if (library.isEmpty()) return

        val lib = File(GlobalManager.getInstance().mainActivity.filesDir, LIB_FILE)

        ObjectOutputStream(FileOutputStream(lib)).use { oos ->
            lib.createNewFile()
            oos.writeObject(VERSION)
            oos.writeObject(fileCount)
            libraryLet { oos.writeObject(it) }
        }

        libraryRun { shuffle() }
        currentIndex = 0
    }

    private suspend fun cacheBeatmap(files: List<File>, addUncachedBeatmaps: Boolean) {
        for (file in files) {
            GlobalManager.getInstance().loadingProgress = 50 + 50 * fileCached.get() / fileCount
            ToastLogger.setPercentage(fileCached.get() * 100f / fileCount)

            fileCached.incrementAndGet()

            if (!file.isDirectory) continue

            if (addUncachedBeatmaps && libraryRun { any { it.path == file.path } }) continue

            GlobalManager.getInstance().info = "Loading ${file.name}..."
            val info = BeatmapInfo().apply {
                path = file.path
                scanFolder()
                fillEmptyFields()
            }

            if (info.count < 1) continue

            mutex.withLock { library.add(info) }
        }
    }

    fun deleteMap(info: BeatmapInfo) {
        val dir = File(info.path)
        deleteDir(dir)
        libraryRun { remove(info) }
    }

    fun clearCache() {
        val lib = File(GlobalManager.getInstance().mainActivity.filesDir, LIB_FILE)
        if (lib.delete()) {
            ToastLogger.showText(StringTable.get(R.string.message_lib_cleared), false)
        }
        currentIndex = 0
        fileCount = 0
        fileCached.set(0)
    }

    fun shuffleLibrary() = libraryRun { shuffle() }

    fun getBeatmap(): BeatmapInfo? = getBeatmapByIndex(currentIndex)
    fun getNextBeatmap(): BeatmapInfo? = getBeatmapByIndex(++currentIndex)
    fun getPrevBeatmap(): BeatmapInfo? = getBeatmapByIndex(--currentIndex)

    private fun getBeatmapByIndex(index: Int): BeatmapInfo? = libraryRun {
        Debug.i("Music Changing Info: Require index: $index/$fileCount")
        if (isEmpty()) return@libraryRun null
        currentIndex = if (index < 0 || index >= fileCount) {
            shuffle()
            0
        } else {
            index
        }
        get(currentIndex)
    }

    fun findBeatmap(info: BeatmapInfo): Int = libraryRun {
        forEach { if (it == info) return@libraryRun indexOf(it).also { idx -> currentIndex = idx } }
        currentIndex = 0
        0
    }

    fun findTrackByMD5(md5: String?): TrackInfo? = libraryRun {
        forEach {
            it.tracks.forEach { track ->
                if (track.mD5 == md5) return@libraryRun track
            }
        }
        null
    }

    fun findTrackByFileNameAndMD5(filename: String, md5: String): TrackInfo? = libraryRun {
        forEach {
            it.tracks.forEach { track ->
                if (track.filename == filename && track.mD5 == md5) return@libraryRun track
            }
        }
        null
    }

    fun updateLibrary(forceUpdate: Boolean) {
        if (!loadLibraryCache(forceUpdate)) {
            scanLibrary()
        }
        saveToCache()
    }

    private fun BeatmapInfo.scanFolder() {
        val dir = File(this.path)
        this.date = dir.lastModified()
        val fileList = FileUtils.listFiles(dir, ".osu") ?: return

        for (file in fileList) {
            val parser = BeatmapParser(file)
            if (!parser.openFile()) {
                if (Config.isDeleteUnimportedBeatmaps()) {
                    file.delete()
                }
                continue
            }

            val track = TrackInfo(this).apply {
                filename = file.path
                creator = "unknown"
            }

            val data = parser.parse(true) ?: continue
            if (!data.populateMetadata(this) || !data.populateMetadata(track)) {
                if (Config.isDeleteUnimportedBeatmaps()) {
                    file.delete()
                }
                continue
            }

            if (Config.isDeleteUnsupportedVideos()) {
                data.events.videoFilename?.let { video ->
                    File(this.path, video).let {
                        if (!VideoTexture.isSupportedVideo(it)) it.delete()
                    }
                }
            }
            this.addTrack(track)
        }

        if (Config.isDeleteUnimportedBeatmaps() && this.count == 0) {
            deleteDir(dir)
        }

        this.tracks.sortWith { a, b -> a.difficulty.compareTo(b.difficulty)}
    }

    private fun BeatmapInfo.fillEmptyFields() {
        this.creator = this.getTrack(0).creator
        if (this.title.isEmpty()) {
            this.title = "unknown"
        }

        if (this.artist.isEmpty()) {
            this.artist = "unknown"
        }

        if (this.creator.isEmpty()) {
            this.creator = "unknown"
        }
    }

    fun deleteDir(dir: File) {
        if (!dir.exists()) return
        if (dir.isDirectory) {
            for (file in dir.listFiles() ?: return) {
                if (file.isDirectory) {
                    deleteDir(file)
                } else {
                    file.delete()
                }
            }
            dir.delete()
        }
    }
}