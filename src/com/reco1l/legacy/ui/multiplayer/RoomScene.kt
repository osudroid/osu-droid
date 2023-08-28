package com.reco1l.legacy.ui.multiplayer

import android.app.AlertDialog
import com.reco1l.api.ibancho.IPlayerEventListener
import com.reco1l.api.ibancho.IRoomEventListener
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.ibancho.SpectatorAPI
import com.reco1l.api.ibancho.data.*
import com.reco1l.api.ibancho.data.PlayerStatus.*
import com.reco1l.api.ibancho.data.RoomTeam.*
import com.reco1l.api.ibancho.data.TeamMode.HEAD_TO_HEAD
import com.reco1l.api.ibancho.data.TeamMode.TEAM_VS_TEAM
import com.reco1l.api.ibancho.data.WinCondition.*
import com.reco1l.framework.extensions.orCatch
import com.reco1l.framework.lang.async
import com.reco1l.framework.lang.glThread
import com.reco1l.framework.lang.uiThread
import com.reco1l.legacy.data.modsToString
import com.reco1l.legacy.ui.entity.BeatmapButton
import com.reco1l.legacy.ui.entity.ComposedText
import com.reco1l.legacy.ui.multiplayer.Multiplayer.isConnected
import com.reco1l.legacy.ui.multiplayer.Multiplayer.isRoomHost
import com.reco1l.legacy.ui.multiplayer.Multiplayer.player
import com.reco1l.legacy.ui.multiplayer.Multiplayer.room
import org.anddev.andengine.engine.camera.SmoothCamera
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.SpriteBackground
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.*
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.TextButton
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen.LoadingScene
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.LibraryManager.INSTANCE as library
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources
import ru.nsu.ccfit.zuev.osu.menu.ModMenu.getInstance as getModMenu
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

object RoomScene : Scene(), IRoomEventListener, IPlayerEventListener
{

    /**
     * Indicates that the host can change beatmap (it should be false while a change request was done)
     *
     * This is only used if [player] is the room host.
     */
    @JvmField
    var awaitBeatmapChange = false

    /**
     * Indicates that the player can change its status, its purpose is to await server changes.
     */
    @JvmField
    var awaitStatusChange = false

    /**
     * Indicates that the player can change its mods, its purpose is to await server changes.
     */
    @JvmField
    var awaitModsChange = false


    val chat = RoomChat()

    val chatPreview = ComposedText(0f, 0f, getResources().getFont("smallFont"), 100)

    val leaveDialog = AlertDialog.Builder(getGlobal().mainActivity).apply {

        setTitle("Leave room")
        setMessage("Are you sure?")
        setPositiveButton("Yes") { dialog, _ ->

            dialog.dismiss()
            back()
        }
        setNegativeButton("No") { dialog, _ -> dialog.dismiss() }

    }


    private var backButton: Sprite? = null

    private var readyButton: TextButton? = null

    private var secondaryButton: TextButton? = null

    private var trackButton: BeatmapButton? = null

    private var modsButton: AnimSprite? = null

    private var playerList: RoomPlayerList? = null

    private var options: RoomOptions? = null


    private val onlinePanel = OnlinePanel()

    private val titleText = ChangeableText(20f, 20f, getResources().getFont("bigFont"), "", 100)

    private val stateText = ChangeableText(0f, 0f, getResources().getFont("smallFont"), "", 100)

    private val infoText = ChangeableText(0f, 0f, getResources().getFont("smallFont"), "", 100)


    init
    {
        RoomAPI.playerEventListener = this
        RoomAPI.roomEventListener = this
    }


