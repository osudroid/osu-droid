package com.osudroid.multiplayer.api.data

import com.osudroid.utils.ModHashMap
import com.osudroid.utils.ModUtils
import org.json.JSONArray

class RoomMods @JvmOverloads constructor(val json: String = "") : ModHashMap(ModUtils.deserializeMods(json)) {

    constructor(array: JSONArray) : this(array.toString())

    /**
     * Compares two [RoomMods] instances taking the free-mod setting into account.
     *
     * In free-mod, only the required (non-free) mods need to match; individual player mods are irrelevant for
     * room-level equality. This is intentionally a separate named method rather than overriding [equals] so that
     * [equals] remains a pure, deterministic, context-free operation.
     *
     * @param other The other [RoomMods] to compare against.
     * @param isFreeMod Whether the room currently has free-mod enabled.
     */
    fun equalsWithContext(other: RoomMods, isFreeMod: Boolean): Boolean {
        if (other === this) return true

        if (isFreeMod) {
            val requiredMods = filter { !it.value.isValidForMultiplayerAsFreeMod }
            val otherRequiredMods = other.filter { !it.value.isValidForMultiplayerAsFreeMod }

            return requiredMods.size == otherRequiredMods.size &&
                requiredMods.values.containsAll(otherRequiredMods.values)
        }

        return this == other
    }
}