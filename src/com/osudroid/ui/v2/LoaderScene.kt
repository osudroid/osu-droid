package com.osudroid.ui.v2

import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.ui.Loader
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager

class LoaderScene() : UIScene() {

    init {
        sprite {
            width = Size.Full
            height = Size.Full
            scaleType = ScaleType.Crop
            textureRegion = ResourceManager.getInstance().getTexture("menu-background")

            if (!Config.isSafeBeatmapBg()) {
                textureRegion = ResourceManager.getInstance().getTexture("::background") ?: textureRegion
            }
        }

        box {
            width = Size.Full
            height = Size.Full
            style = {
                color = it.accentColor * 0.1f
                alpha = 0.9f
            }
        }

        attachChild(Loader().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
        })
    }

}