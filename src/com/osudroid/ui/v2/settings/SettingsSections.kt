package com.osudroid.ui.v2.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.InputType
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import com.acivev.VibratorManager
import com.edlplan.ui.fragment.LoadingFragment
import com.google.android.material.snackbar.Snackbar
import com.osudroid.UpdateManager
import com.osudroid.data.DatabaseManager
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.resources.R
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.reco1l.andengine.ui.UISelect
import com.reco1l.andengine.ui.form.PreferenceInput
import com.reco1l.andengine.ui.form.PreferenceSelect
import com.rian.osu.mods.ModAutoplay
import com.rian.osu.utils.ModHashMap
import kotlinx.coroutines.Job
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ConfigBackup
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import java.io.File


val generalSection = listOf(
    CategoryInfo(
        title = "Online", // opt_category_online
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "stayOnline",
                title = R.string.opt_stayonline_title,
                summary = R.string.opt_stayonline_summary
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "loadAvatar",
                title = R.string.opt_loadavatar_title,
                summary = R.string.opt_loadavatar_summary
            ),
            OptionInfo(
                type = OptionType.Select,
                key = "difficultyAlgorithm",
                title = R.string.difficulty_algorithm_title,
                summary = R.string.difficulty_algorithm_summary,
                entries = R.array.difficulty_algorithm_names,
                entryValues = R.array.difficulty_algorithm_values,
                onChange = { value ->
                    if (Multiplayer.isMultiplayer) {
                        @Suppress("UNCHECKED_CAST")
                        val newValue = (value as List<String>)[0]
                        Config.setString("difficultyAlgorithm", newValue)
                        Multiplayer.roomScene?.updateBeatmapInfo()
                    }
                }
            )
        )
    ),

    CategoryInfo(
        title = "Account", // opt_category_account
        options = listOf(
            OptionInfo(
                type = OptionType.Input,
                key = "onlineUsername",
                title = R.string.opt_login_title,
                summary = R.string.opt_login_summary
            ),
            OptionInfo(
                type = OptionType.Input,
                key = "onlinePassword",
                title = R.string.opt_password_title,
                summary = R.string.opt_password_summary,
                onAttach = {
                    (it as PreferenceInput).control.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "registerAcc",
                title = R.string.opt_register_title,
                summary = R.string.opt_register_summary,
                onClick = {
                    val context = GlobalManager.getInstance().mainActivity
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REGISTER_URL)))
                }
            )
        )
    ),

    CategoryInfo(
        title = "Community", // opt_category_community
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "receiveAnnouncements",
                title = R.string.opt_receive_announcements_title,
                summary = R.string.opt_receive_announcements_summary
            )
        )
    ),

    CategoryInfo(
        title = "Updates", // opt_category_updates
        options = listOf(
            OptionInfo(
                type = OptionType.Button,
                key = "update",
                title = R.string.opt_update_title,
                summary = R.string.opt_update_summary,
                onClick = {
                    UpdateManager.checkNewUpdates(false)
                }
            )
        )
    ),

    CategoryInfo(
        title = "Configuration Backup", // opt_category_config_backup
        options = listOf(
            OptionInfo(
                type = OptionType.Button,
                key = "backup",
                title = R.string.opt_config_backup_title,
                summary = R.string.opt_config_backup_summary,
                onClick = {
                    val success = ConfigBackup.exportPreferences()
                    ToastLogger.showText(
                        if (success) R.string.config_backup_info_success else R.string.config_backup_info_fail,
                        true
                    )
                }
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "restore",
                title = R.string.opt_config_backup_restore_title,
                summary = R.string.opt_config_backup_restore_summary,
                onClick = {
                    val context = GlobalManager.getInstance().mainActivity
                    val success = ConfigBackup.importPreferences()

                    if (success) {
                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                        GlobalManager.getInstance().songService.volume = prefs.getInt("bgmvolume", 100) / 100f

                        loadSkin(context, prefs.getString("skinPath", "")!!).invokeOnCompletion {
                            mainThread {
                                ToastLogger.showText(R.string.config_backup_restore_info_success, true)
                            }
                        }
                    } else {
                        ToastLogger.showText(R.string.config_backup_restore_info_fail, true)
                    }
                }
            )
        )
    ),

    CategoryInfo(
        title = "Localization", // opt_category_localization
        options = listOf(
            OptionInfo(
                type = OptionType.Select,
                key = "appLanguage",
                title = R.string.opt_language_title,
                summary = R.string.opt_language_summary,
                entries = R.array.placeholder_array,
                entryValues = R.array.placeholder_array,
                onAttach = {
                    (it as PreferenceSelect).options = listOf(
                        UISelect.Option("system", "System Default"),
                        UISelect.Option("en", "English"),
                        // German
                        UISelect.Option("de", "Deutsch"),
                        // Spanish
                        UISelect.Option("es", "Español"),
                        // French
                        UISelect.Option("fr", "Français"),
                        // Indonesian
                        UISelect.Option("id", "Bahasa Indonesia"),
                        // Italian
                        UISelect.Option("it", "Italiano"),
                        // Japanese
                        UISelect.Option("ja", "日本語"),
                        // Korean
                        UISelect.Option("ko", "한국어"),
                        // Netherlands
                        UISelect.Option("nl", "Nederlands"),
                        // Portuguese
                        UISelect.Option("pt-rBR", "Português (Brasil)"),
                        // Russian
                        UISelect.Option("ru", "Русский"),
                        // Thai
                        UISelect.Option("th", "ไทย"),
                        // Vietnamese
                        UISelect.Option("vi", "Tiếng Việt"),
                        // Chinese
                        UISelect.Option("zh", "中文 (简体)"),
                    )
                },
                onChange = { value ->
                    @Suppress("UNCHECKED_CAST")
                    val selected = (value as List<String>)[0]

                    val locale = LocaleListCompat.forLanguageTags(selected)
                    AppCompatDelegate.setApplicationLocales(locale)

                    val context = GlobalManager.getInstance().mainActivity
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)

                }
            )
        )
    )
)

