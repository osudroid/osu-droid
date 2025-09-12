package com.osudroid.ui.v2.modmenu

import com.osudroid.multiplayer.Multiplayer
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.mods.*
import com.rian.osu.mods.settings.*
import kotlin.reflect.KClass
import ru.nsu.ccfit.zuev.osu.ResourceManager

class ModCustomizationMenu : UIModal(

    card = UIScrollableContainer().apply {
        scrollAxes = Axes.Y
        relativeSizeAxes = Axes.Both
        width = 0.475f
        height = 0.75f
        x = 60f
        y = 90f
        scaleCenter = Anchor.TopCenter
        clipToBounds = true
        scrollPadding = Vec2(0f, 300f)

        +UILinearContainer().apply {
            width = FillParent
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
            width = FillParent
            padding = Vec4(0f, 0f, 0f, 16f)

            +UILinearContainer().apply {
                orientation = Orientation.Horizontal
                width = FillParent
                padding = Vec4(20f, 14f)
                spacing = 12f
                background = UIBox().apply {
                    color = Color4.Black
                    alpha = 0.05f
                    cornerRadius = 12f
                }

                +ModIcon(mod).apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    width = 34f
                    height = 34f
                }

                +UIText().apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = mod.name.uppercase()
                    applyTheme = {
                        color = it.accentColor
                        alpha = 0.9f
                    }
                }
            }

            mod.settings.fastForEach { +ModSettingComponent(mod, it) }
        }
    }
}
