package com.osudroid.multiplayer.api

import com.osudroid.BuildSettings
import com.osudroid.online.AttestationState
import com.osudroid.online.HardwareAttestationManager

/**
 * Builds hardware attestation fields for multiplayer and enforces fail-closed auth requirements.
 */
object MultiplayerAttestation {
    /**
     * Signs a canonical multiplayer payload with the hardware-backed key.
     *
     * @return The base64 signature.
     * @throws IllegalStateException when attestation is not ready or signing fails.
     */
    @JvmStatic
    fun signPayload(payload: String): String {
        if (BuildSettings.DEBUG_SKIP_ATTESTATION) {
            return BuildSettings.DEBUG_ATTESTATION_SIGN
        }

        if (!AttestationState.sessionAttestationReady) {
            throw IllegalStateException("Attestation required for multiplayer auth")
        }

        return HardwareAttestationManager.signToBase64(payload)
            ?: throw IllegalStateException("Cannot sign multiplayer attestation payload")
    }
}