val gameplaySection = listOf(
    CategoryInfo(
        title = "Skin", // opt_category_skin
        options = listOf(
            OptionInfo(
                type = OptionType.Select,
                key = "skinPath",
                title = R.string.opt_skinpath_title,
                summary = R.string.opt_skinpath_summary,
                entries = R.array.placeholder_array,
                entryValues = R.array.placeholder_array,
                onAttach = {
                    val skinMain = File(Config.getSkinTopPath())
                    val skins = Config.getSkins().map { skin ->
                        UISelect.Option(skin.value, skin.key)
                    }.toMutableList()
                    skins.add(0, UISelect.Option(skinMain.path, skinMain.name + " (Default)"))

                    (it as PreferenceSelect).options = skins
                },
                onChange = { value ->
                    @Suppress("UNCHECKED_CAST")
                    val newValue = (value as List<String>)[0]
                    val context = GlobalManager.getInstance().mainActivity
                    loadSkin(context, newValue)
                }
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "hud_editor",
                title = R.string.opt_hudEditor_title,
                summary = R.string.opt_hudEditor_summary,
                onClick = {
                    val global = GlobalManager.getInstance()
                    val selectedBeatmap = global.selectedBeatmap

                    if (LibraryManager.getSizeOfBeatmaps() == 0 || selectedBeatmap == null) {
                        ToastLogger.showText("Cannot enter HUD editor with empty beatmap library!", true)
                    } else {
                        val modMap = ModHashMap().apply {
                            put(ModAutoplay::class)
                        }

                        global.gameScene.setOldScene(global.mainScene.scene)
                        global.gameScene.startGame(selectedBeatmap, null, modMap, true)
                    }
                }
            ),
            OptionInfo(
                type = OptionType.Select,
                key = "spinnerstyle",
                title = R.string.opt_spinner_style_title,
                summary = R.string.opt_spinner_style_summary,
                defaultValue = "1",
                entries = R.array.spinner_style_names,
                entryValues = R.array.spinner_style_values
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "skin",
                title = R.string.opt_skin_title,
                summary = R.string.opt_skin_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = "Hit Objects", // opt_category_hit_objects
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "showfirstapproachcircle",
                title = R.string.opt_show_first_approach_circle_title,
                summary = R.string.opt_show_first_approach_circle_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = "Background", // opt_category_background
        options = listOf(
            OptionInfo(
                type = OptionType.Slider,
                key = "bgbrightness",
                title = R.string.opt_bgbrightness_title,
                summary = R.string.opt_bgbrightness_summary,
                defaultValue = 0.25f,
                min = 0f,
                max = 1f,
                step = 0.01f,
                valueFormatter = { "${(it * 100f).toInt()}%" }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "keepBackgroundAspectRatio",
                title = R.string.opt_keep_background_aspect_ratio_title,
                summary = R.string.opt_keep_background_aspect_ratio_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "enableStoryboard",
                title = R.string.opt_enableStoryboard_title,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "enableVideo",
                title = R.string.opt_video_title,
                summary = R.string.opt_video_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = "Playfield", // opt_category_playfield
        options = listOf(
            OptionInfo(
                type = OptionType.Slider,
                key = "playfieldSize",
                title = R.string.opt_setplayfield_title,
                summary = R.string.opt_setplayfield_summary,
                defaultValue = 1f,
                min = 0.5f,
                max = 1f,
                step = 0.01f,
                valueFormatter = { "${(it * 100f).toInt()}%" }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "playfieldHorizontalPosition",
                title = R.string.opt_playfieldHorizontalPosition_title,
                summary = R.string.opt_playfieldHorizontalPosition_summary,
                defaultValue = 0.5f,
                min = 0f,
                max = 1f,
                step = 0.01f,
                valueFormatter = { "${(it * 100f).toInt()}%" }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "playfieldVerticalPosition",
                title = R.string.opt_playfieldVerticalPosition_title,
                summary = R.string.opt_playfieldVerticalPosition_summary,
                defaultValue = 0.5f,
                min = 0f,
                max = 1f,
                step = 0.01f,
                valueFormatter = { "${(it * 100f).toInt()}%" }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "displayPlayfieldBorder",
                title = R.string.opt_display_playfield_border_title,
                summary = R.string.opt_display_playfield_border_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = "HUD", // opt_category_hud
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "hideInGameUI",
                title = R.string.opt_hide_ingame_ui_title,
                summary = R.string.opt_hide_ingame_ui_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "hideReplayMarquee",
                title = R.string.opt_hide_replay_marquee_title,
                summary = R.string.opt_hide_replay_marquee_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "fps",
                title = R.string.opt_fps_title,
                summary = R.string.opt_fps_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "displayScoreStatistics",
                title = R.string.opt_display_score_statistics_title,
                summary = R.string.opt_display_score_statistics_summary,
                defaultValue = true
            )
        )
    )
)

val graphicsSection = listOf(
    CategoryInfo(
        title = "Cursor", // opt_category_cursor
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "showcursor",
                title = R.string.opt_showcursor_title,
                summary = R.string.opt_showcursor_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "cursorSize",
                title = R.string.opt_cursor_size,
                summary = R.string.opt_cursor_size_summary,
                defaultValue = 0.5f,
                min = 0.25f,
                max = 3f,
                step = 0.01f,
                valueFormatter = { "${it}x" }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "particles",
                title = R.string.opt_particles_title,
                summary = R.string.opt_particles_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = "Animations", // opt_category_animations
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "dimHitObjects",
                title = R.string.opt_dim_hit_objects_title,
                summary = R.string.opt_dim_hit_objects_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "comboburst",
                title = R.string.opt_combo_burst_title,
                summary = R.string.opt_combo_burst_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "images",
                title = R.string.opt_largeimages_title,
                summary = R.string.opt_largeimages_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "animateFollowCircle",
                title = R.string.opt_animate_follow_circle_title,
                summary = R.string.opt_animate_follow_circle_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "animateComboText",
                title = R.string.opt_animate_combo_text_title,
                summary = R.string.opt_animate_combo_text_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "snakingInSliders",
                title = R.string.opt_snakingInSliders_title,
                summary = R.string.opt_snakingInSliders_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "snakingOutSliders",
                title = R.string.opt_snakingOutSliders_title,
                summary = R.string.opt_snakingOutSliders_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "noChangeDimInBreaks",
                title = R.string.opt_noChangeDimInBreaks_title,
                summary = R.string.opt_noChangeDimInBreaks_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "bursts",
                title = R.string.opt_bursts_title,
                summary = R.string.opt_bursts_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "hitlighting",
                title = R.string.opt_hitlighting_title,
                summary = R.string.opt_hitlighting_summary,
                defaultValue = false
            )
        )
    )
)

val audioSection = listOf(
    CategoryInfo(
        title = "Volume", // opt_category_volume
        options = listOf(
            OptionInfo(
                type = OptionType.Slider,
                key = "bgmvolume",
                title = R.string.opt_bgm_volume_title,
                summary = R.string.opt_bgm_volume_summary,
                defaultValue = 1f,
                min = 0f,
                max = 1f,
                step = 0.01f,
                valueFormatter = { "${(it * 100f).toInt()}%" },
                onChange = { value ->
                    val floatValue = value as Float
                    GlobalManager.getInstance().songService.volume = floatValue
                }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "soundvolume",
                title = R.string.opt_sound_volume_title,
                summary = R.string.opt_sound_volume_summary,
                defaultValue = 1f,
                min = 0f,
                max = 1f,
                step = 0.01f,
                valueFormatter = { "${(it * 100f).toInt()}%" },
                onChange = { value ->
                    val floatValue = value as Float
                    // Set the configuration now as the sound below depends on this value.
                    Config.setSoundVolume(floatValue)

                    // Use the sound when the osu! cookie is clicked since it is guaranteed to be available and not
                    // skinnable (which means the player cannot silence it via skins).
                    ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false)?.play()
                }
            )
        )
    ),

    CategoryInfo(
        title = "Offset", // opt_category_offset
        options = listOf(
            OptionInfo(
                type = OptionType.Slider,
                key = "offset",
                title = R.string.opt_offset_title,
                summary = R.string.opt_offset_summary,
                defaultValue = 0,
                min = -500f,
                max = 500f,
                step = 1f,
                valueFormatter = { "${it.toInt()}ms" }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "gameAudioSynchronizationThreshold",
                title = R.string.opt_gameAudioSynchronizationThreshold_title,
                summary = R.string.opt_gameAudioSynchronizationThreshold_summary,
                defaultValue = 20f,
                min = 0f,
                max = 100f,
                step = 1f,
                valueFormatter = { "${it.toInt()}ms" }
            )
        )
    ),

    CategoryInfo(
        title = "Effect", // opt_category_effect
        options = listOf(
            OptionInfo(
                type = OptionType.Select,
                key = "metronomeswitch",
                title = R.string.opt_metronome_switch_title,
                summary = R.string.opt_metronome_switch_summary,
                defaultValue = "1",
                entries = R.array.metronome_switch_names,
                entryValues = R.array.metronome_switch_values
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "shiftPitchInRateChange",
                title = R.string.opt_shiftPitchInRateChange_title,
                summary = R.string.opt_shiftPitchInRateChange_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = "Miscellaneous", // opt_category_miscellaneous
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "beatmapSounds",
                title = R.string.opt_sound_title,
                summary = R.string.opt_sound_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "musicpreview",
                title = R.string.opt_musicpreview_title,
                summary = R.string.opt_musicpreview_summary,
                defaultValue = true
            )
        )
    )
)

val librarySection = listOf(
    CategoryInfo(
        title = "Import", // opt_category_import
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "deleteosz",
                title = R.string.opt_deleteosz_title,
                summary = R.string.opt_deleteosz_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "scandownload",
                title = R.string.opt_scandownload_title,
                summary = R.string.opt_scandownload_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "deleteUnimportedBeatmaps",
                title = R.string.opt_deleteUnimportedBeatmaps_title,
                summary = R.string.opt_deleteUnimportedBeatmaps_summary,
                defaultValue = false
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "deleteUnsupportedVideos",
                title = R.string.opt_delete_unsupported_videos_title,
                summary = R.string.opt_delete_unsupported_videos_summary,
                defaultValue = true
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "importReplay",
                title = R.string.opt_import_replay_title,
                summary = R.string.opt_import_replay_summary
            )
        )
    ),

    CategoryInfo(
        title = "Metadata", // opt_category_metadata
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "forceromanized",
                title = R.string.force_romanized,
                summary = R.string.force_romanized_summary,
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = "Storage", // opt_category_storage
        options = listOf(
            OptionInfo(
                type = OptionType.Button,
                key = "clear_beatmap_cache",
                title = R.string.opt_clear_title,
                summary = R.string.opt_clear_summary,
                onClick = {
                    LibraryManager.clearDatabase()
                    ToastLogger.showText(StringTable.get(R.string.library_cleared), true)
                }
            ),
            OptionInfo(
                type = OptionType.Button,
                key = "clear_properties",
                title = R.string.opt_clearprops_title,
                summary = R.string.opt_clearprops_summary,
                onClick = {
                    DatabaseManager.beatmapOptionsTable.deleteAll()
                }
            )
        )
    )
)

