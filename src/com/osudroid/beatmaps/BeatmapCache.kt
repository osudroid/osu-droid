package com.osudroid.beatmaps

import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapSetInfo
import com.reco1l.toolkt.kotlin.fastForEach
import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.GameMode
import com.osudroid.utils.LRUCache
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CoroutineScope
import ru.nsu.ccfit.zuev.osu.helper.FileUtils

/**
 * A cache for caching [Beatmap]s that have been parsed. Supports [Beatmap]s that have been parsed with or without hit
 * objects, and will automatically reparse a [Beatmap] if it is requested with hit objects but is only cached without them.
 *
 * This cache is thread-safe.
 */
object BeatmapCache {
    private const val MAX_SIZE = 20

    private val lock = ReentrantLock()
    private val droidCache = LRUCache<String, CachedBeatmap>(MAX_SIZE)
    private val standardCache = LRUCache<String, CachedBeatmap>(MAX_SIZE)

    /**
     * Obtains a [Beatmap] from the cache, or parses it if it is not present.
     *
     * @param file The [File] of the beatmap to obtain.
     * @param withHitObjects Whether to include hit objects in the returned [Beatmap].
     * @param mode The [GameMode] of the beatmap to obtain. Defaults to [GameMode.Standard].
     * @param scope The [CoroutineScope] to use for parsing the beatmap if it is not present in the cache.
     * @return The [Beatmap] corresponding to the given [File].
     * @throws IOException If an I/O error occurs while parsing the beatmap.
     * @throws IllegalArgumentException If the beatmap is not an osu!standard beatmap.
     * @throws NumberFormatException If the beatmap's file version cannot be determined.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class, IllegalArgumentException::class, NumberFormatException::class)
    fun getBeatmap(
        file: File,
        withHitObjects: Boolean,
        mode: GameMode = GameMode.Standard,
        scope: CoroutineScope? = null
    ): Beatmap {
        // MD5 must be computed eagerly as it serves as the cache key.
        val md5 = FileUtils.getMD5Checksum(file)
        val cache = getBeatmap(md5, withHitObjects, mode, scope)

        if (cache != null) {
            return cache
        }

        return cacheBeatmap(file, md5, mode, withHitObjects, scope).beatmap
    }

    /**
     * Obtains a [Beatmap] from the cache, or parses it if it is not present.
     *
     * @param beatmapInfo The [BeatmapInfo] of the beatmap to obtain.
     * @param withHitObjects Whether to include hit objects in the returned [Beatmap].
     * @param mode The [GameMode] of the beatmap to obtain. Defaults to [GameMode.Standard].
     * @param scope The [CoroutineScope] to use for parsing the beatmap if it is not present in the cache.
     * @return The [Beatmap] corresponding to the given [BeatmapInfo].
     * @throws IOException If an I/O error occurs while parsing the beatmap.
     * @throws IllegalArgumentException If the beatmap is not an osu!standard beatmap.
     * @throws NumberFormatException If the beatmap's file version cannot be determined.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class, IllegalArgumentException::class, NumberFormatException::class)
    fun getBeatmap(
        beatmapInfo: BeatmapInfo,
        withHitObjects: Boolean,
        mode: GameMode = GameMode.Standard,
        scope: CoroutineScope? = null
    ): Beatmap {
        val cache = getBeatmap(beatmapInfo.md5, withHitObjects, mode, scope)

        if (cache != null) {
            return cache
        }

        val file = File(beatmapInfo.path)

        if (!file.exists()) {
            throw IOException("Beatmap file does not exist: ${beatmapInfo.path}")
        }

        return cacheBeatmap(file, beatmapInfo.md5, mode, withHitObjects, scope).beatmap
    }

    /**
     * Invalidates the cache entry of a [Beatmap].
     *
     * @param md5 The MD5 hash of the beatmap to invalidate.
     */
    @JvmStatic
    fun invalidate(md5: String): Unit = lock.withLock {
        droidCache.remove(md5)
        standardCache.remove(md5)
    }

