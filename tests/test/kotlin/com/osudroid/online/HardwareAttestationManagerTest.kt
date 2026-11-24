package com.osudroid.online

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.ProviderException
import java.security.spec.ECGenParameterSpec
import org.junit.Assert
import org.junit.Test

class HardwareAttestationManagerTest {
    private class Recorder(private val failWith: (useStrongBox: Boolean) -> Exception? = { null }) {
        val attempts = mutableListOf<Boolean>()
        var discards = 0

        fun generate(useStrongBox: Boolean) {
            attempts += useStrongBox
            failWith(useStrongBox)?.let { throw it }
        }

        fun discard() {
            discards++
        }

        fun run(strongBoxSupported: Boolean) = HardwareAttestationManager.generateWithStrongBoxFallback(
            strongBoxSupported, ::generate, ::discard
        )
    }

    @Test
    fun `StrongBox is used when it succeeds`() {
        val recorder = Recorder()

        recorder.run(strongBoxSupported = true)

        Assert.assertEquals(listOf(true), recorder.attempts)
        Assert.assertEquals(0, recorder.discards)
    }

    @Test
    fun `TEE is used directly when StrongBox is not supported`() {
        val recorder = Recorder()

        recorder.run(strongBoxSupported = false)

        Assert.assertEquals(listOf(false), recorder.attempts)
        Assert.assertEquals(0, recorder.discards)
    }

    @Test
    fun `StrongBox failure other than unavailability falls back to TEE`() {
        // e.g. Error::Km(UNIMPLEMENTED) or ATTESTATION_KEYS_NOT_PROVISIONED from a present StrongBox.
        val recorder = Recorder { if (it) ProviderException("Failed to generate key pair.") else null }

        recorder.run(strongBoxSupported = true)

        Assert.assertEquals(listOf(true, false), recorder.attempts)
    }

    @Test
    fun `Failed StrongBox key is discarded before falling back`() {
        val recorder = Recorder { if (it) ProviderException("StrongBox") else null }

        recorder.run(strongBoxSupported = true)

        Assert.assertEquals(1, recorder.discards)
    }

    @Test
    fun `TEE failure is thrown with the StrongBox failure suppressed`() {
        val strongBoxError = ProviderException("StrongBox")
        val teeError = ProviderException("TEE")
        val recorder = Recorder { if (it) strongBoxError else teeError }

        val thrown = Assert.assertThrows(ProviderException::class.java) { recorder.run(strongBoxSupported = true) }

        Assert.assertSame(teeError, thrown)
        Assert.assertArrayEquals(arrayOf<Throwable>(strongBoxError), thrown.suppressed)
    }

    @Test
    fun `TEE failure without StrongBox support is thrown as is`() {
        val teeError = ProviderException("TEE")
        val recorder = Recorder { teeError }

        val thrown = Assert.assertThrows(ProviderException::class.java) { recorder.run(strongBoxSupported = false) }

        Assert.assertSame(teeError, thrown)
        Assert.assertEquals(0, thrown.suppressed.size)
    }

    @Test
    fun `Keystore fallback key is used when it succeeds`() {
        var softwareCalls = 0

        val softwareKey = HardwareAttestationManager.generateWithSoftwareFallback(
            generateKeystore = {},
            generateSoftware = {
                softwareCalls++
                generateJvmKeyPair()
            }
        )

        Assert.assertNull(softwareKey)
        Assert.assertEquals(0, softwareCalls)
    }

    @Test
    fun `Software key is used when the keystore fails`() {
        val expected = generateJvmKeyPair()

        val actual: KeyPair? = HardwareAttestationManager.generateWithSoftwareFallback(
            generateKeystore = { throw ProviderException("Keystore") },
            generateSoftware = { expected }
        )

        Assert.assertSame(expected, actual)
    }

    @Test
    fun `Software key failure is thrown`() {
        val softwareError = IllegalStateException("Software")

        val thrown = Assert.assertThrows(IllegalStateException::class.java) {
            HardwareAttestationManager.generateWithSoftwareFallback(
                generateKeystore = { throw ProviderException("Keystore") },
                generateSoftware = { throw softwareError }
            )
        }

        Assert.assertSame(softwareError, thrown)
    }

    private fun generateJvmKeyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
}
