package com.osudroid.ui.v2

import com.osudroid.ui.OsuColors
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.ui.Theme
import com.reco1l.andengine.ui.UIBadge
import com.reco1l.framework.Color4
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * A [UIBadge] for displaying star ratings. Automatically adjusts its styling according to the rating.
 */
class StarRatingBadge : UIBadge() {
    /**
     * The star rating value displayed by this [StarRatingBadge].
     */
    var rating = 0.0
        set(value) {
            if (field != value) {
                field = value
                ratingChanged = true
            }
        }

    // Badge color is determined by rating and should not be affected by themes.
    override var applyTheme: UIComponent.(Theme) -> Unit = {}

    private var ratingChanged = true

    init {
        text = "0.00"
        leadingIcon = UISprite(ResourceManager.getInstance().getTexture("star-xs"))
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        if (ratingChanged) {
            ratingChanged = false

            clearEntityModifiers()
            background?.clearEntityModifiers()

            text = "%.2f".format(rating)
            background?.colorTo(OsuColors.getStarRatingColor(rating), 0.1f)

            if (rating >= 6.5) {
                colorTo(Color4(0xFFFFD966), 0.1f)
                fadeTo(1f, 0.1f)
            } else {
                colorTo(Color4.Black, 0.1f)
                fadeTo(0.75f, 0.1f)
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    companion object {
        init {
            ResourceManager.getInstance().loadHighQualityAsset("star-xs", "star.png")
        }
    }
}