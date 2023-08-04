package com.reco1l.api.ibancho.data

import com.reco1l.legacy.ui.multiplayer.multiLog

data class Room(
        /**
         * The room ID.
         */
        val id: Long,

        /**
         * The room name.
         */
        var name: String,

        /**
         * Indicates if the room has password.
         */
        var isLocked: Boolean,

        /**
         * The max amount of players.
         */
        val maxPlayers: Int,

        /**
         * The active player count.
         */
        var playerCount: Int,

        /**
         * The active player names concatenated.
         */
        var playerNames: String,

        /**
         * The enabled mods.
         */
        var mods: String?,

        /**
         * Free mods condition
         */
        var isFreeMods: Boolean,

        /**
         * The room versus mode.
         */
        var teamMode: TeamMode,

        /**
         * The room win condition.
         */
        var winCondition: WinCondition,

        /**
         * The room status.
         */
        var status: RoomStatus? = null

)
{
    /**
     * The array containing the players, null values are empty slots. The array size correspond to [maxPlayers].
     */
    var players: Array<RoomPlayer?> = arrayOfNulls(maxPlayers)

    /**
     * The host/room owner UID.
     */
    var host: Long = -1
        set(value)
        {
            field = value
            sortPlayers()
        }

    /**
     * The current beatmap.
     */
    var beatmap: RoomBeatmap? = null

    /**
     * Besides [players] it provides an array trimmed with no empty slots.
     */
    val activePlayers
        get() = players.filterNotNull()

    /**
     * Returns an array of all players that are in READY status.
     */
    val readyPlayers
        get() = activePlayers.filter { it.status == PlayerStatus.READY }

    /**
     * Returns an array of all players that has the current beatmap.
     */
    val playersWithBeatmap
        get() = activePlayers.filter { it.status != PlayerStatus.MISSING_BEATMAP }

    /**
     * The array of players that corresponds to the BLUE team.
     */
    val blueTeamPlayers
        get() = activePlayers.filter { it.team == RoomTeam.BLUE }

    /**
     * The array of players that corresponds to the RED team.
     */
    val redTeamPlayers
        get() = activePlayers.filter { it.team == RoomTeam.RED }

    /**
     * Get the host Player instance.
     */
    val hostPlayer
        get() = activePlayers.find { it.id == host }


    /**
     * Get player by the given UID.
     */
    fun getPlayerByUID(uid: Long): RoomPlayer?
    {
        val index = activePlayers.find { it.id == uid }

        if (index == null)
            multiLog("getPlayerByUID() - Unable to find user with UID: $uid")

        return index
    }


    /**
     * Special handling to add a player in the array.
     */
    fun addPlayer(player: RoomPlayer)
    {
        val index = players.indexOfFirst { it != null && it.id == player.id || it == null }

        if (index in players.indices)
            players[index] = player
        else
            multiLog("addPlayer() - Invalid index: $index (Array size: ${players.size})")

        sortPlayers()
    }

    /**
     * Special handling to remove a player in the array.
     *
     * @return The removed player or `null` if it wasn't on the array.
     */
    fun removePlayer(uid: Long): RoomPlayer?
    {
        val index = players.indexOfFirst { it != null && it.id == uid }
        val removed = players.getOrNull(index)

        if (removed != null)
        {
            players[index] = null
            sortPlayers()
        }
        else multiLog("WARNING: Tried to remove a player with invalid index: $index")

        return removed
    }


    /**
     * Sort players array placing non-null first.
     */
    private fun sortPlayers()
    {
        players = players
                .sortedWith { a, b -> (a == null).compareTo(b == null) }
                .toTypedArray()
    }
}
