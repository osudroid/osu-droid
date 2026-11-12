package com.osudroid.online

import android.os.Build
import ru.nsu.ccfit.zuev.osu.online.PostBuilder

/**
 * Tells the server about this device and why attestation failed on it.
 */
object AttestationDiagnostics {
    /**
     * The longest acceptable failure description.
     */
    private const val MAX_FAILURE_LENGTH = 255

    /**
     * Describes a failure by the root cause of [error], which is where Keystore errors are reported.
     *
     * @param stage The attestation step that failed.
     * @param error The error that occurred.
     */
    @JvmStatic
    fun describeFailure(stage: String, error: Throwable) = describeFailure(stage, findRootCause(error).toString())

    /**
     * Describes a failure.
     *
     * @param stage The attestation step that failed.
     * @param detail What went wrong.
     */
    @JvmStatic
    fun describeFailure(stage: String, detail: String) = "$stage: $detail".take(MAX_FAILURE_LENGTH)

    /**
     * Adds parameters that describe this device to a login request.
     *
     * @param post The login request.
     */
    @JvmStatic
    fun addDeviceParams(post: PostBuilder) {
        post.addParam("deviceManufacturer", Build.MANUFACTURER)
        post.addParam("deviceModel", Build.MODEL)
        post.addParam("androidSdk", Build.VERSION.SDK_INT.toString())

        AttestationState.keygenDurationMs?.let { post.addParam("keygenMs", it.toString()) }
    }

    private fun findRootCause(error: Throwable): Throwable {
        val visited = mutableSetOf<Throwable>()
        var current = error

        while (visited.add(current)) {
            current = current.cause ?: break
        }

        return current
    }
}
