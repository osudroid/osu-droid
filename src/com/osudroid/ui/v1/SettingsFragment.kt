package com.osudroid.ui.v1

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.XmlRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.core.os.LocaleListCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.forEach
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.acivev.VibratorManager
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.fragment.LoadingFragment
import com.edlplan.ui.fragment.SettingsFragment
import com.google.android.material.snackbar.Snackbar
import com.osudroid.UpdateManager
import com.osudroid.data.DatabaseManager
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.RoomTeam
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.resources.R.string
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.reco1l.framework.asTimeInterpolator
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.ui.v2.CalibrationScene
import com.reco1l.osu.ui.InputPreference
import com.reco1l.osu.ui.Option
import com.reco1l.osu.ui.SelectPreference
import com.reco1l.framework.android.cornerRadius
import com.reco1l.framework.android.dp
import com.reco1l.framework.android.drawableLeft
import com.reco1l.framework.android.layoutWidth
import com.osudroid.mods.ModAutoplay
import com.osudroid.replay.ReplayImporter
import com.osudroid.utils.ModHashMap
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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


class SettingsFragment : SettingsFragment() {


    private lateinit var sectionSelector: LinearLayout

    private val availableSections = mutableListOf<Section>()

    private val sectionButtons = mutableMapOf<Section, TextView>()

    private val searchQuery = MutableStateFlow("")

    /**
     * The trimmed query that was last received from [searchQuery] after debouncing.
     */
    private var currentQuery = ""

    private var isSearching = false

    /**
     * The sections that are combined into the search preference screen. Empty
     * outside of search mode.
     */
    private var searchSections = emptyList<SettingsSearchSection<Section>>()

