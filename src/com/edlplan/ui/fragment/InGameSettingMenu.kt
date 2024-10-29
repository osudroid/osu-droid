package com.edlplan.ui.fragment

import android.animation.Animator
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.reco1l.osu.mainThread
import com.reco1l.osu.multiplayer.Multiplayer
import com.reco1l.toolkt.android.dp
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.menu.ModMenu
import ru.nsu.ccfit.zuev.osuplus.R
import java.util.Locale
import kotlin.math.roundToInt

class InGameSettingMenu : BaseFragment() {

    private lateinit var speedModifyRow: View
    private lateinit var speedModifyBar: SeekBar
    private lateinit var speedModifyText: TextView
    private lateinit var speedModifyToggle: CheckBox

    private lateinit var followDelayRow: View
    private lateinit var followDelayBar: SeekBar
    private lateinit var followDelayText: TextView

    private lateinit var customDifficultySection: View

    private lateinit var customARToggle: CheckBox
    private lateinit var customARText: TextView
    private lateinit var customARBar: SeekBar

    private lateinit var customODToggle: CheckBox
    private lateinit var customODText: TextView
    private lateinit var customODBar: SeekBar

    private lateinit var customCSToggle: CheckBox
    private lateinit var customCSText: TextView
    private lateinit var customCSBar: SeekBar

    private lateinit var customHPToggle: CheckBox
    private lateinit var customHPText: TextView
    private lateinit var customHPBar: SeekBar

    override val layoutID: Int
        get() = R.layout.mod_customization_fragment

    override fun onLoadView() {
        reload(load())
    }

    override fun onSaveInstanceState(outState: Bundle) = outState.run {
        super.onSaveInstanceState(this)

        putInt("speedModifyRow", speedModifyRow.visibility)
        putInt("speedModifyBar", speedModifyBar.progress)
        putString("speedModifyText", speedModifyText.text.toString())
        putBoolean("speedModifyToggle", speedModifyToggle.isChecked)

        putInt("followDelayRow", followDelayRow.visibility)
        putInt("followDelayBar", followDelayBar.progress)
        putString("followDelayText", followDelayText.text.toString())

        putInt("customDifficultyRow", customDifficultySection.visibility)

        putBoolean("customARToggle", customARToggle.isChecked)
        putString("customARText", customARText.text.toString())
        putInt("customARBar", customARBar.progress)

        putBoolean("customODToggle", customODToggle.isChecked)
        putString("customODText", customODText.text.toString())
        putInt("customODBar", customODBar.progress)

        putBoolean("customCSToggle", customCSToggle.isChecked)
        putString("customCSText", customCSText.text.toString())
        putInt("customCSBar", customCSBar.progress)

        putBoolean("customHPToggle", customHPToggle.isChecked)
        putString("customHPText", customHPText.text.toString())
        putInt("customHPBar", customHPBar.progress)

        putBoolean("enableStoryboard", findViewById<CheckBox>(R.id.enableStoryboard)!!.isChecked)
        putBoolean("showScoreboard", findViewById<CheckBox>(R.id.showScoreboard)!!.isChecked)
        putBoolean("enableVideo", findViewById<CheckBox>(R.id.enableVideo)!!.isChecked)
        putBoolean("enableNCwhenSpeedChange", findViewById<CheckBox>(R.id.enableNCwhenSpeedChange)!!.isChecked)
        putInt("backgroundBrightness", findViewById<SeekBar>(R.id.backgroundBrightnessBar)!!.progress)
    }

    private fun reload(state: SavedState?) {
        if (state != null) {
            this.setInitialSavedState(state)
        }

        findViewById<View>(R.id.showMoreButton)!!.setOnClickListener {
            toggleSettingPanel()
        }

        speedModifyRow = findViewById(R.id.speed_modify)!!
        followDelayRow = findViewById(R.id.follow_delay_row)!!

        customDifficultySection = findViewById(R.id.custom_difficulty)!!

        customARBar = findViewById(R.id.custom_ar_bar)!!
        customARText = findViewById(R.id.custom_ar_text)!!
        customARToggle = findViewById(R.id.custom_ar_toggle)!!

        customODBar = findViewById(R.id.custom_od_bar)!!
        customODText = findViewById(R.id.custom_od_text)!!
        customODToggle = findViewById(R.id.custom_od_toggle)!!

        customCSBar = findViewById(R.id.custom_cs_bar)!!
        customCSText = findViewById(R.id.custom_cs_text)!!
        customCSToggle = findViewById(R.id.custom_cs_toggle)!!

        customHPBar = findViewById(R.id.custom_hp_bar)!!
        customHPText = findViewById(R.id.custom_hp_text)!!
        customHPToggle = findViewById(R.id.custom_hp_toggle)!!

        findViewById<View>(R.id.frg_background)!!.isClickable = false

        findViewById<CheckBox>(R.id.enableStoryboard)!!.apply {
            isChecked = Config.isEnableStoryboard()
            setOnCheckedChangeListener { _, isChecked ->
                Config.setEnableStoryboard(isChecked)
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean("enableStoryboard", isChecked).commit()
            }
        }

        findViewById<CheckBox>(R.id.showScoreboard)!!.apply {
            isChecked = Config.isShowScoreboard()
            setOnCheckedChangeListener { _, isChecked ->
                Config.setShowScoreboard(isChecked)
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean("showscoreboard", isChecked).commit()
            }
        }

        findViewById<CheckBox>(R.id.enableVideo)!!.apply {
            isChecked = Config.isVideoEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                Config.setVideoEnabled(isChecked)
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean("enableVideo", isChecked).commit()
            }
        }

