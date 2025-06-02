package com.osudroid.ui.v2.hud.elements

import com.edlplan.framework.easing.*
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.info.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.texture.*
import com.reco1l.framework.*
import com.osudroid.ui.v2.hud.HUDElement
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*
import ru.nsu.ccfit.zuev.skins.*

class HUDHealthBar : HUDElement() {

    private val fill: UIAnimatedSprite
    private val fillClear: UIBox

    private val marker: UISprite
    private val explode: UISprite

    private val markerNormalTexture: TextureRegion?
    private val markerDangerTexture: TextureRegion?
    private val markerSuperDangerTexture: TextureRegion?

    private val isNewStyle: Boolean


    private var lastHP = 0f


    init {

        val backgroundTexture = ResourceManager.getInstance().getTexture("scorebar-bg")
        val markerTexture = ResourceManager.getInstance().getTextureIfLoaded("scorebar-marker")

        // the marker lookup to decide which display style must be performed on the source of the bg, which is the most common element.
        isNewStyle = backgroundTexture !is BlankTextureRegion && markerTexture != null

        // background implementation is the same for both versions.
        attachChild(UISprite().apply { textureRegion = backgroundTexture })

        fillClear = UIBox()
        fillClear.origin = Anchor.TopRight
        fillClear.clearInfo = ClearInfo.ClearDepthBuffer
        fillClear.depthInfo = DepthInfo.Less
        fillClear.alpha = 0f
        attachChild(fillClear)

        fill = UIAnimatedSprite("scorebar-colour", true, OsuSkin.get().animationFramerate)
        fill.depthInfo = DepthInfo.Default
        fill.width = fill.width // Preserve the first frame width.
        attachChild(fill)

        marker = UISprite()
        marker.origin = Anchor.Center
        marker.blendInfo = BlendInfo.Additive
        attachChild(marker)

        explode = UISprite()
        explode.origin = Anchor.Center
        explode.blendInfo = BlendInfo.Additive
        explode.alpha = 0f
        attachChild(explode)

        if (isNewStyle) {
            fill.setPosition(7.5f * 1.6f, 7.8f * 1.6f)

            marker.textureRegion = markerTexture
            explode.textureRegion = markerTexture

            markerNormalTexture = null
            markerDangerTexture = null
            markerSuperDangerTexture = null
        } else {
            fill.setPosition(3f * 1.6f, 10f * 1.6f)

            markerNormalTexture = ResourceManager.getInstance().getTextureIfLoaded("scorebar-ki")
            markerDangerTexture = ResourceManager.getInstance().getTextureIfLoaded("scorebar-kidanger")
            markerSuperDangerTexture = ResourceManager.getInstance().getTextureIfLoaded("scorebar-kidanger2")

            marker.textureRegion = markerNormalTexture
            explode.textureRegion = markerNormalTexture
        }

        fillClear.width = 0f
        fillClear.height = fill.height
        fillClear.setPosition(fill.x + fill.width, fill.y)

        onContentChanged()
    }


    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        val hp = game.stat.hp

        fillClear.width = Interpolation.floatAt(secondsElapsed.coerceIn(0f, 0.2f), fillClear.width, (1f - hp) * fill.width, 0f, 0.2f, Easing.OutQuint)

        marker.x = fill.x + fill.width - fillClear.width
        marker.y = fill.y + (if (isNewStyle) fill.height / 2 else 0f)

        explode.setPosition(marker)

        if (hp > lastHP) {
            bulge()
        }

        lastHP = hp

        if (isNewStyle) {

            val color = getFillColor(hp)

            fill.color = color
            marker.color = color
            marker.blendInfo = if (hp < EPIC_CUTOFF) BlendInfo.Inherit else BlendInfo.Additive

        } else {

            marker.textureRegion = when {
                hp < 0.2f -> markerSuperDangerTexture
                hp < EPIC_CUTOFF -> markerDangerTexture
                else -> markerNormalTexture
            }

        }
    }

    override fun onNoteHit(statistics: StatisticV2) {
        flash(statistics.hp)
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        isChildrenVisible = !isBreak
    }


    private fun flash(hp: Float) {

        val isEpic = hp >= EPIC_CUTOFF

        bulge()

        explode.clearEntityModifiers()
        explode.blendInfo = if (isEpic) BlendInfo.Additive else BlendInfo.Inherit
        explode.alpha = 1f
        explode.setScale(1f)

        explode.scaleTo(if (isEpic) 2f else 1.6f, 0.12f)
        explode.fadeOut(0.12f)
    }

    private fun bulge() {
        marker.clearEntityModifiers()
        marker.setScale(1.4f)
        marker.scaleTo(1f, 0.2f, Easing.Out)
    }

    private fun getFillColor(hp: Float) = when {

        hp < 0.2f -> Colors.interpolateNonLinear(0.2f - hp, ColorARGB.Black, ColorARGB.Red, 0f, 0.2f)
        hp < EPIC_CUTOFF -> Colors.interpolateNonLinear(0.5f - hp, ColorARGB.White, ColorARGB.Black, 0f, 0.5f)

        else -> ColorARGB.White
    }

    companion object {

        const val EPIC_CUTOFF = 0.5f

    }

}