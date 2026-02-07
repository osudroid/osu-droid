package com.osudroid.ui.v2.mainmenu

import com.reco1l.andengine.box
import com.reco1l.andengine.sprite.UIShapedSprite
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.ui.SizeVariant
import com.reco1l.andengine.ui.UITextButton
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring

class PlayerButton : UITextButton() {

    private val avatar = UIShapedSprite().apply {
        shape = box {
            width = Size.Full
            height = Size.Full
            style = {
                radius = Radius.MD
            }
        }
        inheritAncestorsColor = false
        textureRegion = ResourceManager.getInstance().getTexture("emptyavatar")
    }

    private val onlineManager = OnlineManager.getInstance()
    private val onlineScoring = OnlineScoring.getInstance()

    private var avatarUri = ""
    private var avatarLoaded = false


    init {
        leadingIcon = avatar
        sizeVariant = SizeVariant.Large
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        text = onlineManager.username

        if (avatarUri != onlineManager.avatarURL || avatarLoaded != onlineScoring.isAvatarLoaded) {
            avatarUri = onlineManager.avatarURL
            avatarLoaded = onlineScoring.isAvatarLoaded

            avatar.textureRegion = if (!avatarLoaded || avatarUri.isEmpty()) {
                ResourceManager.getInstance().getTexture("emptyavatar")
            } else {
                ResourceManager.getInstance().getAvatarTextureIfLoaded(avatarUri)
            }
        }
        super.onManagedUpdate(deltaTimeSec)
    }
}