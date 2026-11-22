package com.osudroid.online

import android.os.Build
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.VisibleForTesting
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * Handles hardware-backed key generation and payload signing for online attestation.
 *
 * This uses StrongBox for key generation where available and falls back to TEE-backed
 * keys when StrongBox is missing or fails.
 */
object HardwareAttestationManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "osudroid_attestation_key"

    /**
     * Creates a fresh hardware-backed ECDSA key pair bound to a server challenge.
     *
     * @param challenge Server-issued nonce bytes to embed in attestation metadata.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun generateKeyPair(challenge: ByteArray) {
        deleteKey()

        val startTime = SystemClock.elapsedRealtime()

        try {
            generateKeyPairInternal(challenge)
        } finally {
            AttestationState.keygenDurationMs = SystemClock.elapsedRealtime() - startTime
        }
    }

    private fun generateKeyPairInternal(challenge: ByteArray) {
        generateWithStrongBoxFallback(
            strongBoxSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
            generate = { useStrongBox -> generateKey(challenge, useStrongBox) },
            discardFailedKey = ::deleteKey
        )

        AttestationState.keyGeneratedAt = System.currentTimeMillis()
    }

    private fun generateKey(challenge: ByteArray, useStrongBox: Boolean) {
        val specBuilder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAttestationChallenge(challenge)

        if (useStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            specBuilder.setIsStrongBoxBacked(true)
        }

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)

        generator.initialize(specBuilder.build())
        generator.generateKeyPair()
    }

    /**
     * Generates a key in StrongBox when [strongBoxSupported], and falls back to TEE if that fails for any reason.
     *
     * @param strongBoxSupported Whether the OS supports requesting StrongBox at all.
     * @param generate Generates the key, in StrongBox if its argument is `true`, otherwise in TEE.
     * @param discardFailedKey Removes whatever a failed StrongBox attempt may have left behind.
     * @throws Exception The TEE failure, with the StrongBox failure (if any) added as suppressed.
     */
    @VisibleForTesting
    @Throws(Exception::class)
    internal fun generateWithStrongBoxFallback(
        strongBoxSupported: Boolean,
        generate: (useStrongBox: Boolean) -> Unit,
        discardFailedKey: () -> Unit
    ) {
        if (!strongBoxSupported) {
            generate(false)
            return
        }

        try {
            generate(true)
        } catch (strongBoxError: Exception) {
            // We catch everything here, not just StrongBoxUnavailableException.
            // A StrongBox that exists but cannot attest throws ProviderException instead (e.g. ATTESTATION_KEYS_NOT_PROVISIONED,
            // UNIMPLEMENTED without an attestation key, or failing to fetch remotely provisioned keys). TEE attestation is equally valid.
            discardFailedKey()

            try {
                generate(false)
            } catch (teeError: Exception) {
                teeError.addSuppressed(strongBoxError)
                throw teeError
            }
        }
    }

    /**
     * Obtains the attestation certificate chain in PEM format. Returns `null` when the key alias is missing.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun getAttestationChainPem(): String? {
        val keyStore = getKeyStore()
        val chain = keyStore.getCertificateChain(KEY_ALIAS) ?: return null

        if (chain.isEmpty()) {
            return null
        }

        return buildString {
            for (cert in chain) {
                append("-----BEGIN CERTIFICATE-----\n")
                append(Base64.encodeToString(cert.encoded, Base64.NO_WRAP))
                append("\n-----END CERTIFICATE-----\n")
            }
        }
    }

    /**
     * Signs a canonical request payload with the hardware-backed private key.
     *
     * @return The base64 signature or `null` when key material is unavailable.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun signToBase64(payload: String): String? {
        val keyStore = getKeyStore()
        val key = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey ?: return null

        val signature = Signature.getInstance("SHA256withECDSA")

        signature.initSign(key)
        signature.update(payload.toByteArray(StandardCharsets.UTF_8))

        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }

    /**
     * Deletes the current attestation key alias, if present.
     */
    @JvmStatic
    fun deleteKey() {
        try {
            val keyStore = getKeyStore()

            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (_: Exception) {}
    }

    private fun getKeyStore() = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
}

