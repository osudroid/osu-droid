package com.reco1l.legacy.ui.multiplayer

import android.text.format.DateFormat
import android.util.Log
import com.reco1l.api.ibancho.data.Room
import com.reco1l.api.ibancho.data.RoomPlayer
import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.toDate
import com.reco1l.legacy.data.jsonToScoreboardItem
import com.reco1l.legacy.data.jsonToStatistic
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import java.io.File
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

object Multiplayer
{

    /**
     * The current room, it can become `null` if socket disconnects.
     */
    @JvmField
    var room: Room? = null

    /**
     * The player that correspond us according to client UID, it can become `null` if socket disconnects.
     */
    @JvmField
    var player: RoomPlayer? = null

    /**
     * Indicates if player is in multiplayer mode
     */
    @JvmField
    var isMultiplayer = false

    /**
     * Indicates if player is room host
     */
    @JvmField
    var isRoomHost = false

    /**
     * Indicates that the player is in a room or not
     */
    @JvmField
    var isConnected = false

    /**
     * Array containing live leaderboard
     */
    @JvmStatic
    var liveData: Array<ScoreBoardItem>? = null

    /**
     * Array containing final leaderboard
     */
    @JvmField
    var finalData: Array<StatisticV2>? = null


    private val LOG_FILE = File("${Config.getCorePath()}/Log", "multi_log.txt")

    init
    {
        LOG_FILE.writeText("[${"yyyy/MM/dd hh:mm:ss".toDate()}] Client ${MainActivity.versionName} started.")
    }


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

        if (getGlobal().engine.scene == getGlobal().gameScene.scene)
            getGlobal().gameScene.scoreBoard?.initScoreboard()
    }

    fun onFinalLeaderboard(array: JSONArray)
    {
        finalData = null

        // Avoiding data parsing if user left from ScoringScene
        if (getGlobal().engine.scene == RoomScene || getGlobal().engine.scene == getGlobal().songMenu.scene)
            return

        if (array.length() == 0)
        {
            multiLog("WARNING: Server provided empty final leaderboard.")
            return
        }

        val list = mutableListOf<StatisticV2>()

        for (i in 0 until array.length())
        {
            val json = array.optJSONObject(i) ?: continue
            list.add(jsonToStatistic(json))
        }

        if (list.isEmpty())
            return

        // Replacing server statistic with local
        val ownScore = getGlobal().gameScene.stat
        val ownScoreIndex = list.indexOfFirst { it.playerName == getOnline().username }.takeUnless { it == -1 }

        if (ownScore != null)
        {
            // This should never happen
            if (ownScoreIndex == null)
            {
                list.add(ownScore)
                multiLog("WARNING: Player score wasn't found in final leaderboard.")
            }
            else list[ownScoreIndex] = ownScore
        }

        finalData = list.toTypedArray()

        // Reloading results screen
        getGlobal().scoring.updateLeaderboard()
    }

    // Logging

    @JvmStatic
    fun log(text: String)
    {
        val timestamp = DateFormat.format("hh:mm:ss", System.currentTimeMillis())

        LOG_FILE.appendText("\n[$timestamp] $text")
    }

    @JvmStatic
    fun log(e: Throwable)
    {
        val timestamp = DateFormat.format("hh:mm:ss", System.currentTimeMillis())

        LOG_FILE.appendText("\n[$timestamp] EXCEPTION: ${e.className}\n${Log.getStackTraceString(e)}")
    }
}

fun multiLog(text: String) = Multiplayer.log(text)

fun multiLog(e: Throwable) = Multiplayer.log(e)