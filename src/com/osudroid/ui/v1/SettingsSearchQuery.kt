package com.osudroid.ui.v1

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The default timeout for settings search debounce.
 */
const val SEARCH_DEBOUNCE_MILLIS = 250L

/**
 * Debounces settings search text. Blank text is emitted immediately so clearing the search restores the full list
 * without waiting, and consecutive duplicate values are dropped.
 *
 * @param timeoutMillis The timeout to debounce for. Defaults to [SEARCH_DEBOUNCE_MILLIS].
 */
@OptIn(FlowPreview::class)
fun Flow<String>.debounceSearchQuery(timeoutMillis: Long = SEARCH_DEBOUNCE_MILLIS): Flow<String> =
    debounce { if (it.isBlank()) 0L else timeoutMillis }.distinctUntilChanged()
