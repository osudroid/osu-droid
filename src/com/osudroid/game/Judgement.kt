package com.osudroid.game

import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderHead
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.reco1l.framework.Color4
import ru.nsu.ccfit.zuev.osu.game.GameScene

/**
 * Represents a judgement of a [HitObject].
 *
 * For now, this is used to ensure that [HitObject] judgements are ordered by their start time.
 */
class Judgement : IPoolable {
    override var isRecycled = false

    /**
     * Possible [Judgement] types.
     */
    enum class Type {
        /**
         * A [Judgement] on a [HitCircle].
         */
        CIRCLE_HIT,

        /**
         * A [Judgement] on a [SliderHead].
         */
        SLIDER_HEAD,

        /**
         * A [Judgement] that is used to register the accuracy of a [HitObject] after it is judged, which is used for
         * hit error statistics and accuracy calculation.
         */
        REGISTER_ACCURACY
    }

    /**
     * The type of [Judgement].
     */
    @JvmField
    var type = Type.CIRCLE_HIT

    /**
     * The start time (ms) of the [HitObject] this [Judgement] belongs to. This determines when this [Judgement] is
     * eligible to be flushed.
     */
    @JvmField
    var objectStartTime = 0.0

    /**
     * The index-based position of the [HitObject] this [Judgement] belongs to.
     *
     * Used by [Type.CIRCLE_HIT].
     */
    @JvmField
    var objectId = 0

    /**
     * The score awarded for the [Judgement] of the [HitObject].
     *
     * Used by [Type.CIRCLE_HIT].
     */
    @JvmField
    var score = 0

    /**
     * Whether the [HitObject] this [Judgement] belongs to is the last [HitObject] in its combo.
     *
     * Used by [Type.CIRCLE_HIT].
     */
    @JvmField
    var endCombo = false

    /**
     * Whether [GameScene.createHitEffect] should be called when this [Judgement] is flushed.
     * `false` for miss hits whose pop-up was already shown immediately.
     *
     * Used by [Type.CIRCLE_HIT].
     */
    @JvmField
    var showHitEffect = false

    /**
     * The X position of this [Judgement]'s hit effect.
     *
     * Used by [Type.CIRCLE_HIT].
     */
    @JvmField
    var posX = 0f

    /**
     * The Y position of this [Judgement]'s hit effect.
     *
     * Used by [Type.CIRCLE_HIT].
     */
    @JvmField
    var posY: Float = 0f

    /**
     * The color that this [Judgement]'s hit effect should have.
     *
     * Used by [Type.CIRCLE_HIT].
     */
    @JvmField
    var color: Color4? = null

    /**
     * The [HitObjectType] of the [HitObject] that this [Judgement] belongs to.
     *
     * Used by [Type.REGISTER_ACCURACY].
     */
    @JvmField
    var hitObjectType = HitObjectType.Normal

    /**
     * The hit offset of the [HitObject] that this [Judgement] belongs to, in seconds.
     *
     * Used by [Type.REGISTER_ACCURACY].
     */
    @JvmField
    var accuracy = 0.0

    /**
     * Resets this [Judgement] and releases it back to the pool.
     */
    fun release() {
        type = Type.CIRCLE_HIT
        objectStartTime = 0.0
        objectId = 0
        score = 0
        endCombo = false
        showHitEffect = false
        posX = 0f
        posY = 0f
        color = null
        hitObjectType = HitObjectType.Normal
        accuracy = 0.0

        pool.release(this)
    }

    companion object {
        private val pool = SynchronizedPool<Judgement>(20).apply {
            release(Judgement())
        }

        /**
         * Obtains a new [Judgement] from the pool or creates a new one if the pool is empty.
         */
        @JvmStatic
        fun obtain() = pool.acquire() ?: Judgement()
    }
}
