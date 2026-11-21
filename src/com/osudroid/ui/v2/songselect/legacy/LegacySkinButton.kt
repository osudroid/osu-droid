package com.osudroid.ui.v2.songselect.legacy

import com.reco1l.andengine.*
import com.reco1l.andengine.component.scaleCenter
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.ui.*
import org.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.OsuSkin

/**
 * A button of the legacy song menu, drawn with an image or an animation of the skin and with its `-over` image
 * while pressed.
 */
internal class LegacySkinButton(

    private val textureName: String,

    /**
     * The name of the layout that a skin can set for the button, see [OsuSkin.getLayout].
     */
    private val layoutName: String,

    /**
     * The size of the button relative to its image, unless the layout of the skin sets one.
     */
    private val imageScale: Float = 1f,

    /**
     * How much the button grows from its bottom left corner while it is pressed.
     */
    private val pressedScale: Float = 1f,

) : UIClickableContainer() {

    private val sprite = UIAnimatedSprite(arrayOf())

    private var pressedTexture: TextureRegion? = null

    private var growsWhilePressed = true


    init {
        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        scaleCenter = Anchor.BottomLeft
        isVisible = false

        +sprite
    }


    /**
     * Shows the button with the images of the current skin if it is [wanted].
     *
     * @return `false` if it is not shown, also because the skin has no image for it. Skins hide buttons with an
     * empty image of a single pixel, which could not be tapped.
     */
    fun show(wanted: Boolean): Boolean {
        val resources = ResourceManager.getInstance()
        val frames = ArrayList<TextureRegion?>()

        if (wanted) {
            // An animation has numbered frames. Without them the skin has a single image, or none and the one of
            // the default skin is used.
            for (i in 0 until resources.getFrameCount(textureName)) {
                resources.getTextureIfLoaded("$textureName-$i")?.let { frames += it }
            }

            if (frames.isEmpty()) {
                resources.getTexture(textureName)?.let { frames += it }
            }
        }

        val first = frames.firstOrNull()?.takeIf { it.width >= MIN_SIZE && it.height >= MIN_SIZE }

        if (first == null) {
            frames.clear()
        }

        pressedTexture = resources.getTextureIfLoaded("$textureName-over")

        // Like the legacy song menu, as many frames per second as there are frames if the skin sets no rate.
        val framerate = OsuSkin.get().animationFramerate.takeIf { it > 0f } ?: frames.size.toFloat()

        sprite.frames = frames.toTypedArray()
        sprite.frameTime = if (framerate > 0f) 1f / framerate else 1f
        sprite.play()

        // Skins made for osu!, which are converted on load, move the buttons down this way for example.
        val skin = OsuSkin.get()
        val layout = skin.getLayout(layoutName)
        val appliedLayout = layout?.takeIf { skin.isUseNewLayout }
        val scale = appliedLayout?.scale?.takeIf { it != -1f } ?: imageScale

        sprite.width = (appliedLayout?.width?.takeIf { it != -1f } ?: first?.width?.toFloat() ?: 0f) * scale
        sprite.height = (appliedLayout?.height?.takeIf { it != -1f } ?: first?.height?.toFloat() ?: 0f) * scale

        // The layout counts upwards from the bottom of the screen.
        y = -(appliedLayout?.yPosition?.takeUnless { it.isNaN() } ?: 0f)

        growsWhilePressed = layout?.property?.optBoolean("scaleWhenHold", true) ?: true

        isVisible = first != null
        return isVisible
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        sprite.textureRegion = if (isPressed && pressedTexture != null) pressedTexture else sprite.frames.getOrNull(sprite.frameIndex)

        val scale = if (isPressed && growsWhilePressed) pressedScale else 1f
        scaleX = scale
        scaleY = scale

        super.onManagedUpdate(deltaTimeSec)
    }


    companion object {
        private const val MIN_SIZE = 8
    }

}
