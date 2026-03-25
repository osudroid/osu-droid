package com.reco1l.framework.android

import android.animation.TimeInterpolator
import android.view.View
import android.view.ViewPropertyAnimator


private fun <T> View.animateTo(

    function: ViewPropertyAnimator.(T) -> ViewPropertyAnimator,
    value: T,
    time: Long,
    delay: Long,
    timeInterpolator: TimeInterpolator?,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

): View {

    animate().apply {

        function(value)
        withStartAction(onStart)
        withEndAction(onEnd)

        duration = time
        startDelay = delay

        if (timeInterpolator != null) {
            interpolator = timeInterpolator
        }

        start()
    }

    return this
}


fun View.cancelAnimators(): View {
    animate().cancel()
    return this
}


fun View.toAlpha(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::alpha, value, time, delay, ease, onStart, onEnd)

fun View.toScaleX(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::scaleX, value, time, delay, ease, onStart, onEnd)

fun View.toScaleY(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::scaleY, value, time, delay, ease, onStart, onEnd)

fun View.toTranslationX(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::translationX, value, time, delay, ease, onStart, onEnd)

fun View.toTranslationY(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::translationY, value, time, delay, ease, onStart, onEnd)

fun View.toX(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::x, value, time, delay, ease, onStart, onEnd)

fun View.toY(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::y, value, time, delay, ease, onStart, onEnd)

fun View.toRotation(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::rotation, value, time, delay, ease, onStart, onEnd)

fun View.toRotationX(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::rotationX, value, time, delay, ease, onStart, onEnd)

fun View.toRotationY(

    value: Float,
    time: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null

) = animateTo(ViewPropertyAnimator::rotationY, value, time, delay, ease, onStart, onEnd)