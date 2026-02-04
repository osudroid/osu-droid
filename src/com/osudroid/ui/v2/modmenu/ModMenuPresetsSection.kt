package com.osudroid.ui.v2.modmenu

import com.osudroid.data.*
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.modmenu.ModMenu.addMod
import com.osudroid.ui.v2.modmenu.ModMenu.removeMod
import com.osudroid.utils.searchContiguously
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.ui.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.utils.*

class ModMenuPresetsSection : ModMenuSection("Presets") {


    private val addButton: UITextButton

    private var modPresets = listOf<ModPreset>()


    init {
        style = {
            width = 9f.rem
        }

        addButton = UITextButton().apply {
            width = Size.Full
            text = "Add preset"
            leadingIcon = FontAwesomeIcon(Icon.Plus)
            onActionUp = {
                ModPresetsForm(this@ModMenuPresetsSection).show()
            }
        }
        toggleContainer += addButton
    }


    fun saveModPreset(name: String) {

        if (name.isEmpty()) {
            return
        }

        val modPreset = ModPreset(
            name = name,
            serializedMods = ModMenu.enabledMods.serializeMods(includeIrrelevantMods = true)
        )

        DatabaseManager.modPresetTable.insert(modPreset)
        loadPresets()
        onModsChanged()
    }

    fun loadPresets() {
        toggleContainer.detachChildren { it is ModPresetToggle }

        modPresets = DatabaseManager.modPresetTable.getAll()

        for (preset in modPresets) {

            preset.mods = ModUtils.deserializeMods(preset.serializedMods)
            toggleContainer += ModPresetToggle(preset)
        }
    }


    fun onModsChanged() {
        val enabledMods = ModMenu.enabledMods

        addButton.isEnabled = enabledMods.isNotEmpty()

        toggleContainer.callOnChildren { toggle ->
            if (toggle is ModPresetToggle) {
                toggle.isSelected = toggle.preset.mods == enabledMods
            }
        }
    }

    override fun onSearchTermUpdate(searchTerm: String) {
        toggleContainer.callOnChildren { toggle ->
            if (toggle is ModPresetToggle) {
                toggle.isVisible = toggle.preset.name.searchContiguously(searchTerm)
            }
        }
    }

    inner class ModPresetToggle(val preset: ModPreset) : UIButton() {

        init {
            orientation = Orientation.Vertical
            spacing = 8f
            width = Size.Full
            cullingMode = CullingMode.CameraBounds

            onActionUp = {
                if (isSelected) {
                    ModMenu.clear()
                } else {
                    ModMenu.modToggles.fastForEach { toggle ->
                        val presetMod = preset.mods[toggle.mod::class]

                        if (presetMod != null) {
                            // Prevent operations from modifying mods in the preset.
                            addMod(presetMod.deepCopy())
                        } else {
                            removeMod(toggle.mod)
                        }
                    }
                }
            }

            onActionLongPress = {
                UIMessageDialog().apply {
                    title = "Delete preset"
                    text = "Delete preset \"${preset.name}\"?"

                    addButton(UITextButton().apply {
                        text = "Delete"
                        isSelected = true
                        onActionUp = {
                            hide()
                            DatabaseManager.modPresetTable.delete(preset)
                            loadPresets()
                        }
                    })

                    addButton(UITextButton().apply {
                        text = "Cancel"
                        onActionUp = {
                            hide()
                        }
                    })
                    show()
                }
            }

            text {
                text = preset.name
                fontSize = FontSize.SM
            }

            +ModsIndicator().apply {
                iconSize = 21f
                mods = preset.mods.values
            }
        }

    }
}