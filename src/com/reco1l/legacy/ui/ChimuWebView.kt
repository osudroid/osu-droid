package com.reco1l.legacy.ui

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.view.View
import com.edlplan.ui.fragment.LoadingFragment
import com.edlplan.ui.fragment.WebViewFragment
import com.reco1l.framework.net.IDownloaderObserver
import im.delight.android.webview.AdvancedWebView
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osuplus.R

import com.reco1l.framework.extensions.decodeUtf8
import com.reco1l.framework.extensions.forFilesystem
import com.reco1l.framework.net.Downloader
import com.reco1l.framework.net.SizeMeasure
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.ui.multiplayer.RoomScene
import net.lingala.zip4j.ZipFile
import org.apache.commons.io.FilenameUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import java.io.IOException

object ChimuWebView : WebViewFragment(), IDownloaderObserver {
    private val MIRROR: Uri = Uri.parse("https://chimu.moe/en/beatmaps?mode=0")
    const val FILE_EXTENSION = ".osz"

    private lateinit var mFragment: DownloadingFragment
    private lateinit var mWebView: AdvancedWebView
    private lateinit var mCurrentFilename: String

    private val mActivity = GlobalManager.getInstance().mainActivity

    init {
        super.setURL(MIRROR.toString())
    }

    override val layoutID: Int
        get() = R.layout.fragment_chimu

    override fun onLoadView() {
        super.onLoadView()
        mWebView = findViewById(R.id.web)!!
        mWebView.addPermittedHostname(MIRROR.host)
        mWebView.setListener(mActivity,
            object : AdvancedWebView.Listener {
                val fragment = LoadingFragment()

                override fun onPageStarted(url: String?, favicon: Bitmap?) = fragment.show()

                override fun onPageFinished(url: String?) = fragment.dismiss()

                override fun onPageError(
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                }

                override fun onDownloadRequested(
                    url: String?,
                    suggestedFilename: String?,
                    mimeType: String?,
                    contentLength: Long,
                    contentDisposition: String?,
                    userAgent: String?
                ) {
                    startDownload(url!!, suggestedFilename!!)
                }

                override fun onExternalPageRequest(url: String?) =
                    startActivity(Intent(ACTION_VIEW, Uri.parse(url)))
            }
        )
    }

    fun startDownload(url: String, suggestedFilename: String) {
        val name = suggestedFilename.decodeUtf8()
        val filename = name.forFilesystem()

        if (!filename.endsWith(FILE_EXTENSION)) {
            ToastLogger.showText("Failed to start download. Invalid file extension", true)
            return
        }

        mCurrentFilename = FilenameUtils.removeExtension(filename)

        val directory = mActivity.getExternalFilesDir(DIRECTORY_DOWNLOADS)
        val file = directory?.resolve(filename + FILE_EXTENSION)!!

        val downloader = Downloader(file, url)

        mFragment = DownloadingFragment()
        mFragment.setDownloader(downloader) {
            mFragment.text.visibility = View.VISIBLE
            mFragment.text.text = R.string.chimu_connecting.toString()
            mFragment.button.visibility = View.VISIBLE
            mFragment.button.text = R.string.chimu_cancel.toString()

            downloader.observer = this@ChimuWebView
            downloader.download()

            mFragment.button.setOnClickListener {
                downloader.cancel()
            }
        }
        mFragment.show()
    }

    override fun dismiss() {
        mWebView.destroy()
        super.dismiss()
    }

    override fun onDownloadStart(downloader: Downloader) {
        mActivity.runOnUiThread {
            mFragment.text.text = StringTable.format(R.string.chimu_downloading, mCurrentFilename)
        }
    }

    override fun onDownloadEnd(downloader: Downloader) {
        mActivity.runOnUiThread {
            mFragment.progressBar.visibility = View.GONE
            mFragment.progressBar.isIndeterminate = true
            mFragment.progressBar.visibility = View.VISIBLE

            mFragment.text.text = StringTable.format(R.string.chimu_importing, mCurrentFilename)
            mFragment.button.visibility = View.GONE
        }

        val file = downloader.file

        try {
            ZipFile(file).use {
                if (!it.isValidZipFile) {
                    mActivity.runOnUiThread(mFragment::dismiss)
                    ToastLogger.showText("Import failed, invalid ZIP file.", true)
                    return
                }

                if (!FileUtils.extractZip(file.path, Config.getBeatmapPath())) {
                    mActivity.runOnUiThread(mFragment::dismiss)
                    ToastLogger.showText("Import failed, failed to extract ZIP file.", true)
                    return
                }

                LibraryManager.INSTANCE.updateLibrary(true)
            }
        } catch (e: IOException) {
            ToastLogger.showText("Import failed:" + e.message, true)
        }

        mActivity.runOnUiThread(mFragment::dismiss)

        if (Multiplayer.isConnected)
            RoomScene.onRoomBeatmapChange(Multiplayer.room!!.beatmap)
    }

    override fun onDownloadCancel(downloader: Downloader) {
        ToastLogger.showText("Download canceled.", true)
        mActivity.runOnUiThread(mFragment::dismiss)
    }

    override fun onDownloadUpdate(downloader: Downloader) {
        val info = String.format("\n%.3f kb/s (%d%%)", downloader.getSpeed(SizeMeasure.MBPS), downloader.progress.toInt())

        mActivity.runOnUiThread {
            mFragment.text.text = StringTable.format(R.string.chimu_downloading, mCurrentFilename) + info
            mFragment.progressBar.isIndeterminate = false
            mFragment.progressBar.progress = downloader.progress.toInt()
        }
    }

    override fun onDownloadFail(downloader: Downloader) {
        mActivity.runOnUiThread(mFragment::dismiss)
        ToastLogger.showText("Download failed. " + downloader.exception?.message, true)
    }
}