package com.osudroid.online

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * Handles hardware-backed key generation and payload signing for online attestation.
 *
 * This uses StrongBox for key generation where available and falls back to TEE-backed keys.
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

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)

        val specBuilder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAttestationChallenge(challenge)

        // Use StrongBox if available. Otherwise, fall back to TEE.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                specBuilder.setIsStrongBoxBacked(true)

                generator.initialize(specBuilder.build())
                generator.generateKeyPair()
                AttestationState.keyGeneratedAt = System.currentTimeMillis()

                return
            } catch (_: StrongBoxUnavailableException) {
                specBuilder.setIsStrongBoxBacked(false)
            }
        }

        generator.initialize(specBuilder.build())
        generator.generateKeyPair()
        AttestationState.keyGeneratedAt = System.currentTimeMillis()
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