    /**
     * The filter of the search preference screen.
     *
     * `null` until the preferences have been bound, since the filter must
     * be created after all bind handlers have run.
     */
    private var searchFilter: SettingsSearchFilter<Section>? = null


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
                        tempFile!!.outputStream().use { output ->
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
        sectionSelector.setPadding(0, 32.dp, 0, 32.dp)

        availableSections.clear()
        sectionButtons.clear()

        isSearching = false
        searchSections = emptyList()
        searchFilter = null
        currentQuery = ""
        searchQuery.value = ""


        if (Multiplayer.isMultiplayer) {

            fun createDivider(text: String) {

                val divider = TextView(ContextThemeWrapper(context, R.style.settings_tab_divider))
                divider.text = text

                sectionSelector.addView(divider)
            }

            createDivider("Multiplayer")
            createSectionButton(Section.Player)

            if (Multiplayer.isRoomHost) {
                createSectionButton(Section.Room)
            }

            createDivider("Game")

        }


        if (!Multiplayer.isMultiplayer) {
            createSectionButton(Section.General)
        }

        createSectionButton(Section.Gameplay)
        createSectionButton(Section.Graphics)
        createSectionButton(Section.Audio)

        if (!Multiplayer.isMultiplayer) {
            createSectionButton(Section.Library)
        }

        createSectionButton(Section.Input)
        createSectionButton(Section.Advanced)

        findViewById<View>(R.id.close)!!.setOnClickListener {
            dismiss()
        }

        val searchInput = findViewById<EditText>(R.id.search_input)!!
        val searchClear = findViewById<View>(R.id.search_clear)!!

        searchInput.doAfterTextChanged {
            searchQuery.value = it?.toString().orEmpty()
            searchClear.visibility = if (it.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        searchInput.setOnEditorActionListener { view, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                return@setOnEditorActionListener false
            }

            requireContext().getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(view.windowToken, 0)
            true
        }

        searchClear.setOnClickListener {
            searchInput.setText("")
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            searchQuery.debounceSearchQuery().collect(::onSearchQuery)
        }
    }


    private fun createSectionButton(section: Section) {
        val button = TextView(ContextThemeWrapper(context, R.style.settings_tab_text))

        button.cornerRadius = 15f.dp
        button.layoutWidth = 200.dp
        button.text = section.title
        button.drawableLeft = requireContext().getDrawable(section.icon)!!
        button.drawableLeft!!.setTint(Color.WHITE)

        button.setOnClickListener {
            if (isSearching) {
                scrollToSection(section)
            } else {
                selectSection(section)
            }
        }

        availableSections.add(section)
        sectionButtons[section] = button
        sectionSelector.addView(button)

        if (this.section == section) {
            button.callOnClick()
        }
    }


    private fun selectSection(section: Section) {
        clearPreferenceFocus()

        this.section = section
        showSectionList(null)

        // Older SDKs may potentially throw an IllegalStateException when trying to change
        // preference screen, so we need to remove all views first to prevent that.
        listView.removeAllViews()
        setPreferencesFromResource(section.xml, null)
    }


    /**
     * Clears the focus of the focused view, except the search input.
     */
    private fun clearPreferenceFocus() {
        // Workaround to IllegalStateException being thrown when an EditText is focused
        // while trying to change the preference screen.
        root!!.findFocus()?.let {
            if (it.id != R.id.search_input) {
                it.clearFocus()
            }
        }
    }


    private fun onSearchQuery(query: String) {
        currentQuery = query.trim()

        if (currentQuery.isEmpty()) {
            if (isSearching) {
                exitSearchMode()
            }
            return
        }

        if (!isSearching) {
            // The filter is applied in onBindPreferences once the combined screen has been bound.
            enterSearchMode()
            return
        }

        // If the filter is null, the preferences are still being bound, and the bind will apply currentQuery.
        searchFilter?.let { showSearchResults(it.apply(currentQuery)) }
    }


    /**
     * Replaces the preference screen with one that combines every available section, each preceded by a header.
     */
    @SuppressLint("RestrictedApi")
    private fun enterSearchMode() {
        clearPreferenceFocus()

        val context = requireContext()
        val screen = preferenceManager.createPreferenceScreen(context)
        val sections = ArrayList<SettingsSearchSection<Section>>(availableSections.size)

        for (section in availableSections) {
            val header = SectionHeaderPreference(context).apply {
                title = section.title
                key = "search_header_${section.name}"
            }

            screen.addPreference(header)

            val firstCategoryIndex = screen.preferenceCount

            // Inflating into an existing screen merges the XML's children into it.
            preferenceManager.inflateFromResource(context, section.xml, screen)

            val categories = (firstCategoryIndex until screen.preferenceCount)
                .map { screen.getPreference(it) as PreferenceGroup }

            sections.add(SettingsSearchSection(section, header, categories))
        }

        searchSections = sections
        searchFilter = null
        isSearching = true

        listView.removeAllViews()
        preferenceScreen = screen
    }


    /**
     * Leaves search mode and goes back to the section that was selected before searching.
     */
    private fun exitSearchMode() {
        isSearching = false
        searchFilter = null
        searchSections = emptyList()

        findViewById<View>(R.id.search_empty)!!.visibility = View.GONE

        // This also restores the section list.
        selectSection(section)
    }


    private fun showSearchResults(matched: List<Section>) {
        findViewById<View>(R.id.search_empty)!!.visibility = if (matched.isEmpty()) View.VISIBLE else View.GONE
        showSectionList(matched)
    }


    @SuppressLint("RestrictedApi")
    private fun scrollToSection(section: Section) {
        val header = searchSections.firstOrNull { it.id == section }?.header ?: return
        val adapter = listView.adapter as? PreferenceGroupAdapter ?: return
        val position = adapter.getPreferenceAdapterPosition(header)

        if (position == RecyclerView.NO_POSITION) {
            return
        }

        (listView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 0)
    }


    /**
     * Updates the visibility and highlight of the section list.
     *
     * @param matched The sections to show, or `null` to show all sections with the current [section] highlighted.
     */
    private fun showSectionList(matched: List<Section>?) {
        sectionSelector.forEach { it.visibility = if (matched == null) View.VISIBLE else View.GONE }

        for ((section, button) in sectionButtons) {

            if (matched != null) {
                button.visibility = if (section in matched) View.VISIBLE else View.GONE
            }

            button.setBackgroundColor(
                if (matched == null && section == this.section) 0xFF363653.toInt() else Color.TRANSPARENT
            )
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) = Unit


    @SuppressLint("RestrictedApi")
    override fun onCreateAdapter(preferenceScreen: PreferenceScreen) = SettingsCardAdapter(preferenceScreen)


    // For whatever reason this is restricted API when it wasn't in previous SDKs.
    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        if (!isSearching) {
            bindSection(section)
            return
        }

        for (searchSection in searchSections) {
            bindSection(searchSection.id)
        }

        // The filter snapshots hidden preferences, so it must be created after all bind handlers have run.
        val filter = SettingsSearchFilter(searchSections)

        searchFilter = filter
        showSearchResults(filter.apply(currentQuery))
    }


    private fun bindSection(section: Section) = when (section) {

        Section.General -> handleGeneralSectionPreferences()
        Section.Graphics -> handleGraphicsSectionPreferences()
        Section.Gameplay -> handleGameplaySectionPreferences()
        Section.Audio -> handleAudioSectionPreferences()
        Section.Library -> handleLibrarySectionPreferences()
        Section.Advanced -> handleAdvancedSectionPreferences()
        Section.Input -> handleInputSectionPreferences()
        Section.Player -> handlePlayerSectionPreferences()
        Section.Room -> handleRoomSectionPreferences()
    }


    override fun dismiss() {
        Config.loadConfig(requireActivity())

        if (!Multiplayer.isMultiplayer) {
            GlobalManager.getInstance().mainScene.reloadOnlinePanel()
            GlobalManager.getInstance().mainScene.loadTimingPoints(false)
            GlobalManager.getInstance().songService.isGaming = false
        } else if (Multiplayer.isConnected) {
            Multiplayer.roomScene?.chat?.show()
            Multiplayer.roomScene?.updatePlayerList()
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

                loadSkin(prefs.getString("skinPath", "")!!).invokeOnCompletion {
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
                Multiplayer.roomScene?.updateBeatmapInfo()
            }
            true
        }

        findPreference<SelectPreference>("appLanguage")!!.apply {
            if (Multiplayer.isMultiplayer) {
                isEnabled = false
                return
            }

            options = mutableListOf(
                Option("System Default", "system"),
                Option("English", "en"),
                // German
                Option("Deutsch", "de"),
                // Spanish
                Option("Español", "es"),
                // French
                Option("Français", "fr"),
                // Indonesian
                Option("Bahasa Indonesia", "id"),
                // Italian
                Option("Italiano", "it"),
                // Japanese
                Option("日本語", "ja"),
                // Korean
                Option("한국어", "ko"),
                // Netherlands
                Option("Nederlands", "nl"),
                // Portuguese
                Option("Português (Brasil)", "pt-rBR"),
                // Russian
                Option("Русский", "ru"),
                // Thai
                Option("ไทย", "th"),
                // Vietnamese
                Option("Tiếng Việt", "vi"),
                // Chinese
                Option("中文 (简体)", "zh"),
            )

            setOnPreferenceChangeListener { _, newValue ->
                val locale = LocaleListCompat.forLanguageTags(newValue as String)
                AppCompatDelegate.setApplicationLocales(locale)

                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)

                true
            }
        }
    }


