package com.osudroid.ui.v2

import android.text.InputType
import androidx.annotation.IdRes
import com.edlplan.framework.easing.Easing
import com.osudroid.utils.mainThread
import com.osudroid.utils.updateThread
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.box
import com.reco1l.andengine.circle
import com.reco1l.andengine.component.scaleCenter
import com.reco1l.andengine.component.setText
import com.reco1l.andengine.container.FlexDirection
import com.reco1l.andengine.container.JustifyContent
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.flexContainer
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.modifier.ModifierType
import com.reco1l.andengine.modifier.UniversalModifier
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.shape.UICircle
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.textButton
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.ui.UICheckbox
import com.reco1l.andengine.ui.UIModal
import com.reco1l.andengine.ui.UITextButton
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import com.reco1l.osu.ui.PromptDialog
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.math.Interpolation
import kotlin.math.abs
import kotlin.math.roundToInt
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R.string

object CalibrationScene : UIModal(
    card = UIContainer().apply {
        width = FillParent
        height = FillParent
        anchor = Anchor.TopLeft
        origin = Anchor.TopLeft
        clipToBounds = false
        scaleCenter = Anchor.Center
    }
) {
    private const val APPROACH_SCALE_START = 3f
    private const val CIRCLE_DIAMETER = 130f
    private const val STEP_MS = 1
    private const val STEP_MS_LONG = 10
    private const val STEP_BPM = 1
    private const val STEP_BPM_LONG = 10
    private const val OFFSET_MIN = -500
    private const val OFFSET_MAX = 500
    private const val MAX_TAP_SAMPLES = 20

    private var currentBpm = 60

    private val beatInterval
        get() = 60f / currentBpm

    private var beatTimer = 0f
    private var metronomeTime = 0.0
    private var pendingOffset = 0

    private val judgementHitWindow = DroidHitWindow(10f)
    private val tapOffsets = mutableListOf<Double>()
    private var wasMusicPlaying = false
    private var successfulTapStreak = 0

    /**
     * Invoked on the main thread after the overlay closes (Back or SET).
     * Set by the caller so it can re-show itself (e.g. Settings → Audio).
     */
    internal var onClosed: (() -> Unit)? = null

    private lateinit var approachCircle: UICircle
    private lateinit var hitCircleFill: UICircle
    private lateinit var rippleCircle: UICircle
    private var circleContainer: UIContainer
    private lateinit var bpmValueText: UITextButton
    private lateinit var offsetValueText: UITextButton
    private lateinit var tapFeedbackText: UIText
    private lateinit var judgementText: UIText
    private lateinit var streakText: UIText
    private lateinit var highPrecisionToggle: UICheckbox

    init {
        staticBackdrop = true

        background = UIBox().apply {
            applyTheme = { color = it.accentColor * 0.08f }
        }

        card.setScale(1f)
        card.background = null

        ResourceManager.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png")

        val tapAreaSize = CIRCLE_DIAMETER * APPROACH_SCALE_START

        card.apply {
            textButton {
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                translationX = 60f
                translationY = 12f
                text = "Back"
                leadingIcon = UISprite().apply {
                    textureRegion = ResourceManager.getInstance().getTexture("back-arrow")
                    width = 28f
                    height = 28f
                }
                onActionUp = {
                    playShortClickConfirmSound()
                    hide()
                }
                onActionCancel = { playShortClickSound() }
            }

            circleContainer = object : UIContainer() {
                override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                    if (event.isActionDown) {
                        val cx = tapAreaSize / 2f
                        val cy = tapAreaSize / 2f
                        val dx = localX - cx
                        val dy = localY - cy

                        if (dx * dx + dy * dy <= cx * cx) {
                            onPlayfieldTap()
                        }
                    }

                    return true
                }
            }.apply {
                width = CIRCLE_DIAMETER
                height = CIRCLE_DIAMETER
                anchor = Anchor.Center
                origin = Anchor.Center

                circle {
                    width = FillParent
                    height = FillParent
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    applyTheme = { color = it.accentColor * 0.35f }
                    hitCircleFill = this
                }

                circle {
                    width = FillParent
                    height = FillParent
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    paintStyle = PaintStyle.Outline
                    lineWidth = 8f
                    applyTheme = { color = it.accentColor }
                }

                circle {
                    width = FillParent
                    height = FillParent
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    paintStyle = PaintStyle.Outline
                    lineWidth = 6f
                    scaleCenter = Anchor.Center
                    setScale(APPROACH_SCALE_START)
                    applyTheme = { color = it.accentColor * 0.85f }
                    approachCircle = this
                }

                circle {
                    width = FillParent
                    height = FillParent
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    paintStyle = PaintStyle.Outline
                    lineWidth = 6f
                    scaleCenter = Anchor.Center
                    alpha = 0f
                    applyTheme = { color = it.accentColor }
                    rippleCircle = this
                }
            }
            +circleContainer

            text {
                font = ResourceManager.getInstance().getFont("smallFont")
                alignment = Anchor.Center
                anchor = Anchor.Center
                origin = Anchor.BottomCenter
                translationY = -(CIRCLE_DIAMETER / 2 + 20f)
                scaleCenter = Anchor.Center
                alpha = 0f
                judgementText = this
            }

            text {
                font = ResourceManager.getInstance().getFont("smallFont")
                alignment = Anchor.Center
                anchor = Anchor.Center
                origin = Anchor.TopCenter
                translationY = CIRCLE_DIAMETER / 2 + 24f
                tapFeedbackText = this
            }

            text {
                font = ResourceManager.getInstance().getFont("smallFont")
                alignment = Anchor.Center
                anchor = Anchor.Center
                origin = Anchor.TopCenter
                translationY = CIRCLE_DIAMETER / 2 + 56f
                scaleCenter = Anchor.Center
                streakText = this
            }

            text {
                setText(string.opt_offset_calibration_tap_hint)
                font = ResourceManager.getInstance().getFont("smallFont")
                alignment = Anchor.BottomCenter
                anchor = Anchor.BottomCenter
                origin = Anchor.BottomCenter
                translationY = -28f
                applyTheme = { color = it.accentColor * 0.45f }
            }

            flexContainer {
                width = 300f
                height = MatchContent
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
                translationX = -20f
                direction = FlexDirection.Column
                justifyContent = JustifyContent.Center
                gap = 20f
                padding = Vec4(24f)

                background = UIBox().apply {
                    cornerRadius = 16f
                    applyTheme = { color = it.accentColor * 0.15f }
                }

                text {
                    setText(string.opt_offset_calibration_calibration)
                    font = ResourceManager.getInstance().getFont("smallFont")
                    alignment = Anchor.TopCenter
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    applyTheme = { color = it.accentColor }
                }

                box {
                    width = FillParent
                    height = 1f
                    applyTheme = { color = it.accentColor * 0.2f }
                }

                text {
                    text = "BPM"
                    font = ResourceManager.getInstance().getFont("smallFont")
                    alignment = Anchor.TopCenter
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    applyTheme = { color = it.accentColor * 0.6f }
                }

                linearContainer {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    spacing = 12f

                    textButton {
                        leadingIcon = FontAwesomeIcon(Icon.CaretDown).apply {
                            applyTheme = { color = it.accentColor }
                        }
                        onActionUp = { changeBpm(-STEP_BPM) }
                        onActionLongPress = { changeBpm(-STEP_BPM_LONG) }
                    }

                    textButton {
                        text = currentBpm.toString()
                        alignment = Anchor.Center
                        minWidth = 110f
                        background = UIBox().apply {
                            cornerRadius = 8f
                            applyTheme = { color = it.accentColor * 0.12f }
                        }
                        applyTheme = { color = it.accentColor }
                        onActionUp = { showBpmInputDialog() }
                        bpmValueText = this
                    }

                    textButton {
                        leadingIcon = FontAwesomeIcon(Icon.CaretUp).apply {
                            applyTheme = { color = it.accentColor }
                        }
                        onActionUp = { changeBpm(STEP_BPM) }
                        onActionLongPress = { changeBpm(STEP_BPM_LONG) }
                    }
                }

                box {
                    width = FillParent
                    height = 1f
                    applyTheme = { color = it.accentColor * 0.2f }
                }

                text {
                    setText(string.opt_offset_calibration_offset)
                    font = ResourceManager.getInstance().getFont("smallFont")
                    alignment = Anchor.TopCenter
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    applyTheme = { color = it.accentColor * 0.6f }
                }

                linearContainer {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    spacing = 12f

                    textButton {
                        leadingIcon = FontAwesomeIcon(Icon.CaretDown).apply {
                            applyTheme = { color = it.accentColor }
                        }
                        onActionUp = { changeOffset(-STEP_MS) }
                        onActionLongPress = { changeOffset(-STEP_MS_LONG) }
                    }

                    textButton {
                        text = formatOffset(pendingOffset)
                        alignment = Anchor.Center
                        minWidth = 110f
                        background = UIBox().apply {
                            cornerRadius = 8f
                            applyTheme = { color = it.accentColor * 0.12f }
                        }
                        applyTheme = { color = it.accentColor }
                        onActionUp = { showOffsetInputDialog() }
                        offsetValueText = this
                    }

                    textButton {
                        leadingIcon = FontAwesomeIcon(Icon.CaretUp).apply {
                            applyTheme = { color = it.accentColor }
                        }
                        onActionUp = { changeOffset(STEP_MS) }
                        onActionLongPress = { changeOffset(STEP_MS_LONG) }
                    }
                }

                textButton {
                    setText(string.opt_offset_calibration_set)
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    onActionUp = {
                        playShortClickConfirmSound()
                        applyOffset()
                        hide()
                    }
                    onActionCancel = { playShortClickSound() }
                }

                textButton {
                    setText(string.opt_offset_calibration_reset)
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    applyTheme = {}
                    color = Color4(0xFFFFBFBF)
                    background?.color = Color4(0xFF342121)
                    onActionUp = {
                        playShortClickSound()
                        pendingOffset = 0
                        successfulTapStreak = 0
                        tapOffsets.clear()
                        updateOffsetDisplay()
                        updateTapFeedback()
                        updateStreakDisplay()
                    }
                    onActionCancel = { playShortClickSound() }
                }

            }

            flexContainer {
                width = 400f
                height = MatchContent
                anchor = Anchor.BottomLeft
                origin = Anchor.BottomLeft
                translationX = 20f
                translationY = -20f
                direction = FlexDirection.Column
                justifyContent = JustifyContent.Center
                gap = 8f
                padding = Vec4(14f)

                background = UIBox().apply {
                    cornerRadius = 16f
                    applyTheme = { color = it.accentColor * 0.15f }
                }

                // Header
                text {
                    setText(string.opt_offset_calibration_settings)
                    font = ResourceManager.getInstance().getFont("smallFont")
                    alignment = Anchor.TopCenter
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    applyTheme = { color = it.accentColor }
                }

                // Divider
                box {
                    width = FillParent
                    height = 1f
                    applyTheme = { color = it.accentColor * 0.2f }
                }

                // Setting row: label (left) + ON/OFF toggle (right)
                linearContainer {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.TopLeft
                    origin = Anchor.TopLeft
                    spacing = 8f
                    width = FillParent

                    text {
                        setText(string.opt_highPrecisionInput_title)
                        font = ResourceManager.getInstance().getFont("smallFont")
                        alignment = Anchor.CenterLeft
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        applyTheme = { color = it.accentColor * 0.9f }
                    }

                    highPrecisionToggle = object : UICheckbox(Config.isHighPrecisionInput()) {
                        override fun onValueChanged() {
                            super.onValueChanged()

                            Config.setBoolean("highPrecisionInput", value)
                        }
                    }
                    +highPrecisionToggle
                }

                // Description below the row
                text {
                    setText(string.opt_highPrecisionInput_summary)
                    font = ResourceManager.getInstance().getFont("smallFont")
                    width = FillParent
                    clipToBounds = true
                    alignment = Anchor.TopLeft
                    anchor = Anchor.TopLeft
                    origin = Anchor.TopLeft
                    applyTheme = { color = it.accentColor * 0.5f }
                }
            }
        }
    }

    /** No scale pop for a full-screen overlay – just a clean fade in. */
    override fun createShowAnimation(): () -> UniversalModifier = {
        card.setScale(1f)
        // Backdrop is already opaque (set in onShow); only the card fades in.
        card.fadeTo(1f, 0.2f)
    }

    override fun createHideAnimation(): () -> UniversalModifier = {
        // Only the card fades out; the opaque backdrop stays solid until onHidden.
        card.fadeTo(0f, 0.15f)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        super.onManagedUpdate(deltaTimeSec)

        if (!isVisible) {
            return
        }

        metronomeTime += deltaTimeSec.toDouble()
        beatTimer += deltaTimeSec

        val progress = (beatTimer / beatInterval).coerceIn(0f, 1f)
        approachCircle.setScale(Interpolation.linear(APPROACH_SCALE_START, 1f, progress))

        while (beatTimer >= beatInterval) {
            beatTimer -= beatInterval
            onBeat()
        }
    }

    override fun onShow() {
        pendingOffset = Config.getOffset().toInt()
        beatTimer = 0f
        metronomeTime = 0.0
        successfulTapStreak = 0
        tapOffsets.clear()
        updateOffsetDisplay()
        updateTapFeedback()

        if (::highPrecisionToggle.isInitialized) {
            highPrecisionToggle.value = Config.isHighPrecisionInput()
        }

        val songService = GlobalManager.getInstance().songService
        wasMusicPlaying = songService.status == Status.PLAYING

        if (wasMusicPlaying) {
            songService.pause()
        }

        // Make the backdrop INSTANTLY opaque so the main menu is never visible.
        // The card starts invisible and fades in via createShowAnimation.
        alpha = 1f
        card.alpha = 0f

        super.onShow() // attaches to the top-most scene if not already attached
    }

    // We re-show the settings fragment here so it appears on the Android UI layer
    // instantly, while the card quietly fades out behind it in the OpenGL layer.
    override fun onHide() {
        val callback = onClosed
        onClosed = null

        if (wasMusicPlaying) {
            wasMusicPlaying = false
            GlobalManager.getInstance().songService.play()
        }

        mainThread { callback?.invoke() }
    }

    override fun onHidden() {
        super.onHidden()
        // Reset modal alpha to 0 so the next onShow() can make the backdrop
        // opaque again from a clean state.
        alpha = 0f
    }

    // -------------------------------------------------------------------------
    // Beat / animation
    // -------------------------------------------------------------------------

    private fun onBeat() {
        ResourceManager.getInstance().getSound("drum-hitclap")?.play()

        hitCircleFill.apply {
            clearModifiers(ModifierType.Alpha)
            alpha = 0.7f
            fadeTo(0.35f, beatInterval * 0.5f, Easing.Out)
        }
    }

    // -------------------------------------------------------------------------
    // Tap calibration
    // -------------------------------------------------------------------------

    private fun onPlayfieldTap() {
        val beatMs = beatInterval * 1000
        val currentMs = metronomeTime * 1000
        val phase = currentMs % beatMs
        val error = if (phase <= beatMs / 2) phase else phase - beatMs
        val absErr = abs(error).roundToInt()

        tapOffsets.add(error)

        if (tapOffsets.size > MAX_TAP_SAMPLES) {
            tapOffsets.removeAt(0)
        }

        pendingOffset = tapOffsets.average().roundToInt().coerceIn(OFFSET_MIN, OFFSET_MAX)

        val judgement = when {
            absErr < judgementHitWindow.greatWindow -> Judgement.PERFECT
            absErr < judgementHitWindow.okWindow -> Judgement.GOOD
            absErr < judgementHitWindow.mehWindow -> Judgement.MEH
            else -> Judgement.MISS
        }

        when (judgement) {
            Judgement.PERFECT, Judgement.GOOD -> {
                successfulTapStreak++
                playShortClickConfirmSound()
            }

            else -> {
                successfulTapStreak = 0
                playShortClickSound()
            }
        }

        updateOffsetDisplay()
        updateTapFeedback(error)
        updateStreakDisplay(judgement)

        showJudgement(judgement)
        triggerRipple(judgement)
        punchCircle()
    }

    private enum class Judgement(@field:IdRes val label: Int, val color: Color4) {
        PERFECT(string.opt_offset_calibration_judgement_perfect, Color4(0xFF6ECFFF)),
        GOOD(string.opt_offset_calibration_judgement_good, Color4(0xFF64DC28)),
        MEH(string.opt_offset_calibration_judgement_meh, Color4(0xFFc8b46e)),
        MISS(string.opt_offset_calibration_judgement_miss, Color4(0xFFFF4444))
    }

    /** Pop-in judgement badge above the hit circle: scale 0.7 → 1.1 → fade out. */
    private fun showJudgement(judgement: Judgement) {
        if (!::judgementText.isInitialized) {
            return
        }

        judgementText.apply {
            clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            setText(judgement.label)
            color = judgement.color
            setScale(0.7f)
            alpha = 1f

            scaleTo(1.1f, 0.1f, Easing.Out).after {
                scaleTo(1f, 0.05f)
                fadeOut(0.45f, Easing.In)
            }
        }
    }

    /** Expanding outline ring that bursts outward and fades – colored by judgement. */
    private fun triggerRipple(judgement: Judgement) {
        if (!::rippleCircle.isInitialized) {
            return
        }

        rippleCircle.apply {
            clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            color = judgement.color
            setScale(1f)
            alpha = 0.75f

            scaleTo(1.5f, 0.2f)
            fadeOut(0.2f)
        }
    }

    /** Brief scale-up of the whole circle group – the classic osu! hit punch. */
    private fun punchCircle() {
        circleContainer.apply {
            clearModifiers(ModifierType.ScaleXY)
            setScale(1f)

            scaleTo(1.12f, 0.08f, Easing.Out).after {
                scaleTo(1f, 0.12f, Easing.In)
            }
        }
    }


    private fun updateStreakDisplay(judgement: Judgement? = null) {
        if (!::streakText.isInitialized) {
            return
        }

        streakText.apply {
            when {
                successfulTapStreak >= 10 -> {
                    text = StringTable.format(string.opt_offset_calibration_streak, successfulTapStreak)
                    color = Color4(0xFFFFAA00)

                    // tiny pulse on milestone
                    if (judgement != null && successfulTapStreak % 10 == 0) {
                        clearModifiers(ModifierType.ScaleXY)
                        scaleTo(1.3f, 0.1f, Easing.Out).after {
                            scaleTo(1f, 0.1f, Easing.In)
                        }
                    }
                }

                successfulTapStreak >= 3 -> {
                    text = StringTable.format(string.opt_offset_calibration_streak_combo, successfulTapStreak)
                    color = Color4(0xFF88FF88)
                }

                else -> text = ""
            }
        }
    }

    private fun updateTapFeedback(lastError: Double? = null) {
        if (!::tapFeedbackText.isInitialized) {
            return
        }

        if (lastError != null) {
            val absErr = abs(lastError).roundToInt()

            val label = StringTable.get(when {
                absErr < judgementHitWindow.greatWindow -> string.opt_offset_calibration_feedback_perfect
                lastError > 0 -> string.opt_offset_calibration_feedback_late
                else -> string.opt_offset_calibration_feedback_early
            })

            val detail =
                if (absErr < 16) label
                else "$label (${if (lastError > 0) "+" else ""}${lastError.roundToInt()} ms)"

            tapFeedbackText.text = "$detail  •  ${tapOffsets.size} ${StringTable.get(string.opt_offset_calibration_feedback_tap_count)}"
            tapFeedbackText.color = when {
                absErr < judgementHitWindow.greatWindow -> Color4(0xFF88FF88)
                absErr < judgementHitWindow.okWindow -> Color4(0xFFFFFF88)
                else -> Color4(0xFFFF8888)
            }
        } else {
            tapFeedbackText.text = if (tapOffsets.isNotEmpty()) "${tapOffsets.size} ${StringTable.get(string.opt_offset_calibration_feedback_tap_count)}" else ""
            tapFeedbackText.color = Color4(0xFFAAAAAA)
        }
    }

    // -------------------------------------------------------------------------
    // Offset / BPM helpers
    // -------------------------------------------------------------------------

    private fun changeOffset(delta: Int) {
        tapOffsets.clear()
        pendingOffset = (pendingOffset + delta).coerceIn(OFFSET_MIN, OFFSET_MAX)
        updateOffsetDisplay()
        updateTapFeedback()
        playShortClickSound()
    }

    private fun changeBpm(delta: Int) {
        currentBpm = (currentBpm + delta).coerceIn(30, 300)
        beatTimer = 0f
        metronomeTime = 0.0
        updateBpmDisplay()
        playShortClickSound()
    }

    private fun showOffsetInputDialog() {
        mainThread {
            PromptDialog().apply {
                setTitle(StringTable.get(com.osudroid.resources.R.string.opt_offset_title))
                setMessage(StringTable.get(com.osudroid.resources.R.string.opt_offset_summary))
                setInput(pendingOffset.toString())
                setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)

                addButton("OK") { dialog ->
                    dialog as PromptDialog

                    val newValue = dialog.input?.toIntOrNull() ?: pendingOffset
                    pendingOffset = newValue.coerceIn(OFFSET_MIN, OFFSET_MAX)

                    updateThread {
                        tapOffsets.clear()
                        updateOffsetDisplay()
                        updateTapFeedback()
                    }

                    dismiss()
                }

                addButton("Cancel") { dialog ->
                    dialog.dismiss()
                }

                show()
            }
        }
    }

    private fun showBpmInputDialog() {
        mainThread {
            PromptDialog().apply {
                setTitle("BPM")
                setInput(currentBpm.toString())
                setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)

                addButton("OK") { dialog ->
                    dialog as PromptDialog

                    val newValue = dialog.input?.toIntOrNull() ?: currentBpm

                    updateThread {
                        changeBpm(newValue - currentBpm)
                    }

                    dismiss()
                }

                addButton("Cancel") { dialog ->
                    dialog.dismiss()
                }

                show()
            }
        }
    }

    private fun updateOffsetDisplay() {
        if (::offsetValueText.isInitialized) {
            offsetValueText.text = formatOffset(pendingOffset)
        }
    }

    private fun updateBpmDisplay() {
        if (::bpmValueText.isInitialized) {
            bpmValueText.text = currentBpm.toString()
        }
    }

    private fun formatOffset(ms: Int) = when {
        ms > 0 -> "+${ms} ms"
        ms < 0 -> "$ms ms"
        else -> "0 ms"
    }

    private fun playShortClickSound() {
        ResourceManager.getInstance().getSound("click-short")?.play()
    }

    private fun playShortClickConfirmSound() {
        ResourceManager.getInstance().getSound("click-short-confirm")?.play()
    }

    private fun applyOffset() {
        Config.setOffset(pendingOffset.toFloat())
        Config.setInt("offset", pendingOffset)
    }
}
