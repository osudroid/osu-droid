package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * The bar above the [BeatmapCarrousel] holding the leaderboard switch button, the filter button and the search
 * input. Below them a line names the leaderboard and tells how many beatmap sets are listed.
 *
 * The search box is collapsed to the filter button's size when unused, and spans the rest of the bar when in use.
 */
class SongSelectFilterBar(leaderboard: SongSelectLeaderboard) : UILinearContainer() {

    private lateinit var filterButton: UIIconButton

    /**
     * Tells how many beatmap sets are listed, and by which filters if not all of them are.
     */
    private val statusText = UIText().apply {
        font = ResourceManager.getInstance().getFont("xs")
        height = FilterBarTab.HEIGHT
        alignment = Anchor.CenterLeft
        applyTheme = { color = it.accentColor }
    }

    private val switchButton = LeaderboardSwitchButton(leaderboard)
    private val searchBox: UIContainer
    private val searchInput: UITextInput
    private lateinit var searchIconButton: UIClickableContainer
    private lateinit var searchIcon: UISprite
    private lateinit var clearIcon: FontAwesomeIcon

    /**
     * The animated search box width, negative until the first layout.
     */
    private var searchBoxWidth = -1f

    private var statusCollection: String? = null
    private var statusFavoritesOnly = false
    private var statusListed = -1
    private var statusTotal = -1


    init {
        ResourceManager.getInstance().loadHighQualityAsset("filters", "filters.png")
        ResourceManager.getInstance().loadHighQualityAsset("search-small", "search-small.png")

        width = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        padding = Vec4(4f.srem, 4f.srem, 8f.srem, 4f.srem)
        orientation = Orientation.Vertical
        spacing = FilterBarTab.SPACING

        searchInput = object : UITextInput("") {

            private var lastValueChangeMillis = -1L

            init {
                key = "query"
                width = FillParent
                height = FillParent
                // Room for the search icon below.
                padding = Vec4(12f, 0f, 64f, 0f)
                hideOutlineWhenIdle = true
            }

            override fun onValueChanged() {
                super.onValueChanged()
                lastValueChangeMillis = System.currentTimeMillis()
            }

            override fun onManagedUpdate(deltaTimeSec: Float) {
                super.onManagedUpdate(deltaTimeSec)

                if (lastValueChangeMillis > 0L && System.currentTimeMillis() - lastValueChangeMillis > 300) {
                    lastValueChangeMillis = -1L
                    SongSelect.loadBeatmaps(query = value)
                }
            }
        }

        searchBox = UIContainer().apply {
            // No clipping, it would cut off the input's outline.

            +searchInput

            // The search icon opens the search, and turns into a clear button while there is text.
            // As wide as the collapsed box, so the icon is centered in it.
            searchIconButton = clickableContainer {
                height = FillParent
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
                onActionUp = {
                    if (searchInput.value.isEmpty()) {
                        searchInput.focus()
                    } else {
                        searchInput.value = ""
                    }
                }

                searchIcon = sprite {
                    width = 52f
                    height = 28f
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    textureRegion = ResourceManager.getInstance().getTexture("search-small")
                    applyTheme = { color = it.accentColor }
                }

                clearIcon = FontAwesomeIcon(Icon.Xmark).apply {
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    iconSize = 20f
                    applyTheme = { color = it.accentColor }
                }
                +clearIcon
            }
        }

        // A linear container, the flex container would lock the search box to its content width.
        linearContainer {
            orientation = Orientation.Horizontal
            width = FillParent
            spacing = BUTTON_SPACING

            +switchButton

            // Square, the other buttons of the bar take its size.
            filterButton = iconButton {
                icon = ResourceManager.getInstance().getTexture("filters")
                padding = Vec4(16f)
                onActionUp = { SongSelectFilterPopup(this@linearContainer).show() }
            }

            +searchBox
        }

        linearContainer {
            orientation = Orientation.Horizontal
            spacing = 2f.srem

            +switchButton.modeTab
            +statusText
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        val hasText = searchInput.value.isNotEmpty()
        searchIcon.isVisible = !hasText
        clearIcon.isVisible = hasText
        filterButton.isSelected = SongSelect.isFilterActive

        // Not before the library is loaded.
        statusText.isVisible = SongSelect.setCount > 0
        statusText.text = statusOf(SongSelect.collection, SongSelect.isFavoritesOnly, SongSelect.listedSetCount, SongSelect.setCount)

        switchButton.setButtonSize(filterButton.width, filterButton.height)
        searchBox.height = filterButton.height
        searchIconButton.width = filterButton.width

        val collapsedWidth = filterButton.width
        val buttonsWidth = filterButton.width + switchButton.width + BUTTON_SPACING * 2f
        val fullWidth = innerWidth - buttonsWidth
        val isInUse = searchInput.isFocused || searchInput.value.isNotEmpty()
        val targetWidth = if (isInUse) maxOf(collapsedWidth, fullWidth) else collapsedWidth

        if (collapsedWidth > 0f) {
            searchBoxWidth = if (searchBoxWidth < 0f) targetWidth else Interpolation.floatLerpWithSnap(deltaTimeSec, 0.15f, searchBoxWidth, targetWidth, 0.5f)
            searchBox.width = searchBoxWidth
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    private fun statusOf(collection: String?, isFavoritesOnly: Boolean, listed: Int, total: Int): String {
        // Called every frame, the text is only built again when something changed.
        if (collection == statusCollection && isFavoritesOnly == statusFavoritesOnly && listed == statusListed && total == statusTotal) {
            return statusText.text
        }

        statusCollection = collection
        statusFavoritesOnly = isFavoritesOnly
        statusListed = listed
        statusTotal = total

        return buildString {
            if (listed != total) {
                append(listed).append(" of ")
            }
            append(total).append(if (total == 1) " map" else " maps")

            if (collection != null) {
                append(SEPARATOR).append(collection)
            }
            if (isFavoritesOnly) {
                append(SEPARATOR).append("Favorites")
            }
        }
    }


    companion object {
        private const val SEPARATOR = " · "
        private const val BUTTON_SPACING = 14f
    }

}
