package com.osudroid.multiplayer.api

import org.junit.Assert.assertEquals
import org.junit.Test

class MultiplayerClientEventTest {
    @Test
    fun `event names are unique`() {
        val names = MultiplayerClientEvent.entries.map { it.eventName }

        assertEquals(names.size, names.toSet().size)
    }
}
