package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import ru.nsu.ccfit.zuev.osu.Config
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

    private val modeTab = FilterBarTab()


    init {
        orientation = Orientation.Vertical
        spacing = FilterBarTab.SPACING

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
        modeTab.text = if (Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP) "PP" else "Score"
    }

}
