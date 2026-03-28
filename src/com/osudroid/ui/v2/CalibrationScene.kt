package com.osudroid.ui.v2

import android.text.InputType
import com.reco1l.osu.ui.PromptDialog
import androidx.preference.PreferenceManager
import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.*
import org.anddev.andengine.input.touch.TouchEvent
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Full-screen calibration overlay.
 *
 * Extends [UIModal] instead of [UIScene]:
 *  - Attaches as a child entity on the current scene (no setChildScene needed).
 *  - Uses the modal's built-in backdrop for the full-screen blackout.
 *  - Beat animation runs via [onManagedUpdate] like any other UI component.
 *  - show() / hide() handle the fade animation automatically.
 */
object CalibrationScene : UIModal(
    // Full-screen card – fills the whole modal area instead of being a centred popup.
    card = UIContainer().apply {
        width  = FillParent
        height = FillParent
        anchor = Anchor.TopLeft
        origin = Anchor.TopLeft
        clipToBounds  = false
        scaleCenter   = Anchor.Center
    }
) {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private val BPM_OPTIONS = intArrayOf(60, 120, 180)

    private const val APPROACH_SCALE_START = 3f
    private const val CIRCLE_DIAMETER      = 130f
    private const val STEP_MS              = 1
    private const val STEP_MS_LONG         = 10
    private const val OFFSET_MIN           = -500
    private const val OFFSET_MAX           = 500
    private const val MAX_TAP_SAMPLES      = 20
    private const val RIGHT_PANEL_WIDTH    = 300f

    // -------------------------------------------------------------------------
    // Runtime state
    // -------------------------------------------------------------------------

    private var selectedBpmIndex = 1
    private val currentBpm  get() = BPM_OPTIONS[selectedBpmIndex]
    private val beatInterval get() = 60f / currentBpm

    private var beatTimer     = 0f
    private var metronomeTime = 0.0
    private var pendingOffset = 0

    private val tapOffsets = mutableListOf<Double>()
    private var wasMusicPlaying = false
    private var streak = 0   // consecutive PERFECT/GOOD taps

    /**
     * Invoked on the main thread after the overlay closes (Back or SET).
     * Set by the caller so it can re-show itself (e.g. Settings → Audio).
     */
    internal var onClosed: (() -> Unit)? = null

    // -------------------------------------------------------------------------
    // UI node references
    // -------------------------------------------------------------------------

    private lateinit var approachCircle  : UICircle
    private lateinit var hitCircleFill   : UICircle
    private lateinit var rippleCircle    : UICircle   // expands on tap
    private lateinit var circleGroup     : UIContainer // the tappable group (for punch)
    private lateinit var offsetValueText : UITextButton
    private lateinit var tapFeedbackText : UIText
    private lateinit var judgementText   : UIText     // PERFECT / GOOD / MEH / MISS popup
    private lateinit var streakText      : UIText     // consecutive good taps
    private lateinit var bpmLabel        : UIText
    private lateinit var precisionToggle : UICheckbox    // High Precision Input toggle
    private val bpmButtons = arrayOfNulls<UITextButton>(3)

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    init {
        staticBackdrop = true   // don't dismiss by tapping the backdrop
        detachOnHide   = false  // keep attached so we can re-show later

        // Fully-opaque dark backdrop – replaces UIModal's default 30 % black.
        background = UIBox().apply {
            applyTheme = { color = it.accentColor * 0.08f }
        }

        // Reset UIModal defaults that don't suit a full-screen layout.
        card.scaleX  = 1f
        card.scaleY  = 1f
        card.background = null  // backdrop IS the background

        ResourceManager.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png")

        buildUI()
    }

    // -------------------------------------------------------------------------
    // Animation overrides
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Beat loop – driven by the parent scene's update thread via onManagedUpdate
    // -------------------------------------------------------------------------

    override fun onManagedUpdate(deltaTimeSec: Float) {
        super.onManagedUpdate(deltaTimeSec)
        if (!isVisible) return

        metronomeTime += deltaTimeSec.toDouble()
        beatTimer     += deltaTimeSec

        val progress = (beatTimer / beatInterval).coerceIn(0f, 1f)
        approachCircle.setScale(APPROACH_SCALE_START - (APPROACH_SCALE_START - 1f) * progress)

        if (beatTimer >= beatInterval) {
            beatTimer -= beatInterval
            onBeat()
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onShow() {
        pendingOffset = Config.getOffset().toInt()
        beatTimer     = 0f
        metronomeTime = 0.0
        streak        = 0
        tapOffsets.clear()
        updateOffsetDisplay()
        updateTapFeedback()
        updateBpmButtons()
        if (::precisionToggle.isInitialized) {
            precisionToggle.value = Config.isHighPrecisionInput()
        }

        val songService = GlobalManager.getInstance().songService
        wasMusicPlaying = songService != null && songService.status == Status.PLAYING
        if (wasMusicPlaying) songService!!.pause()

        // Make the backdrop INSTANTLY opaque so the main menu is never visible.
        // The card starts invisible and fades in via createShowAnimation.
        alpha      = 1f
        card.alpha = 0f

        super.onShow()  // attaches to the top-most scene if not already attached
    }

    // Called the moment hide() is triggered, BEFORE the fade-out animation starts.
    // We re-show the settings fragment here so it appears on the Android UI layer
    // instantly, while the card quietly fades out behind it in the OpenGL layer.
    override fun onHide() {
        val callback = onClosed
        onClosed = null

        if (wasMusicPlaying) {
            wasMusicPlaying = false
            GlobalManager.getInstance().songService?.play()
        }

        callback?.let { GlobalManager.getInstance().mainActivity.runOnUiThread(it) }
    }

    override fun onHidden() {
        super.onHidden()
        // Reset modal alpha to 0 so the next onShow() can make the backdrop
        // opaque again from a clean state.
        alpha = 0f
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private fun buildUI() {
        val tapAreaSize = CIRCLE_DIAMETER * APPROACH_SCALE_START

        card.apply {

            // ── BACK button – top-left ────────────────────────────────────────
            +UITextButton().apply {
                anchor       = Anchor.TopLeft
                origin       = Anchor.TopLeft
                translationX = 60f
                translationY = 12f
                text = "Back"
                leadingIcon = UISprite().apply {
                    textureRegion = ResourceManager.getInstance().getTexture("back-arrow")
                    width  = 28f
                    height = 28f
                }
                onActionUp     = { ResourceManager.getInstance().getSound("click-short-confirm")?.play(); hide() }
                onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
            }

            // ── BPM label – top-centre ────────────────────────────────────────
            bpmLabel = UIText().apply {
                text         = "${currentBpm} BPM"
                font         = ResourceManager.getInstance().getFont("smallFont")
                anchor       = Anchor.TopCenter
                origin       = Anchor.TopCenter
                translationY = 24f
                applyTheme   = { color = it.accentColor * 0.6f }
            }
            +bpmLabel

            // ── Hit-circle – centred on the full screen ───────────────────────
            circleGroup = object : UIContainer() {
                override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                    if (event.isActionDown) {
                        val cx = tapAreaSize / 2f
                        val cy = tapAreaSize / 2f
                        val dx = localX - cx
                        val dy = localY - cy
                        if (dx * dx + dy * dy <= cx * cx) onPlayfieldTap()
                    }
                    return true
                }
            }.apply {
                width  = tapAreaSize
                height = tapAreaSize
                anchor = Anchor.Center
                origin = Anchor.Center

                hitCircleFill = UICircle().apply {
                    width  = CIRCLE_DIAMETER; height = CIRCLE_DIAMETER
                    anchor = Anchor.Center;   origin = Anchor.Center
                    applyTheme = { color = it.accentColor * 0.35f }
                }
                attachChild(hitCircleFill)

                attachChild(UICircle().apply {
                    width  = CIRCLE_DIAMETER; height = CIRCLE_DIAMETER
                    anchor = Anchor.Center;   origin = Anchor.Center
                    paintStyle = PaintStyle.Outline
                    lineWidth  = 4f
                    applyTheme = { color = it.accentColor }
                })

                approachCircle = UICircle().apply {
                    width  = CIRCLE_DIAMETER; height = CIRCLE_DIAMETER
                    anchor = Anchor.Center;   origin = Anchor.Center
                    paintStyle  = PaintStyle.Outline
                    lineWidth   = 3f
                    scaleCenter = Anchor.Center
                    setScale(APPROACH_SCALE_START)
                    applyTheme  = { color = it.accentColor * 0.85f }
                }
                attachChild(approachCircle)

                // Ripple ring – expands outward and fades on each tap
                rippleCircle = UICircle().apply {
                    width       = CIRCLE_DIAMETER; height = CIRCLE_DIAMETER
                    anchor      = Anchor.Center;   origin = Anchor.Center
                    paintStyle  = PaintStyle.Outline
                    lineWidth   = 3f
                    scaleCenter = Anchor.Center
                    alpha       = 0f
                    applyTheme  = { color = it.accentColor }
                }
                attachChild(rippleCircle)
            }
            +circleGroup

            // ── Judgement popup – pops above the circle on each tap ───────────
            judgementText = UIText().apply {
                text         = ""
                font         = ResourceManager.getInstance().getFont("smallFont")
                alignment    = Anchor.Center
                anchor       = Anchor.Center
                origin       = Anchor.BottomCenter
                translationY = -(CIRCLE_DIAMETER / 2 + 20f)
                scaleCenter  = Anchor.Center
                alpha        = 0f
            }
            +judgementText

            // ── Tap-feedback – just below the circle ──────────────────────────
            tapFeedbackText = UIText().apply {
                text         = ""
                font         = ResourceManager.getInstance().getFont("smallFont")
                alignment    = Anchor.Center
                anchor       = Anchor.Center
                origin       = Anchor.TopCenter
                translationY = CIRCLE_DIAMETER / 2 + 24f
            }
            +tapFeedbackText

            // ── Streak counter – below feedback text ──────────────────────────
            streakText = UIText().apply {
                text         = ""
                font         = ResourceManager.getInstance().getFont("smallFont")
                alignment    = Anchor.Center
                anchor       = Anchor.Center
                origin       = Anchor.TopCenter
                translationY = CIRCLE_DIAMETER / 2 + 56f
                scaleCenter  = Anchor.Center
            }
            +streakText

            // ── Bottom hint ───────────────────────────────────────────────────
            +UIText().apply {
                text         = "TAP ALONG WITH THE BEAT"
                font         = ResourceManager.getInstance().getFont("smallFont")
                alignment    = Anchor.BottomCenter
                anchor       = Anchor.BottomCenter
                origin       = Anchor.BottomCenter
                translationY = -28f
                applyTheme   = { color = it.accentColor * 0.45f }
            }

            // ── Right control panel – floating card ───────────────────────────
            +UIFlexContainer().apply {
                width          = RIGHT_PANEL_WIDTH
                height         = MatchContent
                anchor         = Anchor.CenterRight
                origin         = Anchor.CenterRight
                translationX   = -20f
                direction      = FlexDirection.Column
                justifyContent = JustifyContent.Center
                gap     = 20f
                padding = Vec4(24f)

                background = UIBox().apply {
                    cornerRadius = 16f
                    applyTheme   = { color = it.accentColor * 0.15f }
                }

                +UIText().apply {
                    text      = "CALIBRATION"
                    font      = ResourceManager.getInstance().getFont("smallFont")
                    alignment = Anchor.TopCenter
                    anchor    = Anchor.TopCenter; origin = Anchor.TopCenter
                    applyTheme = { color = it.accentColor }
                }

                +UIBox().apply {
                    width = FillParent; height = 1f
                    applyTheme = { color = it.accentColor * 0.2f }
                }

                +UIText().apply {
                    text      = "BPM"
                    font      = ResourceManager.getInstance().getFont("smallFont")
                    alignment = Anchor.TopCenter
                    anchor    = Anchor.TopCenter; origin = Anchor.TopCenter
                    applyTheme = { color = it.accentColor * 0.6f }
                }

                +UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    anchor      = Anchor.TopCenter; origin = Anchor.TopCenter
                    spacing     = 8f

                    BPM_OPTIONS.forEachIndexed { index, bpm ->
                        val btn = UITextButton().apply {
                            text = "$bpm"
                            onActionUp = {
                                if (selectedBpmIndex != index) {
                                    ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                                    selectBpm(index)
                                }
                            }
                            onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                        }
                        bpmButtons[index] = btn
                        +btn
                    }
                }

                +UIBox().apply {
                    width = FillParent; height = 1f
                    applyTheme = { color = it.accentColor * 0.2f }
                }

                +UIText().apply {
                    text      = "OFFSET"
                    font      = ResourceManager.getInstance().getFont("smallFont")
                    alignment = Anchor.TopCenter
                    anchor    = Anchor.TopCenter; origin = Anchor.TopCenter
                    applyTheme = { color = it.accentColor * 0.6f }
                }

                +UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    anchor      = Anchor.TopCenter; origin = Anchor.TopCenter
                    spacing     = 12f

                    +UITextButton().apply {
                        text              = "▼"
                        onActionDown      = { changeOffset(-STEP_MS) }
                        onActionLongPress = { changeOffset(-STEP_MS_LONG) }
                        onActionCancel    = { ResourceManager.getInstance().getSound("click-short")?.play() }
                    }

                    offsetValueText = UITextButton().apply {
                        text      = formatOffset(pendingOffset)
                        alignment = Anchor.Center
                        minWidth  = 110f
                        background = UIBox().apply {
                            cornerRadius = 8f
                            applyTheme   = { color = it.accentColor * 0.12f }
                        }
                        applyTheme = { color = it.accentColor }
                        onActionUp = { showOffsetInputDialog() }
                    }
                    +offsetValueText

                    +UITextButton().apply {
                        text              = "▲"
                        onActionDown      = { changeOffset(+STEP_MS) }
                        onActionLongPress = { changeOffset(+STEP_MS_LONG) }
                        onActionCancel    = { ResourceManager.getInstance().getSound("click-short")?.play() }
                    }
                }

                +UITextButton().apply {
                    text   = "SET"
                    anchor = Anchor.TopCenter; origin = Anchor.TopCenter
                    onActionUp = {
                        ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                        applyOffset()
                        hide()
                    }
                    onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                }

                +UITextButton().apply {
                    text   = "RESET"
                    anchor = Anchor.TopCenter; origin = Anchor.TopCenter
                    applyTheme        = {}
                    color             = Color4(0xFFFFBFBF)
                    background?.color = Color4(0xFF342121)
                    onActionUp = {
                        ResourceManager.getInstance().getSound("click-short")?.play()
                        pendingOffset = 0
                        streak        = 0
                        tapOffsets.clear()
                        updateOffsetDisplay()
                        updateTapFeedback()
                        updateStreakDisplay()
                    }
                    onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                }

            }

            // ── Input Settings card – bottom-left ────────────────────────────
            +UIFlexContainer().apply {
                width          = 400f
                height         = MatchContent
                anchor         = Anchor.BottomLeft
                origin         = Anchor.BottomLeft
                translationX   = 20f
                translationY   = -20f
                direction      = FlexDirection.Column
                justifyContent = JustifyContent.Center
                gap            = 8f
                padding        = Vec4(14f)

                background = UIBox().apply {
                    cornerRadius = 16f
                    applyTheme   = { color = it.accentColor * 0.15f }
                }

                // Header
                +UIText().apply {
                    text       = "SETTINGS"
                    font       = ResourceManager.getInstance().getFont("smallFont")
                    alignment  = Anchor.TopCenter
                    anchor     = Anchor.TopCenter; origin = Anchor.TopCenter
                    applyTheme = { color = it.accentColor }
                }

                // Divider
                +UIBox().apply {
                    width      = FillParent; height = 1f
                    applyTheme = { color = it.accentColor * 0.2f }
                }

                // Setting row: label (left) + ON/OFF toggle (right)
                +UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    anchor      = Anchor.TopLeft; origin = Anchor.TopLeft
                    spacing     = 8f
                    width       = FillParent

                    +UIText().apply {
                        text       = "High Precision Input"
                        font       = ResourceManager.getInstance().getFont("smallFont")
                        alignment  = Anchor.CenterLeft
                        anchor     = Anchor.CenterLeft; origin = Anchor.CenterLeft
                        applyTheme = { color = it.accentColor * 0.9f }
                    }

                    precisionToggle = UICheckbox(Config.isHighPrecisionInput()).apply {
                        onChange = { enabled ->
                            PreferenceManager.getDefaultSharedPreferences(
                                GlobalManager.getInstance().mainActivity
                            ).edit().putBoolean("highPrecisionInput", enabled).apply()
                        }
                    }
                    +precisionToggle
                }

                // Description below the row
                +UIText().apply {
                    text       = "Uses more touch samples for\nmore accurate offset measurement."
                    font       = ResourceManager.getInstance().getFont("smallFont")
                    width      = FillParent
                    alignment  = Anchor.TopLeft
                    anchor     = Anchor.TopLeft; origin = Anchor.TopLeft
                    applyTheme = { color = it.accentColor * 0.5f }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Beat / animation
    // -------------------------------------------------------------------------

    private fun onBeat() {
        ResourceManager.getInstance().getSound("drum-hitclap")?.play()
        val flashDuration = beatInterval * 0.5f
        hitCircleFill.clearModifiers(ModifierType.Alpha)
        hitCircleFill.fadeTo(1f, 0f)
        hitCircleFill.fadeTo(0.35f, flashDuration, Easing.Out)
    }

    // -------------------------------------------------------------------------
    // Tap calibration
    // -------------------------------------------------------------------------

    private fun onPlayfieldTap() {
        val beatMs    = beatInterval * 1000.0
        val currentMs = metronomeTime * 1000.0
        val phase     = currentMs % beatMs
        val error     = if (phase <= beatMs / 2.0) phase else phase - beatMs
        val absErr    = abs(error).roundToInt()

        tapOffsets.add(error)
        if (tapOffsets.size > MAX_TAP_SAMPLES) tapOffsets.removeAt(0)

        pendingOffset = tapOffsets.average().roundToInt().coerceIn(OFFSET_MIN, OFFSET_MAX)

        // ── Classify tap accuracy (osu!-style windows) ────────────────────────
        val judgement = when {
            absErr < 16  -> Judgement.PERFECT
            absErr < 70  -> Judgement.GOOD
            absErr < 135 -> Judgement.MEH
            else         -> Judgement.MISS
        }

        // ── Streak: reset on MEH or MISS ──────────────────────────────────────
        if (judgement == Judgement.PERFECT || judgement == Judgement.GOOD) streak++
        else streak = 0

        // ── Sound: better hits → richer sound ────────────────────────────────
        val sound = when (judgement) {
            Judgement.PERFECT, Judgement.GOOD -> "click-short-confirm"
            else                              -> "click-short"
        }
        ResourceManager.getInstance().getSound(sound)?.play()

        updateOffsetDisplay()
        updateTapFeedback(error)
        updateStreakDisplay(judgement)

        // ── Visual effects ────────────────────────────────────────────────────
        showJudgement(judgement)
        triggerRipple(judgement)
        punchCircle()
    }

    // osu! hit windows mapped to calibration labels
    private enum class Judgement(val label: String, val color: Color4) {
        PERFECT("PERFECT", Color4(0xFF6ECFFF)),
        GOOD   ("GOOD",    Color4(0xFF64DC28)),
        MEH    ("MEH",     Color4(0xFFc8b46e)),
        MISS   ("MISS",    Color4(0xFFFF4444))
    }

    /** Pop-in judgement badge above the hit circle: scale 0.7 → 1.1 → fade out. */
    private fun showJudgement(j: Judgement) {
        if (!::judgementText.isInitialized) return
        judgementText.clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
        judgementText.text  = j.label
        judgementText.color = j.color
        judgementText.setScale(0.7f)
        judgementText.alpha = 1f
        judgementText.scaleTo(1.1f, 0.10f, Easing.Out).after {
            judgementText.scaleTo(1.0f, 0.05f)
            judgementText.fadeTo(0f, 0.45f, Easing.In)
        }
    }

    /** Expanding outline ring that bursts outward and fades – coloured by judgement. */
    private fun triggerRipple(j: Judgement) {
        if (!::rippleCircle.isInitialized) return
        rippleCircle.clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
        rippleCircle.color = j.color
        rippleCircle.setScale(1.0f)
        rippleCircle.alpha  = 0.75f
        rippleCircle.scaleTo(2.8f, 0.5f, Easing.Out)
        rippleCircle.fadeTo(0f,   0.5f, Easing.Out)
    }

    /** Brief scale-up of the whole circle group – the classic osu! hit punch. */
    private fun punchCircle() {
        if (!::circleGroup.isInitialized) return
        circleGroup.clearModifiers(ModifierType.ScaleXY)
        circleGroup.setScale(1.0f)
        circleGroup.scaleTo(1.12f, 0.08f, Easing.Out).after {
            circleGroup.scaleTo(1.0f, 0.12f, Easing.In)
        }
    }


    private fun updateStreakDisplay(judgement: Judgement? = null) {
        if (!::streakText.isInitialized) return
        when {
            streak >= 10 -> {
                streakText.text  = "Keep Going! $streak combo"
                streakText.color = Color4(0xFFFFAA00)
                // tiny pulse on milestone
                if (judgement != null && streak % 10 == 0) {
                    streakText.clearModifiers(ModifierType.ScaleXY)
                    streakText.scaleTo(1.3f, 0.1f, Easing.Out).after {
                        streakText.scaleTo(1.0f, 0.1f, Easing.In)
                    }
                }
            }
            streak >= 3  -> {
                streakText.text  = "$streak combo"
                streakText.color = Color4(0xFF88FF88)
            }
            else -> streakText.text = ""
        }
    }

    private fun updateTapFeedback(lastError: Double? = null) {
        if (!::tapFeedbackText.isInitialized) return
        if (lastError != null) {
            val absErr = abs(lastError).roundToInt()
            val label  = when { absErr < 16 -> "Perfect!"; lastError > 0 -> "Late"; else -> "Early" }
            val detail = if (absErr < 16) label else "$label (${if (lastError > 0) "+" else ""}${lastError.roundToInt()} ms)"
            tapFeedbackText.text  = "$detail  •  ${tapOffsets.size} taps"
            tapFeedbackText.color = when { absErr < 16 -> Color4(0xFF88FF88); absErr < 50 -> Color4(0xFFFFFF88); else -> Color4(0xFFFF8888) }
        } else {
            tapFeedbackText.text  = if (tapOffsets.isNotEmpty()) "${tapOffsets.size} taps" else ""
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
        ResourceManager.getInstance().getSound("click-short")?.play()
    }

    private fun selectBpm(index: Int) {
        selectedBpmIndex = index
        beatTimer = 0f; metronomeTime = 0.0
        tapOffsets.clear()
        updateOffsetDisplay()
        updateTapFeedback()
        if (::bpmLabel.isInitialized) bpmLabel.text = "${currentBpm} BPM"
        updateBpmButtons()
    }

    private fun updateBpmButtons() {
        bpmButtons.forEachIndexed { index, btn -> btn?.isSelected = (index == selectedBpmIndex) }
    }

    private fun showOffsetInputDialog() {
        GlobalManager.getInstance().mainActivity.runOnUiThread {
            PromptDialog().apply {
                setTitle("Global Offset")
                setMessage("Music offset in ms (positive values mean objects appear earlier)")
                setInput(pendingOffset.toString())
                setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)

                addButton("OK") { dialog ->
                    dialog as PromptDialog
                    val newValue = dialog.input?.toIntOrNull() ?: pendingOffset
                    pendingOffset = newValue.coerceIn(OFFSET_MIN, OFFSET_MAX)
                    tapOffsets.clear()
                    updateOffsetDisplay()
                    updateTapFeedback()
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
        if (::offsetValueText.isInitialized) offsetValueText.text = formatOffset(pendingOffset)
    }

    private fun formatOffset(ms: Int) = when { ms > 0 -> "+${ms} ms"; ms < 0 -> "${ms} ms"; else -> "0 ms" }

    private fun applyOffset() {
        Config.setOffset(pendingOffset.toFloat())
        PreferenceManager.getDefaultSharedPreferences(GlobalManager.getInstance().mainActivity)
            .edit().putInt("offset", pendingOffset).apply()
    }
}
