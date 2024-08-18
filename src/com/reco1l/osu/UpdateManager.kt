package com.reco1l.osu

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.reco1l.framework.net.FileRequest
import com.reco1l.framework.net.IDownloaderObserver
import okhttp3.Request
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.updateEndpoint
import ru.nsu.ccfit.zuev.osuplus.BuildConfig.APPLICATION_ID
import ru.nsu.ccfit.zuev.osuplus.R.string.beatmap_downloader_cancel
import ru.nsu.ccfit.zuev.osuplus.R.string.update_dialog_button_changelog
import ru.nsu.ccfit.zuev.osuplus.R.string.update_dialog_button_update
import ru.nsu.ccfit.zuev.osuplus.R.string.update_dialog_message
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_download_canceled
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_updated
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_checking
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_check_failed
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_downloading
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_download_failed
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_latest
import java.io.File


object UpdateManager: IDownloaderObserver
{

    private val mainActivity = GlobalManager.getInstance().mainActivity

    private val preferences
        get() = PreferenceManager.getDefaultSharedPreferences(mainActivity)

    private val cacheDirectory = File(mainActivity.cacheDir, "updates").apply { mkdirs() }

    private val snackBar = Snackbar.make(mainActivity.window.decorView, "", LENGTH_INDEFINITE)

    
    private var downloadURL: String? = null
    
    private var newVersionCode: Long = mainActivity.versionCode
    

    fun onActivityStart() = mainThread {
        // Finding if there's a "pending changelog". This means the game was previously updated, we're
        // showing the changelog after update with a prompt asking user to show.
        preferences.apply {

            val latestUpdate = getLong("latestVersionCode", mainActivity.versionCode)
            val changelogLink = getString("changelogLink", null)

            if (!changelogLink.isNullOrEmpty()) {
                if (latestUpdate > mainActivity.versionCode) {
                    snackBar.apply {

                        // Will only dismiss if user wants.
                        duration = LENGTH_INDEFINITE

                        // Show changelog button.
                        setAction(update_dialog_button_changelog) {
                            context.startActivity(Intent(ACTION_VIEW, Uri.parse(changelogLink)))
                        }
                        setText(update_info_updated)
                        show()
                    }
                }
                // Now we're removing the cached changelog.
                edit().putString("changelogLink", null).apply()
            }
        }

        checkNewUpdates(true)
    }


    /**
     * Check for new game updates.
     * 
     * @param silently If `true` no prompt will be shown unless there's new updates.
     */
    fun checkNewUpdates(silently: Boolean)
    {
        if (!silently) {
            snackBar.apply {

                duration = LENGTH_INDEFINITE

                setText(update_info_checking)
                setAction(null, null)
                show()
            }
        }

        async {
            // Cleaning update directory first, checking if there's a newer package downloaded and
            // then installing it.
            cacheDirectory.listFiles()?.also { list ->

                var newestVersionDownloaded: Long = mainActivity.versionCode

                list.forEach {

                    val version = it.nameWithoutExtension.toLongOrNull() ?: return@forEach

                    // Deleting the file corresponding to this version if still present.
                    if (version == mainActivity.versionCode)
                        it.delete()

                    // Finding the newest package.
                    if (version > newestVersionDownloaded)
                        newestVersionDownloaded = version
                }

                // Directly navigate to installation if there's already a newer package.
                if (newestVersionDownloaded > mainActivity.versionCode) {
                    newVersionCode = newestVersionDownloaded
                    onFoundNewUpdate(silently)
                    return@async
                }
            }
                
            // Requesting to server asking for new updates.
            try {
                
                // Avoid new request if one was already done.
                if (downloadURL != null) {
                    onFoundNewUpdate(silently)
                    return@async
                }
                
                val request = Request.Builder()
                    .url(updateEndpoint + mainActivity.resources.configuration.locale.language)
                    .build()

                OnlineManager.client.newCall(request).execute().use {

                    val response = JSONObject(it.body!!.string())
                    val changelogLink = response.getString("changelog")

                    downloadURL = response.getString("link")
                    newVersionCode = response.getLong("version_code")

                    // Previous implementation has this check, server returning an older version 
                    // shouldn't happen.
                    if (newVersionCode <= mainActivity.versionCode) {
                        onAlreadyLatestVersion(silently)
                        return@async
                    }

                    // Storing change log link to show once the user update to next version.
                    preferences.apply {
                        edit().putString("changelogLink", changelogLink).apply()
                    }

                    onFoundNewUpdate(silently)
                }
            } catch (exception: Exception) {

                exception.printStackTrace()

                onUpdateCheckFailed(silently)
            }
        }
    }
    
    
    private fun onInstallNewUpdate(file: File) {

        val intent = Intent(ACTION_VIEW).apply {

            val uri = FileProvider.getUriForFile(mainActivity, "$APPLICATION_ID.fileProvider", file)

            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        }
        mainActivity.startActivity(intent)
    }
    