val inputSection = listOf(
    CategoryInfo(
        title = "Gameplay", // opt_category_gameplay
        options = listOf(
            OptionInfo(
                type = OptionType.Button,
                key = "block_areas",
                title = R.string.block_area_preference_title,
                summary = R.string.block_area_preference_summary,
                onClick = {
                    com.osudroid.ui.v1.BlockAreaEditorFragment().show()
                }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "back_button_press_time",
                title = R.string.opt_backButtonPressTime_title,
                summary = R.string.opt_backButtonPressTime_summary,
                defaultValue = 300f,
                min = 0f,
                max = 300f,
                step = 1f,
                valueFormatter = { "${it.toInt()}ms" }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "removeSliderLock",
                title = "Remove slider and spinner lock",
                summary = "[UNRANKED] Allow circles and sliders to be hittable when another slider or spinner is currently active.",
                defaultValue = false
            )
        )
    ),

    CategoryInfo(
        title = "Vibration",
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "vibrationCircle",
                title = "Circle",
                defaultValue = false,
                onChange = { value ->
                    VibratorManager.isCircleVibrationEnabled = value as Boolean
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "vibrationSlider",
                title = "Slider",
                defaultValue = false,
                onChange = { value ->
                    VibratorManager.isSliderVibrationEnabled = value as Boolean
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "vibrationSpinner",
                title = "Spinner",
                defaultValue = false,
                onChange = { value ->
                    VibratorManager.isSpinnerVibrationEnabled = value as Boolean
                }
            ),
            OptionInfo(
                type = OptionType.Slider,
                key = "seekBarVibrateIntensity",
                title = R.string.opt_seekBarVibrateIntensity_title,
                summary = R.string.opt_seekBarVibrateIntensity_summary,
                defaultValue = 1.27f,
                min = 0.01f,
                max = 2.55f,
                step = 0.01f,
                valueFormatter = { "${((it / 2.55f) * 100f).toInt()}%" },
                onChange = { value ->
                    val floatValue = value as Float
                    VibratorManager.intensity = (floatValue * 100).toInt()
                }
            )
        )
    ),

    CategoryInfo(
        title = "Synchronization", // opt_category_synchronization
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "fixFrameOffset",
                title = R.string.opt_fix_frame_offset_title,
                summary = R.string.opt_fix_frame_offset_summary,
                defaultValue = true
            )
        )
    )
)

