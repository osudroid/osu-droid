package com.osudroid.ui.v2.modmenu

import com.osudroid.data.*
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.modmenu.ModMenu.addMod
import com.osudroid.ui.v2.modmenu.ModMenu.removeMod
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.ui.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.utils.*
import org.json.*
import ru.nsu.ccfit.zuev.osu.*

class ModMenuPresetsSection : ModMenuSection("Presets") {


    private val addButton: TextButton

    private var modPresets = listOf<ModPreset>()


    init {
        width = 300f

        addButton = TextButton().apply {
            width = FillParent
            text = "Add preset"
            leadingIcon = ExtendedSprite(ResourceManager.getInstance().getTexture("plus"))
            onActionUp = {
                ModMenu.attachChild(ModPresetsForm(this@ModMenuPresetsSection))
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

            preset.mods = ModUtils.deserializeMods(JSONArray(preset.serializedMods))
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


    inner class ModPresetToggle(val preset: ModPreset) : Button() {

        init {
            orientation = Orientation.Vertical
            spacing = 8f
            width = FillParent

            onActionUp = {
                ModMenu.modToggles.fastForEach { toggle ->
                    if (toggle.mod in preset.mods) {
                        addMod(toggle.mod)
                    } else {
                        removeMod(toggle.mod)
                    }
                }
            }
            onActionLongPress = {
                ModMenu.attachChild(MessageDialog().apply {
                    title = "Delete preset"
                    text = "Delete preset \"${preset.name}\"?"

                    addButton(TextButton().apply {
                        text = "Delete"
                        isSelected = true
                        onActionUp = {
                            hide()
                            DatabaseManager.modPresetTable.delete(preset)
                            loadPresets()
                        }
                    })

                    addButton(TextButton().apply {
                        text = "Cancel"
                        onActionUp = {
                            hide()
                        }
                    })
                    show()
                })
            }

            text {
                text = preset.name
                font = ResourceManager.getInstance().getFont("smallFont")
            }

            +ModsIndicator(preset.mods, 21f).apply {
                isExpanded = false
            }
        }

    }
}