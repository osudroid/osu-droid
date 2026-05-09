package com.rian.andengine.modifier

import com.edlplan.framework.easing.Easing
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.reco1l.andengine.component.UIComponent
import com.reco1l.framework.Color4

/**
 * A sequence of [UniversalModifier]s all operating upon the same [UIComponent]. Can be used to group and chain
 * [UniversalModifier]s that should run at the same time as others (parallel) or after one another (sequence).
 *
 * It is recommended to use this with try-with-resources to ensure that this [UniversalModifierSequence] is recycled
 * after use.
 */
class UniversalModifierSequence : IPoolable, AutoCloseable {
    //region Initialization

    override var isRecycled = false

    private var origin: UIComponent? = null

    private var startTime = 0f
    private var currentTime = 0f
    private var lastEndTime = 0f

    /**
     * The [UniversalModifier] that will end the latest among all [UniversalModifier]s applied to this
     * [UniversalModifierSequence], if any.
     */
    var lastActiveModifier: UniversalModifier? = null
        private set

    /**
     * The time at which all [UniversalModifier]s applied to this [UniversalModifierSequence] ends relative to the
     * underlying [UIComponent.modifierStartTime], in seconds.
     */
    val endTime
        get() = lastEndTime

    /**
     * The duration at which all [UniversalModifier]s applied to this [UniversalModifierSequence] runs for, in seconds.
     */
    val duration
        get() = endTime - startTime

    fun init(origin: UIComponent) {
        this.origin = origin

        startTime = origin.modifierStartTime
        currentTime = startTime
        lastEndTime = startTime
    }

    //endregion

    //region Translation

