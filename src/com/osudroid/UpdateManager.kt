package com.osudroid

import android.content.*
import android.util.*
import androidx.core.content.*
import com.osudroid.resources.R
import com.osudroid.utils.*
import com.reco1l.framework.net.*
import com.reco1l.osu.ui.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.*
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.*

object UpdateManager: IFileRequestObserver
{

    private val apksDirectory = File(Config.getCachePath(), "updates").apply(File::mkdirs)


    private var progressDialog: ProgressDialog? = null


    @JvmStatic
    fun onActivityStart() = mainThread {

        val activity = GlobalManager.getInstance().mainActivity
        val version = Config.getLong("version", 0)

        // Ignoring debug because otherwise every compiled build will show the dialog.
        if (!BuildConfig.DEBUG && version < activity.versionCode) {

            MessageDialog()
                .setTitle(StringTable.get(R.string.update_info_updated))
                .setMessage("Game was updated to a newer version.\nDo you want to see the changelog?")
                .addButton("Yes") {
                    it.dismiss()

                    val changelogFile = File(activity.cacheDir, "changelog.html")

                    // Copying the changelog file to the cache directory.
                    activity.assets.open("app/changelog.html").use { i ->
                        changelogFile.outputStream().use { o -> i.copyTo(o) }
                    }

                    val changelogUri = FileProvider.getUriForFile(activity, "${BuildConfig.APPLICATION_ID}.fileProvider", changelogFile)

                    GlobalManager.getInstance().mainActivity.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(changelogUri, "text/html")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                }
                .addButton("No", clickListener = MessageDialog::dismiss)
                .show()

            Config.setLong("version", activity.versionCode)
        }

        checkNewUpdates(true)
    }

    /**
     * Check for new game updates.
     *
     * @param silently If `true`, no prompts will be shown unless there's new updates.
     */
    @JvmStatic
    fun checkNewUpdates(silently: Boolean) {

        if (!silently) {
            ToastLogger.showText(R.string.update_info_checking, false)
        }

        async {
            // Cleaning up old updates.
            apksDirectory.listFiles()?.forEach { it.delete() }

            try {
                JsonObjectRequest(OnlineManager.updateEndpoint).use { request ->

                    val response = request.execute().json

                    val newVersion = response.getLong("version_code")
                    val link = response.getString("link")

                    if (newVersion <= GlobalManager.getInstance().mainActivity.versionCode) {
                        if (!silently) {
                            ToastLogger.showText(R.string.update_info_latest, false)
                        }
                        return@use
                    }

                    MessageDialog()
                        .setTitle("New update available!")
                        .setMessage(StringTable.get(R.string.update_dialog_message))
                        .addButton(StringTable.get(R.string.update_dialog_button_update)) { dialog ->
                            dialog.dismiss()

                            progressDialog = ProgressDialog().apply {
                                indeterminate = true
                                allowDismiss = false
                                max = 100
                                title = "Downloading update"
                                message = StringTable.format(R.string.update_info_downloading, 0)
                                show()
                            }

                            async {
                                val file = File(apksDirectory, "${newVersion}.apk")
                                if (file.exists()) {
                                    file.delete()
                                }
                                file.createNewFile()

                                val fileRequest = FileRequest(file, link)
                                fileRequest.observer = this@UpdateManager
                                fileRequest.execute()
                            }
                        }
                        .addButton("Update later") { it.dismiss() }
                        .show()
                }

            } catch (e: Exception) {
                Log.e("UpdateManager", "Failed to check for updates.", e)

                if (!silently) {
                    ToastLogger.showText(R.string.update_info_check_failed, false)
                }
            }
        }
    }


    override fun onDownloadUpdate(request: FileRequest) = mainThread {
        val progress = request.progress.toInt()

        progressDialog?.indeterminate = false
        progressDialog?.progress = progress
        progressDialog?.setMessage(StringTable.format(R.string.update_info_downloading, progress))
    }

    override fun onDownloadEnd(request: FileRequest) {
        mainThread { progressDialog?.dismiss() }

        val activity = GlobalManager.getInstance().mainActivity
        val uri = FileProvider.getUriForFile(activity, "${BuildConfig.APPLICATION_ID}.fileProvider", request.file)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activity.startActivity(intent)
    }

    override fun onDownloadFail(request: FileRequest, exception: Exception) {
        Log.e("UpdateManager", "Failed to download update.", exception)
        ToastLogger.showText(R.string.update_info_download_failed, false)

        mainThread { progressDialog?.dismiss() }
        request.file.delete()
    }

    override fun onDownloadCancel(request: FileRequest) {
        ToastLogger.showText(R.string.update_info_download_canceled, false)

        mainThread { progressDialog?.dismiss() }
        request.file.delete()
    }
}