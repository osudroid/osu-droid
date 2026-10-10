package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode

/**
 * Switches the leaderboard between local and global scores. For the global leaderboard a tab below the
 * button shows and switches the ranking (PP or score).
 */
class LeaderboardSwitchButton(private val leaderboard: SongSelectLeaderboard) : UILinearContainer() {

    private val switchIcon = FontAwesomeIcon(Icon.Mobile).apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        iconSize = 22f
    }

    private val switchButton = UIButton().apply {
        +switchIcon
    }

    private val modeText = UIText().apply {
        font = ResourceManager.getInstance().getFont("xs")
        anchor = Anchor.Center
        origin = Anchor.Center
        applyTheme = { color = it.accentColor }
    }

    private val modeTab = UIClickableContainer().apply {
        height = MODE_TAB_HEIGHT
        background = UIBox().apply {
            cornerRadius = 0.5f.srem
            applyTheme = { color = it.accentColor * 0.1f }
        }
        +modeText
    }


    init {
        orientation = Orientation.Vertical
        spacing = MODE_TAB_SPACING

        +switchButton.apply {
            onActionUp = {
                leaderboard.leaderboardType = if (leaderboard.leaderboardType == LeaderboardType.Local) LeaderboardType.Global else LeaderboardType.Local
                updateState()
            }
        }

        +modeTab.apply {
            onActionUp = {
                val mode = if (Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP)
                    BeatmapLeaderboardScoringMode.SCORE
                else
                    BeatmapLeaderboardScoringMode.PP

                Config.setBeatmapLeaderboardScoringMode(mode)
                updateState()
                leaderboard.onBeatmapSelected(SongSelect.selectedBeatmap)
            }
        }

        updateState()
    }


    /**
     * Sets the size of the button itself, the mode tab below it has the same width.
     */
    fun setButtonSize(width: Float, height: Float) {
        switchButton.width = width
        switchButton.height = height
        modeTab.width = width
    }

    private fun updateState() {
        val isGlobal = leaderboard.leaderboardType == LeaderboardType.Global

        switchIcon.icon = if (isGlobal) Icon.Globe else Icon.Mobile
        modeTab.isVisible = isGlobal
        modeText.text = if (Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP) "PP" else "Score"
    }

    companion object {
        private const val MODE_TAB_HEIGHT = 32f
        private const val MODE_TAB_SPACING = 6f
    }

}
