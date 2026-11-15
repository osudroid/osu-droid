package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode

/**
 * Switches the leaderboard from the local scores to the global ones ranked by PP, to the global ones ranked by
 * score and back to the local ones. Its [modeTab] names the current one.
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

    /**
     * Placed by the [SongSelectFilterBar] in the row below its buttons.
     */
    val modeTab = FilterBarTab()

    private val isGlobal
        get() = leaderboard.leaderboardType == LeaderboardType.Global


    init {
        +switchButton.apply {
            onActionUp = {
                when {
                    !isGlobal -> {
                        Config.setBeatmapLeaderboardScoringMode(BeatmapLeaderboardScoringMode.PP)
                        leaderboard.leaderboardType = LeaderboardType.Global
                    }

                    Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP -> {
                        Config.setBeatmapLeaderboardScoringMode(BeatmapLeaderboardScoringMode.SCORE)
                        // The type stays the same, so the scores are not loaded again on their own.
                        leaderboard.onBeatmapSelected(SongSelect.selectedBeatmap)
                    }

                    else -> leaderboard.leaderboardType = LeaderboardType.Local
                }

                updateState()
            }
        }

        updateState()
    }


    /**
     * Sets the size of the button itself, the mode tab has the same width for all of its texts.
     */
    fun setButtonSize(width: Float, height: Float) {
        switchButton.width = width
        switchButton.height = height
        modeTab.width = width
    }

    private fun updateState() {
        switchIcon.icon = if (isGlobal) Icon.Globe else Icon.Mobile
        modeTab.text = when {
            !isGlobal -> "Local"
            Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP -> "PP"
            else -> "Score"
        }
    }

}
