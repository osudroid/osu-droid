package com.reco1l.legacy

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import androidx.preference.PreferenceManager
import com.edlplan.ui.fragment.MarkdownFragment
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.reco1l.framework.lang.async
import com.reco1l.framework.lang.uiThread
import com.reco1l.framework.net.Downloader
import com.reco1l.framework.net.IDownloaderObserver
import okhttp3.Request
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.updateEndpoint
import ru.nsu.ccfit.zuev.osuplus.R.string.changelog_title
import ru.nsu.ccfit.zuev.osuplus.R.string.chimu_cancel
import ru.nsu.ccfit.zuev.osuplus.R.string.update_dialog_button_changelog
import ru.nsu.ccfit.zuev.osuplus.R.string.update_dialog_button_update
import ru.nsu.ccfit.zuev.osuplus.R.string.update_dialog_message
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_canceled
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_updated
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_checking
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_downloading
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_failed
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_latest
import ru.nsu.ccfit.zuev.osuplus.R.string.update_info_starting
import java.io.File


object UpdateManager: IDownloaderObserver
{

    private val mainActivity = GlobalManager.getInstance().mainActivity

    private val cacheDirectory = File(mainActivity.cacheDir, "updates").apply { mkdirs() }

    private val snackBar = Snackbar.make(mainActivity.window.decorView, "", LENGTH_INDEFINITE)

    
    private var downloadURL: String? = null
    
    private var newVersionCode: Long = mainActivity.versionCode
    

    fun onActivityStart()
    {
        // Finding if there's a "pending changelog". This means the game was previously updated, we're 
        // showing the changelog after update with a prompt asking user to show.
        PreferenceManager.getDefaultSharedPreferences(mainActivity).apply {

            val pendingChangelog = getString("pendingChangelog", null)
            if (!pendingChangelog.isNullOrEmpty())
            {
                snackBar.apply {
                    
                    // Will only dismiss if user wants.
                    duration = LENGTH_INDEFINITE
                    
                    // Show changelog button.
                    setAction(update_dialog_button_changelog) {

                        MarkdownFragment().apply {
                            setTitle(changelog_title)
                            setMarkdown(pendingChangelog)
                            show()
                        }
                    }
                    setText(update_info_updated)
                    show()
                }

                // Now we're removing the cached changelog.
                edit().putString("pendingChangelog", null).apply()
            }
        }

        checkNewUpdates(true)
    }


    /**
     * Check for new game updates.
     * 
     * @param silently If `true` no prompt will be show unless there's new updates.
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
                    onFoundNewUpdate()
                    return@async
                }
            }
                
            // Requesting to server asking for new updates.
            try {
                
                // Avoid new request if one was already done.
                if (downloadURL != null) {
                    onFoundNewUpdate()
                    return@async
                }
                
                val request = Request.Builder()
                    .url(updateEndpoint + mainActivity.resources.configuration.locale.language)
                    .build()

                OnlineManager.client.newCall(request).execute().use {

                    val response = JSONObject(it.body!!.string())
                    val changelogUrl = response.getString("changelog")

                    downloadURL = response.getString("link")
                    newVersionCode = response.getLong("version_code")

                    // Previous implementation has this check, server returning an older version 
                    // shouldn't happen.
                    if (newVersionCode <= mainActivity.versionCode) {
                        onAlreadyLatestVersion(silently)
                        return@async
                    }

                    // Storing change log link to show once the user update to next version.
                    PreferenceManager.getDefaultSharedPreferences(mainActivity).apply {
                        edit().putString("pendingChangelog", changelogUrl).apply()
                    }

                    onFoundNewUpdate()
                }
            } catch (_: Exception) {
                // TODO: Custom prompt for errors?
                onAlreadyLatestVersion(silently)
            }
        }
    }
    
    
    private fun onInstallNewUpdate(file: File) {

        val intent = Intent(ACTION_VIEW).apply {

            setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        mainActivity.startActivity(intent)
    }
    
    private fun onDownloadNewUpdate(file: File) {

        val url = downloadURL ?: return
        
        snackBar.apply {

            // Will only dismiss if user wants.
            duration = LENGTH_INDEFINITE

            setText(update_info_starting)
            setAction(null, null)
            show()
        }
        
        Downloader(file, url).apply {
            observer = this@UpdateManager
            download()
        }
    }
    
    
    private fun onFoundNewUpdate() = uiThread {

        if (newVersionCode <= mainActivity.versionCode) {
            onAlreadyLatestVersion(true)
            return@uiThread
        }

        snackBar.apply {
            
            // Will only dismiss if user wants.
            duration = LENGTH_INDEFINITE
            
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

    private fun onAlreadyLatestVersion(silently: Boolean) = uiThread {

        if (silently) {
            snackBar.dismiss()
            return@uiThread
        }

        snackBar.apply {

            duration = LENGTH_SHORT

            setText(update_info_latest)
            setAction(null, null)
            show()
        }
    }

    
    override fun onDownloadStart(downloader: Downloader) = uiThread {

        snackBar.apply {

            duration = LENGTH_INDEFINITE

            setText(StringTable.format(update_info_downloading, 0))
            setAction(chimu_cancel) { downloader.cancel() }
            show()
        }
    }

    override fun onDownloadUpdate(downloader: Downloader) = uiThread {

        snackBar.setText(StringTable.format(update_info_downloading, downloader.progress))
    }

    override fun onDownloadEnd(downloader: Downloader) {
        uiThread(snackBar::dismiss)
        onInstallNewUpdate(downloader.file)
    }

    override fun onDownloadFail(downloader: Downloader) {
        uiThread {
            snackBar.apply {

                duration = LENGTH_SHORT

                setText(update_info_failed)
                setAction(null, null)
                show()
            }
        }
        downloader.file.delete()
    }

    override fun onDownloadCancel(downloader: Downloader) {
        uiThread {
            snackBar.apply {

                duration = LENGTH_SHORT

                setText(update_info_canceled)
                setAction(null, null)
                show()
            }
        }
        downloader.file.delete()
    }
}