package com.osudroid.online

import java.security.ProviderException
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild
import org.robolectric.util.ReflectionHelpers
import android.os.Build
import ru.nsu.ccfit.zuev.osu.online.PostBuilder

@RunWith(RobolectricTestRunner::class)
class AttestationDiagnosticsTest {
    private class CapturingPostBuilder : PostBuilder() {
        val params = mutableMapOf<String, String>()

        override fun addParamInternal(key: String, value: String) {
            params[key] = value
        }

        override fun getRequestBody(): RequestBody = throw UnsupportedOperationException()
    }

    @After
    fun tearDown() {
        AttestationState.keygenDurationMs = null
    }

    @Test
    fun `Test failure description names the stage and the root cause`() {
        val error = RuntimeException("outer", ProviderException("middle", IllegalStateException("Keystore error 7")))

        Assert.assertEquals(
            "keygen: java.lang.IllegalStateException: Keystore error 7",
            AttestationDiagnostics.describeFailure("keygen", error)
        )
    }

    @Test
    fun `Test failure description without a message omits it`() {
        Assert.assertEquals(
            "keygen: java.lang.IllegalStateException",
            AttestationDiagnostics.describeFailure("keygen", IllegalStateException())
        )
    }

    @Test
    fun `Test failure description survives a cycle of causes`() {
        val first = RuntimeException("first")
        val second = RuntimeException("second", first)
        first.initCause(second)

        Assert.assertTrue(AttestationDiagnostics.describeFailure("keygen", first).startsWith("keygen: "))
    }

    @Test
    fun `Test failure description is capped to what the server stores`() {
        val description = AttestationDiagnostics.describeFailure("keygen", IllegalStateException("x".repeat(1000)))

        Assert.assertEquals(255, description.length)
    }

    @Test
    fun `Test text failure description names the stage`() {
        Assert.assertEquals("chain: empty", AttestationDiagnostics.describeFailure("chain", "empty"))
    }

    @Test
    fun `Test device parameters describe the device`() {
        ShadowBuild.setManufacturer("samsung")
        ShadowBuild.setModel("SM-G991B")
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 34)

        val post = CapturingPostBuilder()
        AttestationDiagnostics.addDeviceParams(post)

        Assert.assertEquals("samsung", post.params["deviceManufacturer"])
        Assert.assertEquals("SM-G991B", post.params["deviceModel"])
        Assert.assertEquals("34", post.params["androidSdk"])
    }

    @Test
    fun `Test key generation time is sent once measured`() {
        AttestationState.keygenDurationMs = 1234L

        val post = CapturingPostBuilder()
        AttestationDiagnostics.addDeviceParams(post)

        Assert.assertEquals("1234", post.params["keygenMs"])
    }

    @Test
    fun `Test key generation time is omitted before it is measured`() {
        val post = CapturingPostBuilder()
        AttestationDiagnostics.addDeviceParams(post)

        Assert.assertFalse(post.params.containsKey("keygenMs"))
    }
}
