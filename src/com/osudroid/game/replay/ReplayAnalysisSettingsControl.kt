package com.osudroid.game.replay

import com.reco1l.andengine.ui.UICard
import com.reco1l.andengine.ui.form.FormCheckbox
import com.reco1l.andengine.ui.form.FormSlider
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R

/**
 * Controls the analysis overlay settings when replaying.
 */
class ReplayAnalysisSettingsControl : UICard() {
    private val showTapMarkersCheckbox = FormCheckbox().apply {
        label = StringTable.get(R.string.replay_analysis_show_tap_markers)
        onValueChanged = { onShowTapMarkersChanged?.invoke(it) }
    }

    private val showFrameMarkersCheckbox = FormCheckbox().apply {
        label = StringTable.get(R.string.replay_analysis_show_frame_markers)
        onValueChanged = { onShowFrameMarkersChanged?.invoke(it) }
    }

    private val showCursorPathCheckbox = FormCheckbox().apply {
        label = StringTable.get(R.string.replay_analysis_show_cursor_path)
        onValueChanged = { onShowCursorPathChanged?.invoke(it) }
    }

    private val hideGameplayCursorCheckbox = FormCheckbox().apply {
        label = StringTable.get(R.string.replay_analysis_hide_gameplay_cursor)
        onValueChanged = { onHideGameplayCursorChanged?.invoke(it) }
    }

    private val displayLengthSlider = FormSlider(800f).apply {
        label = StringTable.get(R.string.replay_analysis_display_length)
        control.max = 2000f
        control.min = 200f
        control.step = 200f
        valueFormatter = { "${it.roundToInt()} ms" }
        onValueChanged = { onDisplayLengthChanged?.invoke(it.roundToInt()) }
    }

    /**
     * Called when the tap markers visibility was changed.
     */
    var onShowTapMarkersChanged: ((Boolean) -> Unit)? = null

    /**
     * Called when the frame markers visibility was changed.
     */
    var onShowFrameMarkersChanged: ((Boolean) -> Unit)? = null

    /**
     * Called when the cursor path visibility was changed.
     */
    var onShowCursorPathChanged: ((Boolean) -> Unit)? = null

    /**
     * Called when the gameplay cursor visibility was changed.
     */
    var onHideGameplayCursorChanged: ((Boolean) -> Unit)? = null

    /**
     * Called when the display length was changed. The argument is the display length in milliseconds.
     */
    var onDisplayLengthChanged: ((Int) -> Unit)? = null

    init {
        width = FillParent
        title = StringTable.get(R.string.replay_analysis_title)

        content += showTapMarkersCheckbox
        content += showFrameMarkersCheckbox
        content += showCursorPathCheckbox
        content += hideGameplayCursorCheckbox
        content += displayLengthSlider
    }
}
