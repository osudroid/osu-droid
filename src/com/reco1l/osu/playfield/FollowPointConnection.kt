package com.reco1l.osu.playfield

import com.reco1l.framework.*
import com.reco1l.osu.*
import com.reco1l.osu.graphics.*
import com.rian.osu.beatmap.hitobject.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.util.modifier.*
import org.anddev.andengine.util.modifier.ease.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.math.*

object FollowPointConnection {


    private const val SPACING = 32

    private const val PREEMPT = 800


    @JvmStatic
    val pool = Pool(initialSize = 16) {

        // For optimization we avoid to use AnimatedSprite if there's one frame.
        if (ResourceManager.getInstance().isTextureLoaded("followpoint-0")) {
            AnimatedSprite("followpoint", true, OsuSkin.get().animationFramerate)
        } else {
            ExtendedSprite(ResourceManager.getInstance().getTexture("followpoint"))
        }
    }


    private val expire = object : ModifierListener() {
        override fun onModifierFinished(pModifier: IModifier<IEntity>, fp: IEntity) {
            updateThread {
                fp.detachSelf()
                pool.free(fp as ExtendedSprite)
            }
        }
    }


    @JvmStatic
    fun addConnection(scene: Scene, secPassed: Float, start: HitObject, end: HitObject) {

        // Reference: https://github.com/ppy/osu/blob/7bc8908ca9c026fed1d831eb6e58df7624a8d614/osu.Game.Rulesets.Osu/Objects/Drawables/Connections/FollowPointConnection.cs
        val scale = start.gameplayScale
        val startTime = start.getEndTime()

        val startPosition = start.getGameplayStackedEndPosition()
        val endPosition = end.gameplayStackedPosition

        val distanceX = endPosition.x - startPosition.x
        val distanceY = endPosition.y - startPosition.y

        val distance = hypot(distanceX, distanceY).toInt()
        val rotation = atan2(distanceY, distanceX) * (180f / Math.PI).toFloat()

        var d = (SPACING * 1.5f).toInt()

        while (d < distance - SPACING) {

            val fraction = d.toFloat() / distance

            val pointStartX = startPosition.x + distanceX * (fraction - 0.1f)
            val pointStartY = startPosition.y + distanceY * (fraction - 0.1f)

            val pointEndX = startPosition.x + distanceX * fraction
            val pointEndY = startPosition.y + distanceY * fraction

            val duration = end.startTime - startTime

            // Preempt time can go below 800ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
            // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear preempt function.
            // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
            val preempt = PREEMPT * min(1.0, start.timePreempt / HitObject.PREEMPT_MIN)

            val fadeOutTime = (startTime + fraction * duration).toFloat() / 1000f
            val fadeInTime = (fadeOutTime - preempt / 1000f).toFloat()

            val fp = pool.obtain()

            fp.setPosition(pointStartX, pointStartY)
            fp.setOrigin(Origin.Center)
            fp.setScale(1.5f * scale)
            fp.rotation = rotation
            fp.alpha = 0f

            val endFadeInTime = end.timeFadeIn.toFloat() / 1000f

            fp.clearEntityModifiers()
            fp.registerEntityModifier(Modifiers.sequence(null,
                Modifiers.delay(fadeInTime - secPassed),
                Modifiers.parallel(null,
                    Modifiers.fadeIn(endFadeInTime),
                    Modifiers.scale(endFadeInTime, 1.5f * scale, scale, null, EaseQuadOut.getInstance()),
                    Modifiers.move(endFadeInTime, pointStartX, pointEndX, pointStartY, pointEndY, null, EaseQuadOut.getInstance()),
                    Modifiers.sequence(null,
                        Modifiers.delay(fadeOutTime - fadeInTime),
                        Modifiers.fadeOut(endFadeInTime, expire)
                    )
                )
            ))

            scene.attachChild(fp, 0)
            d += SPACING
        }
    }
}
