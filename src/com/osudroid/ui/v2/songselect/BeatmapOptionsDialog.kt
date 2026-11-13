package com.osudroid.ui.v2.songselect

import com.edlplan.ui.fragment.BaseFragment
import com.edlplan.ui.fragment.CollectionsManagerFragment
import com.osudroid.data.BeatmapOptions
import com.osudroid.data.DatabaseManager
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4

/**
 * The options of a beatmap set, shown when one of its panels in the [BeatmapCarrousel] is held.
 */
class BeatmapOptionsDialog(private val model: BeatmapSetModel) : UIDialog<UILinearContainer>(innerContent = UILinearContainer().apply {
    orientation = Orientation.Vertical
    width = FillParent
}) {

    private val beatmapSetInfo = model.beatmapSetInfo

    private val options = DatabaseManager.beatmapOptionsTable.getOptions(beatmapSetInfo.directory)
        ?: BeatmapOptions(beatmapSetInfo.directory)

    private val wasFavorite = options.isFavorite

    private val favoriteIcon = optionIcon(Icon.Heart)

    private val favoriteButton = optionButton(favoriteIcon) {
        onActionUp = {
            options.isFavorite = !options.isFavorite
            model.isFavorite = options.isFavorite
            saveOptions()
            updateFavoriteButton()
        }
    }


    init {
        title = beatmapSetInfo.beatmaps.getOrNull(0)?.titleText ?: "Properties"

        innerContent.apply {
            +OffsetInput(options.offset).apply {
                onValueChanged = {
                    options.offset = it.toIntOrNull() ?: 0
                    saveOptions()
                }
            }

            +optionRow(favoriteButton)

            +optionRow(optionButton(optionIcon(Icon.Folder)) {
                text = "Add to collection"
                onActionUp = {
                    mainThread {
                        CollectionsManagerFragment().apply {
                            // The set may no longer belong to the list.
                            onDismissListener = BaseFragment.OnDismissListener {
                                if (SongSelect.collection != null) {
                                    SongSelect.loadBeatmaps()
                                }
                            }
                        }.showToAddToFolder(beatmapSetInfo.directory)
                    }
                }
            })

            +optionRow(optionButton(optionIcon(Icon.Trash)) {
                text = "Delete beatmap"
                applyTheme = { theme ->
                    background?.color = theme.accentColor * 0.175f
                    color = DELETE_COLOR
                }
                onActionUp = {
                    hide()
                    showDeleteConfirmation()
                }
            })
        }

        updateFavoriteButton()

        addButton {
            text = "Close"
            isSelected = true
            onActionUp = { hide() }
        }
    }


    private fun optionIcon(icon: Int) = FontAwesomeIcon(icon).apply { iconSize = ICON_SIZE }

    /**
     * A full width button with an icon in front of its text.
     */
    private fun optionButton(icon: FontAwesomeIcon, block: UITextButton.() -> Unit) = UITextButton().apply {
        width = FillParent
        alignment = Anchor.CenterLeft
        leadingIcon = icon
        block()
    }

    /**
     * Wraps a button in the horizontal margin of the dialog's other content.
     */
    private fun optionRow(button: UITextButton) = UIContainer().apply {
        width = FillParent
        padding = Vec4(24f, 6f)
        +button
    }

    /**
     * A filled heart for favorites, an outlined one otherwise.
     */
    private fun updateFavoriteButton() {
        favoriteIcon.iconVariant = if (options.isFavorite) IconVariant.Solid else IconVariant.Regular
        favoriteIcon.color = if (options.isFavorite) ACCENT_RED else Theme.current.accentColor
        favoriteButton.text = if (options.isFavorite) "Remove from favorites" else "Add to favorites"
    }

    private fun saveOptions() {
        // A copy, the options may change again before they are written.
        val saved = options.copy()
        async { DatabaseManager.beatmapOptionsTable.upsert(saved) }
    }

    private fun showDeleteConfirmation() {
        UIConfirmDialog().apply {
            title = "Delete beatmap"
            text = "Are you sure?"
            onConfirm = { SongSelect.deleteBeatmapSet(beatmapSetInfo) }
        }.show()
    }

    override fun onHidden() {
        super.onHidden()

        // The set may no longer belong to the list.
        if (SongSelect.isFavoritesOnly && options.isFavorite != wasFavorite) {
            SongSelect.loadBeatmaps()
        }
    }


    /**
     * The offset input with a button on each side to change it by one millisecond.
     */
    private class OffsetInput(offset: Int) : FormControl<String, UITextInput>(offset.toString()) {

        override val control = IntegerTextInput(offset, -MAX_OFFSET, MAX_OFFSET)

        init {
            orientation = Orientation.Vertical
            spacing = 12f
            showResetButton = false
            label = "Offset (ms)"

            // The label is anchored for a row.
            linearContainer {
                width = FillParent
                padding = Vec4(0f, 12f)
                +labelText
            }

            flexContainer {
                width = FillParent
                gap = 12f

                +stepButton(Icon.Minus, -1)
                +control.apply {
                    flexRules {
                        grow = 1f
                        basis = 0f
                    }
                }
                +stepButton(Icon.Plus, 1)
            }
        }

        private fun stepButton(icon: Int, step: Int) = UIButton().apply {
            +FontAwesomeIcon(icon).apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                iconSize = ICON_SIZE
            }
            onActionUp = {
                val offset = (control.value.toIntOrNull() ?: 0) + step
                control.value = offset.coerceIn(-MAX_OFFSET, MAX_OFFSET).toString()
            }
        }
    }


    companion object {
        private const val MAX_OFFSET = 250
        private const val ICON_SIZE = 20f

        private val DELETE_COLOR = Color4(255, 191, 191)
    }

}