    private fun handleGraphicsSectionPreferences() {
        findPreference<SelectPreference>("frameLimiter")!!.setOnPreferenceChangeListener { _, newValue ->
            val frameLimiter = Config.parseFrameLimiter(newValue.toString())

            // Store it right away: MainActivity re-applies the setting from Config whenever the display changes, and
            // Config is otherwise only reloaded once the settings are dismissed.
            Config.setFrameLimiter(frameLimiter)
            (requireActivity() as MainActivity).applyFrameLimiterSetting(frameLimiter)
            true
        }

        findPreference<SelectPreference>("skinPath")!!.apply {

            val skinMain = File(Config.getSkinTopPath())
            val skinMap = Config.getSkins()
            val skins = skinMap.mapTo(ArrayList(skinMap.size + 1)) { Option(it.key, it.value) }

            skins.sortBy { it.text.toString() }
            skins.add(0, Option(skinMain.name + " (Default)", skinMain.path))

            options = skins

            setOnPreferenceChangeListener { _, newValue ->
                loadSkin(newValue.toString())
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
    }


    private fun handleGameplaySectionPreferences() {
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

        val offsetPreference = findPreference<SeekBarPreference>("offset")!!

        findPreference<Preference>("offset_calibration")!!.setOnPreferenceClickListener {
            CalibrationScene.settingsFragment = this
            CalibrationScene.OFFSET_MIN = offsetPreference.min
            CalibrationScene.OFFSET_MAX = offsetPreference.max
            CalibrationScene.show()

            // We only want to dismiss the fragment, not reapply preferences (which is what the override does).
            super.dismiss()

            true
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
            isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

            setOnPreferenceChangeListener { _, newValue ->
                (requireActivity() as MainActivity).applyRefreshRateSetting(newValue as Boolean)
                true
            }
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
            isEnabled = Multiplayer.room!!.teamMode == TeamMode.TeamVS
            value = Multiplayer.player!!.team?.ordinal?.toString()

            setOnPreferenceChangeListener { _, newValue ->
                RoomAPI.setPlayerTeam(RoomTeam.entries.getOrNull((newValue as String).toInt()) ?: return@setOnPreferenceChangeListener false)
                true
            }
        }

        findPreference<CheckBoxPreference>("player_nightcore")!!.apply {

            setOnPreferenceChangeListener { _, newValue ->
                Config.setUseNightcoreOnMultiplayer(newValue as Boolean)
                Multiplayer.roomScene?.onRoomModsChange(Multiplayer.room!!.mods)
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
                RoomAPI.setRoomTeamMode(TeamMode.entries.getOrNull((newValue as String).toInt()) ?: return@setOnPreferenceChangeListener false)
                true
            }
        }

        findPreference<SelectPreference>("room_win_condition")!!.apply {
            value = Multiplayer.room!!.winCondition.ordinal.toString()

            setOnPreferenceChangeListener { _, newValue ->
                RoomAPI.setRoomWinCondition(WinCondition.entries.getOrNull((newValue as String).toInt()) ?: return@setOnPreferenceChangeListener false)
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


    private fun loadSkin(path: String): Job {
        val loading = LoadingFragment()

        loading.isDismissOnBackPress = false
        loading.show()

        return async {
            BeatmapSkinManager.getInstance().clearSkin()
            // Setting the skin now so that any setting that relies on skins (i.e., the HUD editor) receives
            // the correct skin path.
            Config.setSkinPath(path)
            ResourceManager.getInstance().loadSkin(path)
            GlobalManager.getInstance().engine.onResume()

            mainThread {
                loading.dismiss()
                Snackbar.make(requireActivity().window.decorView, string.message_loaded_skin, 1500).show()
            }
        }
    }


    private enum class Section(
        @param:XmlRes val xml: Int,
        val title: String,
        @param:DrawableRes val icon: Int,
    ) {

        General(R.xml.settings_general, "General", R.drawable.grid_view_24px),
        Gameplay(R.xml.settings_gameplay, "Gameplay", R.drawable.videogame_asset_24px),
        Graphics(R.xml.settings_graphics, "Graphics", R.drawable.display_settings_24px),
        Audio(R.xml.settings_audio, "Audio", R.drawable.headphones_24px),
        Library(R.xml.settings_library, "Library", R.drawable.library_music_24px),
        Input(R.xml.settings_input, "Input", R.drawable.trackpad_input_24px),
        Advanced(R.xml.settings_advanced, "Advanced", R.drawable.manufacturing_24px),

        // Multiplayer exclusive
        Room(R.xml.multiplayer_room_settings, "Room", R.drawable.groups_24px),
        Player(R.xml.multiplayer_player_settings, "Player", R.drawable.person_24px)

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