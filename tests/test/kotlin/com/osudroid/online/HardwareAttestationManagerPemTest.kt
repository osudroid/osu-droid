package com.osudroid.online

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HardwareAttestationManagerPemTest {
    @Test
    fun `Public key PEM round-trips`() {
        val publicKey = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair().public

        val pem = HardwareAttestationManager.toPublicKeyPem(publicKey)

        Assert.assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----\n"))
        Assert.assertTrue(pem.endsWith("-----END PUBLIC KEY-----\n"))
        Assert.assertFalse(pem.contains("\r"))

        val lines = pem.trimEnd().lines()
        val bodyLines = lines.subList(1, lines.size - 1)

        // A P-256 SubjectPublicKeyInfo encodes to 124 base64 characters.
        Assert.assertEquals(listOf(64, 60), bodyLines.map { it.length })

        val decoded = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(Base64.decode(bodyLines.joinToString(""), Base64.NO_WRAP)))

        Assert.assertEquals(publicKey, decoded)
    }
}
