package com.reco1l.osu.hitobjects

import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.sprite.*
import com.reco1l.framework.*
import com.reco1l.osu.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.beatmap.hitobject.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.math.*

object FollowPointConnection {


    private const val SPACING = 32

    private const val PREEMPT = 800


    @JvmStatic
    val pool = Pool {

        // For optimization, we avoid using AnimatedSprite if there's one frame.
        if (ResourceManager.getInstance().isTextureLoaded("followpoint-0")) {
            AnimatedSprite("followpoint", true, OsuSkin.get().animationFramerate).also { sprite ->
                sprite.frames.fastForEach {
                    it?.applyFollowPointMaxSize()
                }

                sprite.onContentSizeMeasured()
            }
        } else {
            ExtendedSprite(ResourceManager.getInstance().getTexture("followpoint")).also {
                it.textureRegion?.applyFollowPointMaxSize()
                it.onContentSizeMeasured()
            }
        }
    }


    private val expire = OnModifierFinished { fp ->
        updateThread {
            fp.detachSelf()
            fp.reset()
            pool.free(fp as ExtendedSprite)
        }
    }


    private fun TextureRegion.applyFollowPointMaxSize() {
        // Reference: https://github.com/ppy/osu/blob/0811de728e4205a45e485d53ccdaf19a937c6033/osu.Game.Rulesets.Osu/Skinning/Legacy/OsuLegacySkinTransformer.cs#L95-L97
        val newWidth = min(width, HitObject.OBJECT_RADIUS.toInt() * 2)
        val newHeight = min(height, HitObject.OBJECT_RADIUS.toInt())

        if (width != newWidth || height != newHeight) {

            // Crop the texture from the center.
            setTexturePosition(width / 2 - newWidth / 2, height / 2 - newHeight / 2)

            width = newWidth
            height = newHeight
        }
    }

    @JvmStatic
    fun addConnection(scene: Scene, secPassed: Float, start: HitObject, end: HitObject) {

        // Reference: https://github.com/ppy/osu/blob/7bc8908ca9c026fed1d831eb6e58df7624a8d614/osu.Game.Rulesets.Osu/Objects/Drawables/Connections/FollowPointConnection.cs

        val scale = start.gameplayScale
        val startTime = (start.endTime / 1000f).toFloat()

        val startPosition = start.gameplayStackedEndPosition
        val endPosition = end.gameplayStackedPosition

        val distanceX = endPosition.x - startPosition.x
        val distanceY = endPosition.y - startPosition.y
        val rotation = atan2(distanceY, distanceX) * (180f / Math.PI).toFloat()

        val endFadeInTime = end.timeFadeIn.toFloat() / 1000f
        val duration = (end.startTime - start.endTime).toFloat() / 1000f

        // Preempt time can go below 800ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
        // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear preempt function.
        // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
        val preempt = PREEMPT * min(1.0, start.timePreempt / HitObject.PREEMPT_MIN).toFloat() / 1000f

        // Since the unit of spacing is in osu!pixels, we cannot directly port the reference code. As such, we need to
        // approach it with another method. We use the distance between the start and end positions in osu!pixels to
        // determine the amount of sprites to spawn, and then map them into gameplay positions in pixels.
        val osuPixelsStartPosition = start.difficultyStackedEndPosition
        val osuPixelsEndPosition = end.difficultyStackedPosition

        val osuPixelsDistance = hypot(osuPixelsEndPosition.x - osuPixelsStartPosition.x, osuPixelsEndPosition.y - osuPixelsStartPosition.y).toInt()

        var d = (SPACING * 1.5f).toInt()
        while (d < osuPixelsDistance - SPACING) {

            val fraction = d.toFloat() / osuPixelsDistance

            val pointStartX = startPosition.x + distanceX * (fraction - 0.1f)
            val pointStartY = startPosition.y + distanceY * (fraction - 0.1f)

            val pointEndX = startPosition.x + distanceX * fraction
            val pointEndY = startPosition.y + distanceY * fraction

            val fadeOutTime = startTime + fraction * duration
            val fadeInTime = fadeOutTime - preempt

            val fp = pool.obtain()

            fp.clearEntityModifiers()
            fp.setPosition(pointStartX, pointStartY)
            fp.setOrigin(Anchor.Center)
            fp.setScale(1.5f * scale)
            fp.rotation = rotation
            fp.alpha = 0f

            fp.registerEntityModifier(
                Modifiers.sequence(expire,
                    Modifiers.delay(fadeInTime - secPassed),
                    Modifiers.parallel(null,
                        Modifiers.fadeIn(endFadeInTime),
                        Modifiers.scale(endFadeInTime, 1.5f * scale, scale, null, Easing.OutQuad),
                        Modifiers.move(endFadeInTime, pointStartX, pointEndX, pointStartY, pointEndY, null, Easing.OutQuad),
                        Modifiers.sequence(null,
                            Modifiers.delay(fadeOutTime - fadeInTime),
                            Modifiers.fadeOut(endFadeInTime)
                        )
                    )
                )
            )

            scene.attachChild(fp, 0)
            d += SPACING
        }
    }
}
