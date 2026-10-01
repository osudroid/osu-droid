package com.osudroid.ui.v2.songselect

import com.osudroid.ui.v2.OsuSkinnableSprite
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.scaleCenter
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UILinearContainer
import kotlin.math.min

/**
 * A row of skinnable `star` sprites visualizing a star rating, like the legacy song menu did:
 * one full star per whole point and a scaled-down star for the fractional part.
 */
class DifficultyStars : UILinearContainer() {

    /**
     * The star rating to display. Values above [MAX_STARS] are capped.
     */
    var rating = 0f
        set(value) {
            if (field != value) {
                field = value
                updateStars()
            }
        }

    /**
     * The size of a single star in scene units.
     */
    var starSize = 16f
        set(value) {
            if (field != value) {
                field = value
                stars.forEach { it.setSize(value, value) }
                updateStars()
            }
        }


    private val stars = Array(MAX_STARS) {
        OsuSkinnableSprite("star").apply {
            width = starSize
            height = starSize
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            scaleCenter = Anchor.CenterLeft
            isVisible = false
        }
    }


    init {
        orientation = Orientation.Horizontal
        spacing = 2f

        for (star in stars) {
            attachChild(star)
        }
    }


    private fun updateStars() {
        val capped = min(rating, MAX_STARS.toFloat())
        val fullStars = capped.toInt()
        val fraction = capped - fullStars

        for (i in stars.indices) {
            val star = stars[i]

            when {
                i < fullStars -> {
                    star.isVisible = true
                    star.setScale(1f)
                }

                i == fullStars && fraction > 0f -> {
                    star.isVisible = true
                    star.setScale(fraction)
                }

                else -> {
                    star.isVisible = false
                    star.setScale(1f)
                }
            }
        }
    }


    companion object {
        const val MAX_STARS = 10
    }

}
