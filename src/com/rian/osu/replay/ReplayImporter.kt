package com.rian.osu.replay

import com.edlplan.replay.OsuDroidReplayPack
import com.reco1l.osu.data.DatabaseManager.scoreInfoTable
import com.reco1l.toolkt.data.extensionLowercase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.scoring.Replay

/**
 * Utilities for performing replay imports.
 */
object ReplayImporter {
    /**
     * Imports a replay from the specified path.
     *
     * @param path The path to the replay file. Must be of `.odr` or `.edr` extension.
     * @return `true` if the replay was imported successfully, `false` otherwise.
     */
    @JvmStatic
    fun import(path: String): Boolean = import(File(path))

    /**
     * Imports a replay from the specified file.
     *
     * @param file The replay file. Must be of `.odr` or `.edr` extension.
     * @return `true` if the replay was imported successfully, `false` otherwise.
     */
    @JvmStatic
    fun import(file: File): Boolean {
        if (!file.exists()) {
            throw NoSuchFileException(file, reason = "File does not exist")
        }

        if (!file.isFile) {
            throw FileSystemException(file, reason = "Path is not a file")
        }

        return when (file.extensionLowercase) {
            "odr" -> importOdr(file)
            "edr" -> importEdr(file)

            else -> throw UnsupportedOperationException("Unsupported file type: ${file.extensionLowercase}")
        }
    }

    private fun importOdr(file: File): Boolean {
        val replay = Replay()

        if (!replay.loadInfo(file.path)) {
            throw Exception("Failed to load replay info")
        }

        if (replay.replayVersion < 3) {
            throw Exception("Replay is too old. Must be a replay from version 1.6.7 or later")
        }

        if (scoreInfoTable.insertScore(replay.stat.toScoreInfo()) < 0) {
            throw Exception("Failed to insert score to score database")
        }

        val replayFile = File(Config.getScorePath() + file.name)

        if (file.canonicalFile != replayFile.canonicalFile) {
            file.copyTo(replayFile, true)
        }

        return true
    }

    private fun importEdr(file: File): Boolean {
        val entry = OsuDroidReplayPack.unpack(FileInputStream(file))
        val replayFile = File(entry.scoreInfo.replayPath)

        if (!replayFile.exists() && !replayFile.createNewFile()) {
            throw FileSystemException(replayFile, reason = "Failed to create move replay file")
        }

        FileOutputStream(replayFile).use {
            it.write(entry.replayFile)
        }

        if (scoreInfoTable.insertScore(entry.scoreInfo) < 0) {
            throw Exception("Failed to insert score to score database")
        }

        return true
    }
}