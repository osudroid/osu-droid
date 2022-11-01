package com.reco1l.ui.fragments.extras;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.SkinPathPreference;
import com.google.android.material.snackbar.Snackbar;
import com.reco1l.enums.Screens;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;
import com.reco1l.ui.data.DialogTable;
import com.reco1l.UI;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.io.File;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineInitializer;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 18/7/22 22:13

public class SettingsMenu extends UIFragment {

    public static SettingsMenu instance;

    protected Tabs currentTab;

    private FrameLayout container;
    private SettingsFragment fragment;
    private View body, navbar, tabIndicator, layer, loading;

    private int panelWidth, navbarWidth;
    private boolean reloadBackground = false;

    //--------------------------------------------------------------------------------------------//

    private enum Tabs {
        general,
        appearance,
        gameplay,
        graphics,
        sounds,
        library,
        advanced,
    }

    @Override
    protected String getPrefix() {
        return "sm";
    }

    @Override
    protected int getLayout() {
        return R.layout.settings_panel;
    }

    //--------------------------------------------------------------------------------------------//

    private final OsuAsyncCallback loadSettings = new OsuAsyncCallback() {
        @Override
        public void run() {
            mActivity.runOnUiThread(() ->
                    platform.manager.beginTransaction()
                            .replace(container.getId(), fragment)
                            .commit());
        }

        @Override
        public void onComplete() {
            new Animation(loading).fade(1, 0).scale(1, 0.8f)
                    .play(200);
            new Animation(container).fade(0, 1)
                    .delay(200)
                    .play(200);
        }
    };

    @Override
    protected void onLoad() {
        setDismissMode(true, true);

        panelWidth = (int) Resources.dimen(R.dimen.settingsPanelWidth);
        navbarWidth = (int) Resources.dimen(R.dimen.settingsPanelNavBarWidth);

        if (fragment == null)
            fragment = new SettingsFragment();

        tabIndicator = find("tabIndicator");
        container = find("container");
        navbar = find("navbar");
        layer = find("layer");
        body = find("body");

        new Animation(rootBackground).fade(0, 1)
                .play(300);
        new Animation(platform.renderView).moveX(0, -80)
                .play(300);

        new Animation(navbar).moveX(navbarWidth, 0)
                .interpolator(Easing.OutExpo)
                .play(350);

        new Animation(layer).moveX(panelWidth + navbarWidth, -navbarWidth)
                .interpolator(Easing.OutExpo)
                .play(350);
        new Animation(body).moveX(panelWidth + navbarWidth, -navbarWidth)
                .interpolator(Easing.OutExpo)
                .delay(50)
                .play(400);

        loading = find("loading");
        currentTab = Tabs.general;

        new Animation(loading).fade(0, 1).scale(0.8f, 1)
                .runOnEnd(() -> new AsyncTaskLoader().execute(loadSettings))
                .delay(200)
                .play(200);

        bindTouchListener(find("close"), () -> {
            unbindTouchListeners();
            close();
        });

        for (Tabs tab : Tabs.values()) {
            bindTouchListener(find(tab.name()), () -> navigateTo(tab));
        }
    }

    private void navigateTo(Tabs tab) {
        if (currentTab == tab)
            return;

        currentTab = tab;
        final boolean toTop = tabIndicator.getTranslationY() > find(tab.name()).getY();

        new Animation(tabIndicator)
                .moveY(tabIndicator.getTranslationY(), find(tab.name()).getY())
                .play(400);

        new Animation(container).moveY(0, toTop ? 80 : -80).fade(1, 0)
                .runOnEnd(() -> fragment.navigate(tab))
                .play(160);

        new Animation(container).moveY(toTop ? -80 : 80, 0).fade(0, 1)
                .delay(160)
                .play(300);

    }

    //--------------------------------------------------------------------------------------------//

    private void applySettings() {
        Config.loadConfig(mActivity);
        onlineHelper.update();
        OnlineScoring.getInstance().login();

        if (reloadBackground && engine.currentScreen == Screens.MAIN) {
            // global.getMainScene().loadTimeingPoints(false);
            reloadBackground = false;
        }

        global.getSongService().setGaming(false);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isShowing)
            return;

        currentTab = null;

        new Animation(container).fade(1, 0)
                .runOnEnd(() -> platform.manager.beginTransaction().remove(fragment).commit())
                .play(300);

        new Animation(platform.renderView).moveX(-80, 0)
                .play(400);
        new Animation(rootBackground).fade(1, 0)
                .play(300);

        new Animation(body).moveX(-navbarWidth, panelWidth + navbarWidth)
                .interpolator(Easing.InExpo)
                .play(350);
        new Animation(layer).moveX(-navbarWidth, panelWidth + navbarWidth)
                .interpolator(Easing.InExpo)
                .delay(50)
                .play(400);

