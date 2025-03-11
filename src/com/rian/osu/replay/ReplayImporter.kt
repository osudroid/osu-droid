package com.rian.osu.replay

import com.edlplan.replay.OsuDroidReplayPack
import com.reco1l.osu.data.DatabaseManager.scoreInfoTable
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import ru.nsu.ccfit.zuev.osu.scoring.Replay

/**
 * Utilities for performing replay imports.
 */
object ReplayImporter {
    /**
     * Imports a replay from the specified path.
     *
     * [Exception]s will be thrown when necessary.
     *
     * @param path The path to the replay file.
     */
    @JvmStatic
    fun import(path: String) = import(File(path))

    /**
     * Imports a replay from the specified file.
     *
     * [Exception]s will be thrown when necessary.
     *
     * @param file The replay file.
     */
    @JvmStatic
    fun import(file: File) {
        if (!file.exists()) {
            throw NoSuchFileException(file, reason = "File does not exist")
        }

        if (!file.isFile) {
            throw FileSystemException(file, reason = "Path is not a file")
        }

        // Both odr and edr files are application/octet-stream files. Differentiating them is quite tricky.
        // Here, we use the fact that odr files only contain 1 entry called "data" and edr files contain 2 entries
        // called "entry.json" and the replay file itself. This is not a foolproof way, but works for proper files.
        return ZipInputStream(file.inputStream()).use { zip ->
            val zipEntryNames = mutableListOf<String>()
            var zipEntry: ZipEntry?

            while (zip.nextEntry.also { zipEntry = it } != null) {
                zipEntryNames.add(zipEntry!!.name)
                zip.closeEntry()

                if (zipEntryNames.size > 1) {
                    break
                }
            }

            if (zipEntryNames.size == 1 && zipEntryNames[0] == "data") {
                return@use importOdr(file)
            }

            if (zipEntryNames.size == 2) {
                var entryJsonFound = false
                var replayFileFound = false

                for (entry in zipEntryNames) {
                    when {
                        entry == "entry.json" -> entryJsonFound = true
                        entry.endsWith(".odr") -> replayFileFound = true
                    }
                }

                if (entryJsonFound && replayFileFound) {
                    return@use importEdr(file)
                }
            }

            throw UnsupportedOperationException("Unsupported file. Please choose a valid replay file")
        }
    }

    private fun importOdr(file: File) {
        val replay = Replay()

        if (!replay.load(file.path, false)) {
            throw Exception("Failed to load replay info")
        }

        if (replay.replayVersion < 3) {
            throw Exception("Replay is too old. Must be a replay from osu!droid version 1.6.7 onwards")
        }

        val scoreInfo = replay.stat.toScoreInfo().apply {
            // For temporary replays, we need to change the extension of the replay to odr.
            // While we are at it, rename the replay file into something meaningful and not conflict other replays.
            if (replayFilename.startsWith("importedReplay") && replayFilename.endsWith(".tmp")) {
                replayFilename = playerName + "_" + replay.beatmapsetName + "_" + replay.beatmapName + "_" + System.currentTimeMillis() + ".odr"
            }
        }

        if (scoreInfoTable.insertScore(scoreInfo) < 0) {
            throw Exception("Failed to insert score to score database")
        }

        val replayFile = File(scoreInfo.replayPath)

        if (file.canonicalFile != replayFile.canonicalFile) {
            file.copyTo(replayFile, true)
        }
    }

    private fun importEdr(file: File) {
        val entry = OsuDroidReplayPack.unpack(file.inputStream()).apply {
            // Ensure the replay file does not conflict existing replays.
            scoreInfo.replayFilename = scoreInfo.replayFilename.substringBeforeLast('.') + System.currentTimeMillis() + ".odr"
        }

        val replayFile = File(entry.scoreInfo.replayPath)

        if (!replayFile.exists() && !replayFile.createNewFile()) {
            throw FileSystemException(replayFile, reason = "Failed to create move replay file")
        }

        replayFile.outputStream().use {
            it.write(entry.replayFile)
        }

        if (scoreInfoTable.insertScore(entry.scoreInfo) < 0) {
            throw Exception("Failed to insert score to score database")
        }
    }
}