    fun load()
    {
        detachChildren()
        clearTouchAreas()

        isBackgroundEnabled = true

        // Background dim
        val dim = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        dim.setColor(0f, 0f, 0f, 0.5f)
        attachChild(dim, 0)

        // Top bar
        val top = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), 120f)
        top.setColor(0f, 0f, 0f, 0.3f)
        attachChild(top)

        // Title
        attachChild(titleText)

        // State
        stateText.setPosition(20f, titleText.y + titleText.height)
        stateText.setColor(0.8f, 0.8f, 0.8f)
        attachChild(stateText)

        // Track selection button
        trackButton = BeatmapButton().also {

            it.setPosition(Config.getRES_WIDTH() - it.width + 20f, 130f + 40f)

            registerTouchArea(it)
            attachChild(it)
        }

        // Preview text
        chatPreview.setPosition(trackButton!!.x + 20f, 130f + 10f)
        attachChild(chatPreview)

        // Info text
        infoText.setPosition(trackButton!!.x + 20f, trackButton!!.y + trackButton!!.height + 10f)
        attachChild(infoText)

        // Ready button, this button will switch player status
        readyButton = object : TextButton(getResources().getFont("CaptionFont"), "Ready")
        {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!event.isActionUp || awaitStatusChange)
                    return false

                getResources().getSound("menuclick")?.play()
                awaitStatusChange = true

                // Switching status
                when (player!!.status)
                {
                    NOT_READY ->
                    {
                        if (room!!.beatmap == null)
                        {
                            ToastLogger.showText("Cannot ready when the host is changing beatmap.", true)
                            awaitStatusChange = false
                            return true
                        }

                        if (room!!.teamMode == TEAM_VS_TEAM && player!!.team == null)
                        {
                            ToastLogger.showText("You must select a team first!", true)
                            awaitStatusChange = false
                            return true
                        }
                        RoomAPI.setPlayerStatus(READY)
                    }

                    READY -> invalidateStatus()
                    MISSING_BEATMAP ->
                    {
                        ToastLogger.showText("Beatmap is missing, cannot ready.", true)
                        awaitStatusChange = false
                    }

                    else -> awaitStatusChange = false /*This case can never happen, the PLAYING status is set when a game starts*/
                }
                return true
            }
        }.also {

            it.width = 400f
            it.setColor(0.2f, 0.9f, 0.2f)
            it.setPosition(Config.getRES_WIDTH() - it.width - 20f, Config.getRES_HEIGHT() - it.height - 20f)

            registerTouchArea(it)
            attachChild(it)
        }

        // It'll only be shown if the player is the room host, if host status is set to READY, this button will start
        // the game otherwise it'll the options button.
        secondaryButton = object : TextButton(getResources().getFont("CaptionFont"), "Options")
        {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!event.isActionUp || awaitStatusChange)
                    return false

                if (player!!.status == READY)
                {
                    // Button shouldn't be visible at this point so ignoring input
                    if (!isRoomHost)
                        return false

                    // This can never happen, but we handle it just in case.
                    if (room!!.beatmap == null)
                    {
                        ToastLogger.showText("You must select a beatmap first.", true)
                        return true
                    }

                    // If it's team vs team we check if there's at least one per team
                    if (room!!.teamMode == TEAM_VS_TEAM)
                    {
                        val team = room!!.teamMap

                        if (team[RED].isNullOrEmpty() || team[BLUE].isNullOrEmpty())
                        {
                            ToastLogger.showText("At least 1 player per team is needed to start a match!", true)
                            return true
                        }
                    }

                    // Filtering players that can start the match, we ignore players that doesn't have the beatmap
                    val players = room!!.activePlayers.filter { it.status != MISSING_BEATMAP }

                    // Checking if there's at least 2 players
                    if (players.isEmpty() || players.size == 1)
                    {
                        ToastLogger.showText("At least 2 players need to be ready!", true)
                        return true
                    }

                    // Checking if all players that can play are ready.
                    if (players.all { it.status == READY })
                    {
                        getResources().getSound("menuhit")?.play()
                        RoomAPI.notifyMatchPlay()
                        return true
                    }
                    ToastLogger.showText("All players with the beatmap need to be ready!", true)
                }
                else uiThread {
                    options = RoomOptions()
                    options!!.show()
                }
                return false
            }
        }.also {

            it.width = 400f
            it.setColor(0.2f, 0.2f, 0.2f)
            it.setPosition(Config.getRES_WIDTH() - it.width - 20f, readyButton!!.y - it.height - 20f)

            registerTouchArea(it)
            attachChild(it)
        }

        // Buttons code copied from legacy code but improved, don't blame on me.
        val layoutBackButton = OsuSkin.get().getLayout("BackButton")
        val layoutMods = OsuSkin.get().getLayout("ModsButton")

        val loadedBackTextures = mutableListOf<String>()

        if (getResources().isTextureLoaded("menu-back-0"))
        {
            for (i in 0..59)
                if (getResources().isTextureLoaded("menu-back-$i")) loadedBackTextures.add("menu-back-$i")
        }
        else loadedBackTextures.add("menu-back")

        backButton = object : AnimSprite(0f, 0f, loadedBackTextures.size.toFloat(), *loadedBackTextures.toTypedArray<String>())
        {
            var scaleWhenHold = layoutBackButton?.property?.optBoolean("scaleWhenHold", true) ?: false

            var moved = false
            var dx = 0f
            var dy = 0f

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (event.isActionDown)
                {
                    if (scaleWhenHold) this.setScale(1.25f)

                    moved = false
                    dx = localX
                    dy = localY

                    getResources().getSound("menuback")?.play()
                    return true
                }
                if (event.isActionUp)
                {
                    this.setScale(1f)

                    if (!moved)
                        uiThread { leaveDialog.show() }
                    return true
                }
                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 50)
                {
                    this.setScale(1f)
                    moved = true
                }
                return false
            }
        }.also {

            if (OsuSkin.get().isUseNewLayout)
            {
                layoutBackButton?.baseApply(it)
                it.setPosition(0f, Config.getRES_HEIGHT() - it.heightScaled)
            }
            else it.setPosition(0f, Config.getRES_HEIGHT() - it.height)

            registerTouchArea(it)
            attachChild(it)
        }

        // Mods button, shown only if free mods or if the player it's the host
        modsButton = object : AnimSprite(0f, 0f, 0f, "selection-mods", "selection-mods-over")
        {
            var moved = false
            var dx = 0f
            var dy = 0f

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!isRoomHost && !room!!.isFreeMods || awaitModsChange || player!!.status == READY)
                    return true

                if (event.isActionDown)
                {
                    moved = false
                    dx = localX
                    dy = localY

                    frame = 1
                    return true
                }
                if (event.isActionUp)
                {
                    frame = 0
                    if (!moved)
                        getModMenu().show(this@RoomScene, getGlobal().selectedTrack)
                    return true
                }
                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 50)
                {
                    frame = 0
                    moved = true
                }
                return false
            }
        }.also {

            it.isVisible = false
            it.setScale(1.5f)

            registerTouchArea(it)
            attachChild(it)

            if (OsuSkin.get().isUseNewLayout)
            {
                layoutMods?.baseApply(it)
                it.setPosition(backButton!!.x + backButton!!.width, Config.getRES_HEIGHT() - it.heightScaled)
            }
            else it.setPosition(backButton!!.x + backButton!!.width, (Config.getRES_HEIGHT() - 90f))
        }

        // Online panel
        onlinePanel.setPosition(Config.getRES_WIDTH() - 410f - 6f, 6f)
        attachChild(onlinePanel)
    }

    // Update events

    @JvmStatic
    fun updateOnlinePanel() = glThread {

        onlinePanel.setInfo()
        onlinePanel.setAvatar()
    }

    private fun updateBackground(path: String?)
    {
        val texture = if (path != null && !Config.isSafeBeatmapBg())
            getResources().loadBackground(path) else getResources().getTexture("menu-background")

        val height = texture.height * (Config.getRES_WIDTH() / texture.width.toFloat())
        val width = Config.getRES_WIDTH().toFloat()

        background = SpriteBackground(Sprite(0f, (Config.getRES_HEIGHT() - height) / 2f, width, height, texture))
    }

    private fun updateInformation()
    {
        // Update room name text
        titleText.text = room!!.name

        // Update room state text
        stateText.text = buildString {

            append(room!!.activePlayers.size).append(" / ").append(room!!.maxPlayers).append(" players")
            append(" - ")
            append(room!!.readyPlayers.size).append(" ready")

            // Showing the amount of players per team if it's team vs team mode.
            if (room!!.teamMode == TEAM_VS_TEAM)
            {
                val team = room!!.teamMap

                append(" - ")
                append("Red Team: ").append(team[RED]?.size ?: 0).append(" vs Blue Team: ").append(team[BLUE]?.size ?: 0)
            }
        }

        // Update room info text
        infoText.text = """
            Host: ${room!!.hostPlayer?.name}
            Team mode: ${if (room!!.teamMode == HEAD_TO_HEAD) "Head-to-head" else "Team VS"}
            Win condition: ${
                when (room!!.winCondition)
                {
                    SCORE_V1 -> "Score V1"
                    ACCURACY -> "Accuracy"
                    MAX_COMBO -> "Max combo"
                    SCORE_V2 -> "Score V2"
                }
            }
            Mods: ${room!!.mods}
            Remove Slider Lock: ${if (room!!.isRemoveSliderLock) "Enabled" else "Disabled" }
        """.trimIndent()
    }

    private fun updateButtons()
    {
        if (player!!.status == READY)
        {
            readyButton!!.setText("Not ready")
            readyButton!!.setColor(0.9f, 0.2f, 0.2f)

            modsButton!!.isVisible = false
            secondaryButton!!.isVisible = isRoomHost

            if (isRoomHost)
            {
                secondaryButton!!.setText("Start match!")
                secondaryButton!!.setColor(0.2f, 0.9f, 0.2f)
            }
            return
        }

        readyButton!!.setText("Ready")
        readyButton!!.setColor(0.2f, 0.9f, 0.2f)

        secondaryButton!!.isVisible = true
        secondaryButton!!.setText("Options")
        secondaryButton!!.setColor(0.2f, 0.2f, 0.2f)

        modsButton!!.isVisible = isRoomHost || room!!.isFreeMods
    }

    // Actions

    fun invalidateStatus()
    {
        awaitStatusChange = true

        var newStatus = NOT_READY

        if (room!!.beatmap != null && getGlobal().selectedTrack == null)
            newStatus = MISSING_BEATMAP

        if (player!!.status != newStatus)
            RoomAPI.setPlayerStatus(newStatus)
        else
            awaitStatusChange = false
    }

    fun clear()
    {
        room = null
        player = null
        isRoomHost = false
        isConnected = false

        // Clearing chat
        chat.log.clear()
        chat.dismiss()

        // Hide any player menu if its shown
        uiThread {
            playerList?.menu?.dismiss()
            options?.dismiss()
            Unit
        }

        // Clearing player list
        playerList?.detachSelf()
        playerList = null
    }


    // Navigation

    override fun back()
    {
        { RoomAPI.disconnect() }.orCatch { }
        clear()
        LobbyScene.show()
    }

    fun show()
    {
        (getGlobal().camera as SmoothCamera).apply {

            setZoomFactorDirect(1f)

            if (Config.isShrinkPlayfieldDownwards())
                setCenterDirect(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f)
        }

        if (!isConnected)
        {
            back()
            return
        }

        getGlobal().engine.scene = this

        // Updating beatmap just in case only if there's no await lock.
        if (!awaitBeatmapChange)
            onRoomBeatmapChange(room!!.beatmap)

        // Invalidating status
        invalidateStatus()

        chat.show()
    }


    // Communication

    override fun onServerError(error: String) = ToastLogger.showText(error, true)

    override fun onRoomChatMessage(uid: Long?, message: String)
    {
        // If username is null considering it as system message
        if (uid != null)
        {
            val player = room!!.playersMap[uid] ?: run {

                multiLog("WARNING: Unable to find user by ID on chat message")
                return
            }

            if (!player.isMuted)
                chat.onRoomChatMessage(player, message)
        }
        else
            chat.onSystemChatMessage(message, "#007BFF")
    }

    // Connection

    override fun onRoomConnect(newRoom: Room)
    {
        if (room != newRoom)
            chat.onSystemChatMessage("Welcome to osu!droid multiplayer", "#007BFF")

        // Setting new room
        room = newRoom
        isConnected = true

        // Releasing await locks just in case
        awaitModsChange = false
        awaitBeatmapChange = false
        awaitStatusChange = false

        // Finding our player object
        player = newRoom.playersMap[getOnline().userId] ?: run {

            multiLog("ERROR: Unable to find player in the map.")
            ToastLogger.showText("Unable to find player", false)
            back()
            return
        }

        async {
            SpectatorAPI.joinRoom(newRoom.id, player!!)
        }

        // Determine if it's the host
        isRoomHost = player!!.id == newRoom.host

        // Reloading player list
        playerList?.detachSelf()
        playerList = RoomPlayerList(newRoom)
        attachChild(playerList, 1)

        // Reloading mod menu, we set player mods first in case the scene was reloaded (due to skin change).
        getModMenu().setMods(player!!.mods, false)
        getModMenu().init()
        getModMenu().setMods(newRoom.mods, newRoom.isFreeMods)

        // Updating player mods for other clients
        awaitModsChange = true

        RoomAPI.setPlayerMods(
            modsToString(getModMenu().mod),
            getModMenu().changeSpeed,
            getModMenu().fLfollowDelay,
            if (getModMenu().isEnableForceAR) getModMenu().forceAR else null
        )

        // Updating UI
        updateButtons()
        updateInformation()
        playerList!!.updateItems()

        show()
    }

    override fun onRoomDisconnect(reason: String?)
    {
        val roomId = room!!.id
        val uid = player!!.id

        async {
            SpectatorAPI.leaveRoom(roomId, uid)
        }

        clear()
        ToastLogger.showText("Disconnected from the room${reason?.let { ": $it" } ?: "" }", true)

        // If player is in one of these scenes we go back.
        if (getGlobal().engine.scene != getGlobal().gameScene.scene)
            back()
        else
            multiLog("Disconnected from socket while playing.")
    }

    override fun onRoomConnectFail(error: String?)
    {
        val roomId = room!!.id
        val uid = player!!.id

        async {
            SpectatorAPI.leaveRoom(roomId, uid)
        }

        clear()
        ToastLogger.showText("Failed to connect to the room: $error", true)
        back()
    }


    // Room changes

    override fun onRoomNameChange(name: String)
    {
        room!!.name = name
        updateInformation()
    }

    override fun onRoomBeatmapChange(beatmap: RoomBeatmap?)
    {
        // Updating values
        room!!.beatmap = beatmap

        // Searching the beatmap in the library
        getGlobal().selectedTrack = library.findTrackByMD5(beatmap?.md5)

        // Updating track button
        trackButton!!.updateBeatmap(beatmap)

        // Preventing from change song when host is in room while other players are in gameplay
        if (getGlobal().engine.scene != this)
        {
            awaitBeatmapChange = false
            return
        }

        // Notify to the host when other players can't download the beatmap.
        if (isRoomHost && beatmap != null && beatmap.parentSetID == null)
            ToastLogger.showText("This beatmap isn't available on Chimu.", false)

        // Updating player status
        invalidateStatus()

        // Updating background
        updateBackground(getGlobal().selectedTrack?.background)

        // Releasing await lock
        awaitBeatmapChange = false

        if (getGlobal().selectedTrack == null)
        {
            getGlobal().songService.stop()
            return
        }

        if (isRoomHost && beatmap != null)
            async {
                SpectatorAPI.changeBeatmap(room!!.id, beatmap.md5)
            }

        getGlobal().songService.preLoad(getGlobal().selectedTrack.beatmap.music)
        getGlobal().songService.play()
    }

    override fun onRoomHostChange(uid: Long)
    {
        room!!.host = uid

        chat.onSystemChatMessage("Player ${room!!.playersMap[uid]?.name} is now the room host.", "#007BFF")

        // Defining if is the host
        isRoomHost = getOnline().userId == uid

        // Reloading mod menu
        glThread {
            clearChildScene()

            getModMenu().init()
            getModMenu().update()

            // If we're the host we set our mods as room mods
            if (isRoomHost)
            {
                awaitModsChange = true

                RoomAPI.setRoomMods(
                    modsToString(getModMenu().mod),
                    getModMenu().changeSpeed,
                    getModMenu().fLfollowDelay,
                    if (getModMenu().isEnableForceAR) getModMenu().forceAR else null
                )
            }
        }

        // Updating host text
        updateInformation()

        // Updating buttons visibility
        updateButtons()

        // Updating player list
        playerList!!.updateItems()
    }


    // Mods

    override fun onRoomModsChange(mods: RoomMods)
    {
        if (mods != room!!.mods)
            invalidateStatus()

        room!!.mods = mods

        // If free mods is enabled it'll keep player mods and enforce speed changing mods and ScoreV2
        getModMenu().setMods(mods, room!!.isFreeMods)

        if (isRoomHost)
            async {
                SpectatorAPI.changeMods(room!!.id, mods)
            }

        // Updating player mods
        awaitModsChange = true

        RoomAPI.setPlayerMods(
            modsToString(getModMenu().mod),
            getModMenu().changeSpeed,
            getModMenu().fLfollowDelay,
            if (getModMenu().isEnableForceAR) getModMenu().forceAR else null
        )

        // Update room info text
        updateInformation()
    }

    override fun onRoomRemoveSliderLockChange(isEnabled: Boolean) {
        room!!.isRemoveSliderLock = isEnabled

        updateInformation()
    }

    /**This method is used purely to update UI in other clients*/
    override fun onPlayerModsChange(uid: Long, mods: RoomMods)
    {
        // Updating player mods
        room!!.playersMap[uid]!!.mods = mods

        // Updating player list
        playerList!!.updateItems()

        // Removing await lock
        if (uid == player!!.id)
            awaitModsChange = false
    }

    override fun onRoomFreeModsChange(isFreeMods: Boolean)
    {
        room!!.isFreeMods = isFreeMods

        // Update room info text
        updateInformation()

        // Closing mod menu, to enforce mod menu scene update
        clearChildScene()
        // Hiding mod button in case isn't the host when free mods is disabled
        modsButton!!.isVisible = isRoomHost || room!!.isFreeMods

        // Updating mod set
        getModMenu().setMods(room!!.mods, room!!.isFreeMods)

        // Updating player mods
        awaitModsChange = true

        RoomAPI.setPlayerMods(
            modsToString(getModMenu().mod),
            getModMenu().changeSpeed,
            getModMenu().fLfollowDelay,
            if (getModMenu().isEnableForceAR) getModMenu().forceAR else null
        )

        // Invalidating player status
        invalidateStatus()
    }

    override fun onRoomTeamModeChange(mode: TeamMode)
    {
        room!!.teamMode = mode

        if (isRoomHost)
            async {
                SpectatorAPI.changeTeamMode(room!!.id, mode)
            }

        // Update room info text
        updateInformation()

        // Updating player list
        playerList!!.updateItems()

        // Setting player status to NOT_READY
        awaitStatusChange = true

        // Invalidating player status
        invalidateStatus()
    }

    override fun onRoomWinConditionChange(winCondition: WinCondition)
    {
        room!!.winCondition = winCondition

        // Update room info text
        updateInformation()

        if (isRoomHost)
        {
            awaitModsChange = true

            // If win condition is Score V2 we add the mod.
            val roomMods = room!!.mods.set.apply {

                if (winCondition == SCORE_V2)
                    add(MOD_SCOREV2)
                else
                    remove(MOD_SCOREV2)
            }

            // Applying to all room
            RoomAPI.setRoomMods(
                modsToString(roomMods),
                getModMenu().changeSpeed,
                getModMenu().fLfollowDelay,
                if (getModMenu().isEnableForceAR) getModMenu().forceAR else null
            )
        }

        // Updating player list
        playerList!!.updateItems()

        // Invalidating player status
        invalidateStatus()
    }


    // Match

    override fun onRoomMatchPlay()
    {
        if (player!!.status == READY)
        {
            if (getGlobal().selectedTrack == null)
            {
                multiLog("WARNING: Attempt to start match with null track.")
                return
            }

            getGlobal().songMenu.stopMusic()

            Replay.oldMod = getModMenu().mod
            Replay.oldChangeSpeed = getModMenu().changeSpeed
            Replay.oldForceAR = getModMenu().forceAR
            Replay.oldEnableForceAR = getModMenu().isEnableForceAR
            Replay.oldFLFollowDelay = getModMenu().fLfollowDelay

            getGlobal().gameScene.startGame(getGlobal().selectedTrack, null)

            // Hiding any player menu if its shown
            uiThread { playerList!!.menu.dismiss() }
        }

        if (isRoomHost)
            async {
                SpectatorAPI.startPlaying(room!!.id)
            }

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onRoomMatchStart()
    {
        if (getGlobal().engine.scene is LoadingScene)
            getGlobal().gameScene.start()

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onRoomMatchSkip()
    {
        if (getGlobal().engine.scene != getGlobal().gameScene.scene)
            return

        getGlobal().gameScene.skip()
    }


    // Leaderboard

    override fun onRoomLiveLeaderboard(leaderboard: JSONArray) = Multiplayer.onLiveLeaderboard(leaderboard)

    override fun onRoomFinalLeaderboard(leaderboard: JSONArray) = Multiplayer.onFinalLeaderboard(leaderboard)


    // Player related events

    override fun onPlayerJoin(player: RoomPlayer)
    {
        // We send the message if the player wasn't in the room, sometimes this event can be called by a reconnection.
        if (room!!.addPlayer(player))
            chat.onSystemChatMessage("Player ${player.name} (ID: ${player.id}) joined.", "#007BFF")

        // Updating state text
        updateInformation()

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerLeft(uid: Long)
    {
        val player = room!!.removePlayer(uid)

        // Notifying in chat
        if (player != null)
            chat.onSystemChatMessage("Player ${player.name} (ID: $uid) left.", "#007BFF")

        // Updating state text
        updateInformation()

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerKick(uid: Long)
    {
        if (uid == player!!.id)
        {
            multiLog("Kicked from room.")
            back()
            uiThread {
                AlertDialog.Builder(getGlobal().mainActivity).apply {

                    setTitle("Message")
                    setMessage("You've been kicked by room host.")
                    setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }

                }.show()
            }
            return
        }

        val player = room!!.removePlayer(uid)

        // Notifying in chat
        if (player != null)
            chat.onSystemChatMessage("Player ${player.name} (ID: $uid) was kicked.", "#FF0000")

        // Updating state text
        updateInformation()

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerStatusChange(uid: Long, status: PlayerStatus)
    {
        // Updating player status
        room!!.playersMap[uid]!!.status = status

        if (uid == player!!.id)
        {
            awaitStatusChange = false
            updateButtons()
        }

        // Updating state text
        updateInformation()

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerTeamChange(uid: Long, team: RoomTeam?)
    {
        // Updating player team
        room!!.playersMap[uid]!!.team = team

        if (uid == player!!.id && team != null)
            async {
                SpectatorAPI.changeTeam(room!!.id, uid, team)
            }

        // Updating player list
        playerList!!.updateItems()

        // Update information text
        updateInformation()
    }

    fun init() = Unit
}