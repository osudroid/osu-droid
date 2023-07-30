package com.reco1l.api.ibancho.data

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
        var winCondition: WinCondition

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
     * The room status.
     */
    var status: RoomStatus? = null

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
     * Get player by the given UID.
     */
    fun getPlayerByUID(uid: Long) = activePlayers.find { it.id == uid }


    /**
     * Special handling to add a player in the array.
     */
    fun addPlayer(player: RoomPlayer)
    {
        val index = players.indexOfFirst { it?.id == player.id || it == null }

        players[index] = player
        sortPlayers()
    }

    /**
     * Special handling to remove a player in the array.
     *
     * @return The removed player or `null` if it wasn't on the array.
     */
    fun removePlayer(uid: Long): RoomPlayer?
    {
        val index = players.indexOfFirst { it?.id == uid }.takeUnless { it == -1 } ?: return null
        val removed = players[index]

        players[index] = null
        sortPlayers()

        return removed
    }


    /**
     * Sort players array placing host and non-null first.
     */
    private fun sortPlayers()
    {
        players = players.sortedWith { a, b ->
            when
            {
                // Empty slots goes at the end
                a == null -> 1
                b == null -> -1
                // If it's the host we set it always first
                a.id == host -> -1
                b.id == host -> 1
                // If none of the conditions above are true, means the position of the item is correct.
                else -> 0
            }
        }.toTypedArray()
    }
}
