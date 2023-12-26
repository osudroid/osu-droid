package com.edlplan.ui.fragment

import android.animation.Animator
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.edlplan.framework.easing.Easing
import com.edlplan.framework.math.FMath
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.reco1l.framework.lang.uiThread
import com.reco1l.legacy.Multiplayer
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.menu.ModMenu
import ru.nsu.ccfit.zuev.osuplus.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class InGameSettingMenu : BaseFragment() {
    private lateinit var speedModifyRow: View
    private lateinit var speedModifyBar: SeekBar
    private lateinit var speedModifyText: TextView
    private lateinit var speedModifyToggle: CheckBox

    private lateinit var followDelayRow: View
    private lateinit var followDelayBar: SeekBar
    private lateinit var followDelayText: TextView

    private lateinit var forceDifficultyStatisticsSplitter: View

    private lateinit var customARLayout: RelativeLayout
    private lateinit var customARToggle: CheckBox
    private lateinit var customARText: TextView
    private lateinit var customARBar: SeekBar

    private lateinit var customODLayout: RelativeLayout
    private lateinit var customODToggle: CheckBox
    private lateinit var customODText: TextView
    private lateinit var customODBar: SeekBar

    private lateinit var customCSLayout: RelativeLayout
    private lateinit var customCSToggle: CheckBox
    private lateinit var customCSText: TextView
    private lateinit var customCSBar: SeekBar

    private lateinit var customHPLayout: RelativeLayout
    private lateinit var customHPToggle: CheckBox
    private lateinit var customHPText: TextView
    private lateinit var customHPBar: SeekBar

    override val layoutID: Int
        get() = R.layout.fragment_in_game_option

    override fun onLoadView() {
        reload(load())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("speedModifyRow", speedModifyRow.visibility)
        outState.putInt("speedModifyBar", speedModifyBar.progress)
        outState.putString("speedModifyText", speedModifyText.text.toString())
        outState.putBoolean("speedModifyToggle", speedModifyToggle.isChecked)

        outState.putInt("followDelayRow", followDelayRow.visibility)
        outState.putInt("followDelayBar", followDelayBar.progress)
        outState.putString("followDelayText", followDelayText.text.toString())

        outState.putInt("forceDifficultyStatisticsSplitter", forceDifficultyStatisticsSplitter.visibility)

        outState.putInt("customARLayout", customARLayout.visibility)
        outState.putBoolean("customARToggle", customARToggle.isChecked)
        outState.putString("customARText", customARText.text.toString())
        outState.putInt("customARBar", customARBar.progress)

        outState.putInt("customODLayout", customODLayout.visibility)
        outState.putBoolean("customODToggle", customODToggle.isChecked)
        outState.putString("customODText", customODText.text.toString())
        outState.putInt("customODBar", customODBar.progress)

        outState.putInt("customCSLayout", customCSLayout.visibility)
        outState.putBoolean("customCSToggle", customCSToggle.isChecked)
        outState.putString("customCSText", customCSText.text.toString())
        outState.putInt("customCSBar", customCSBar.progress)

        outState.putInt("customHPLayout", customHPLayout.visibility)
        outState.putBoolean("customHPToggle", customHPToggle.isChecked)
        outState.putString("customHPText", customHPText.text.toString())
        outState.putInt("customHPBar", customHPBar.progress)

        outState.putBoolean("enableStoryboard", findViewById<CheckBox>(R.id.enableStoryboard)!!.isChecked)
        outState.putBoolean("showScoreboard", findViewById<CheckBox>(R.id.showScoreboard)!!.isChecked)
        outState.putBoolean("enableVideo", findViewById<CheckBox>(R.id.enableVideo)!!.isChecked)
        outState.putBoolean("enableNCwhenSpeedChange", findViewById<CheckBox>(R.id.enableNCwhenSpeedChange)!!.isChecked)
        outState.putInt("backgroundBrightness", findViewById<SeekBar>(R.id.backgroundBrightnessBar)!!.progress)
    }

    private fun reload(state: SavedState?) {
        if (state != null) {
            this.setInitialSavedState(state)
        }

        val showMore = findViewById<View>(R.id.showMoreButton) ?: return
        showMore.setOnTouchListener { v, event ->
            if (event.action == TouchEvent.ACTION_DOWN) {
                v.animate().cancel()
                v.animate().scaleX(0.9f).scaleY(0.9f).translationY(v.height * 0.1f).setDuration(100)
                    .start()
                toggleSettingPanel()
                return@setOnTouchListener true
            } else if (event.action == TouchEvent.ACTION_UP) {
                v.animate().cancel()
                v.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(100).start()
                return@setOnTouchListener true
            }
            false
        }

        speedModifyRow = findViewById(R.id.speed_modify)!!
        followDelayRow = findViewById(R.id.follow_delay_row)!!

        forceDifficultyStatisticsSplitter = findViewById(R.id.force_diffstat_split_view)!!

        customARLayout = findViewById(R.id.custom_ar_layout)!!
        customARBar = findViewById(R.id.custom_ar_bar)!!
        customARText = findViewById(R.id.custom_ar_text)!!
        customARToggle = findViewById(R.id.custom_ar_toggle)!!

        customODLayout = findViewById(R.id.custom_od_layout)!!
        customODBar = findViewById(R.id.custom_od_bar)!!
        customODText = findViewById(R.id.custom_od_text)!!
        customODToggle = findViewById(R.id.custom_od_toggle)!!

        customCSLayout = findViewById(R.id.custom_cs_layout)!!
        customCSBar = findViewById(R.id.custom_cs_bar)!!
        customCSText = findViewById(R.id.custom_cs_text)!!
        customCSToggle = findViewById(R.id.custom_cs_toggle)!!

        customHPLayout = findViewById(R.id.custom_hp_layout)!!
        customHPBar = findViewById(R.id.custom_hp_bar)!!
        customHPText = findViewById(R.id.custom_hp_text)!!
        customHPToggle = findViewById(R.id.custom_hp_toggle)!!

        findViewById<RelativeLayout>(R.id.frg_background)!!.isClickable = false

        val enableStoryboard = findViewById<CheckBox>(R.id.enableStoryboard)!!
        enableStoryboard.isChecked = Config.isEnableStoryboard()
        enableStoryboard.setOnCheckedChangeListener { _, isChecked ->
            Config.setEnableStoryboard(isChecked)
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean("enableStoryboard", isChecked).commit()
        }

        val showScoreboard = findViewById<CheckBox>(R.id.showScoreboard)!!
        showScoreboard.isChecked = Config.isShowScoreboard()
        showScoreboard.setOnCheckedChangeListener { _, isChecked ->
            Config.setShowScoreboard(isChecked)
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean("showscoreboard", isChecked).commit()
        }

        val enableVideo = findViewById<CheckBox>(R.id.enableVideo)!!
        enableVideo.isChecked = Config.isVideoEnabled()
        enableVideo.setOnCheckedChangeListener { _, isChecked ->
            Config.setVideoEnabled(isChecked)
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean("enableVideo", isChecked).commit()
        }

        val enableNCWhenSpeedChange = findViewById<CheckBox>(R.id.enableNCwhenSpeedChange)!!
        enableNCWhenSpeedChange.isChecked = ModMenu.getInstance().isEnableNCWhenSpeedChange
        enableNCWhenSpeedChange.setOnCheckedChangeListener { _, isChecked ->
            ModMenu.getInstance().isEnableNCWhenSpeedChange = isChecked
        }

        speedModifyText = findViewById(R.id.changeSpeedText)!!

        speedModifyToggle = findViewById(R.id.enableSpeedChange)!!
        speedModifyToggle.isChecked = ModMenu.getInstance().changeSpeed != 1f
        speedModifyToggle.setOnCheckedChangeListener { _, isChecked ->
            speedModifyBar.isEnabled = isChecked
            if (!isChecked) {
                ModMenu.getInstance().changeSpeed = 1f
                speedModifyBar.progress = 10
                speedModifyText.text = String.format(Locale.getDefault(), "%.2fx", ModMenu.getInstance().changeSpeed)
                ModMenu.getInstance().updateMultiplierText()
            }
        }

        val backgroundBrightness = findViewById<SeekBar>(R.id.backgroundBrightnessBar)!!
        backgroundBrightness.progress = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt("bgbrightness", 25)
        backgroundBrightness.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    findViewById<TextView>(R.id.brightPreviewText)!!.text = progress.toString()
                    findViewById<TextView>(R.id.bgBrightnessText)!!.text = "$progress%"
                    val clamped = FMath.clamp(255 * (progress / 100f), 0f, 255f).roundToInt()
                    findViewById<View>(R.id.brightnessPreview)!!.setBackgroundColor(
                        0xFF shl 24 or (clamped shl 16) or (clamped shl 8) or clamped
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    findViewById<RelativeLayout>(R.id.brightnessPreviewLayout)!!.visibility =
                        View.VISIBLE
                    val progress = seekBar!!.progress
                    findViewById<TextView>(R.id.brightPreviewText)!!.text = progress.toString()
                    findViewById<TextView>(R.id.bgBrightnessText)!!.text = "$progress%"
                    val clamped = FMath.clamp(255 * (progress / 100f), 0f, 255f).roundToInt()
                    findViewById<View>(R.id.brightnessPreview)!!.setBackgroundColor(
                        0xFF shl 24 or (clamped shl 16) or (clamped shl 8) or clamped
                    )
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    findViewById<RelativeLayout>(R.id.brightnessPreviewLayout)!!.visibility =
                        View.GONE
                    val progress = seekBar!!.progress
                    findViewById<TextView>(R.id.bgBrightnessText)!!.text = "$progress%"
                    Config.setBackgroundBrightness(seekBar.progress / 100f)
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putInt("bgbrightness", progress).commit()
                }
            }
        )

        findViewById<TextView>(R.id.bgBrightnessText)!!.text = "${
            PreferenceManager.getDefaultSharedPreferences(context).getInt("bgbrightness", 25)
        }%"

        speedModifyBar = findViewById(R.id.changeSpeedBar)!!
        speedModifyBar.progress = (ModMenu.getInstance().changeSpeed * 20 - 10).toInt()
        speedModifyBar.isEnabled = speedModifyToggle.isChecked
        speedModifyBar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val p = 0.5f + 0.05f * progress
                    speedModifyText.text = String.format(Locale.getDefault(), "%.2fx", p)
                    ModMenu.getInstance().changeSpeed = p
                    ModMenu.getInstance().updateMultiplierText()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    val progress = seekBar!!.progress
                    val p = 0.5f + 0.05f * progress
                    speedModifyText.text = String.format(Locale.getDefault(), "%.2fx", p)
                    ModMenu.getInstance().changeSpeed = p
                    ModMenu.getInstance().updateMultiplierText()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val progress = seekBar!!.progress
                    val p = 0.5f + 0.05f * progress
                    speedModifyText.text = String.format(Locale.getDefault(), "%.2fx", p)
                    ModMenu.getInstance().changeSpeed = p
                    ModMenu.getInstance().updateMultiplierText()
                }
            }
        )

        speedModifyText.text =
            String.format(Locale.getDefault(), "%.2fx", ModMenu.getInstance().changeSpeed)

        followDelayText = findViewById(R.id.flashlightFollowDelayText)!!
        followDelayBar = findViewById(R.id.flashlightFollowDelayBar)!!
        followDelayBar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {

                var containsFlashlight = false

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (!containsFlashlight) return

                    ModMenu.getInstance().fLfollowDelay =
                        ((progress * ModMenu.DEFAULT_FL_FOLLOW_DELAY).roundToInt()).toFloat() // (progress * 1200f / (10f * 1000f)).roundToInt().toFloat()
                    followDelayText.text = "${progress * FlashLightEntity.defaultMoveDelayMS}ms"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    containsFlashlight = ModMenu.getInstance().mod.contains(GameMod.MOD_FLASHLIGHT)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (containsFlashlight) return

                    seekBar!!.progress = 0
                    ModMenu.getInstance().resetFLFollowDelay()
                    followDelayText.text =
                        "${(ModMenu.getInstance().fLfollowDelay * 1000f).toInt()}ms"
                }
            }
        )

        initializeDifficultyAdjustViews()
        updateVisibility()
    }

    private fun initializeDifficultyAdjustViews() {
        customARBar.max = 125
        customODBar.max = 110
        customCSBar.max = 150
        customHPBar.max = 110

        customARToggle.setOnCheckedChangeListener(null)
        customODToggle.setOnCheckedChangeListener(null)
        customCSToggle.setOnCheckedChangeListener(null)
        customHPToggle.setOnCheckedChangeListener(null)

        updateDifficultyAdjustValues()

        customARToggle.setOnCheckedChangeListener { _, isChecked ->
            ModMenu.getInstance().customAR = if (isChecked) customARBar.progress / 10f else null
            customARBar.isEnabled = isChecked

            updateDifficultyAdjustValues()
        }

        customODToggle.setOnCheckedChangeListener { _, isChecked ->
            ModMenu.getInstance().customOD = if (isChecked) customODBar.progress / 10f else null
            customODBar.isEnabled = isChecked

            updateDifficultyAdjustValues()
        }

        customCSToggle.setOnCheckedChangeListener { _, isChecked ->
            ModMenu.getInstance().customCS = if (isChecked) customCSBar.progress / 10f else null
            customCSBar.isEnabled = isChecked

            updateDifficultyAdjustValues()
        }

        customHPToggle.setOnCheckedChangeListener { _, isChecked ->
            ModMenu.getInstance().customHP = if (isChecked) customHPBar.progress / 10f else null
            customHPBar.isEnabled = isChecked

            updateDifficultyAdjustValues()
        }

        customARBar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) ModMenu.getInstance().customAR = progress / 10f
                    customARText.text = "${progress / 10f}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    ModMenu.getInstance().updateMultiplierText()
                }
            }
        )

        customODBar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) ModMenu.getInstance().customOD = progress / 10f
                    customODText.text = "${progress / 10f}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    ModMenu.getInstance().updateMultiplierText()
                }
            }
        )

        customCSBar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) ModMenu.getInstance().customCS = progress / 10f
                    customCSText.text = "${progress / 10f}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    ModMenu.getInstance().updateMultiplierText()
                }
            }
        )

        customHPBar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) ModMenu.getInstance().customHP = progress / 10f
                    customHPText.text = "${progress / 10f}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    ModMenu.getInstance().updateMultiplierText()
                }
            }
        )
    }

    private fun updateDifficultyAdjustValues() {
        val track = GlobalManager.getInstance().selectedTrack
        var visibility = View.VISIBLE

        if (Multiplayer.room != null) {
            val settings = Multiplayer.room!!.gameplaySettings

            if (!Multiplayer.isRoomHost && (!settings.isFreeMod || !settings.allowForceDifficultyStatistics)) {
                visibility = View.GONE
            }
        }

        forceDifficultyStatisticsSplitter.visibility = visibility

        val customAR = ModMenu.getInstance().customAR
        customARLayout.visibility = visibility
        customARToggle.isChecked = customAR != null
        customARBar.isEnabled = customAR != null
        customARBar.progress = (((customAR ?: track?.approachRate) ?: 10f) * 10).toInt()
        customARText.text = "${customARBar.progress / 10f}"

        val customOD = ModMenu.getInstance().customOD
        customODLayout.visibility = visibility
        customODToggle.isChecked = customOD != null
        customODBar.isEnabled = customOD != null
        customODBar.progress = (((customOD ?: track?.overallDifficulty) ?: 10f) * 10).toInt()
        customODText.text = "${customODBar.progress / 10f}"

        val customCS = ModMenu.getInstance().customCS
        customCSLayout.visibility = visibility
        customCSToggle.isChecked = customCS != null
        customCSBar.isEnabled = customCS != null
        customCSBar.progress = (((customCS ?: track?.circleSize) ?: 10f) * 10).toInt()
        customCSText.text = "${customCSBar.progress / 10f}"

        val customHP = ModMenu.getInstance().customHP
        customHPLayout.visibility = visibility
        customHPToggle.isChecked = customHP != null
        customHPBar.isEnabled = customHP != null
        customHPBar.progress = (((customHP ?: track?.hpDrain) ?: 10f) * 10).toInt()
        customHPText.text = "${customHPBar.progress / 10f}"

        ModMenu.getInstance().updateMultiplierText()
    }

    override fun dismiss() {
        super.dismiss()
        uiThread { super.save() }
        ModMenu.getInstance().hideByFrag()
    }

    private fun isSettingPanelShow(): Boolean {
        return abs(findViewById<LinearLayout>(R.id.fullLayout)?.translationY ?: 0f) < 10
    }

    private fun updateVisibility() {
        val flFollowDelay = ModMenu.getInstance().fLfollowDelay
        followDelayRow.visibility =
            if (ModMenu.getInstance().mod.contains(GameMod.MOD_FLASHLIGHT)) View.VISIBLE else View.GONE
        followDelayBar.progress =
            (flFollowDelay * 1000f / FlashLightEntity.defaultMoveDelayMS).toInt()
        followDelayText.text = "${(flFollowDelay * 1000f).toInt()}ms"

        if (Multiplayer.isMultiplayer) {
            speedModifyRow.visibility = if (Multiplayer.isRoomHost) View.VISIBLE else View.GONE
        }
    }

    private fun toggleSettingPanel() {
        updateVisibility()

        val background = findViewById<RelativeLayout>(R.id.frg_background)!!
        if (isSettingPanelShow()) {
            playHidePanelAnim()
            background.setOnTouchListener(null)
            background.isClickable = false
        } else {
            playShowPanelAnim()
            background.setOnTouchListener { _, event ->
                if (event.action == TouchEvent.ACTION_DOWN && isSettingPanelShow()) {
                    toggleSettingPanel()
                    return@setOnTouchListener true
                }
                false
            }
            background.isClickable = true
        }
    }

    private fun playShowPanelAnim() {
        val layout = findViewById<View>(R.id.fullLayout)
        layout?.animate()?.cancel()
        layout?.animate()
            ?.translationY(0f)
            ?.setDuration(200)
            ?.setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            ?.setListener(
                object : BaseAnimationListener() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        val background = findViewById<RelativeLayout>(R.id.frg_background)!!
                        background.isClickable = true
                        background.setOnClickListener {
                            playShowPanelAnim()
                        }
                    }
                }
            )
            ?.start()
    }

    private fun playHidePanelAnim() {
        val layout = findViewById<View>(R.id.fullLayout)
        layout?.animate()?.cancel()
        layout?.animate()
            ?.translationY(findViewById<LinearLayout>(R.id.optionBody)!!.height.toFloat())
            ?.setDuration(200)
            ?.setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            ?.setListener(
                object : BaseAnimationListener() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        findViewById<RelativeLayout>(R.id.frg_background)!!.isClickable = false
                    }
                }
            )
            ?.start()
    }

//    companion object {
//        val instance: InGameSettingMenu by lazy(LazyThreadSafetyMode.PUBLICATION) { InGameSettingMenu() }
//    }
}