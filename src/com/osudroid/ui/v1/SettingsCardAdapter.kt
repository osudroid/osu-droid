package com.osudroid.ui.v1

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceViewHolder
import ru.nsu.ccfit.zuev.osuplus.R

/**
 * A [PreferenceGroupAdapter] that decides the background of each settings row when it is bound, rather than in XML.
 *
 * The last visible row of a card is given rounded bottom corners, so rows that get hidden or filtered out never leave
 * a card with a square bottom edge.
 */
@SuppressLint("RestrictedApi")
class SettingsCardAdapter(group: PreferenceGroup) : PreferenceGroupAdapter(group) {
    override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        if (getItem(position)!!.isCardBoundary()) {
            return
        }

        val row = holder.itemView

        if (isBottomRow(position)) {
            row.setBackgroundResource(R.drawable.rounded_half_bottom)
            row.backgroundTintList = ColorStateList.valueOf(ROW_COLOR)
        } else {
            row.setBackgroundColor(ROW_COLOR)
            row.backgroundTintList = null
        }
    }

    /**
     * Whether the row at [position] is the last row of its card.
     */
    fun isBottomRow(position: Int): Boolean {
        if (getItem(position)!!.isCardBoundary()) {
            return false
        }

        val next = if (position + 1 < itemCount) getItem(position + 1) else null

        return next?.isCardBoundary() ?: true
    }

    private fun Preference.isCardBoundary() = this is PreferenceGroup || this is SectionHeaderPreference

    companion object {
        private const val ROW_COLOR = 0xFF161622.toInt()
    }
}
