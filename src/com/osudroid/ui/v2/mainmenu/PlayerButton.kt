package com.osudroid.ui.v2.mainmenu

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.sprite
import com.reco1l.andengine.sprite.ScaleType
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.UIClickableContainer
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring

class PlayerButton : UIClickableContainer() {

    private val onlineManager = OnlineManager.getInstance()
    private val onlineScoring = OnlineScoring.getInstance()

    private var avatarUri = ""
    private var avatarLoaded = false

    private lateinit var nameText: UIText
    private lateinit var informationText: UIText
    private lateinit var avatarSprite: UISprite


    init {
        linearContainer {
            orientation = Orientation.Horizontal
            style = {
                radius = Radius.XL
                padding = Vec4(2.25f.srem)
                spacing = 2f.srem
                backgroundColor = (it.accentColor * 0.1f).copy(alpha = 0.2f)
                minWidth = 13f.rem
            }

            avatarSprite = sprite {
                style = {
                    width = 3f.rem
                    height = 3f.rem
                    backgroundColor = (it.accentColor * 0.1f).copy(alpha = 0.3f)
                    radius = Radius.XL
                }
                scaleType = ScaleType.Crop
                textureRegion = ResourceManager.getInstance().getTexture("emptyavatar")
            }

            linearContainer {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                orientation = Orientation.Vertical

                nameText = text {
                    text = "Player"
                    style = {
                        color = it.accentColor
                    }
                }

                informationText = text {
                    text = "Loading..."
                    style = {
                        fontSize = FontSize.XS
                        color = it.accentColor * 0.8f
                    }
                }
            }
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        nameText.text = onlineManager.username
        informationText.text = OnlineManager.getInstance().failMessage

        if (avatarUri != onlineManager.avatarURL || avatarLoaded != onlineScoring.isAvatarLoaded) {
            avatarUri = onlineManager.avatarURL
            avatarLoaded = onlineScoring.isAvatarLoaded

            avatarSprite.textureRegion = if (!avatarLoaded || avatarUri.isEmpty()) {
                ResourceManager.getInstance().getTexture("emptyavatar")
            } else {
                ResourceManager.getInstance().getAvatarTextureIfLoaded(avatarUri)
            }
        }
        super.onManagedUpdate(deltaTimeSec)
    }
}