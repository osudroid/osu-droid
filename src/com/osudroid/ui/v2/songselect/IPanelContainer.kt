package com.osudroid.ui.v2.songselect

/**
 * Interface representing a container for panels in the song selection UI.
 */
interface IPanelContainer<T : Any> {

    /**
     * Indicates the currently selected panel.
     */
    var selectedPanel: T?

}