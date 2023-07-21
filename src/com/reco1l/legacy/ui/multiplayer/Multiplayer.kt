package com.reco1l.legacy.ui.multiplayer

import com.reco1l.legacy.data.jsonToScoreboardItem
import com.reco1l.legacy.data.jsonToStatistic
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoard.ScoreBoardItems
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

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
    var liveData: Array<ScoreBoardItems> = emptyArray()

    /**Array containing final leaderboard*/
    @JvmStatic
    var finalData: Array<StatisticV2>? = null


    @JvmStatic
    fun clearLeaderboard()
    {
        liveData = emptyArray()
        finalData = null
    }


    fun onLiveLeaderboard(array: JSONArray)
    {
        liveData = Array(array.length()) { i ->
            val json = array.getJSONObject(i)

            jsonToScoreboardItem(json)
        }
        GlobalManager.getInstance().gameScene.scoreBoard?.initScoreboard()
    }

    fun onFinalLeaderboard(array: JSONArray)
    {
        finalData = null

        if (array.length() == 0)
            return

        val dataList = mutableListOf<StatisticV2>().apply {

            for (i in 0 until array.length())
            {
                val json = array.optJSONObject(i) ?: continue
                add(jsonToStatistic(json))
            }
        }

        dataList.find { it.playerName == OnlineManager.getInstance().username } ?: run {
            dataList.add(GlobalManager.getInstance().gameScene.stat)
        }

        finalData = dataList.toTypedArray()

        GlobalManager.getInstance().scoring.setRoomStatistics(finalData)
    }
}



