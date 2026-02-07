package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.osu.*

class SongSelectFilterBar : UIFillContainer() {

    init {
        ResourceManager.getInstance().loadHighQualityAsset("filters", "filters.png")
        ResourceManager.getInstance().loadHighQualityAsset("search-small", "search-small.png")

        width = Size.Full
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        padding = Vec4(32f, 16f)
        spacing = 14f

        /*onSubmit = {
            SongSelect.loadBeatmaps(
                query = it.optString("query"),
                sort = it.optJSONArray("sort")?.optString(0) ?: "title",
            )
        }*/

        iconButton {
            icon = UISprite(ResourceManager.getInstance().getTexture("filters"))
        }

        container {
            height = Size.Full

            +object : UITextInput("") {

                private var lastValueChangeMillis = -1L

                init {
                    key = "query"
                    width = Size.Full
                    height = Size.Full
                }

                override fun onValueChanged() {
                    super.onValueChanged()
                    lastValueChangeMillis = System.currentTimeMillis()
                }

                override fun onManagedUpdate(deltaTimeSec: Float) {
                    super.onManagedUpdate(deltaTimeSec)

                    if (lastValueChangeMillis > 0L && System.currentTimeMillis() - lastValueChangeMillis > 300) {
                        lastValueChangeMillis = -1L
                        //submit()
                    }
                }
            }

            sprite {
                width = 52f
                height = 28f
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
                textureRegion = ResourceManager.getInstance().getTexture("search-small")
                style = { color = it.accentColor }
            }
        }


        /*flexContainer {
            width = Size.Full
            gap = 8f

            iconButton {
                icon = ResourceManager.getInstance().getTexture("heart")
                onActionUp = {
                    isSelected = !isSelected
                }
            }

            +UISelect<String>().apply {
                placeholder = "Collection"
                options = listOf(
                    Option("All", "All"),
                    Option("Standard", "Standard"),
                    Option("Taiko", "Taiko"),
                    Option("Catch", "Catch"),
                )
                flexRules {
                    grow = 1f
                    basis = 0f
                }
            }

            +object : UISelect<String>() {

                init {
                    key = "sort"
                    placeholder = "Sort by"
                    options = listOf(
                         Option(if (Config.isForceRomanized()) "titleUnicode" else "title", "Title"),
                         Option(if (Config.isForceRomanized()) "artistUnicode" else "artist", "Artist"),
                         Option("length", "Length"),
                         Option("creator", "Creator"),
                         Option("dateImported", "Date imported"),
                         Option("mostCommonBPM", "Most common BPM"),
                         Option("droidStarRating", "Droid star rating"),
                         Option("standardStarRating", "Standard star rating"),
                    )
                    flexRules {
                        grow = 1f
                        basis = 0f
                    }
                }

                override fun onValueChanged() {
                    super.onValueChanged()
                    //submit()
                }
            }

        }*/
    }


    companion object {
        init {
            ResourceManager.getInstance().loadHighQualityAsset("heart", "heart.png")
        }
    }
}