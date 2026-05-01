package com.osudroid.multiplayer.api.data

import com.osudroid.multiplayer.Multiplayer

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
    var maxPlayers: Int,

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
    var mods: RoomMods,

    /**
     * Gameplay-related settings.
     */
    var gameplaySettings: RoomGameplaySettings,

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
    var status: RoomStatus? = null,

    /**
     * The unique session ID.
     */
    val sessionID: String? = null
) {
    /**
     * The players in this [Room]. `null` values are empty slots. The array size corresponds to [maxPlayers].
     *
     * Slot indices mirror the server's layout exactly: index `i` here corresponds to slot `i` on the
     * server. The array is never re-sorted locally so the two views never diverge.
     *
     * Marked [Volatile] so that when [resizePlayers] replaces the array reference, all threads immediately see the new
     * reference without a stale cached copy. **Compound read-modify-write operations must still go through a
     * [synchronized] method** (see [addPlayer], [removePlayer], and [resizePlayers]).
     */
    @Volatile
    var players: Array<RoomPlayer?> = arrayOfNulls(maxPlayers)
        @Synchronized set(value) {
            field = value
            _playersMap = null
        }

    /**
     * The host/room owner UID.
     */
    @Volatile
    var host: Long = -1

    /**
     * The current beatmap.
     */
    var beatmap: RoomBeatmap? = null
        set(value) {
            if (field != null) {
                previousBeatmap = field
            }

            field = value
        }

    /**
     * The previous beatmap.
     */
    var previousBeatmap: RoomBeatmap? = null
        private set

    /**
     * Besides [players] it provides an array trimmed with no empty slots.
     */
    val activePlayers
        @Synchronized get() = players.filterNotNull()

    /**
     * Returns an array of all players that are in READY status.
     */
    val readyPlayers
        @Synchronized get() = activePlayers.filter { it.status == PlayerStatus.Ready }

    /**
     * All players in this [Room], mapped by their user ID.
     */
    val playersMap
        @Synchronized get() = _playersMap
            ?: players.filterNotNull().associateByTo(HashMap()) { it.id }.also { _playersMap = it }

    @Volatile
    private var _playersMap: Map<Long, RoomPlayer>? = null

    /**
     * All [players] in this [Room], grouped by their team.
     */
    val teamMap
        @Synchronized get() = activePlayers.groupBy { it.team }

    /**
     * Whether this [Room] is in Team VS mode.
     */
    val isTeamVersus
        get() = teamMode == TeamMode.TeamVersus

    /**
     * Special handling to add a player to this [Room].
     *
     * The player is placed in the first available `null` slot and [players] is NOT re-sorted to preserve
     * server-assigned slot positions for all existing players.
     *
     * @return `true` if it was successfully added, `false` it if wasn't or if it was already in [players] (this can
     * happen due to reconnection).
     */
    @Synchronized
    fun addPlayer(player: RoomPlayer): Boolean {
        /** @see [RoomPlayer.equals] */
        var index = players.indexOf(player)
        val wasAlready = index in players.indices

        if (wasAlready) {
            Multiplayer.log("WARNING: Tried to add player while it was already in the array.")
        } else {
            index = players.indexOfFirst { it == null }
        }

        // Handling invalid index
        if (index !in players.indices) {
            Multiplayer.log("WARNING: Tried to add player with invalid index: $index")
            return false
        }

        players[index] = player
        _playersMap = null
        return !wasAlready
    }

    /**
     * Special handling to remove a player from this [Room].
     *
     * The vacated slot is set to `null` in-place; [players] is NOT re-sorted so that all remaining players keep their
     * server-assigned slot positions.
     *
     * @return The removed player, or `null` if it wasn't in [players].
     */
    @Synchronized
    fun removePlayer(uid: Long): RoomPlayer? {
        val index = players.indexOfFirst { it != null && it.id == uid }
        val removed = players.getOrNull(index)

        if (removed != null) {
            players[index] = null
            _playersMap = null
        } else {
            Multiplayer.log("WARNING: Tried to remove a player with invalid index: $index")
        }

        return removed
    }

    /**
     * Atomically resizes [players]. Players whose slot index falls within the new size are preserved; those beyond it
     * are truncated (the server is authoritative on capacity).
     *
     * Callers are expected to identify and surface any truncated players **before** calling
     * this method — see [com.osudroid.ui.v2.multi.RoomScene.onRoomMaxPlayersChange].
     *
     * Callers outside this class must use this instead of assigning [players] directly so the
     * operation is covered by the same monitor that guards [addPlayer] / [removePlayer].
     */
    @Synchronized
    fun resizePlayers(newSize: Int) {
        players = players.copyOf(newSize)
    }
}
