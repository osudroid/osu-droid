package com.osudroid.multiplayer.api

import org.junit.Assert.assertEquals
import org.junit.Test

class MultiplayerServerEventTest {
    @Test
    fun `event names are unique`() {
        val names = MultiplayerServerEvent.entries.map { it.eventName }

        assertEquals(names.size, names.toSet().size)
    }
}
