package com.rian.osu.mods

import kotlin.math.max
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import org.json.JSONObject

/**
 * Represents the Muted mod.
 */
class ModMuted : Mod() {
    override val name = "Muted"
    override val acronym = "MU"
    override val description = "Can you still feel the rhythm without music?"
    override val type = ModType.Fun

    /**
     * Increase volume as combo builds.
     */
    @get:JvmName("isInverseMuting")
    var inverseMuting by object : BooleanModSetting(
        name = "Start muted",
        defaultValue = false
    ) {
        override var value
            get() = super.value
            set(value) {
                super.value = value
                onValueChange(value)
            }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            super.setValue(thisRef, property, value)
            onValueChange(value)
        }

        private fun onValueChange(newValue: Boolean) {
            (::muteComboCount).run {
                isAccessible = true
                (getDelegate() as IntegerModSetting).minValue = if (newValue) 1 else 0
            }
        }
    }

    /**
     * Add a metronome beat to help the player keep track of the rhythm.
     */
    @get:JvmName("isEnableMetronome")
    var enableMetronome by BooleanModSetting(
        name = "Enable metronome",
        defaultValue = true
    )

    /**
     * The combo count at which point the track reaches its final volume.
     */
    var muteComboCount by IntegerModSetting(
        name = "Final volume at combo",
        defaultValue = 100,
        minValue = 0,
        maxValue = 500
    )

    /**
     * Hit sounds are also muted alongside the track.
     */
    @get:JvmName("affectsHitSounds")
    var affectsHitSounds by BooleanModSetting(
        name = "Mute hit sounds",
        defaultValue = true
    )

    /**
     * Obtains the volume at a given combo.
     *
     * @param combo The combo.
     * @return The volume at [combo], where 0 is muted and 1 is full volume.
     */
    fun volumeAt(combo: Int): Float {
        val volume = (combo / max(1f, muteComboCount.toFloat())).coerceIn(0f, 1f)

        return if (inverseMuting) volume else 1 - volume
    }

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        inverseMuting = settings.optBoolean("inverseMuting", inverseMuting)
        enableMetronome = settings.optBoolean("enableMetronome", enableMetronome)
        muteComboCount = settings.optInt("muteComboCount", muteComboCount)
        affectsHitSounds = settings.optBoolean("affectsHitSounds", affectsHitSounds)
    }

    override fun serializeSettings() = JSONObject().apply {
        put("inverseMuting", inverseMuting)
        put("enableMetronome", enableMetronome)
        put("muteComboCount", muteComboCount)
        put("affectsHitSounds", affectsHitSounds)
    }

    override fun deepCopy() = ModMuted().also {
        it.inverseMuting = inverseMuting
        it.enableMetronome = enableMetronome
        it.muteComboCount = muteComboCount
        it.affectsHitSounds = affectsHitSounds
    }
}