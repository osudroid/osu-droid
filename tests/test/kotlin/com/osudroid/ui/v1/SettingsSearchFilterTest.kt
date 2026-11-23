package com.osudroid.ui.v1

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsSearchFilterTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val screen = PreferenceManager(context).createPreferenceScreen(context)

    @Test
    fun `Match title case-insensitively`() {
        val vibrate = createLeaf("Vibrate on circle")
        val section = createSection("input", createCategory(vibrate))

        val result = SettingsSearchFilter(listOf(section)).apply("VIBRA")

        assertTrue(vibrate.isVisible)
        assertEquals(listOf("input"), result)
    }

    @Test
    fun `Match summary`() {
        val row = createLeaf("Audio offset", summary = "Compensates for audio latency")
        val other = createLeaf("Unrelated", summary = "Nothing here")
        val section = createSection("audio", createCategory(row, other))

        SettingsSearchFilter(listOf(section)).apply("latency")

        assertTrue(row.isVisible)
        assertFalse(other.isVisible)
    }

    @Test
    fun `Trim query`() {
        val vibrate = createLeaf("Vibrate on circle")
        val other = createLeaf("Volume")
        val section = createSection("input", createCategory(vibrate, other))

        val result = SettingsSearchFilter(listOf(section)).apply("  vibra  ")

        assertTrue(vibrate.isVisible)
        assertFalse(other.isVisible)
        assertEquals(listOf("input"), result)
    }

    @Test
    fun `Hide category without matches`() {
        val matching = createCategory(createLeaf("Vibrate on circle"))
        val other = createCategory(createLeaf("Volume"))
        val section = createSection("input", matching, other)

        SettingsSearchFilter(listOf(section)).apply("vibra")

        assertTrue(matching.isVisible)
        assertFalse(other.isVisible)
    }

    @Test
    fun `Hide and omit section without matches`() {
        val first = createSection("input", createCategory(createLeaf("Vibrate on circle")))
        val second = createSection("audio", createCategory(createLeaf("Volume")))

        val result = SettingsSearchFilter(listOf(first, second)).apply("vibra")

        assertTrue(first.header.isVisible)
        assertFalse(second.header.isVisible)
        assertEquals(listOf("input"), result)
    }

    @Test
    fun `Show nothing when only hidden preferences match`() {
        val hidden = createLeaf("Vibrate on circle").apply { isVisible = false }
        val category = createCategory(hidden, createLeaf("Volume"))
        val section = createSection("input", category)

        val result = SettingsSearchFilter(listOf(section)).apply("vibra")

        assertFalse(hidden.isVisible)
        assertFalse(category.isVisible)
        assertFalse(section.header.isVisible)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `Match disabled preference without enabling it`() {
        val disabled = createLeaf("Vibrate on circle").apply { isEnabled = false }
        val section = createSection("input", createCategory(disabled))

        SettingsSearchFilter(listOf(section)).apply("vibra")

        assertTrue(disabled.isVisible)
        assertFalse(disabled.isEnabled)
    }

    @Test
    fun `Restore matches after widening query`() {
        val vibrate = createLeaf("Vibrate on circle")
        val intensity = createLeaf("Vibration intensity")
        val section = createSection("input", createCategory(vibrate, intensity))
        val filter = SettingsSearchFilter(listOf(section))

        filter.apply("vibration intensity")

        assertFalse(vibrate.isVisible)
        assertTrue(intensity.isVisible)

        filter.apply("vibra")

        assertTrue(vibrate.isVisible)
        assertTrue(intensity.isVisible)
    }

    @Test
    fun `Show all originally visible preferences on blank query`() {
        val shown = createLeaf("Vibrate on circle")
        val hiddenLeaf = createLeaf("Hidden leaf").apply { isVisible = false }
        val shownCategory = createCategory(shown, hiddenLeaf)
        val hiddenCategory = createCategory(createLeaf("Inside hidden category")).apply { isVisible = false }
        val shownSection = createSection("input", shownCategory, hiddenCategory)
        val hiddenSection = createSection("audio", createCategory(createLeaf("Volume"))).apply { header.isVisible = false }

        val filter = SettingsSearchFilter(listOf(shownSection, hiddenSection))

        filter.apply("x")
        val result = filter.apply("")

        assertTrue(shown.isVisible)
        assertTrue(shownCategory.isVisible)
        assertTrue(shownSection.header.isVisible)
        assertFalse(hiddenLeaf.isVisible)
        assertFalse(hiddenCategory.isVisible)
        assertFalse(hiddenSection.header.isVisible)
        assertEquals(listOf("input"), result)
    }

    @Test
    fun `Section header is not selectable or persistent`() {
        val header = SectionHeaderPreference(context)

        assertFalse(header.isSelectable)
        assertFalse(header.isPersistent)
        assertFalse(header.isIconSpaceReserved)
    }

    @Test
    fun `Section header is a card boundary`() {
        val header = SectionHeaderPreference(context).apply { title = "Section" }
        screen.addPreference(header)

        val category = PreferenceCategory(context)
        screen.addPreference(category)

        category.addPreference(Preference(context).apply { key = "a" })

        val adapter = SettingsCardAdapter(screen)

        assertFalse(adapter.isBottomRow((0 until adapter.itemCount).first { adapter.getItem(it) === header }))
    }

    private fun createLeaf(title: String, summary: String? = null) = Preference(context).also {
        it.title = title
        it.summary = summary
    }

    private fun createCategory(vararg leaves: Preference): PreferenceCategory {
        val category = PreferenceCategory(context)
        screen.addPreference(category)
        leaves.forEach { category.addPreference(it) }
        return category
    }

    private fun createSection(id: String, vararg categories: PreferenceCategory): SettingsSearchSection<String> {
        val header = SectionHeaderPreference(context).apply { title = id }
        screen.addPreference(header)
        return SettingsSearchSection(id, header, categories.toList())
    }
}
