package com.osudroid.online

import android.os.Build
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.VisibleForTesting
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
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
    private const val PEM_LINE_LENGTH = 64

    /**
     * The in-memory fallback key pair, used when the Keystore could not generate one.
     */
    @Volatile
    private var softwareKeyPair: KeyPair? = null

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
     * Creates a fresh ECDSA key pair that is not attested. This is used for sessions that
     * cannot prove hardware attestation.
     *
     * The key lives in the Keystore when possible, and in memory when the Keystore fails.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun generateFallbackKeyPair() {
        deleteKey()

        softwareKeyPair = generateWithSoftwareFallback(
            generateKeystore = {
                try {
                    generateFallbackKeystoreKey()
                } catch (e: Exception) {
                    deleteKey()
                    throw e
                }
            },
            generateSoftware = {
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
                    .apply { initialize(ECGenParameterSpec("secp256r1")) }
                    .generateKeyPair()
            }
        )

        AttestationState.keyGeneratedAt = System.currentTimeMillis()
    }

    private fun generateFallbackKeystoreKey() {
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER).apply {
            initialize(spec)
            generateKeyPair()
        }
    }

    /**
     * Generates a key in the Keystore, and falls back to an in-memory key
     * if that fails for any reason.
     *
     * @param generateKeystore Generates the key in the Keystore.
     * @param generateSoftware Generates the key in memory.
     * @return The in-memory key pair, or `null` if the Keystore generated the key.
     * @throws Exception The failure of [generateSoftware], if any.
     */
    @VisibleForTesting
    @Throws(Exception::class)
    internal fun generateWithSoftwareFallback(
        generateKeystore: () -> Unit,
        generateSoftware: () -> KeyPair
    ): KeyPair? {
        try {
            generateKeystore()
        } catch (_: Exception) {
            return generateSoftware()
        }

        return null
    }

    /**
     * Obtains the active key's public half as a PEM-encoded SubjectPublicKeyInfo.
     *
     * Returns `null` when there is no key.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun getPublicKeyPem(): String? {
        val publicKey = softwareKeyPair?.public
            ?: getKeyStore().getCertificate(KEY_ALIAS)?.publicKey
            ?: return null

        return toPublicKeyPem(publicKey)
    }

    @VisibleForTesting
    internal fun toPublicKeyPem(key: PublicKey) = buildString {
        append("-----BEGIN PUBLIC KEY-----\n")

        for (line in Base64.encodeToString(key.encoded, Base64.NO_WRAP).chunked(PEM_LINE_LENGTH)) {
            append(line)
            append('\n')
        }

        append("-----END PUBLIC KEY-----\n")
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
     * Signs a canonical request payload with the active private key (the in-memory fallback
     * key if there is one, otherwise the Keystore key).
     *
     * @return The base64 signature or `null` when key material is unavailable.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun signToBase64(payload: String): String? {
        val key = softwareKeyPair?.private ?: getKeyStore().getKey(KEY_ALIAS, null) as? PrivateKey ?: return null

        val signature = Signature.getInstance("SHA256withECDSA")

        signature.initSign(key)
        signature.update(payload.toByteArray(StandardCharsets.UTF_8))

        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }

    /**
     * Deletes the current attestation key alias, if present, and drops the in-memory fallback key.
     */
    @JvmStatic
    fun deleteKey() {
        softwareKeyPair = null

        try {
            val keyStore = getKeyStore()

            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (_: Exception) {}
    }

    private fun getKeyStore() = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
}

