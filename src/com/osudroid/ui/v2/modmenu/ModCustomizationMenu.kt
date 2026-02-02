package com.osudroid.ui.v2.modmenu

import com.osudroid.multiplayer.Multiplayer
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.pct
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.mods.*
import com.rian.osu.mods.settings.*
import kotlin.reflect.KClass

class ModCustomizationMenu(trigger: UIButton) : UIModal(

    card = UIScrollableContainer().apply {
        scrollAxes = Axes.Y
        width = 0.475f.pct
        height = 0.675f.pct
        scaleCenter = Anchor.TopCenter
        clipToBounds = true
        style = {
            val (_, triggerBottom) = trigger.convertLocalToSceneCoordinates(0f, trigger.height)

            x = UIEngine.current.safeArea.x
            y = triggerBottom + 3f.srem

            scrollPadding = Vec2(0f, 4f.rem)
            backgroundColor = it.accentColor * 0.15f
            radius = Radius.LG
        }

        +UILinearContainer().apply {
            width = Size.Full
            orientation = Orientation.Vertical
        }
    }

) {

    private val modSettings = mutableMapOf<KClass<out Mod>, ModSettingsSection>()
    private val modSettingsContainer: UILinearContainer = card[0]!!
    private val modSettingComponents = mutableListOf<IModSettingComponent<*>>()


    //region Mods

    fun onModAdded(mod: Mod) {
        if (mod.settings.isEmpty()) {
            return
        }

        val section = ModSettingsSection(mod)

        modSettings[mod::class]?.detachSelf()
        modSettings[mod::class] = section

        modSettingsContainer += section
    }

    fun onModRemoved(mod: Mod) {
        modSettings[mod::class]?.detachSelf()
        modSettings.remove(mod::class)
    }

    fun isEmpty() = modSettings.isEmpty()

    //endregion

    //region Component lifecycle

    fun updateComponents() {
        modSettingComponents.fastForEach { it.update() }
    }

    fun updateComponentEnabledStates() {
        val room = Multiplayer.room
        val isHost = Multiplayer.isRoomHost

        modSettingComponents.fastForEach {
            it.isEnabled = if (room != null) {
                it.mod.isValidForMultiplayer && (isHost || (room.gameplaySettings.isFreeMod && it.mod.isValidForMultiplayerAsFreeMod))
            } else {
                true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ModSettingComponent(mod: Mod, setting: ModSetting<*>): ModSettingComponent<*, *> {

        val component = when (setting) {
            is FloatModSetting ->
                if (setting.useManualInput) FloatModSettingTextInput(mod, setting)
                else FloatModSettingSlider(mod, setting)

            is IntegerModSetting ->
                if (setting.useManualInput) IntegerModSettingTextInput(mod, setting)
                else IntegerModSettingSlider(mod, setting)

            is NullableFloatModSetting ->
                if (setting.useManualInput) NullableFloatModSettingTextInput(mod, setting)
                else NullableFloatModSettingSlider(mod, setting)

            is NullableIntegerModSetting ->
                if (setting.useManualInput) NullableIntegerModSettingTextInput(mod, setting)
                else NullableIntegerModSettingSlider(mod, setting)

            is BooleanModSetting -> ModSettingCheckbox(mod, setting)

            is EnumModSetting<*> -> ModSettingEnum(mod, setting as EnumModSetting<Enum<*>>)

            else -> throw IllegalArgumentException("Unsupported mod setting type: ${setting::class.java.simpleName}")
        }

        modSettingComponents.add(component)
        component.update()

        component.isEnabled =
            if (Multiplayer.isMultiplayer && Multiplayer.room != null) {
                mod.isValidForMultiplayer && (Multiplayer.isRoomHost ||
                    (Multiplayer.room!!.gameplaySettings.isFreeMod && mod.isValidForMultiplayerAsFreeMod))
            } else true

        return component
    }

    //endregion


    private inner class ModSettingsSection(val mod: Mod) : UILinearContainer() {

        init {
            orientation = Orientation.Vertical
            width = Size.Full
            style = {
                padding = Vec4(0f, 0f, 0f, 4f.srem)
            }

            +UILinearContainer().apply {
                orientation = Orientation.Horizontal
                width = Size.Full
                style = {
                    padding = Vec4(3f.srem)
                    spacing = 2f.srem
                    backgroundColor = Color4.Black.copy(alpha = 0.05f)
                    radius = Radius.LG
                }

                +ModIcon(mod).apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    width = FontSize.SM
                    height = FontSize.SM
                }

                +UIText().apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    fontSize = FontSize.SM
                    text = mod.name.uppercase()
                    style = {
                        color = it.accentColor
                        alpha = 0.9f
                    }
                }
            }

            linearContainer {
                orientation = Orientation.Vertical
                width = Size.Full
                style = {
                    padding = Vec4(2f.srem, 0f)
                }
                mod.settings.fastForEach { +ModSettingComponent(mod, it) }
            }
        }
    }
}
