package com.reco1l.osu.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.XmlRes
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.edlplan.ui.SkinPathPreference
import com.edlplan.ui.fragment.LoadingFragment
import com.google.android.material.snackbar.Snackbar
import com.reco1l.osu.UpdateManager
import com.reco1l.osu.async
import com.reco1l.osu.mainThread
import com.reco1l.osu.ui.Section.Advanced
import com.reco1l.osu.ui.Section.Gameplay
import com.reco1l.osu.ui.Section.General
import com.reco1l.osu.ui.Section.Graphics
import com.reco1l.osu.ui.Section.Input
import com.reco1l.osu.ui.Section.Library
import com.reco1l.osu.ui.Section.Sounds
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.skins.SkinManager
import java.io.File


// Important: Order should match the order in the selector layout declared in the XML file.
enum class Section(@XmlRes val xml: Int) {

    General(R.xml.settings_general),
    Gameplay(R.xml.settings_gameplay),
    Graphics(R.xml.settings_graphics),
    Sounds(R.xml.settings_audio),
    Library(R.xml.settings_library),
    Input(R.xml.settings_input),
    Advanced(R.xml.settings_advanced)

}


class SettingsFragment : com.edlplan.ui.fragment.SettingsFragment() {


    private lateinit var sectionSelector: LinearLayout


    private var section: Section? = null
        set(value) {

            if (value == null) {
                throw IllegalArgumentException("Cannot set section to null!")
            }

            if (value != field) {
                field = value

                sectionSelector.forEachIndexed { i, it ->

                    it as TextView

                    if (i == value.ordinal) {
                        it.setBackgroundResource(R.drawable.rounded_rect)
                        it.background.setTint(0xFF363653.toInt())
                    } else {
                        it.background = null
                    }

                }

                setPreferencesFromResource(value.xml, null)
            }
        }


    override fun onLoadView() {

        val scrollview = findViewById<ScrollView>(R.id.scrollview)!!

        sectionSelector = scrollview[0] as LinearLayout
        sectionSelector.forEachIndexed { i, it ->
            it.setOnClickListener {
                section = Section.entries[i]
            }
        }

        findViewById<View>(R.id.close)!!.setOnClickListener {
            dismiss()
        }

        section = General
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) = Unit


    // For whatever reason this is restricted API when it wasn't in previous SDKs.
    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {

        when(section) {

            null -> Unit

            General -> {
                findPreference<EditTextPreference>("onlinePassword")!!.setOnBindEditTextListener {
                    it.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
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

            Gameplay -> {
                findPreference<SkinPathPreference>("skinPath")!!.apply {

                    reloadSkinList()
                    setOnPreferenceChangeListener { _, newValue ->

                        if (GlobalManager.getInstance().skinNow == newValue.toString()) {
                            return@setOnPreferenceChangeListener false
                        }

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
            }

            Library -> {
                findPreference<Preference>("clear")!!.setOnPreferenceClickListener {
                    LibraryManager.INSTANCE.clearCache()
                    true
                }

                findPreference<Preference>("clear_properties")!!.setOnPreferenceClickListener {
                    PropertiesLibrary.getInstance().clear(requireActivity())
                    true
                }
            }

            Advanced -> {
                findPreference<EditTextPreference>("skinTopPath")!!.setOnPreferenceChangeListener { it, newValue ->

                    it as EditTextPreference

                    if (newValue.toString().trim { it <= ' ' }.isEmpty()) {
                        it.text = Config.getCorePath() + "Skin/"
                        Config.loadConfig(requireActivity())
                        return@setOnPreferenceChangeListener false
                    }

                    val file = File(newValue.toString())

                    if (!file.exists() && !file.mkdirs()) {
                        ToastLogger.showText(StringTable.get(R.string.message_error_dir_not_found), true)
                        return@setOnPreferenceChangeListener false
                    }

                    it.text = newValue.toString()
                    Config.loadConfig(requireActivity())
                    false
                }
            }

            Graphics -> Unit
            Sounds -> Unit
            Input -> Unit
        }

    }


    override fun dismiss() {
        Config.loadConfig(requireActivity())
        GlobalManager.getInstance().mainScene.reloadOnlinePanel()
        GlobalManager.getInstance().mainScene.loadTimingPoints(false)
        GlobalManager.getInstance().songService.volume = Config.getBgmVolume()
        GlobalManager.getInstance().songService.isGaming = false
        super.dismiss()
    }


    companion object {

        const val REGISTER_URL: String = "https://${OnlineManager.hostname}/user/?action=register"

    }
}