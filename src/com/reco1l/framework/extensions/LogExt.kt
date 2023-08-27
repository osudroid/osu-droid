@file:JvmName("Logger")
/*
 * @author Reco1l
 */

package com.reco1l.framework.extensions

import android.util.Log
import ru.nsu.ccfit.zuev.osuplus.BuildConfig


/**
 * Notify new instance creation of a class.
 */
fun Any.logInit() = "New instance created: ${className}@${hashCode()}".logI("JVM")

/**
 * Notify VM load of a class.
 */
fun Class<*>.logLoad() = "Class static loaded: $simpleName".logI("JVM")


/**
 * As specified on [Log.i]
 */
fun String.logI(tag: Any) = Log.i(tag.toString(), this)

/**
 * As specified on [Log.w]
 */
fun String.logW(tag: Any) = Log.w(tag.toString(), this)

/**
 * As specified on [Log.e]
 */
fun String.logE(tag: Any) = Log.e(tag.toString(), this)

/**
 * Print a log if the current build is debug.
 */
fun String.logIfDebug(tag: Any)
{
    if (BuildConfig.DEBUG)
    {
        Log.i(tag.toString(), this)
    }
}


inline fun Throwable.logWithMessage(tag: Any, message: () -> String) = Log.e(tag.toString(), message(), this)