        findViewById<CheckBox>(R.id.enableNCwhenSpeedChange)!!.apply {
            isChecked = ModMenu.getInstance().isEnableNCWhenSpeedChange
            setOnCheckedChangeListener { _, isChecked ->
                ModMenu.getInstance().isEnableNCWhenSpeedChange = isChecked
            }
        }

        speedModifyText = findViewById(R.id.changeSpeedText)!!

        speedModifyToggle = findViewById(R.id.enableSpeedChange)!!
        speedModifyToggle.isChecked = ModMenu.getInstance().changeSpeed != 1f
        speedModifyToggle.isEnabled = speedModifyToggle.isChecked
        speedModifyToggle.setOnCheckedChangeListener { _, isChecked ->
            speedModifyToggle.isEnabled = isChecked

            if (!isChecked) {
                ModMenu.getInstance().changeSpeed = 1f
                speedModifyBar.progress = 10
                speedModifyText.text = String.format(Locale.getDefault(), "%.2fx", ModMenu.getInstance().changeSpeed)
                ModMenu.getInstance().updateMultiplierText()
            }
        }

        val backgroundBrightness = findViewById<SeekBar>(R.id.backgroundBrightnessBar)!!
        backgroundBrightness.progress = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getInt("bgbrightness", 25)
        backgroundBrightness.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    findViewById<TextView>(R.id.bgBrightnessText)!!.text = "$progress%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    findViewById<TextView>(R.id.bgBrightnessText)!!.text = "${seekBar!!.progress}%"
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val progress = seekBar!!.progress
                    findViewById<TextView>(R.id.bgBrightnessText)!!.text = "$progress%"

                    Config.setBackgroundBrightness(seekBar.progress / 100f)
                    PreferenceManager.getDefaultSharedPreferences(context!!).edit().putInt("bgbrightness", progress).commit()
                }
            }
        )

        findViewById<TextView>(R.id.bgBrightnessText)!!.text = "${
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt("bgbrightness", 25)
        }%"

        speedModifyBar = findViewById(R.id.changeSpeedBar)!!
        speedModifyBar.apply {
            progress = (ModMenu.getInstance().changeSpeed * 20 - 10).toInt()

            setOnSeekBarChangeListener(
                object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) = update(progress)

                    override fun onStartTrackingTouch(seekBar: SeekBar?) = update(seekBar!!.progress)

                    override fun onStopTrackingTouch(seekBar: SeekBar?) = update(seekBar!!.progress)

                    private fun update(progress: Int) {
                        val p = 0.5f + 0.05f * progress
                        speedModifyText.text = String.format(Locale.getDefault(), "%.2fx", p)
                        speedModifyToggle.isChecked = p != 1f
                        ModMenu.getInstance().changeSpeed = p
                        ModMenu.getInstance().updateMultiplierText()
                    }
                }
            )
        }

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
        val beatmapInfo = GlobalManager.getInstance().selectedBeatmap
        var visibility = View.VISIBLE

        if (Multiplayer.room != null) {
            val settings = Multiplayer.room!!.gameplaySettings

            if (!Multiplayer.isRoomHost && (!settings.isFreeMod || !settings.allowForceDifficultyStatistics)) {
                visibility = View.GONE
            }
        }

        customDifficultySection.visibility = visibility

        val customAR = ModMenu.getInstance().customAR
        customARToggle.isChecked = customAR != null
        customARBar.isEnabled = customAR != null
        customARBar.progress = (((customAR ?: beatmapInfo?.approachRate) ?: 10f) * 10).toInt()
        customARText.text = "${customARBar.progress / 10f}"

        val customOD = ModMenu.getInstance().customOD
        customODToggle.isChecked = customOD != null
        customODBar.isEnabled = customOD != null
        customODBar.progress = (((customOD ?: beatmapInfo?.overallDifficulty) ?: 10f) * 10).toInt()
        customODText.text = "${customODBar.progress / 10f}"

        val customCS = ModMenu.getInstance().customCS
        customCSToggle.isChecked = customCS != null
        customCSBar.isEnabled = customCS != null
        customCSBar.progress = (((customCS ?: beatmapInfo?.circleSize) ?: 10f) * 10).toInt()
        customCSText.text = "${customCSBar.progress / 10f}"

        val customHP = ModMenu.getInstance().customHP
        customHPToggle.isChecked = customHP != null
        customHPBar.isEnabled = customHP != null
        customHPBar.progress = (((customHP ?: beatmapInfo?.hpDrainRate) ?: 10f) * 10).toInt()
        customHPText.text = "${customHPBar.progress / 10f}"

        ModMenu.getInstance().updateMultiplierText()
    }

    override fun dismiss() {
        super.dismiss()
        mainThread { super.save() }
        ModMenu.getInstance().hideByFrag()
    }

    private fun isSettingPanelShow(): Boolean {
        return findViewById<LinearLayout>(R.id.fullLayout)!!.translationX == 0f
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

        val background = findViewById<View>(R.id.frg_background)!!
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
            ?.translationX(0f)
            ?.setDuration(200)
            ?.setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            ?.setListener(
                object : BaseAnimationListener() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        val background = findViewById<View>(R.id.frg_background)!!
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
            ?.translationX((-450f).dp)
            ?.setDuration(200)
            ?.setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            ?.setListener(
                object : BaseAnimationListener() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        findViewById<View>(R.id.frg_background)!!.isClickable = false
                    }
                }
            )
            ?.start()
    }
}