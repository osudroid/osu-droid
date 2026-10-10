package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * The bar above the [BeatmapCarrousel] holding the leaderboard switch button, the filter button and the search input.
 *
 * The search box is collapsed to the filter button's size when unused, and spans the rest of the bar when in use.
 */
class SongSelectFilterBar(leaderboard: SongSelectLeaderboard) : UILinearContainer() {

    private val filterButton: UIIconButton
    private val switchButton = LeaderboardSwitchButton(leaderboard)
    private val searchBox: UIContainer
    private val searchInput: UITextInput

    /**
     * The animated search box width, negative until the first layout.
     */
    private var searchBoxWidth = -1f


    init {
        ResourceManager.getInstance().loadHighQualityAsset("filters", "filters.png")
        ResourceManager.getInstance().loadHighQualityAsset("search-small", "search-small.png")

        width = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        padding = Vec4(32f, 16f)
        // A linear container, the flex container would lock the search box to its content width.
        orientation = Orientation.Horizontal
        spacing = 14f

        +switchButton

        filterButton = iconButton {
            icon = ResourceManager.getInstance().getTexture("filters")
        }

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

        searchBox = container {
            // No clipping, it would cut off the input's outline.

            +searchInput

            sprite {
                width = 52f
                height = 28f
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
                textureRegion = ResourceManager.getInstance().getTexture("search-small")
                applyTheme = { color = it.accentColor }
            }
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        switchButton.setButtonSize(filterButton.width, filterButton.height)
        // The switch button grows below the bar when its mode tab is shown, the search box keeps the button height.
        searchBox.height = filterButton.height

        val collapsedWidth = filterButton.width
        val fullWidth = innerWidth - filterButton.width - switchButton.width - spacing * 2f
        val isInUse = searchInput.isFocused || searchInput.value.isNotEmpty()
        val targetWidth = if (isInUse) maxOf(collapsedWidth, fullWidth) else collapsedWidth

        if (collapsedWidth > 0f) {
            searchBoxWidth = if (searchBoxWidth < 0f) targetWidth else Interpolation.floatLerpWithSnap(deltaTimeSec, 0.15f, searchBoxWidth, targetWidth, 0.5f)
            searchBox.width = searchBoxWidth
        }

        super.onManagedUpdate(deltaTimeSec)
    }


}
