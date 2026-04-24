package com.osudroid.online

/**
 * Attestation state for the current online session.
 *
 * Values are reset on login attempts and should never be treated as persisted trust.
 */
object AttestationState {
    const val KEY_TTL_MS = 15 * 60 * 1000L

    /**
     * The last challenge issued by the server and consumed for key generation.
     */
    @Volatile
    @JvmStatic
    var pendingChallenge: ByteArray? = null

    /**
     * The last token issued by the server and consumed for key generation.
     */
    @Volatile
    @JvmStatic
    var pendingToken: String? = null

    /**
     * The PEM-encoded attestation certificate chain sent with password login.
     */
    @Volatile
    @JvmStatic
    var attestationChain: String? = null

    /**
     * Whether attestation is ready to be used. `true` once login succeeds with a valid attestation payload.
     */
    @Volatile
    @JvmStatic
    var sessionAttestationReady = false

    /**
     * The time at which the current attestation key was generated, in [System.currentTimeMillis] time base.
     * `null` if no key is currently generated.
     */
    @Volatile
    @JvmStatic
    var keyGeneratedAt: Long? = null

    /**
     * Determines whether the currently generated key is within the time-to-live window.
     */
    @JvmStatic
    fun isKeyValid(): Boolean {
        val t = keyGeneratedAt ?: return false

        return System.currentTimeMillis() - t < KEY_TTL_MS
    }

    /**
     * Clears all attestation session fields.
     */
    @JvmStatic
    fun clearSession() {
        pendingChallenge = null
        pendingToken = null
        attestationChain = null
        sessionAttestationReady = false
        keyGeneratedAt = null

        HardwareAttestationManager.deleteKey()
    }
}
