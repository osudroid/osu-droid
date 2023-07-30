package com.reco1l.legacy.ui.multiplayer

import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.logE
import com.reco1l.legacy.data.jsonToScoreboardItem
import com.reco1l.legacy.data.jsonToStatistic
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoard.ScoreBoardItems
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

object Multiplayer
{
    /**Indicates if player is in multiplayer mode */
    @JvmField
    var isMultiplayer = false

    /**Indicates if player is room host */
    @JvmField
    var isRoomHost = false

    /**Indicates that the player is in a room or not */
    @JvmField
    var isConnected = false

    /**Array containing live leaderboard*/
    @JvmStatic
    var liveData: Array<ScoreBoardItems>? = null

    /**Array containing final leaderboard*/
    @JvmStatic
    var finalData: Array<StatisticV2>? = null


    @JvmStatic
    fun clearLeaderboard()
    {
        liveData = null
        finalData = null
    }


    fun onLiveLeaderboard(array: JSONArray)
    {
        liveData = Array(array.length()) { i ->
            val json = array.getJSONObject(i)

            jsonToScoreboardItem(json)
        }
        getGlobal().gameScene.scoreBoard?.initScoreboard()
    }

    fun onFinalLeaderboard(array: JSONArray)
    {
        finalData = null

        if (array.length() == 0)
            return

        val list = mutableListOf<StatisticV2>().apply {

            for (i in 0 until array.length())
            {
                val json = array.optJSONObject(i) ?: continue
                add(jsonToStatistic(json))
            }
        }

        // Replacing server statistic with local
        val ownScoreIndex = list.indexOfFirst { it.playerName == getOnline().username }.takeUnless { it == -1 }
        val ownScore = getGlobal().gameScene.stat

        // This should never happen
        if (ownScoreIndex == null)
        {
            list.add(ownScore)
            "Player score wasn't found in final leaderboard".logE(className)
        }
        else list[ownScoreIndex] = ownScore

        finalData = list.toTypedArray()

        getGlobal().scoring.setRoomStatistics(finalData)
    }
}



