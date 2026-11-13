package com.osudroid.ui.v2.songselect

import com.osudroid.data.DatabaseManager
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * Sets the order of the [BeatmapCarrousel] and which beatmap sets it lists. Changes apply right away.
 */
class SongSelectFilterPopup(trigger: UIComponent) : UIPopover(trigger, card = UILinearContainer().apply {
    orientation = Orientation.Vertical
    width = POPUP_WIDTH
    padding = Vec4(0f, 0f, 0f, 2f.srem)
}) {

    private val sortSelect = FormSelect<BeatmapSortOrder>().apply {
        width = COLUMN_WIDTH
        label = "Sort by"
        showResetButton = false
        // Both star rating orders are listed as one, see starsSortOrderOf.
        options = (BeatmapSortOrder.entries - BeatmapSortOrder.StandardStars).map { UISelect.Option(it, it.label) }
        value = listOf(SongSelect.sortOrder.takeUnless { it == BeatmapSortOrder.StandardStars } ?: BeatmapSortOrder.DroidStars)
        onValueChanged = {
            SongSelect.sortOrder = starsSortOrderOf(it.firstOrNull() ?: BeatmapSortOrder.Title)
            reloadBeatmaps()
        }
    }

    private val collectionSelect = FormSelect<String>().apply {
        width = COLUMN_WIDTH
        label = "Collection"
        showResetButton = false
        options = listOf(UISelect.Option(ALL_BEATMAPS, "All beatmaps")) +
            DatabaseManager.beatmapCollectionsTable.getCollections().map { UISelect.Option(it, it) }
        value = listOf(SongSelect.collection ?: ALL_BEATMAPS)
        onValueChanged = {
            SongSelect.collection = it.firstOrNull()?.takeUnless { name -> name == ALL_BEATMAPS }
            reloadBeatmaps()
        }
    }

    private val favoritesCheckbox = FormCheckbox(SongSelect.isFavoritesOnly).apply {
        label = "Favorites only"
        showResetButton = false
        onValueChanged = {
            SongSelect.isFavoritesOnly = it
            reloadBeatmaps()
        }
    }

    /**
     * Whether several controls are being changed at once, the list is then reloaded once afterwards.
     */
    private var isResetting = false


    init {
        card.apply {
            container {
                width = FillParent

                text {
                    width = FillParent
                    text = "Sort and filter"
                    font = ResourceManager.getInstance().getFont("smallFont")
                    alignment = Anchor.Center
                    padding = Vec4(0f, 3f.srem)
                    applyTheme = { color = it.accentColor * 0.7f }
                }

                container {
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    padding = Vec4(0f, 0f, 3f.srem, 0f)

                    textButton {
                        text = "Reset"
                        font = ResourceManager.getInstance().getFont("xs")
                        padding = Vec4(3f.srem, 2f.srem)
                        onActionUp = { resetFilters() }
                    }
                }
            }

            +divider()

            // Side by side to keep the popup short.
            linearContainer {
                +sortSelect
                +collectionSelect
            }

            +favoritesCheckbox
            +divider()

            // Not a filter, changes which star rating is shown and sorted by.
            container {
                width = FillParent
                padding = Vec4(24f, 2f.srem, 24f, 0f)

                text {
                    text = "Star rating"
                    font = ResourceManager.getInstance().getFont("smallFont")
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    applyTheme = { color = it.accentColor }
                }

                +UITabSelector().apply {
                    width = STAR_RATING_SELECTOR_WIDTH
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight

                    addButton("osu!droid") { setDifficultyAlgorithm(DifficultyAlgorithm.droid) }
                    addButton("osu!standard") { setDifficultyAlgorithm(DifficultyAlgorithm.standard) }

                    selectedTab = if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) 1 else 0
                }
            }
        }
    }


    private fun divider() = UIBox().apply {
        width = FillParent
        height = 1f
        applyTheme = {
            color = it.accentColor
            alpha = 0.1f
        }
    }

    private fun reloadBeatmaps() {
        if (!isResetting) {
            SongSelect.loadBeatmaps()
        }
    }

    /**
     * Back to the default order with nothing filtered out, the controls apply their new values themselves.
     */
    private fun resetFilters() {
        isResetting = true
        sortSelect.value = listOf(BeatmapSortOrder.Title)
        collectionSelect.value = listOf(ALL_BEATMAPS)
        favoritesCheckbox.value = false
        isResetting = false

        reloadBeatmaps()
    }

    private fun setDifficultyAlgorithm(algorithm: DifficultyAlgorithm) {
        if (Config.getDifficultyAlgorithm() == algorithm) {
            return
        }

        Config.setString("difficultyAlgorithm", if (algorithm == DifficultyAlgorithm.standard) "1" else "0")
        SongSelect.sortOrder = starsSortOrderOf(SongSelect.sortOrder)
        SongSelect.onDifficultyAlgorithmChanged()
    }

    /**
     * Replaces a star rating order with the one of the current difficulty algorithm.
     */
    private fun starsSortOrderOf(order: BeatmapSortOrder) = when (order) {
        BeatmapSortOrder.DroidStars, BeatmapSortOrder.StandardStars ->
            if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) BeatmapSortOrder.StandardStars else BeatmapSortOrder.DroidStars

        else -> order
    }


    companion object {
        /**
         * Stands for no collection, collection names cannot be empty.
         */
        private const val ALL_BEATMAPS = ""

        private val COLUMN_WIDTH
            get() = 16.5f.rem

        private val POPUP_WIDTH
            get() = COLUMN_WIDTH * 2f

        private val STAR_RATING_SELECTOR_WIDTH
            get() = 21f.rem
    }

}
