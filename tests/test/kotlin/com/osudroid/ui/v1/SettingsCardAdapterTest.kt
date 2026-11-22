package com.osudroid.ui.v1

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@SuppressLint("RestrictedApi")
@RunWith(RobolectricTestRunner::class)
class SettingsCardAdapterTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `Last row of each category is bottom`() {
        val screen = createScreen()
        val first = screen.addCategory("a" to true, "b" to true)
        val second = screen.addCategory("c" to true)

        val adapter = SettingsCardAdapter(screen)

        assertEquals(5, adapter.itemCount)
        assertFalse(adapter.isBottomRow(adapter.positionOf("a")))
        assertTrue(adapter.isBottomRow(adapter.positionOf("b")))
        assertTrue(adapter.isBottomRow(adapter.positionOf("c")))

        for (i in 0 until adapter.itemCount) {
            if (adapter.getItem(i) === first || adapter.getItem(i) === second) {
                assertFalse(adapter.isBottomRow(i))
            }
        }
    }

    @Test
    fun `Hidden last row makes previous row bottom`() {
        val screen = createScreen()
        screen.addCategory("a" to true, "b" to false)

        val adapter = SettingsCardAdapter(screen)

        assertTrue(adapter.isBottomRow(adapter.positionOf("a")))
    }

    @Test
    fun `Single visible row in category is bottom`() {
        val screen = createScreen()
        screen.addCategory("a" to false, "b" to true, "c" to false)

        val adapter = SettingsCardAdapter(screen)

        assertTrue(adapter.isBottomRow(adapter.positionOf("b")))
    }

    private fun createScreen() = PreferenceManager(context).createPreferenceScreen(context)

    private fun PreferenceScreen.addCategory(vararg rows: Pair<String, Boolean>): PreferenceCategory {
        val category = PreferenceCategory(context)
        addPreference(category)

        for ((key, visible) in rows) {
            category.addPreference(Preference(context).apply {
                this.key = key
                isVisible = visible
            })
        }

        return category
    }

    private fun SettingsCardAdapter.positionOf(key: String) =
        (0 until itemCount).first { getItem(it)?.key == key }
}
