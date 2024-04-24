package com.reco1l.legacy.ui.beatmapdownloader

import android.os.Environment.DIRECTORY_DOWNLOADS
import android.view.View
import com.reco1l.framework.extensions.decodeUtf8
import com.reco1l.framework.extensions.forFilesystem
import com.reco1l.framework.net.Downloader
import com.reco1l.framework.net.IDownloaderObserver
import com.reco1l.framework.net.SizeMeasure
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.ui.DownloadFragment
import com.reco1l.legacy.ui.multiplayer.RoomScene
import net.lingala.zip4j.ZipFile
import org.apache.commons.io.FilenameUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.IOException

object BeatmapDownloader : IDownloaderObserver {


    private lateinit var fragment: DownloadFragment

    private lateinit var currentFilename: String


    private val context = GlobalManager.getInstance().mainActivity

    // TODO: We should adapt the system to allow multiple downloads at a time.
    private var isDownloading = false


    fun download(url: String, suggestedFilename: String) {
        if (isDownloading) {
            return
        }
        isDownloading = true

        val name = suggestedFilename.decodeUtf8()
        val filename = name.forFilesystem()

        if (!filename.endsWith(".osz")) {
            ToastLogger.showText("Failed to start download. Invalid file extension", true)
            return
        }

        currentFilename = FilenameUtils.removeExtension(filename)

        val directory = context.getExternalFilesDir(DIRECTORY_DOWNLOADS)
        val file = directory?.resolve("$filename.osz")!!

        val downloader = Downloader(file, url)

        fragment = DownloadFragment()
        fragment.setDownloader(downloader) {

            fragment.text.visibility = View.VISIBLE
            fragment.text.text = context.getString(R.string.beatmap_downloader_connecting)

            fragment.button.visibility = View.VISIBLE
            fragment.button.text = context.getString(R.string.beatmap_downloader_cancel)

            downloader.observer = this@BeatmapDownloader
            downloader.download()

            fragment.button.setOnClickListener {
                downloader.cancel()
            }
        }
        fragment.show()
    }


    override fun onDownloadStart(downloader: Downloader) {
        context.runOnUiThread {
            fragment.text.text = StringTable.format(R.string.beatmap_downloader_downloading, currentFilename)
        }
    }

    override fun onDownloadEnd(downloader: Downloader) {
        context.runOnUiThread {
            fragment.progressBar.visibility = View.GONE
            fragment.progressBar.isIndeterminate = true
            fragment.progressBar.visibility = View.VISIBLE

            fragment.text.text = StringTable.format(R.string.beatmap_downloader_importing, currentFilename)
            fragment.button.visibility = View.GONE
        }

        val file = downloader.file

        try {
            ZipFile(file).use {
                if (!it.isValidZipFile) {
                    context.runOnUiThread(fragment::dismiss)
                    ToastLogger.showText("Import failed, invalid ZIP file.", true)
                    return
                }

                if (!FileUtils.extractZip(file.path, Config.getBeatmapPath())) {
                    context.runOnUiThread(fragment::dismiss)
                    ToastLogger.showText("Import failed, failed to extract ZIP file.", true)
                    return
                }

                LibraryManager.INSTANCE.updateLibrary(true)
            }
        } catch (e: IOException) {
            ToastLogger.showText("Import failed:" + e.message, true)
        }

        context.runOnUiThread(fragment::dismiss)

        if (Multiplayer.isConnected)
            RoomScene.onRoomBeatmapChange(Multiplayer.room!!.beatmap)

        isDownloading = false
    }

    override fun onDownloadCancel(downloader: Downloader) {
        ToastLogger.showText("Download canceled.", true)

        context.runOnUiThread(fragment::dismiss)
        isDownloading = false
    }

    override fun onDownloadUpdate(downloader: Downloader) {

        val info = "\n%.3f kb/s (%d%%)".format(downloader.getSpeed(SizeMeasure.MBPS), downloader.progress.toInt())

        context.runOnUiThread {
            fragment.text.text = context.getString(R.string.beatmap_downloader_downloading).format(
                currentFilename
            ) + info
            fragment.progressBar.isIndeterminate = false
            fragment.progressBar.progress = downloader.progress.toInt()
        }
    }

    override fun onDownloadFail(downloader: Downloader) {
        ToastLogger.showText("Download failed. " + downloader.exception?.message, true)

        context.runOnUiThread(fragment::dismiss)
        isDownloading = false
    }
}