package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.ui.*

/**
 * A search input field for filtering mods and mod presets in the mod menu.
 */
class ModMenuSearchInput : UITextInput("") {
    /**
     * The function to call when the search term changes.
     */
    var onSearchTermUpdate: (searchTerm: String) -> Unit = {}

    private var lastValueChange = Long.MAX_VALUE

    init {
        placeholder = "Search..."
    }

    override fun onValueChanged() {
        lastValueChange = System.currentTimeMillis()
        super.onValueChanged()
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        if (lastValueChange != Long.MAX_VALUE && System.currentTimeMillis() - lastValueChange >= DEBOUNCE_MS) {
            onSearchTermUpdate(value)
            lastValueChange = Long.MAX_VALUE
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    companion object {
        private const val DEBOUNCE_MS = 200L
    }
}