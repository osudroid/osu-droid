package com.osudroid.ui.v2

import com.edlplan.framework.easing.Easing
import com.osudroid.ui.OsuColors
import com.reco1l.andengine.buffered.BufferSharingMode
import com.reco1l.andengine.buffered.CompoundBuffer
import com.reco1l.andengine.buffered.MutableReference
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.Fonts
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.ui.ColorVariant
import com.reco1l.andengine.ui.Theme
import com.reco1l.andengine.ui.UIBadge
import com.reco1l.framework.Color4
import com.reco1l.framework.Interpolation
import ru.nsu.ccfit.zuev.osu.ResourceManager


private val backgroundBufferRef = MutableReference<UIBox.BoxVBO?>(null)
private val textBufferRef = MutableReference<CompoundBuffer?>(null)
private val iconBufferRef = MutableReference<CompoundBuffer?>(null)

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
                ratingColor = OsuColors.getStarRatingColor(field)
                textColor = OsuColors.getStarRatingTextColor(field)
                text = String.format("%.2f", field)
            }
        }

    private var ratingColor = OsuColors.getStarRatingColor(0.0)
    private var textColor = OsuColors.getStarRatingTextColor(0.0)


    init {
        // Badge color is determined by rating and should not be styled.
        style = {
            applySizeStyle()
            fontFamily = Fonts.TorusBold
        }
        text = "0.00"
        backgroundColor = ratingColor
        color = textColor

        bufferSharingMode = BufferSharingMode.Dynamic
        bufferReference = textBufferRef
        background?.apply {
            bufferSharingMode = BufferSharingMode.Dynamic
            bufferReference = backgroundBufferRef
        }

        leadingIcon = FontAwesomeIcon(Icon.Star).apply {
            bufferSharingMode = BufferSharingMode.Dynamic
            bufferReference = iconBufferRef
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        val animationDuration = 0.3f
        val animationTime = deltaTimeSec.coerceIn(0f, 0.3f)

        backgroundColor = Interpolation.colorAt(animationTime, backgroundColor, ratingColor, 0f, animationDuration, Easing.OutQuad)
        color = Interpolation.colorAt(animationTime, color, if (rating >= 6.5) textColor else Color4.Black.copy(alpha = 0.75f), 0f, animationDuration, Easing.OutQuad)

        super.onManagedUpdate(deltaTimeSec)
    }


    fun applyColorsInstantly() {
        backgroundColor = ratingColor
        color = textColor
    }
}