val advancedSection = listOf(
    CategoryInfo(
        title = "Directories", // opt_category_directories
        options = listOf(
            OptionInfo(
                type = OptionType.Input,
                key = "corePath",
                title = R.string.opt_corepath_title,
                summary = R.string.opt_corepath_summary
            ),
            OptionInfo(
                type = OptionType.Input,
                key = "skinTopPath",
                title = R.string.opt_skin_top_path_title,
                summary = R.string.opt_skin_top_path_summary,
                onChange = { value ->
                    val newValue = value as String

                    if (newValue.trim { it <= ' ' }.isEmpty()) {
                        Config.loadConfig(GlobalManager.getInstance().mainActivity)
                        return@OptionInfo
                    }

                    val file = File(newValue)

                    if (!file.exists() && !file.mkdirs()) {
                        ToastLogger.showText(StringTable.get(R.string.message_error_dir_not_found), true)
                        return@OptionInfo
                    }

                    Config.loadConfig(GlobalManager.getInstance().mainActivity)
                }
            ),
            OptionInfo(
                type = OptionType.Input,
                key = "directory",
                title = R.string.opt_directory_title,
                summary = R.string.opt_directory_summary,
                defaultValue = "/sdcard/osu!droid/Songs"
            )
        )
    ),

    CategoryInfo(
        title = "Miscellaneous", // opt_category_miscellaneous
        options = listOf(
            OptionInfo(
                type = OptionType.Checkbox,
                key = "forceMaxRefreshRate",
                title = R.string.opt_force_max_refresh_rate_title,
                summary = R.string.opt_force_max_refresh_rate_summary,
                defaultValue = false,
                onAttach = { component ->
                    // Obtaining supported refresh rates is only available on Android 12 and above.
                    // See https://developer.android.com/reference/android/view/Display.Mode#getAlternativeRefreshRates().
                    component.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                }
            ),
            OptionInfo(
                type = OptionType.Checkbox,
                key = "safebeatmapbg",
                title = R.string.opt_safe_beatmap_bg_title,
                summary = R.string.opt_safe_beatmap_bg_summary,
                defaultValue = false
            )
        )
    )
)

// Constants
const val REGISTER_URL: String = "https://${OnlineManager.hostname}/user/?action=register"

// Helper functions
fun loadSkin(context: Context, path: String): Job {
    val loading = LoadingFragment()

    loading.isDismissOnBackPress = false
    loading.show()

    return async {
        BeatmapSkinManager.getInstance().clearSkin()
        Config.setSkinPath(path)
        ResourceManager.getInstance().loadSkin(path)
        GlobalManager.getInstance().engine.textureManager.reloadTextures()

        mainThread {
            loading.dismiss()
            context.startActivity(Intent(context, MainActivity::class.java))
            Snackbar.make(
                GlobalManager.getInstance().mainActivity.window.decorView,
                R.string.message_loaded_skin,
                1500
            ).show()
        }
    }
}
