package com.osudroid.ui.v1

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import ru.nsu.ccfit.zuev.osuplus.R

/**
 * A non-interactive header that labels a group of settings categories.
 */
class SectionHeaderPreference(context: Context) : Preference(context) {
    init {
        layoutResource = R.layout.settings_preference_section_header
        isSelectable = false
        isPersistent = false
        isIconSpaceReserved = false
    }
}

/**
 * A section of the settings screen, consisting of a [header] followed by its [categories].
 *
 * @param id An identifier for this [SettingsSearchSection]. This is returned by [SettingsSearchFilter.apply]
 * when this [SettingsSearchSection] is visible.
 */
class SettingsSearchSection<T>(
    val id: T,
    val header: SectionHeaderPreference,
    val categories: List<PreferenceGroup>,
)

/**
 * Whether this [Preference]'s title or summary contains [query], ignoring case.
 *
 * @param query The query, already trimmed and non-blank.
 */
internal fun Preference.matchesSearch(query: String) =
    title?.contains(query, ignoreCase = true) == true || summary?.contains(query, ignoreCase = true) == true

/**
 * Filters [SettingsSearchSection]s by a search query by toggling [Preference.isVisible].
 *
 * [Preference]s that are hidden when the filter is constructed stay hidden regardless of the query.
 * [Preference.isEnabled] is never modified.
 */
class SettingsSearchFilter<T>(private val sections: List<SettingsSearchSection<T>>) {
    private val hiddenPreferences = HashSet<Preference>()

    init {
        for (section in sections) {
            snapshotIfHidden(section.header)

            for (category in section.categories) {
                snapshotIfHidden(category)
                category.leaves.forEach(::snapshotIfHidden)
            }
        }
    }

    /**
     * Applies [query] to the [SettingsSearchSection]s.
     *
     * @param query The query. It will be trimmed. A blank query shows every originally visible [Preference].
     * @return The IDs of the [SettingsSearchSection]s that remain visible, in their original order.
     */
    fun apply(query: String): List<T> {
        val trimmed = query.trim()
        val visibleIds = ArrayList<T>()

        for (section in sections) {
            var hasVisibleCategory = false

            for (category in section.categories) {
                var hasVisibleLeaf = false

                for (leaf in category.leaves) {
                    leaf.isVisible = leaf !in hiddenPreferences && (trimmed.isEmpty() || leaf.matchesSearch(trimmed))
                    hasVisibleLeaf = hasVisibleLeaf || leaf.isVisible
                }

                category.isVisible = category !in hiddenPreferences && hasVisibleLeaf
                hasVisibleCategory = hasVisibleCategory || category.isVisible
            }

            section.header.isVisible = section.header !in hiddenPreferences && hasVisibleCategory

            if (section.header.isVisible) {
                visibleIds.add(section.id)
            }
        }

        return visibleIds
    }

    private fun snapshotIfHidden(preference: Preference) {
        if (!preference.isVisible) {
            hiddenPreferences.add(preference)
        }
    }

    /**
     * All non-group descendants of this [PreferenceGroup].
     */
    private val PreferenceGroup.leaves: List<Preference>
        get() = buildList {
            for (i in 0 until preferenceCount) {
                val child = getPreference(i)

                if (child is PreferenceGroup) {
                    addAll(child.leaves)
                } else {
                    add(child)
                }
            }
        }
}
