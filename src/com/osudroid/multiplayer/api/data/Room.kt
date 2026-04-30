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
     * The array containing the players, null values are empty slots. The array size corresponds to [maxPlayers].
     *
     * Marked @Volatile so that when [sortPlayers] (or [resizePlayers]) replaces the array reference,
     * all threads immediately see the new reference without a stale cached copy.
     * Compound read-modify-write operations must still go through a @Synchronized method.
     */
    @Volatile
    var players: Array<RoomPlayer?> = arrayOfNulls(maxPlayers)

    /**
     * The host/room owner UID.
     */
    var host: Long = -1
        @Synchronized set(value) {
            field = value
            sortPlayers()
        }

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
     * Get the players list in map format using UIDs as keys.
     *
     * Backed by a cache that is invalidated whenever the players array is mutated
     * (see [sortPlayers] and [resizePlayers]).  Callers that already hold the lock
     * (e.g. every @Synchronized method in this class) get the cached map for free
     * on repeated accesses within the same event; callers on other threads pay at
     * most one rebuild per mutation event.
     */
    val playersMap: Map<Long, RoomPlayer>
        @Synchronized get() = _playersMap
            ?: players.filterNotNull().associateByTo(HashMap()) { it.id }.also { _playersMap = it }

    // Backing field for the playersMap cache.  Null means the cache is stale.
    // @Volatile so the null-write in sortPlayers/resizePlayers is immediately
    // visible to threads that read playersMap without going through the lock
    // (e.g. a quick null-check before acquiring).
    @Volatile
    private var _playersMap: Map<Long, RoomPlayer>? = null

    /**
     * Get the players list of a team in a map format.
     */
    val teamMap
        @Synchronized get() = activePlayers.groupBy { it.team }

    /**
     * Determines if the room team mode is team vs team.
     */
    val isTeamVersus
        get() = teamMode == TeamMode.TeamVersus


    /**
     * Special handling to add a player in the array.
     *
     * @return `true` if it was successfully added, `false` it if wasn't or if it was already in the array (this can
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
        sortPlayers()
        return !wasAlready
    }

    /**
     * Special handling to remove a player in the array.
     *
     * @return The removed player or `null` if it wasn't on the array.
     */
    @Synchronized
    fun removePlayer(uid: Long): RoomPlayer? {
        val index = players.indexOfFirst { it != null && it.id == uid }
        val removed = players.getOrNull(index)

        if (removed != null) {
            players[index] = null
            sortPlayers()
        } else {
            Multiplayer.log("WARNING: Tried to remove a player with invalid index: $index")
        }

        return removed
    }


    /**
     * Atomically resize the players array. Players whose slot index falls within the new size
     * are preserved; those beyond it are truncated (the server is authoritative on capacity).
     * Callers are expected to identify and surface any truncated players **before** calling
     * this method — see [com.osudroid.ui.v2.multi.RoomScene.onRoomMaxPlayersChange] (SI-2).
     * Callers outside this class must use this instead of assigning [players] directly so the
     * operation is covered by the same monitor that guards [addPlayer] / [removePlayer].
     */
    @Synchronized
    fun resizePlayers(newSize: Int) {
        players = players.copyOf(newSize)
        _playersMap = null
    }

    /**
     * Sort players array placing non-null first.
     * Also invalidates the [playersMap] cache since the array content has changed.
     */
    @Synchronized
    private fun sortPlayers() {
        players = players.sortedWith { a, b -> (a == null).compareTo(b == null) }.toTypedArray()
        _playersMap = null
    }
}
