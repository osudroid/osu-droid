package com.rian.osu.beatmap.parser

import android.util.Log
import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.BeatmapProcessor
import com.rian.osu.beatmap.constants.BeatmapSection
import com.rian.osu.beatmap.parser.sections.*
import okio.BufferedSource
import okio.buffer
import okio.source
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A parser for parsing `.osu` files.
 */
class BeatmapParser {
    /**
     * The `.osu` file.
     */
    private val file: File

    /**
     * The [CoroutineScope] to use for coroutines.
     */
    private val scope: CoroutineScope?

    /**
     * The precomputed MD5 hash of the beatmap file, if available.
     *
     * This is used to avoid unnecessary MD5 calculations when parsing.
     */
    private val precomputedMD5: String?

    /**
     * @param file The `.osu` file.
     * @param scope The [CoroutineScope] to use for coroutines.
     */
    @JvmOverloads
    constructor(file: File, scope: CoroutineScope? = null, precomputedMD5: String? = null) {
        this.file = file
        this.scope = scope
        this.precomputedMD5 = precomputedMD5
    }

    /**
     * @param path The path to the `.osu` file.
     * @param scope The [CoroutineScope] to use for coroutines.
     */
    @JvmOverloads
    constructor(path: String, scope: CoroutineScope? = null, precomputedMD5: String? = null) : this(File(path), scope, precomputedMD5)

    /**
     * Parses the `.osu` file.
     *
     * @param withHitObjects Whether to parse hit objects. Setting this to `false` will improve parsing time significantly.
     * @param mode The [GameMode] to parse for. Defaults to [GameMode.Standard].
     * @return A [Beatmap] containing relevant information of the beatmap file.
     * @throws IOException If an I/O error occurs while reading the file.
     * @throws NumberFormatException If the beatmap's file version cannot be determined.
     * @throws IllegalArgumentException If the beatmap is not an osu!standard beatmap.
     */
    @JvmOverloads
    @Throws(IOException::class, IllegalArgumentException::class, NumberFormatException::class)
    fun parse(withHitObjects: Boolean, mode: GameMode = GameMode.Standard) = openFile().use { source ->
        scope?.ensureActive()

        // Check for format version first to avoid unnecessary MD5 calculation.
        val formatVersion = getFormatVersion(source)

        var currentLine: String?
        var currentSection: BeatmapSection? = null

        val beatmap = Beatmap(mode).also {
            it.md5 = precomputedMD5 ?: FileUtils.getMD5Checksum(file)
            it.filePath = file.path
            it.formatVersion = formatVersion
        }

        while (source.readUtf8Line().also { currentLine = it } != null) {
            scope?.ensureActive()

            if (beatmap.general.mode != 0) {
                throw IllegalArgumentException("Not an osu!standard beatmap")
            }

            var line = currentLine ?: continue

            // Handle space comments
            if (line.startsWith(" ") || line.startsWith("_")) {
                continue
            }

            // Now that we've handled space comments, we can trim space
            line = line.trim { it <= ' ' }

            // Handle C++ style comments and empty lines
            if (line.startsWith("//") || line.isEmpty()) {
                continue
            }

            // [SectionName]
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = BeatmapSection.parse(line.substring(1, line.length - 1))

                // HitObjects are always in the last section (since it is dependent on other things such as
                // difficulty, control points, etc.)
                // We can stop here if we do not need to parse them.
                if (currentSection == BeatmapSection.HitObjects && !withHitObjects) {
                    break
                }

                continue
            }

            if (currentSection == null) {
                continue
            }

            scope?.ensureActive()

            try {
                when (currentSection) {
                    BeatmapSection.General ->
                        BeatmapGeneralParser.parse(beatmap, line, scope)

                    BeatmapSection.Metadata ->
                        BeatmapMetadataParser.parse(beatmap, line, scope)

                    BeatmapSection.Difficulty ->
                        BeatmapDifficultyParser.parse(beatmap, line, scope)

                    BeatmapSection.Events ->
                        BeatmapEventsParser.parse(beatmap, line, scope)

                    BeatmapSection.TimingPoints ->
                        BeatmapControlPointsParser.parse(beatmap, line, scope)

                    BeatmapSection.Colors ->
                        BeatmapColorParser.parse(beatmap, line, scope)

                    BeatmapSection.HitObjects ->
                        BeatmapHitObjectsParser.parse(beatmap, line, scope)

                    else -> continue
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }

                Log.e("BeatmapParser.parse", "Unable to parse line", e)
            }
        }

        val processor = BeatmapProcessor(beatmap, scope)

        processor.preProcess()

        beatmap.hitObjects.objects.forEach {
            scope?.ensureActive()

            it.applyDefaults(beatmap.controlPoints, beatmap.difficulty, mode, scope)
            it.applySamples(beatmap.controlPoints, scope)
        }

        processor.postProcess()

        beatmap
    }

    private fun openFile() = file.source().buffer()

    @Throws(IOException::class, NumberFormatException::class)
    private fun getFormatVersion(source: BufferedSource): Int {
        val head = source.readUtf8Line() ?: throw IOException("Empty file")
        val pattern = Pattern.compile("osu file format v(\\d+)")
        val matcher = pattern.matcher(head)

        if (!matcher.find()) {
            throw NumberFormatException("Invalid format version")
        }

        return head.substring(head.indexOf("file format v") + 13).toIntOrNull()
            ?: throw NumberFormatException("Invalid format version")
    }
}
