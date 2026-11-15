package com.osudroid.ui.v2.songselect

import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.edlplan.replay.OsuDroidReplayPack
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.online.model.BeatmapScoreModel
import com.osudroid.resources.R
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.osudroid.utils.updateThread
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.File

/**
 * The options of a local score, shown when its item in the [SongSelectLeaderboard] is held.
 */
class ScoreOptionsDialog(
    private val beatmapInfo: BeatmapInfo,
    score: BeatmapScoreModel,
    private val onDeleted: () -> Unit,
) : OptionsDialog() {

    private val scoreId = score.scoreId.toInt()


    init {
        title = score.playerName

        innerContent.apply {
            +optionRow(optionButton(optionIcon(Icon.FileExport)) {
                text = "Export replay"
                onActionUp = {
                    hide()
                    async { exportReplay() }
                }
            })

            +optionRow(deleteOptionButton {
                text = "Delete score"
                onActionUp = {
                    hide()
                    showDeleteConfirmation()
                }
            })
        }

        addButton {
            text = "Close"
            isSelected = true
            onActionUp = { hide() }
        }
    }


    private fun exportReplay() {
        try {
            val scoreInfo = DatabaseManager.scoreInfoTable.getScore(scoreId) ?: throw IllegalStateException("Score $scoreId not found")

            val directory = File(Environment.getExternalStorageDirectory(), EXPORT_DIRECTORY)
            directory.mkdirs()

            val file = File(directory, "${beatmapInfo.filename.substringBeforeLast('.')} [${scoreInfo.playerName}]-${scoreInfo.time}.edr")
            OsuDroidReplayPack.packTo(file, beatmapInfo, scoreInfo)

            updateThread { showExported(file) }
        } catch (e: Exception) {
            Log.e("ScoreOptionsDialog", "Failed to export replay", e)
            ToastLogger.showText(R.string.frg_score_menu_export_failed, true)
        }
    }

    private fun showExported(file: File) {
        UIMessageDialog().apply {
            title = "Replay exported"
            text = "Saved in $EXPORT_DIRECTORY"

            addButton {
                text = "Share"
                isSelected = true
                onActionUp = {
                    hide()
                    mainThread { shareReplay(file) }
                }
            }

            addButton {
                text = "Close"
                onActionUp = { hide() }
            }
        }.show()
    }

    private fun shareReplay(file: File) {
        try {
            val activity = GlobalManager.getInstance().mainActivity

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity, "${BuildConfig.APPLICATION_ID}.fileProvider", file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.startActivity(Intent.createChooser(intent, null))
        } catch (e: Exception) {
            Log.e("ScoreOptionsDialog", "Failed to share replay", e)
            ToastLogger.showText("Failed to share replay!", true)
        }
    }

    private fun showDeleteConfirmation() {
        UIConfirmDialog().apply {
            title = "Delete score"
            text = "Are you sure?"
            onConfirm = { async { deleteScore() } }
        }.show()
    }

    private fun deleteScore() {
        try {
            val scoreInfoTable = DatabaseManager.scoreInfoTable
            val scoreInfo = scoreInfoTable.getScore(scoreId)

            if (scoreInfo != null && scoreInfoTable.deleteScore(scoreId) != 0) {
                File(scoreInfo.replayPath).delete()

                ToastLogger.showText(R.string.menu_deletescore_delete_success, true)
                onDeleted()
            } else {
                ToastLogger.showText("Failed to delete score!", true)
            }
        } catch (e: Exception) {
            Log.e("ScoreOptionsDialog", "Failed to delete score", e)
            ToastLogger.showText("Failed to delete score!", true)
        }
    }


    companion object {
        /**
         * Where replays are exported to, relative to the external storage.
         */
        private const val EXPORT_DIRECTORY = "osu!droid/export"
    }

}
