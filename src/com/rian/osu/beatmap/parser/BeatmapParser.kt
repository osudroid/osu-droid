package com.rian.osu.beatmap.parser

import android.util.Log
import com.osudroid.resources.R.*
import com.reco1l.toolkt.kotlin.runSafe
import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.BeatmapProcessor
import com.rian.osu.beatmap.constants.BeatmapSection
import com.rian.osu.beatmap.parser.sections.*
import okio.BufferedSource
import okio.buffer
import okio.source
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A parser for parsing `.osu` files.
 */
class BeatmapParser : Closeable {
    /**
     * The `.osu` file.
     */
    private val file: File

    /**
     * The `BufferedSource` responsible for reading the beatmap file's contents.
     */
    private var source: BufferedSource? = null

    /**
     * The format version of the beatmap.
     */
    private var beatmapFormatVersion = 14

    /**
     * The [CoroutineScope] to use for coroutines.
     */
    private val scope: CoroutineScope?

    /**
     * @param file The `.osu` file.
     * @param scope The [CoroutineScope] to use for coroutines.
     */
    @JvmOverloads
    constructor(file: File, scope: CoroutineScope? = null) {
        this.file = file
        this.scope = scope
    }

    /**
     * @param path The path to the `.osu` file.
     * @param scope The [CoroutineScope] to use for coroutines.
     */
    @JvmOverloads
    constructor(path: String, scope: CoroutineScope? = null) : this(File(path), scope)

    /**
     * Attempts to open the beatmap file.
     *
     * @return Whether the beatmap file was successfully opened.
     */
    fun openFile(): Boolean {
        try {
            scope?.ensureActive()

            source = file.source().buffer()
        } catch (e: IOException) {
            Log.e("BeatmapParser.openFile", e.message!!)
            source = null
            return false
        }

        try {
            scope?.ensureActive()

            val head = source!!.readUtf8Line() ?: return false

            scope?.ensureActive()

            val pattern = Pattern.compile("osu file format v(\\d+)")
            val matcher = pattern.matcher(head)

            if (!matcher.find()) {
                return false
            }

            val formatPos = head.indexOf("file format v")

            beatmapFormatVersion = head.substring(formatPos + 13).toIntOrNull() ?: beatmapFormatVersion
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }

            Log.e("BeatmapParser.openFile", e.message!!)
        }

        return true
    }

    /**
     * Parses the `.osu` file.
     *
     * @param withHitObjects Whether to parse hit objects. Setting this to `false` will improve parsing time significantly.
     * @param mode The [GameMode] to parse for. Defaults to [GameMode.Standard].
     * @return A [Beatmap] containing relevant information of the beatmap file,
     * `null` if the beatmap file cannot be opened or a line could not be parsed.
     */
    @JvmOverloads
    fun parse(withHitObjects: Boolean, mode: GameMode = GameMode.Standard): Beatmap? {
        scope?.ensureActive()

        if (source == null && !openFile()) {
            ToastLogger.showText(
                StringTable.format(string.beatmap_parser_cannot_open_file, file.nameWithoutExtension),
                true
            )

            return null
        }

        scope?.ensureActive()

        var currentLine: String?
        var currentSection: BeatmapSection? = null
        val beatmap = Beatmap(mode).also {
            it.md5 = FileUtils.getMD5Checksum(file)
            it.filePath = file.path
            it.formatVersion = beatmapFormatVersion
        }

        try {
            while (source!!.readUtf8Line().also { currentLine = it } != null) {
                scope?.ensureActive()

                // Check if beatmap is not an osu!standard beatmap
                if (beatmap.general.mode != 0) {
                    // Silently ignore (do not log anything to the user)
                    return null
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
                            if (withHitObjects) {
                                BeatmapHitObjectsParser.parse(beatmap, line, scope)
                            }

                        else -> continue
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }

                    Log.e("BeatmapParser.parse", "Unable to parse line", e)
                }
            }
        } catch (e: IOException) {
            Log.e("BeatmapParser.parse", e.message!!)
            return null
        }

        return beatmap.apply {
            hitObjects.objects.forEach {
                scope?.ensureActive()

                it.applyDefaults(controlPoints, difficulty, mode, scope)
                it.applySamples(controlPoints, scope)
            }

            BeatmapProcessor(this, scope).also {
                it.preProcess()
                it.postProcess()
            }
        }
    }

    override fun close() {
        runSafe { source?.close() }

        source = null
    }
}