    private fun onDownloadNewUpdate(file: File) {

        // Empty string: At this point download URL shouldn't be null but if it is the case (which is weird) we set an
        // empty string so the downloader invokes onDownloadFail() and a prompt is shown to user rather than nothing.
        val url = downloadURL ?: ""
        
        val downloader = FileRequest(file, url)
        downloader.observer = this

        async {
            downloader.execute()

            mainThread {
                snackBar.apply {

                    duration = LENGTH_INDEFINITE

                    setText(StringTable.format(update_info_downloading, 0))
                    setAction(beatmap_downloader_cancel) { downloader.cancel() }
                    show()
                }
            }
        }

    }
    
    
    private fun onFoundNewUpdate(silently: Boolean) = mainThread {

        if (newVersionCode <= mainActivity.versionCode) {
            onAlreadyLatestVersion(silently)
            return@mainThread
        }

        preferences.apply {
            edit().putLong("latestVersionCode", newVersionCode).apply()
        }

        snackBar.apply {

            duration = 5000

            setText(update_dialog_message)
            setAction(update_dialog_button_update) {

                val file = File(cacheDirectory, "$newVersionCode.apk")
                
                // Files is already downloaded, navigating to installation.
                if (file.exists()) {
                    onInstallNewUpdate(file)
                    return@setAction
                }                 

                file.createNewFile()
                onDownloadNewUpdate(file)
            }
            show()
        }
    }

    private fun onUpdateCheckFailed(silently: Boolean) = mainThread {
        if (silently) {
            snackBar.dismiss()
            return@mainThread
        }

        snackBar.apply {

            duration = LENGTH_SHORT

            setText(update_info_check_failed)
            setAction(null, null)
            show()
        }
    }

    private fun onAlreadyLatestVersion(silently: Boolean) = mainThread {

        if (silently) {
            snackBar.dismiss()
            return@mainThread
        }

        snackBar.apply {

            duration = LENGTH_SHORT

            setText(update_info_latest)
            setAction(null, null)
            show()
        }
    }


    override fun onDownloadUpdate(downloader: FileRequest) = mainThread {

        snackBar.setText(StringTable.format(update_info_downloading, downloader.progress.toInt()))
    }

    override fun onDownloadEnd(downloader: FileRequest) {
        mainThread(snackBar::dismiss)
        onInstallNewUpdate(downloader.file)
    }

    override fun onDownloadFail(downloader: FileRequest, exception: Exception) {

        exception.printStackTrace()

        mainThread {
            snackBar.apply {

                duration = LENGTH_SHORT

                setText(update_info_download_failed)
                setAction(null, null)
                show()
            }
        }
        downloader.file.delete()
    }

    override fun onDownloadCancel(downloader: FileRequest) {
        mainThread {
            snackBar.apply {

                duration = LENGTH_SHORT

                setText(update_info_download_canceled)
                setAction(null, null)
                show()
            }
        }
        downloader.file.delete()
    }
}