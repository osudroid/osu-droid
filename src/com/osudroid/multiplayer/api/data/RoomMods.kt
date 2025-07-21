package com.osudroid.multiplayer.api.data

import com.osudroid.multiplayer.Multiplayer
import com.rian.osu.utils.ModHashMap
import com.rian.osu.utils.ModUtils
import org.json.JSONArray
import org.json.JSONException

class RoomMods @JvmOverloads constructor(json: JSONArray? = null) : ModHashMap(ModUtils.deserializeMods(json)) {

    @Throws(JSONException::class)
    constructor(str: String) : this(JSONArray(str))

    fun toString(room: Room): String {
        if (isEmpty()) {
            return if (room.gameplaySettings.isFreeMod) "Free mods" else "None"
        }

        return if (room.gameplaySettings.isFreeMod) buildString {

            append("Free mods, ")

            for (mod in values) {
                if (!mod.isValidForMultiplayerAsFreeMod) {
                    append("$mod, ")
                }
            }

        }.substringBeforeLast(',') else toDisplayModString()
    }


    override fun equals(other: Any?): Boolean {

        if (other === this) {
            return true
        }

        if (other !is RoomMods) {
            return false
        }

        val gameplaySettings = Multiplayer.room?.gameplaySettings

        if (gameplaySettings?.isFreeMod == true) {
            val requiredMods = filter { !it.value.isValidForMultiplayerAsFreeMod }
            val otherRequiredMods = other.filter { !it.value.isValidForMultiplayerAsFreeMod }

            return requiredMods.size == otherRequiredMods.size && requiredMods.values.containsAll(otherRequiredMods.values)
        }

        return super.equals(other)
    }

    // Auto-generated this will help to check instance equality aka ===
    override fun hashCode() = super.hashCode()
}