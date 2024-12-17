package com.reco1l.osu

import android.content.Intent
import android.content.Intent.*
import android.util.Log
import androidx.core.content.FileProvider
import com.osudroid.resources.R
import com.edlplan.ui.fragment.MarkdownFragment
import com.reco1l.framework.net.FileRequest
import com.reco1l.framework.net.IDownloaderObserver
import com.reco1l.framework.net.JsonObjectRequest
import com.reco1l.osu.ui.MessageDialog
import com.reco1l.osu.ui.ProgressDialog
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.BuildConfig.APPLICATION_ID
import java.io.File


object UpdateManager: IDownloaderObserver
{

    private val updatesDirectory = File(Config.getCachePath(), "updates").apply(File::mkdirs)


    private var downloadURL: String? = null
    
    private var newVersionCode: Long = GlobalManager.getInstance().mainActivity.versionCode

    private var progressDialog: ProgressDialog? = null


    @JvmStatic
    fun onActivityStart(activity: MainActivity) = mainThread {

        // Finding if there's a "pending changelog". This means the game was previously updated, we're
        // showing the changelog after update with a prompt asking user to show.

        val latestUpdate = Config.getLong("latestVersionCode", activity.versionCode)
        val pendingChangelog = Config.getString("pendingChangelog", null)

        if (!pendingChangelog.isNullOrEmpty()) {

            if (latestUpdate > activity.versionCode) {

                MessageDialog()
                    .setTitle(activity.getString(R.string.update_info_updated))
                    .setMessage("Game was updated to a newer version. Do you want to see the changelog?")
                    .addButton("Yes") {
                        MarkdownFragment()
                            .setTitle(R.string.changelog_title)
                            .setMarkdown(pendingChangelog)
                            .show()

                        it.dismiss()
                    }
                    .addButton("No") { it.dismiss() }
                    .setOnDismiss { Config.setString("pendingChangelog", null) }
                    .show()
            }
        }

        checkNewUpdates(true)
    }

    /**
     * Check for new game updates.
     * 
     * @param silently If `true` no prompt will be shown unless there's new updates.
     */
    @JvmStatic
    fun checkNewUpdates(silently: Boolean) {

        val mainActivity = GlobalManager.getInstance().mainActivity

        if (!silently) {
            ToastLogger.showText(R.string.update_info_checking, false)
        }

        async {

            // Cleaning update directory first, checking if there's a newer package downloaded already.
            updatesDirectory.listFiles()?.also { list ->

                var newestVersionDownloaded = mainActivity.versionCode

                list.forEach {

                    val version = it.nameWithoutExtension.toLongOrNull() ?: return@forEach
                    if (version == mainActivity.versionCode) {
                        it.delete()
                    }

                    if (version > newestVersionDownloaded) {
                        newestVersionDownloaded = version
                    }
                }

                if (newestVersionDownloaded > mainActivity.versionCode) {
                    newVersionCode = newestVersionDownloaded
                    onFoundNewUpdate(silently)
                    return@async
                }
            }
                
            try {
                
                // Avoid new request if it was already done.
                if (downloadURL != null) {
                    onFoundNewUpdate(silently)
                    return@async
                }

                JsonObjectRequest(/*updateEndpoint*/ "http://localhost:80/update.php").use { request ->

                    request.buildRequest { header("User-Agent", "Chrome/Android") }

                    val response = request.execute().json
                    val changelog = response.getString("changelog")

                    downloadURL = response.getString("link")
                    newVersionCode = response.getLong("version_code")

                    // Previous implementation has this check, server returning an older version shouldn't happen.
                    if (newVersionCode <= mainActivity.versionCode) {
                        ToastLogger.showText(R.string.update_info_latest, false)
                        return@async
                    }

                    Config.setString("pendingChangelog", changelog)
                    onFoundNewUpdate(silently)
                }

            } catch (e: Exception) {
                Log.e("UpdateManager", "Failed to check for updates.", e)

                if (!silently) {
                    ToastLogger.showText(R.string.update_info_check_failed, false)
                }
            }
        }
    }


    private fun onFoundNewUpdate(silently: Boolean) = mainThread {

        val activity = GlobalManager.getInstance().mainActivity

        if (newVersionCode <= activity.versionCode) {
            if (!silently) {
                ToastLogger.showText(R.string.update_info_latest, false)
            }
            return@mainThread
        }

        Config.setLong("latestVersionCode", newVersionCode)

        MessageDialog()
            .setTitle("New update available!")
            .setMessage(activity.getString(R.string.update_dialog_message))
            .addButton(activity.getString(R.string.update_dialog_button_update)) {

                val file = File(updatesDirectory, "$newVersionCode.apk")

                // Files is already downloaded, navigating to installation.
                if (file.exists()) {
                    installAPK(file)
                    return@addButton
                }

                file.createNewFile()
                downloadAPK(file)
            }
            .addButton("Update later") { it.dismiss() }
            .show()
    }


    private fun installAPK(file: File) {

        val uri = FileProvider.getUriForFile(GlobalManager.getInstance().mainActivity, "$APPLICATION_ID.fileProvider", file)

        val intent = Intent(ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION)

        GlobalManager.getInstance().mainActivity.startActivity(intent)
    }
    
    private fun downloadAPK(file: File) {

        // Empty string: At this point download URL shouldn't be null but if it is the case (which is weird) we set an
        // empty string so the downloader invokes onDownloadFail() and a prompt is shown to user rather than nothing.
        val url = downloadURL ?: ""

        async {
            val downloader = FileRequest(file, url)
            downloader.observer = this@UpdateManager
            downloader.execute()

            mainThread {
                progressDialog = ProgressDialog().apply {
                    setIndeterminate(true)
                    setTitle("Downloading update")
                    setMessage(StringTable.format(R.string.update_info_downloading, 0))
                    show()
                }
            }
        }
    }


    override fun onDownloadUpdate(downloader: FileRequest) = mainThread {
        progressDialog?.progress = downloader.progress.toInt()
        progressDialog?.setMessage(StringTable.format(R.string.update_info_downloading, downloader.progress.toInt()))
    }

    override fun onDownloadEnd(downloader: FileRequest) {
        progressDialog?.dismiss()
        installAPK(downloader.file)
    }

    override fun onDownloadFail(downloader: FileRequest, exception: Exception) {
        Log.e("UpdateManager", "Failed to download update.", exception)
        ToastLogger.showText(R.string.update_info_download_failed, false)
        downloader.file.delete()
    }

    override fun onDownloadCancel(downloader: FileRequest) {
        ToastLogger.showText(R.string.update_info_download_canceled, false)
        downloader.file.delete()
    }
}