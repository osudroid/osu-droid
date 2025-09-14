package com.osudroid.ui.v1

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.XmlRes
import androidx.core.content.getSystemService
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.updateLayoutParams
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.acivev.VibratorManager
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.fragment.LoadingFragment
import com.edlplan.ui.fragment.SettingsFragment
import com.google.android.material.snackbar.Snackbar
import com.osudroid.resources.R.string
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.RoomTeam
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.UpdateManager
import com.osudroid.utils.async
import com.osudroid.data.DatabaseManager
import com.osudroid.utils.mainThread
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.RoomScene
import com.reco1l.framework.asTimeInterpolator
import com.reco1l.osu.ui.InputPreference
import com.reco1l.osu.ui.Option
import com.reco1l.osu.ui.SelectPreference
import com.reco1l.toolkt.android.bottomMargin
import com.reco1l.toolkt.android.cornerRadius
import com.reco1l.toolkt.android.dp
import com.reco1l.toolkt.android.drawableLeft
import com.reco1l.toolkt.android.layoutWidth
import com.reco1l.toolkt.android.topMargin
import com.rian.osu.mods.ModAutoplay
import com.rian.osu.replay.ReplayImporter
import com.rian.osu.utils.ModHashMap
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ConfigBackup
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Job


class SettingsFragment : SettingsFragment() {


    private lateinit var sectionSelector: LinearLayout


    private var section = when {

        Multiplayer.isRoomHost -> Section.Room
        Multiplayer.isMultiplayer -> Section.Player

        else -> Section.General
    }


    private val replayFilePicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val loading = LoadingFragment()
        loading.show()

