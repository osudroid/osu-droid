package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * The bar above the [BeatmapCarrousel] holding the filter button and the search input.
 */
class SongSelectFilterBar : UIFlexContainer() {

    init {
        ResourceManager.getInstance().loadHighQualityAsset("filters", "filters.png")
        ResourceManager.getInstance().loadHighQualityAsset("search-small", "search-small.png")

        width = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        padding = Vec4(32f, 16f)
        direction = FlexDirection.Row
        gap = 14f

        iconButton {
            icon = ResourceManager.getInstance().getTexture("filters")
        }

        container {
            height = FillParent
            flexRules {
                grow = 1f
                basis = 0f
            }

            +object : UITextInput("") {

                private var lastValueChangeMillis = -1L

                init {
                    key = "query"
                    width = FillParent
                    height = FillParent
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

}