    /**
     * Invalidates the cache entry of a [Beatmap].
     *
     * @param beatmapInfo The [BeatmapInfo] of the beatmap to invalidate.
     */
    @JvmStatic
    fun invalidate(beatmapInfo: BeatmapInfo) = invalidate(beatmapInfo.md5)

    /**
     * Invalidates the cache entries of all [Beatmap]s in a [BeatmapSetInfo].
     *
     * @param beatmapSetInfo The [BeatmapSetInfo] of the beatmaps to invalidate.
     */
    @JvmStatic
    fun invalidate(beatmapSetInfo: BeatmapSetInfo) = lock.withLock {
        beatmapSetInfo.beatmaps.fastForEach {
            droidCache.remove(it.md5)
            standardCache.remove(it.md5)
        }
    }

    /**
     * Clears all entries from the cache.
     */
    @JvmStatic
    fun clear() = lock.withLock {
        droidCache.clear()
        standardCache.clear()
    }

    private fun getBeatmap(md5: String, withHitObjects: Boolean, mode: GameMode, scope: CoroutineScope?): Beatmap? {
        val result = lock.withLock {
            var cache = getCacheFor(mode)[md5]

            if (cache == null) {
                val otherModeCache = when (mode) {
                    GameMode.Droid -> standardCache
                    GameMode.Standard -> droidCache
                }

                cache = otherModeCache[md5]

                if (cache != null) {
                    if (withHitObjects && !cache.withHitObjects) {
                        // A beatmap without hit objects cannot be converted to one with hit objects.
                        return@withLock null
                    }

                    // Found in the other mode's cache — needs conversion.
                    return@withLock NeedsConversion(cache.beatmap)
                }
            }

            if (cache == null) {
                return@withLock null
            }

            if (withHitObjects && !cache.withHitObjects) {
                return@withLock null
            }

            // Cache hit for the requested mode — no conversion needed.
            CacheHit(cache.beatmap)
        }

        return when (result) {
            is CacheHit -> result.beatmap
            is NeedsConversion -> cacheBeatmap(result.beatmap, mode, withHitObjects, scope).beatmap
            else -> null
        }
    }

    @Throws(IOException::class, IllegalArgumentException::class, NumberFormatException::class)
    private fun cacheBeatmap(file: File, md5: String, mode: GameMode, withHitObjects: Boolean, scope: CoroutineScope?): CachedBeatmap {
        val beatmap = BeatmapParser(file, scope, md5).parse(withHitObjects, mode)

        return cacheBeatmap(beatmap, mode, withHitObjects, scope)
    }

    private fun cacheBeatmap(beatmap: Beatmap, mode: GameMode, withHitObjects: Boolean, scope: CoroutineScope?): CachedBeatmap {
        val converted = beatmap.convert(mode, scope = scope)
        val cachedBeatmap = CachedBeatmap(converted, withHitObjects)

        return lock.withLock {
            val cache = getCacheFor(mode)
            val existing = cache[beatmap.md5]

            if (existing != null && existing.withHitObjects && !withHitObjects) {
                // A more complete beatmap was written by another thread while we were parsing; discard ours.
                existing
            } else {
                cache[beatmap.md5] = cachedBeatmap
                cachedBeatmap
            }
        }
    }

    /**
     * Returns the cache for the given [GameMode].
     *
     * **Important:** Must only be called while holding [lock].
     */
    private fun getCacheFor(mode: GameMode) = when (mode) {
        GameMode.Droid -> droidCache
        GameMode.Standard -> standardCache
    }

    private data class CachedBeatmap(val beatmap: Beatmap, val withHitObjects: Boolean)

    /**
     * Sealed interface representing the result of a cache lookup in [getBeatmap].
     */
    private sealed interface CacheLookupResult
    private data class CacheHit(val beatmap: Beatmap) : CacheLookupResult
    private data class NeedsConversion(val beatmap: Beatmap) : CacheLookupResult
}
