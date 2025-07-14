package com.osudroid.ui.v2.modmenu

import com.osudroid.data.*
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.modmenu.ModMenu.addMod
import com.osudroid.ui.v2.modmenu.ModMenu.removeMod
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.ui.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.utils.*
import ru.nsu.ccfit.zuev.osu.*

class ModMenuPresetsSection : ModMenuSection("Presets") {


    private val addButton: UITextButton

    private var modPresets = listOf<ModPreset>()


    init {
        width = 300f

        addButton = UITextButton().apply {
            width = FillParent
            text = "Add preset"
            leadingIcon = UISprite(ResourceManager.getInstance().getTexture("plus"))
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
            serializedMods = ModMenu.enabledMods.serializeMods().toString()
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
        toggleContainer.callOnChildren { toggle ->
            if (toggle is ModPresetToggle) {
                toggle.isSelected = toggle.preset.mods == ModMenu.enabledMods
            }
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        addButton.isEnabled = ModMenu.enabledMods.isNotEmpty()

        super.onManagedUpdate(deltaTimeSec)
    }


    inner class ModPresetToggle(val preset: ModPreset) : UIButton() {

        init {
            orientation = Orientation.Vertical
            spacing = 8f
            width = FillParent
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
                font = ResourceManager.getInstance().getFont("smallFont")
            }

            +ModsIndicator().apply {
                iconSize = 21f
                mods = preset.mods.serializeMods()
            }
        }

    }
}