package com.osudroid.debug

import com.reco1l.andengine.*
import com.reco1l.andengine.utils.ComponentTestScene

object DebugPlaygroundScene : UIScene() {
    init {
        attachChild(ComponentTestScene)
    }
}