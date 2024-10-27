package com.reco1l.osu.beatmaplisting

import android.os.Environment.DIRECTORY_DOWNLOADS
import android.view.View
import com.edlplan.osudroidresource.R.*
import com.reco1l.framework.net.FileRequest
import com.reco1l.framework.net.IDownloaderObserver
import com.reco1l.osu.mainThread
import com.reco1l.osu.multiplayer.Multiplayer
import com.reco1l.osu.multiplayer.RoomScene
import com.reco1l.osu.ui.DownloadFragment
import com.reco1l.toolkt.kotlin.async
import net.lingala.zip4j.ZipFile
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.helper.StringTable
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

        currentFilename = suggestedFilename

        val file = context.getExternalFilesDir(DIRECTORY_DOWNLOADS)!!.resolve("$suggestedFilename.osz")

        val downloader = FileRequest(file, url)
        downloader.buildRequest { header("User-Agent", "Chrome/Android") }

        fragment = DownloadFragment()
        fragment.setDownloader(downloader) {

            fragment.text.visibility = View.VISIBLE
            fragment.text.text = context.getString(string.beatmap_downloader_connecting)

            fragment.button.visibility = View.VISIBLE
            fragment.button.text = context.getString(string.beatmap_downloader_cancel)

            downloader.observer = this@BeatmapDownloader

            async {
                downloader.execute()

                mainThread {
                    fragment.text.text = StringTable.format(string.beatmap_downloader_downloading, currentFilename)
                }
            }

            fragment.button.setOnClickListener {
                downloader.cancel()
            }
        }
        fragment.show()
    }


    override fun onDownloadEnd(downloader: FileRequest) {
        mainThread {
            fragment.progressBar.visibility = View.GONE
            fragment.progressBar.isIndeterminate = true
            fragment.progressBar.visibility = View.VISIBLE

            fragment.text.text = StringTable.format(string.beatmap_downloader_importing, currentFilename)
            fragment.button.visibility = View.GONE
        }

        val file = downloader.file

        try {
            ZipFile(file).use {
                if (!it.isValidZipFile) {
                    mainThread(fragment::dismiss)
                    ToastLogger.showText("Import failed, invalid ZIP file.", true)
                    return
                }

                if (!FileUtils.extractZip(file.path, Config.getBeatmapPath())) {
                    mainThread(fragment::dismiss)
                    ToastLogger.showText("Import failed, failed to extract ZIP file.", true)
                    return
                }

                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()
            }
        } catch (e: IOException) {
            ToastLogger.showText("Import failed:" + e.message, true)
        }

        mainThread(fragment::dismiss)

        if (Multiplayer.isConnected)
            RoomScene.onRoomBeatmapChange(Multiplayer.room!!.beatmap)

        isDownloading = false
    }

    override fun onDownloadCancel(downloader: FileRequest) {
        ToastLogger.showText("Download canceled.", true)

        mainThread(fragment::dismiss)
        isDownloading = false
    }

    override fun onDownloadUpdate(downloader: FileRequest) {

        val info = "\n%.3f kb/s (%d%%)".format(downloader.speedKbps / 1024, downloader.progress.toInt())

        mainThread {
            fragment.text.text = context.getString(string.beatmap_downloader_downloading).format(
                currentFilename
            ) + info
            fragment.progressBar.isIndeterminate = false
            fragment.progressBar.progress = downloader.progress.toInt()
        }
    }

    override fun onDownloadFail(downloader: FileRequest, exception: Exception) {
        ToastLogger.showText("Download failed. " + exception.message, true)

        mainThread(fragment::dismiss)
        isDownloading = false
    }
}