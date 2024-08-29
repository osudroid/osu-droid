package com.reco1l.osu.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.XmlRes
import androidx.core.content.getSystemService
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.edlplan.ui.fragment.LoadingFragment
import com.google.android.material.snackbar.Snackbar
import com.reco1l.ibancho.LobbyAPI
import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.RoomTeam
import com.reco1l.ibancho.data.TeamMode
import com.reco1l.ibancho.data.WinCondition
import com.reco1l.osu.UpdateManager
import com.reco1l.osu.async
import com.reco1l.osu.data.DatabaseManager
import com.reco1l.osu.mainThread
import com.reco1l.osu.multiplayer.Multiplayer
import com.reco1l.osu.multiplayer.RoomScene
import com.reco1l.toolkt.android.bottomMargin
import com.reco1l.toolkt.android.cornerRadius
import com.reco1l.toolkt.android.dp
import com.reco1l.toolkt.android.drawableLeft
import com.reco1l.toolkt.android.layoutWidth
import com.reco1l.toolkt.android.topMargin
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.skins.SkinManager
import java.io.File


enum class Section(@XmlRes val xml: Int) {

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


class SettingsFragment : com.edlplan.ui.fragment.SettingsFragment() {


    private lateinit var sectionSelector: LinearLayout


    private var section = when {

        Multiplayer.isRoomHost -> Section.Room
        Multiplayer.isMultiplayer -> Section.Player

        else -> Section.General
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

        Section.General -> {
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
        }

        Section.Gameplay -> {
            findPreference<SelectPreference>("skinPath")!!.apply {

                val skinMain = File(Config.getSkinTopPath())
                val skins = Config.getSkins().map { Option(it.key, it.value) }.toMutableList()
                skins.add(0, Option(skinMain.name + " (Default)", skinMain.path))

                options = skins

                setOnPreferenceChangeListener { _, newValue ->

                    val loading = LoadingFragment()
                    loading.show()

                    async {
                        GlobalManager.getInstance().skinNow = Config.getSkinPath()
                        SkinManager.getInstance().clearSkin()
                        ResourceManager.getInstance().loadSkin(newValue.toString())
                        GlobalManager.getInstance().engine.textureManager.reloadTextures()

                        mainThread {
                            loading.dismiss()
                            context.startActivity(Intent(context, MainActivity::class.java))
                            Snackbar.make(requireActivity().window.decorView, R.string.message_loaded_skin, 1500).show()
                        }
                    }
                    true
                }
            }
            Unit
        }

        Section.Library -> {
            findPreference<Preference>("clear")!!.setOnPreferenceClickListener {
                LibraryManager.clearDatabase()
                true
            }

            findPreference<Preference>("clear_properties")!!.setOnPreferenceClickListener {
                DatabaseManager.beatmapOptionsTable.deleteAll()
                true
            }
        }

        Section.Advanced -> {
            findPreference<InputPreference>("skinTopPath")!!.setOnPreferenceChangeListener { it, newValue ->

                it as InputPreference

                if (newValue.toString().trim { it <= ' ' }.isEmpty()) {
                    it.setText(Config.getCorePath() + "Skin/")
                    Config.loadConfig(requireActivity())
                    return@setOnPreferenceChangeListener false
                }

                val file = File(newValue.toString())

                if (!file.exists() && !file.mkdirs()) {
                    ToastLogger.showText(StringTable.get(R.string.message_error_dir_not_found), true)
                    return@setOnPreferenceChangeListener false
                }

                it.setText(newValue.toString())
                Config.loadConfig(requireActivity())
                false
            }
        }

        Section.Graphics -> Unit
        Section.Audio -> Unit

        Section.Input -> {
            findPreference<Preference>("block_areas")!!.setOnPreferenceClickListener {
                BlockAreaFragment().show(true)
                true
            }
        }

        Section.Player -> {

            findPreference<SelectPreference>("player_team")!!.apply {
                isEnabled = Multiplayer.room!!.teamMode == TeamMode.TEAM_VS_TEAM
                value = Multiplayer.player!!.team?.ordinal?.toString()

                setOnPreferenceChangeListener { _, newValue ->
                    RoomAPI.setPlayerTeam(RoomTeam.from((newValue as String).toInt()))
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
            Unit
        }

        Section.Room -> {

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

            findPreference<CheckBoxPreference>("room_free_mods")!!.apply {
                isChecked = Multiplayer.room!!.gameplaySettings.isFreeMod

                setOnPreferenceChangeListener { _, newValue ->
                    RoomAPI.setRoomFreeMods(newValue as Boolean)
                    true
                }
            }

            findPreference<CheckBoxPreference>("room_allowForceDifficultyStatistics")!!.apply {
                isChecked = Multiplayer.room!!.gameplaySettings.allowForceDifficultyStatistics

                setOnPreferenceChangeListener { _, newValue ->
                    RoomAPI.setRoomAllowForceDifficultyStatistics(newValue as Boolean)
                    true
                }
            }

            findPreference<SelectPreference>("room_versus_mode")!!.apply {
                value = Multiplayer.room!!.teamMode.ordinal.toString()

                setOnPreferenceChangeListener { _, newValue ->
                    RoomAPI.setRoomTeamMode(TeamMode.from((newValue as String).toInt()))
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
            Unit
        }

    }


    override fun dismiss() {
        Config.loadConfig(requireActivity())

        if (!Multiplayer.isMultiplayer) {
            GlobalManager.getInstance().mainScene.reloadOnlinePanel()
            GlobalManager.getInstance().mainScene.loadTimingPoints(false)
            GlobalManager.getInstance().songService.isGaming = false
        }

        GlobalManager.getInstance().songService.volume = Config.getBgmVolume()
        super.dismiss()
    }


    companion object {

        const val REGISTER_URL: String = "https://${OnlineManager.hostname}/user/?action=register"

    }
}