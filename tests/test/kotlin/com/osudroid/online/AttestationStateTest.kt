package com.osudroid.online

import org.junit.After
import org.junit.Assert
import org.junit.Test

class AttestationStateTest {
    @After
    fun tearDown() {
        AttestationState.pendingToken = null
    }

    @Test
    fun `Consuming the pending token returns it`() {
        AttestationState.pendingToken = "token"

        Assert.assertEquals("token", AttestationState.consumePendingToken())
    }

    @Test
    fun `A consumed pending token cannot be sent again`() {
        AttestationState.pendingToken = "token"

        AttestationState.consumePendingToken()

        Assert.assertNull(AttestationState.pendingToken)
        Assert.assertNull(AttestationState.consumePendingToken())
    }

    @Test
    fun `Consuming without a pending token returns nothing`() {
        Assert.assertNull(AttestationState.consumePendingToken())
    }
}
