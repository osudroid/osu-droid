package com.osudroid.ui.v1

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsSearchQueryTest {
    @Test
    fun `Emit only last query when typing rapidly`() = runTest {
        val query = MutableStateFlow("")
        val collected = mutableListOf<String>()
        backgroundScope.launch { query.debounceSearchQuery().collect { collected.add(it) } }
        runCurrent()

        query.value = "v"
        advanceTimeBy(100.milliseconds)

        query.value = "vi"
        advanceTimeBy(100.milliseconds)

        query.value = "vib"
        advanceTimeBy(250.milliseconds)

        runCurrent()

        val nonBlank = collected.filter { it.isNotBlank() }
        assertEquals(listOf("vib"), nonBlank)
        assertEquals("vib", collected.last())
    }

    @Test
    fun `Emit blank query immediately`() = runTest {
        val query = MutableStateFlow("")
        val collected = mutableListOf<String>()
        backgroundScope.launch { query.debounceSearchQuery().collect { collected.add(it) } }
        runCurrent()

        query.value = "vib"
        advanceTimeBy(250.milliseconds)
        runCurrent()

        query.value = ""
        runCurrent()

        assertEquals("", collected.last())
    }

    @Test
    fun `Hold query until timeout elapses`() = runTest {
        val query = MutableStateFlow("")
        val collected = mutableListOf<String>()
        backgroundScope.launch { query.debounceSearchQuery().collect { collected.add(it) } }
        runCurrent()

        query.value = "vib"
        runCurrent()
        advanceTimeBy(249.milliseconds)

        runCurrent()
        assertTrue("Query must not emit before the timeout", "vib" !in collected)

        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals("vib", collected.last())
    }
}