        async {
            val context = requireContext()
            val decorView = requireActivity().window.decorView
            var importedCount = 0
            var tempFile: File? = null

            for (uri in uris) {
                try {
                    tempFile = File.createTempFile("importedReplay", null, context.externalCacheDir)

                    context.contentResolver.openInputStream(uri)!!.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    ReplayImporter.import(tempFile)
                    importedCount++
                } catch (e: Exception) {
                    Log.e("SettingsFragment", "Failed to import replay from $uri", e)

                    mainThread {
                        Snackbar.make(
                            decorView,
                            StringTable.format(
                                string.replay_import_error,
                                uri.path ?: uri.toString(),
                                e.message ?: "Unknown error"
                            ),
                            3000
                        ).show()
                    }
                } finally {
                    tempFile?.delete()
                    tempFile = null
                }
            }

            mainThread {
                loading.dismiss()

                Snackbar.make(
                    decorView,
                    StringTable.format(string.replay_import_result, importedCount, uris.size),
                    5000
                ).show()
            }
        }
    }


    override fun onLoadView() {

        sectionSelector = findViewById(R.id.section_selector)!!
        sectionSelector.removeAllViews()

        fun createSectionButton(text: String, icon: Int, section: Section) {

            val button = TextView(ContextThemeWrapper(context, R.style.settings_tab_text))

            button.cornerRadius = 15f.dp
            button.layoutWidth = 200.dp
            button.text = text
            button.drawableLeft = requireContext().getDrawable(icon)!!
            button.drawableLeft!!.setTint(Color.WHITE)

            button.setOnClickListener {

                // Workaround to IllegalStateException being thrown when an EditText is focused
                // while trying to change section.
                root!!.findFocus()?.clearFocus()

                sectionSelector.forEach {
                    it.setBackgroundColor(if (it == button) 0xFF363653.toInt() else Color.TRANSPARENT)
                }

                this.section = section

                // Older SDKs may potentially throw an IllegalStateException when trying to change
                // preference screen, so we need to remove all views first to prevent that.
                listView.removeAllViews()
                setPreferencesFromResource(section.xml, null)
            }

            sectionSelector.addView(button)

            if (this.section == section) {
                button.callOnClick()
            }
        }


        if (Multiplayer.isMultiplayer) {

            fun createDivider(text: String) {

                val divider = TextView(ContextThemeWrapper(context, R.style.settings_tab_divider))
                divider.text = text

                sectionSelector.addView(divider)
            }

            createDivider("Multiplayer")
            createSectionButton("Player", R.drawable.person_24px, Section.Player)

            if (Multiplayer.isRoomHost)
                createSectionButton("Room", R.drawable.groups_24px, Section.Room)

            createDivider("Game")

        }


        if (!Multiplayer.isMultiplayer) {
            createSectionButton("General", R.drawable.grid_view_24px, Section.General)
        }

        createSectionButton("Gameplay", R.drawable.videogame_asset_24px, Section.Gameplay)
        createSectionButton("Graphics", R.drawable.display_settings_24px, Section.Graphics)
        createSectionButton("Audio", R.drawable.headphones_24px, Section.Audio)

        if (!Multiplayer.isMultiplayer) {
            createSectionButton("Library", R.drawable.library_music_24px, Section.Library)
        }

        createSectionButton("Input", R.drawable.trackpad_input_24px, Section.Input)
        createSectionButton("Advanced", R.drawable.manufacturing_24px, Section.Advanced)


        sectionSelector[0].topMargin = 32.dp
        sectionSelector[sectionSelector.childCount - 1].bottomMargin = 32.dp

        findViewById<View>(R.id.close)!!.setOnClickListener {
            dismiss()
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) = Unit


    // For whatever reason this is restricted API when it wasn't in previous SDKs.
    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() = when(section) {

        Section.General -> handleGeneralSectionPreferences()
        Section.Gameplay -> handleGameplaySectionPreferences()
        Section.Audio -> handleAudioSectionPreferences()
        Section.Library -> handleLibrarySectionPreferences()
        Section.Advanced -> handleAdvancedSectionPreferences()
        Section.Input -> handleInputSectionPreferences()
        Section.Player -> handlePlayerSectionPreferences()
        Section.Room -> handleRoomSectionPreferences()

        else -> Unit

    }


    override fun dismiss() {
        Config.loadConfig(requireActivity())

        if (!Multiplayer.isMultiplayer) {
            GlobalManager.getInstance().mainScene.reloadOnlinePanel()
            GlobalManager.getInstance().mainScene.loadTimingPoints(false)
            GlobalManager.getInstance().songService.isGaming = false
        } else if (Multiplayer.isConnected) {
            RoomScene.chat.show()
        }

        GlobalManager.getInstance().songService.volume = Config.getBgmVolume()
        super.dismiss()
    }


    private fun handleGeneralSectionPreferences() {
        findPreference<InputPreference>("onlinePassword")!!.setOnTextInputBind {
            inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
        }

        findPreference<Preference>("registerAcc")!!.setOnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REGISTER_URL)))
            true
        }

        findPreference<Preference>("update")!!.setOnPreferenceClickListener {
            UpdateManager.checkNewUpdates(false)
            true
        }

        findPreference<Preference>("backup")!!.setOnPreferenceClickListener {
            val success = ConfigBackup.exportPreferences()

            ToastLogger.showText(
                if (success) string.config_backup_info_success else string.config_backup_info_fail,
                true
            )

            true
        }

        findPreference<Preference>("restore")!!.setOnPreferenceClickListener {
            val context = it.context
            val success = ConfigBackup.importPreferences()

            if (success) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)

                GlobalManager.getInstance().songService.volume = prefs.getInt("bgmvolume", 100) / 100f

                loadSkin(context, prefs.getString("skinPath", "")!!).invokeOnCompletion {
                    mainThread {
                        ToastLogger.showText(string.config_backup_restore_info_success, true)
                        dismiss()
                    }
                }
            } else {
                ToastLogger.showText(string.config_backup_restore_info_fail, true)
            }

            true
        }

        findPreference<SelectPreference>("difficultyAlgorithm")!!.setOnPreferenceChangeListener { _, newValue ->
            if (Multiplayer.isMultiplayer) {
                // We need to manually update it before because the preference is updated after this listener.
                Config.setString("difficultyAlgorithm", newValue as String)
                RoomScene.switchDifficultyAlgorithm()
            }
            true
        }
    }


    private fun handleGameplaySectionPreferences() {
        findPreference<SelectPreference>("skinPath")!!.apply {

            val skinMain = File(Config.getSkinTopPath())
            val skins = Config.getSkins().map { Option(it.key, it.value) }.toMutableList()
            skins.add(0, Option(skinMain.name + " (Default)", skinMain.path))

            options = skins

            setOnPreferenceChangeListener { _, newValue ->
                loadSkin(context, newValue.toString())
                true
            }
        }

        findPreference<Preference>("hud_editor")!!.apply {

            if (Multiplayer.isMultiplayer) {
                isEnabled = false
                return
            }

            setOnPreferenceClickListener {
                val global = GlobalManager.getInstance()
                val selectedBeatmap = global.selectedBeatmap

                if (LibraryManager.getSizeOfBeatmaps() == 0 || selectedBeatmap == null) {
                    ToastLogger.showText("Cannot enter HUD editor with empty beatmap library!", true)
                } else {
                    dismiss()

                    val modMap = ModHashMap().apply {
                        put(ModAutoplay::class)
                    }

                    global.gameScene.setOldScene(global.mainScene.scene)
                    global.gameScene.startGame(selectedBeatmap, null, modMap, true)
                }
                true
            }
        }

        val playfieldAreaDisplay = PlayfieldAreaDisplay()

        findPreference<SeekBarPreference>("playfieldSize")!!.apply {
            updatesContinuously = true

            setOnPreferenceChangeListener { _, newValue ->
                Config.setPlayfieldSize((newValue as Int) / 100f)
                playfieldAreaDisplay.update()
                true
            }
        }

        findPreference<SeekBarPreference>("playfieldHorizontalPosition")!!.apply {
            updatesContinuously = true

            setOnPreferenceChangeListener { _, newValue ->
                Config.setPlayfieldHorizontalPosition((newValue as Int) / 100f)
                playfieldAreaDisplay.update()
                true
            }
        }

        findPreference<SeekBarPreference>("playfieldVerticalPosition")!!.apply {
            updatesContinuously = true

            setOnPreferenceChangeListener { _, newValue ->
                Config.setPlayfieldVerticalPosition((newValue as Int) / 100f)
                playfieldAreaDisplay.update()
                true
            }
        }
    }


    private fun handleAudioSectionPreferences() {
        findPreference<SeekBarPreference>("bgmvolume")!!.apply {
            updatesContinuously = true

            setOnPreferenceChangeListener { _, newValue ->
                GlobalManager.getInstance().songService.volume = (newValue as Int) / 100f
                true
            }
        }

        findPreference<SeekBarPreference>("soundvolume")!!.apply {
            setOnPreferenceChangeListener { _, newValue ->
                // Set the configuration now as the sound below depends on this value.
                Config.setSoundVolume((newValue as Int) / 100f)

                // Use the sound when the osu! cookie is clicked since it is guaranteed to be available and not
                // skinnable (which means the player cannot silence it via skins).
                ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false)?.play()

                true
            }
        }
    }


    private fun handleLibrarySectionPreferences() {
        findPreference<Preference>("clear_beatmap_cache")!!.setOnPreferenceClickListener {
            LibraryManager.clearDatabase()
            ToastLogger.showText(StringTable.get(string.library_cleared), true)
            true
        }

        findPreference<Preference>("clear_properties")!!.setOnPreferenceClickListener {
            DatabaseManager.beatmapOptionsTable.deleteAll()
            true
        }

        findPreference<Preference>("importReplay")!!.setOnPreferenceClickListener {
            replayFilePicker.launch("application/octet-stream")

            true
        }
    }


    private fun handleAdvancedSectionPreferences() {
        findPreference<CheckBoxPreference>("forceMaxRefreshRate")!!.apply {
            // Obtaining supported refresh rates is only available on Android 12 and above.
            // See https://developer.android.com/reference/android/view/Display.Mode#getAlternativeRefreshRates().
            isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

        findPreference<InputPreference>("skinTopPath")!!.setOnPreferenceChangeListener { it, newValue ->

            it as InputPreference

            if (newValue.toString().trim { it <= ' ' }.isEmpty()) {
                it.setText(Config.getCorePath() + "Skin/")
                Config.loadConfig(requireActivity())
                return@setOnPreferenceChangeListener false
            }

            val file = File(newValue.toString())

            if (!file.exists() && !file.mkdirs()) {
                ToastLogger.showText(StringTable.get(string.message_error_dir_not_found), true)
                return@setOnPreferenceChangeListener false
            }

            it.setText(newValue.toString())
            Config.loadConfig(requireActivity())
            false
        }
    }


    private fun handleInputSectionPreferences() {
        findPreference<Preference>("block_areas")!!.setOnPreferenceClickListener {
            BlockAreaEditorFragment().show()
            true
        }

        findPreference<SeekBarPreference>("seekBarVibrateIntensity")!!.apply {
            min = 1
            max = 255
            value = Config.getInt("seekBarVibrateIntensity", 127)
            setOnPreferenceChangeListener { _, newValue ->
                VibratorManager.intensity = newValue as Int
                true
            }
        }

        findPreference<CheckBoxPreference>("vibrationCircle")!!.apply {
            setOnPreferenceChangeListener { _, newValue ->
                VibratorManager.isCircleVibrationEnabled = newValue as Boolean
                true
            }
        }

        findPreference<CheckBoxPreference>("vibrationSlider")!!.apply {
            setOnPreferenceChangeListener { _, newValue ->
                VibratorManager.isSliderVibrationEnabled = newValue as Boolean
                true
            }
        }

        findPreference<CheckBoxPreference>("vibrationSpinner")!!.apply {
            setOnPreferenceChangeListener { _, newValue ->
                VibratorManager.isSpinnerVibrationEnabled = newValue as Boolean
                true
            }
        }
    }


    private fun handlePlayerSectionPreferences() {
        findPreference<SelectPreference>("player_team")!!.apply {
            isEnabled = Multiplayer.room!!.teamMode == TeamMode.TeamVersus
            value = Multiplayer.player!!.team?.ordinal?.toString()

            setOnPreferenceChangeListener { _, newValue ->
                RoomAPI.setPlayerTeam(RoomTeam[(newValue as String).toInt()])
                true
            }
        }

        findPreference<CheckBoxPreference>("player_nightcore")!!.apply {

            setOnPreferenceChangeListener { _, newValue ->
                Config.setUseNightcoreOnMultiplayer(newValue as Boolean)
                RoomScene.onRoomModsChange(Multiplayer.room!!.mods)
                true
            }
        }
    }


    private fun handleRoomSectionPreferences() {
        findPreference<Preference>("room_link")!!.setOnPreferenceClickListener {

            requireContext().getSystemService<ClipboardManager>()!!.apply {

                setPrimaryClip(ClipData.newPlainText(Multiplayer.room!!.name, "${LobbyAPI.INVITE_HOST}/${Multiplayer.room!!.id}/"))
            }

            ToastLogger.showText("Link copied to clipboard. If the room has a password, you can write it at the end of the link.", false)
            true
        }

        findPreference<InputPreference>("room_name")!!.apply {

            setText(Multiplayer.room!!.name)
            setOnPreferenceChangeListener { _, newValue ->

                val newName = newValue as String

                if (newName.isEmpty())
                    return@setOnPreferenceChangeListener false

                RoomAPI.setRoomName(newName)
                true
            }
        }

        findPreference<InputPreference>("room_password")!!.apply {
            setText(null)
            setOnPreferenceChangeListener { _, newValue ->
                RoomAPI.setRoomPassword(newValue as String)
                true
            }
        }

        findPreference<SeekBarPreference>("room_max_players")!!.apply {
            min = max(2, Multiplayer.room!!.activePlayers.size)
            value = Multiplayer.room!!.maxPlayers

            setOnPreferenceChangeListener { _, newValue ->
                RoomAPI.setRoomMaxPlayers(newValue as Int)
                true
            }
        }

        findPreference<CheckBoxPreference>("room_free_mods")!!.apply {
            isChecked = Multiplayer.room!!.gameplaySettings.isFreeMod

            setOnPreferenceChangeListener { _, newValue ->
                RoomAPI.setRoomFreeMods(newValue as Boolean)
                true
            }
        }

        findPreference<SelectPreference>("room_versus_mode")!!.apply {
            value = Multiplayer.room!!.teamMode.ordinal.toString()

            setOnPreferenceChangeListener { _, newValue ->
                RoomAPI.setRoomTeamMode(TeamMode[(newValue as String).toInt()])
                true
            }
        }

        findPreference<SelectPreference>("room_win_condition")!!.apply {
            value = Multiplayer.room!!.winCondition.ordinal.toString()

            setOnPreferenceChangeListener { _, newValue ->
                RoomAPI.setRoomWinCondition(WinCondition.from((newValue as String).toInt()))
                true
            }
        }

        findPreference<CheckBoxPreference>("room_removeSliderLock")!!.apply {
            isChecked = Multiplayer.room!!.gameplaySettings.isRemoveSliderLock

            setOnPreferenceChangeListener { _, newValue ->
                RoomAPI.setRoomRemoveSliderLock(newValue as Boolean)
                true
            }
        }
    }


    private fun loadSkin(context: Context, path: String): Job {
        val loading = LoadingFragment()
        loading.show()

        return async {
            BeatmapSkinManager.getInstance().clearSkin()
            // Setting the skin now so that any setting that relies on skins (i.e., the HUD editor) receives
            // the correct skin path.
            Config.setSkinPath(path)
            ResourceManager.getInstance().loadSkin(path)
            GlobalManager.getInstance().engine.textureManager.reloadTextures()

            mainThread {
                loading.dismiss()
                context.startActivity(Intent(context, MainActivity::class.java))
                Snackbar.make(requireActivity().window.decorView, string.message_loaded_skin, 1500).show()
            }
        }
    }


    private enum class Section(@param:XmlRes val xml: Int) {

        General(R.xml.settings_general),
        Gameplay(R.xml.settings_gameplay),
        Graphics(R.xml.settings_graphics),
        Audio(R.xml.settings_audio),
        Library(R.xml.settings_library),
        Input(R.xml.settings_input),
        Advanced(R.xml.settings_advanced),

        // Multiplayer exclusive
        Room(R.xml.multiplayer_room_settings),
        Player(R.xml.multiplayer_player_settings)

    }


    private inner class PlayfieldAreaDisplay {
        private val view = LayoutInflater.from(context).inflate(R.layout.playfield_area_display, null, false)!!
        private val display = view.findViewById<View>(R.id.display)!!

        private var previousPlayfieldSize = Config.getPlayfieldSize()

        init {
            updateMeasurements()
            requireView().findViewById<FrameLayout>(R.id.root_container)!!.addView(view)
        }

        fun update() {
            val newPlayfieldSize = Config.getPlayfieldSize()

            // We do not need to update states or show anything if the playfield size is 100%, since it will fill the
            // entire screen anyway, making position settings irrelevant.
            if (previousPlayfieldSize == 1f && newPlayfieldSize == 1f) {
                return
            }

            previousPlayfieldSize = newPlayfieldSize
            updateMeasurements()

            display.apply {
                clearAnimation()

                animate()
                    .alpha(0.3f)
                    .setDuration(400)
                    .setInterpolator(Easing.OutQuint.asTimeInterpolator())
                    .withEndAction {
                        animate()
                            .alpha(0f)
                            .setDuration(500)
                    }
            }
        }

        private fun updateMeasurements() {
            val playfieldSize = Config.getPlayfieldSize()
            val width = (playfieldSize * root!!.width).roundToInt()
            val height = (playfieldSize * root!!.height).roundToInt()

            view.x = (root!!.width - width) * Config.getPlayfieldHorizontalPosition()
            view.y = (root!!.height - height) * Config.getPlayfieldVerticalPosition()

            display.updateLayoutParams {
                this.width = width
                this.height = height
            }
        }
    }


    companion object {
        const val REGISTER_URL: String = "https://${OnlineManager.hostname}/user/?action=register"
    }
}