        new Animation(navbar).moveX(0, navbarWidth)
                .interpolator(Easing.OutExpo)
                .runOnEnd(() -> {
                    super.close();
                    applySettings();
                })
                .delay(400)
                .play(200);
    }

    //--------------------------------------------------------------------------------------------//

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings_general, rootKey);
        }

        public void navigate(Tabs tab) {
            switch (tab) {
                case general: setPreferencesFromResource(R.xml.settings_general, null);
                    break;
                case appearance: setPreferencesFromResource(R.xml.settings_appearance, null);
                    break;
                case gameplay: setPreferencesFromResource(R.xml.settings_gameplay, null);
                    break;
                case graphics: setPreferencesFromResource(R.xml.settings_graphics, null);
                    break;
                case sounds: setPreferencesFromResource(R.xml.settings_sounds, null);
                    break;
                case library: setPreferencesFromResource(R.xml.settings_library, null);
                    break;
                case advanced: setPreferencesFromResource(R.xml.settings_advanced, null);
                    break;
            }
            loadPreferences(tab);
        }

        public void loadPreferences(Tabs tab) {

            // General
            if (tab == Tabs.general) {

                Preference register = findPreference("registerAcc");
                // Preference login = findPreference("login"); //TODO login dialog

                if (register == null)
                    return;

                register.setOnPreferenceClickListener(p -> {
                    OnlineInitializer initializer = new OnlineInitializer(getActivity());
                    initializer.createInitDialog();
                    return true;
                });
            }

            // Appearance
            if (tab == Tabs.appearance) {
                SkinPathPreference skinPath = findPreference("skinPath");
                CheckBoxPreference comboColor = findPreference("useCustomColors");

                if (skinPath != null) {
                    skinPath.reloadSkinList();
                    skinPath.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (global.getSkinNow().equals(newValue.toString()))
                            return true;

                        global.setSkinNow(Config.getSkinPath());
                        skinManager.clearSkin();
                        resources.loadSkin(newValue.toString());
                        engine.getTextureManager().reloadTextures();
                        mActivity.startActivity(new Intent(mActivity, MainActivity.class));

                        Snackbar.make(mActivity.findViewById(android.R.id.content),
                                StringTable.get(R.string.message_loaded_skin), 1500).show();
                        return true;
                    });
                }

                if (comboColor != null) {
                    for (int i = 1; i <= 4; i++) {
                        ColorPickerPreference color = findPreference("combo" + i);
                        if (color != null)
                            color.setEnabled(comboColor.isChecked());
                    }

                    comboColor.setOnPreferenceChangeListener((p, val) -> {
                        for (int i = 1; i <= 4; i++) {
                            ColorPickerPreference color = findPreference("combo" + i);
                            if (color != null)
                                color.setEnabled(Boolean.parseBoolean(val.toString()));
                        }
                        return true;
                    });
                }
            }

            // Graphics
            if (tab == Tabs.graphics) {
                CheckBoxPreference dither = findPreference("dither");
                ListPreference background = findPreference("background");

                final boolean oldValue = Config.isUseDither();

                if (dither != null)
                    dither.setOnPreferenceChangeListener((p, val) -> {
                        if (Config.isUseDither() != (boolean) val) {
                            DialogBuilder builder = DialogTable.restart();

                            builder.onClose = () -> dither.setChecked(oldValue);
                            builder.addButton("Cancel", Dialog::close);

                            new Dialog(builder).closeExtras(false).show();
                        }
                        return true;
                    });

                if (background != null)
                    background.setOnPreferenceChangeListener((p, val) -> {
                        if (Config.getBackgroundQuality() != Integer.parseInt(val.toString()))
                            UI.settingsPanel.reloadBackground = true;
                        return true;
                    });
            }

            // Sounds
            if (tab == Tabs.sounds) {
                SeekBarPreference bgmVolume = findPreference("bgmvolume");

                if (bgmVolume != null) {
                    bgmVolume.setOnPreferenceChangeListener((p, val) -> {
                        global.getSongService().setVolume(Float.parseFloat(val.toString()) / 100f);
                        return true;
                    });
                }
            }

            // Library
            if (tab == Tabs.library) {
                Preference clearProperties = findPreference("clear_properties");
                Preference clearCache = findPreference("clear");

                if (clearProperties == null || clearCache == null)
                    return;

                clearProperties.setOnPreferenceClickListener(p -> {
                    library.clearCache();
                    return true;
                });

                clearCache.setOnPreferenceClickListener(p -> {
                    properties.clear(mActivity);
                    return true;
                });
            }

            // Advanced
            if (tab == Tabs.advanced) {
                EditTextPreference skinTopPath = findPreference("skinTopPath");

                if (skinTopPath == null)
                    return;

                skinTopPath.setOnPreferenceChangeListener((p, newValue) -> {

                    if (newValue.toString().trim().length() == 0) {
                        skinTopPath.setText(Config.getCorePath() + "Skin/");
                        Config.loadConfig(mActivity);
                        return false;
                    }

                    File file = new File(newValue.toString());
                    if (!file.exists() && !file.mkdirs()) {
                        ToastLogger.showText(StringTable.get(R.string.message_error_dir_not_found), true);
                        return false;
                    }

                    skinTopPath.setText(newValue.toString());
                    Config.loadConfig(mActivity);
                    return false;
                });
            }
        }
    }
}
