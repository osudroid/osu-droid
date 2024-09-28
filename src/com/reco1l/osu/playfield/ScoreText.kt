package com.reco1l.osu.playfield

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.modifier.ModifierType.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.util.modifier.ease.EaseQuadIn
import org.anddev.andengine.util.modifier.ease.EaseQuadOut
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*
import javax.microedition.khronos.opengles.*


class ScoreText @JvmOverloads constructor(

    private val texturePrefix: StringSkinData,

    private val withPopOut: Boolean = false

) : LinearContainer() {

    /**
     * The text to display.
     */
    var text = ""
        set(value) {
            if (field != value) {
                field = value
                isTextInvalid = true

                if (withPopOut) {
                    popOutText!!.text = value
                    bigPopOutModifier!!.reset()
                }
            }
        }


    private var isTextInvalid = true

    private var popOutText: ScoreText? = null

    private var bigPopOutModifier: UniversalModifier? = null

    private var smallPopOutModifier: UniversalModifier? = null


    private val characters: Map<Char, TextureRegion> = mutableMapOf<Char, TextureRegion>().also {

        for (i in 0..9) {
            it['0' + i] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, i.toString())
        }

        it['.'] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, "comma")
        it['%'] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, "percent")
        it['x'] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, "x")

    }.toMap()


    init {

        if (withPopOut) {
            popOutText = ScoreText(texturePrefix)
            popOutText!!.setAnchor(Anchor.BottomLeft)
            popOutText!!.setScaleCenter(Anchor.BottomLeft)

            // Reference https://github.com/ppy/osu/blob/d159d6b9700d90f6a40cda0f832df59f6086e7ef/osu.Game/Skinning/LegacyComboCounter.cs#L177-L186

            val bigPopOutDuration = 0.3f

            bigPopOutModifier = UniversalModifier(
                type = PARALLEL,
                listener = null,
                UniversalModifier(SCALE, bigPopOutDuration, 1.56f, 1f),
                UniversalModifier(ALPHA, bigPopOutDuration, 0f, 0.6f)
            )

            bigPopOutModifier!!.isRemoveWhenFinished = false

            popOutText!!.registerEntityModifier(bigPopOutModifier)
        }

    }


    private fun allocateSprites(count: Int) {
        if (count > childCount) {
            for (i in childCount until count) {
                attachChild(ExtendedSprite())
            }
        } else {
            for (i in childCount - 1 downTo count) {
                detachChild(getChild(i))
            }
        }
    }


    override fun setScale(scale: Float) {

        if (scaleX == scale && scaleY == scale) {
            return
        }
        super.setScale(scale)

        // We need to recreate the small pop out modifier which is applied to this sprite because the scale has changed.
        // bigPopOutModifier is not affected by this sprite scale.
        if (withPopOut) {

            if (smallPopOutModifier != null) {
                unregisterEntityModifier(smallPopOutModifier)
            }

            val smallPopOutDuration = 0.1f

            smallPopOutModifier = UniversalModifier(
                type = SEQUENCE,
                listener = null,
                UniversalModifier(SCALE, smallPopOutDuration / 2, scale, scale * 1.1f, null, EaseQuadIn.getInstance()),
                UniversalModifier(SCALE, smallPopOutDuration / 2, scale * 1.1f, scale, null, EaseQuadOut.getInstance()),
            )

            smallPopOutModifier!!.isRemoveWhenFinished = false

            registerEntityModifier(smallPopOutModifier)
        }
    }


    override fun onDrawChildren(pGL: GL10, pCamera: Camera) {
        popOutText?.onDraw(pGL, pCamera)
        super.onDrawChildren(pGL, pCamera)
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (isTextInvalid) {
            isTextInvalid = false

            allocateSprites(text.length)

            for (i in text.indices) {

                val char = text[i]
                val sprite = getChild(i) as? ExtendedSprite ?: continue

                sprite.textureRegion = characters[char]
            }

            if (withPopOut) {
                smallPopOutModifier!!.reset()
            }
        }

        popOutText?.onManagedUpdate(pSecondsElapsed)
        super.onManagedUpdate(pSecondsElapsed)
    }

}