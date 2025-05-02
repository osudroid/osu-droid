package com.osudroid.ui.v2

import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.ui.*
import ru.nsu.ccfit.zuev.osu.*

class OsuSkinnableSprite(val textureLookup: String) : ExtendedSprite(), ISkinnable {

    init {
        onSkinChanged()
    }

    override fun onSkinChanged() {
        textureRegion = ResourceManager.getInstance().getTexture(textureLookup)
    }

}