    /**
     * Smoothly adjusts [UIComponent.translationX] and [UIComponent.translationY] over time.
     *
     * @param x The final [UIComponent.translationX] to reach at the end of the [UniversalModifier].
     * @param y The final [UIComponent.translationY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun translateTo(x: Float, y: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.TranslateXY, duration, easing) {
            finalValues[0] = x
            finalValues[1] = y
        }

    /**
     * Smoothly adjusts [UIComponent.translationX] over time.
     *
     * @param value The final [UIComponent.translationX] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun translateToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.TranslateX, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts [UIComponent.translationY] over time.
     *
     * @param value The final [UIComponent.translationY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun translateToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.TranslateY, duration, easing) { finalValues[0] = value }

    //endregion

    //region Move

    /**
     * Smoothly adjusts [UIComponent.mX] and [UIComponent.mY] over time.
     *
     * @param x The final [UIComponent.mX] to reach at the end of the [UniversalModifier].
     * @param y The final [UIComponent.mY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun moveTo(x: Float, y: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.MoveXY, duration, easing) {
            finalValues[0] = x
            finalValues[1] = y
        }

    /**
     * Smoothly adjusts [UIComponent.mX] over time.
     *
     * @param value The final [UIComponent.mX] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun moveToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.MoveX, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts [UIComponent.mY] over time.
     *
     * @param value The final [UIComponent.mY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun moveToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.MoveY, duration, easing) { finalValues[0] = value }

    //endregion

    //region Scale

    /**
     * Smoothly adjusts [UIComponent.mScaleX] and [UIComponent.mScaleY] over time.
     * 
     * @param value The final [UIComponent.mScaleX] and [UIComponent.mScaleY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun scaleTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.ScaleXY, duration, easing) {
            finalValues[0] = value
            finalValues[1] = value
        }

    /**
     * Smoothly adjusts [UIComponent.mScaleX] over time.
     * 
     * @param value The final [UIComponent.mScaleX] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun scaleToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.ScaleX, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts [UIComponent.mScaleY] over time.
     * 
     * @param value The final [UIComponent.mScaleY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun scaleToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.ScaleY, duration, easing) { finalValues[0] = value }

    //endregion
    
    //region Alpha

    /**
     * Smoothly adjusts [UIComponent.alpha] over time.
     * 
     * @param value The final [UIComponent.alpha] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun fadeTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.Alpha, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts [UIComponent.alpha] to 1 over time.
     * 
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */    
    @JvmOverloads
    fun fadeIn(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(1f, duration, easing)

    /**
     * Smoothly adjusts [UIComponent.alpha] from 0 to 1 over time.
     * 
     * @param duration The duration of the [UniversalModifier]. Defaults to 0 seconds.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun fadeInFromZero(duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.Alpha, duration, easing) {
            hasInitialValues = true
            initialValues[0] = 0f
            finalValues[0] = 1f
        }

    /**
     * Smoothly adjusts [UIComponent.alpha] to 0 over time.
     *
     * @param duration The duration of the [UniversalModifier]. Defaults to 0 seconds.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun fadeOut(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(0f, duration, easing)

    //endregion

    //region Color

    /**
     * Smoothly adjusts [UIComponent.color] over time.
     *
     * @param color The final color to reach at the end of the [UniversalModifier], in 0xRRGGBB format.
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun colorTo(color: Long, duration: Float = 0f, easing: Easing = Easing.None) =
        colorTo(
            red = ((color ushr 16) and 0xFF) / 255f,
            green = ((color ushr 8) and 0xFF) / 255f,
            blue = (color and 0xFF) / 255f,
            duration = duration,
            easing = easing
        )

    /**
     * Smoothly adjusts [UIComponent.color] over time.
     *
     * @param color The final [Color4] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun colorTo(color: Color4, duration: Float = 0f, easing: Easing = Easing.None) =
        colorTo(color.red, color.green, color.blue, duration, easing)

    /**
     * Smoothly adjusts [UIComponent.color] over time.
     *
     * @param red The final red component of [UIComponent.color] to reach at the end of the [UniversalModifier], in the
     * range [0, 1].
     * @param green The final green component of [UIComponent.color] to reach at the end of the [UniversalModifier], in
     * the range [0, 1].
     * @param blue The final blue component of [UIComponent.color] to reach at the end of the [UniversalModifier], in
     * the range [0, 1].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun colorTo(red: Float, green: Float, blue: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.Color, duration, easing) {
            finalValues[0] = red
            finalValues[1] = green
            finalValues[2] = blue
        }

    //endregion

    //region Rotation

    /**
     * Smoothly adjusts [UIComponent.mRotation] over time.
     *
     * @param value The final [UIComponent.mRotation] to reach at the end of the [UniversalModifier], in degrees.
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun rotateTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.Rotation, duration, easing) { finalValues[0] = value }

    //endregion

    //region Size

    /**
     * Smoothly adjusts [UIComponent.width] and [UIComponent.height] over time.
     *
     * @param width The final [UIComponent.width] to reach at the end of the [UniversalModifier].
     * @param height The final [UIComponent.height] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun sizeTo(width: Float, height: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.Size, duration, easing) {
            finalValues[0] = width
            finalValues[1] = height
        }

    /**
     * Smoothly adjusts [UIComponent.width] over time.
     *
     * @param width The final [UIComponent.width] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun widthTo(width: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.Width, duration, easing) { finalValues[0] = width }

    /**
     * Smoothly adjusts [UIComponent.height] over time.
     *
     * @param height The final [UIComponent.height] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun heightTo(height: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        append(ModifierType.Height, duration, easing) { finalValues[0] = height }

    //endregion

    /**
     * Sets a callback to the [lastActiveModifier] of this [UniversalModifierSequence]. If no [UniversalModifier]s
     * have been applied to this [UniversalModifierSequence], a [UniversalModifier] that does nothing and starts at
     * [currentTime] will be applied to [origin].
     *
     * This callback will be called when the current [lastActiveModifier] (as of the call of this method) finishes.
     *
     * @param onFinished The callback to call.
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun after(onFinished: OnModifierFinished? = null): UniversalModifierSequence {
        if (lastActiveModifier == null) {
            append(ModifierType.None, 0f, Easing.None) {}
        }

        lastActiveModifier?.after(onFinished)

        return this
    }

    /**
     * Advances the start time of future [UniversalModifier]s added to this [UniversalModifierSequence] by a certain
     * delay.
     *
     * @param delay The delay to apply, in seconds. Defaults to 0.
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun delay(delay: Float = 0f): UniversalModifierSequence {
        currentTime += delay

        return this
    }

    /**
     * Advances the start time of future [UniversalModifier]s added to this [UniversalModifierSequence] to the latest
     * end time of all [UniversalModifier]s in this [UniversalModifierSequence] plus a certain delay.
     *
     * @param delay The delay to apply, in seconds. Defaults to 0.
     * @return A [UniversalModifierSequence] to which further [UniversalModifier]s can be added.
     */
    @JvmOverloads
    fun then(delay: Float = 0f): UniversalModifierSequence {
        currentTime = lastEndTime

        return delay(delay)
    }

    private inline fun append(type: ModifierType, duration: Float, easing: Easing, crossinline block: UniversalModifier.() -> Unit): UniversalModifierSequence {
        val modifier = UniversalModifier.GlobalPool.acquire() ?: UniversalModifier()

        modifier.target = origin
        modifier.type = type
        modifier.startTime = currentTime
        modifier.duration = duration
        modifier.eased(easing)
        modifier.block()

        origin?.appendModifier(modifier)

        if (lastActiveModifier == null || lastEndTime < modifier.endTime) {
            lastEndTime = modifier.endTime
            lastActiveModifier = modifier
        }

        return this
    }

    //region Recycling

    override fun close() {
        origin = null
        lastActiveModifier = null

        startTime = 0f
        currentTime = 0f
        lastEndTime = 0f

        pool.release(this)
    }

    companion object {
        private val pool = SynchronizedPool<UniversalModifierSequence>(5).apply {
            release(UniversalModifierSequence())
        }

        /**
         * Obtains a [UniversalModifierSequence] from the pool or creates a new one if it is not available.
         *
         * @param origin The [UIComponent] that this [UniversalModifierSequence] will be applied to. If `null`, this can
         * be set later by calling [init] on the obtained [UniversalModifierSequence].
         * @return A [UniversalModifierSequence] instance.
         */
        @JvmStatic
        @JvmOverloads
        fun obtain(origin: UIComponent? = null) = (pool.acquire() ?: UniversalModifierSequence()).apply {
            if (origin != null) {
                init(origin)
            }
        }
    }

    //endregion
}