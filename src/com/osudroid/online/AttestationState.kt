package com.osudroid.online

/**
 * Attestation state for the current online session.
 *
 * Values are reset on login attempts and should never be treated as persisted trust.
 */
object AttestationState {
    /**
     * The last challenge issued by the server and consumed for key generation.
     */
    @Volatile
    @JvmStatic
    var pendingChallenge: ByteArray? = null

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
     * Clears all attestation session fields.
     */
    @JvmStatic
    fun clearSession() {
        pendingChallenge = null
        attestationChain = null
        sessionAttestationReady = false

        HardwareAttestationManager.deleteKey()
    }
}