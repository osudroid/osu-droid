package com.reco1l.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.SkinPathPreference;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;
import com.reco1l.data.tables.DialogTable;
import com.reco1l.UI;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.data.tables.AnimationTable;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.data.tables.ResourceTable;
import com.reco1l.utils.helpers.OnlineHelper;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.io.File;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineInitializer;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 18/7/22 22:13

public final class SettingsMenu extends BaseFragment {

    public static SettingsMenu instance;

    protected Tabs currentTab;

    private FrameLayout container;
    private SettingsFragment fragment;

    private View tabIndicator, layer;
    private CardView body, navigationBar;
    private CircularProgressIndicator loading;

    private int panelWidth, navBarWidth;
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
    protected boolean isOverlay() {
        return true;
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

    private final AsyncTask loadSettings = new AsyncTask() {
        @Override
        public void run() {
            Game.activity.runOnUiThread(() -> {
                FragmentTransaction transaction = Game.platform.manager.beginTransaction();

                transaction.replace(container.getId(), fragment);
                transaction.commit();
            });
        }

        @Override
        public void onComplete() {

            Animation.of(loading)
                    .toAlpha(0)
                    .toScale(0.8f)
                    .play(200);

            Animation.of(container)
                    .toAlpha(1)
                    .delay(200)
                    .play(200);
        }
    };

    @Override
    protected void onLoad() {
        setDismissMode(true, true);

        if (fragment == null) {
            fragment = new SettingsFragment();
        }
        currentTab = Tabs.general;

        panelWidth = (int) ResourceTable.dimen(R.dimen.settingsPanelWidth);
        navBarWidth = (int) ResourceTable.dimen(R.dimen.settingsPanelNavBarWidth);

        tabIndicator = find("tabIndicator");
        navigationBar = find("navbar");
        container = find("container");
        loading = find("loading");
        layer = find("layer");
        body = find("body");

        Game.platform.animate(true, true)
                .toX(-navBarWidth)
                .duration(400);

        AnimationTable.fadeIn(rootBackground).play();

        Animation.of(navigationBar)
                .fromX(navBarWidth)
                .toX(0f)
                .interpolator(Easing.OutExpo)
                .play(350);

        Animation.of(layer)
                .fromX(panelWidth + navBarWidth)
                .toX(0f)
                .interpolator(Easing.OutExpo)
                .play(350);

        Animation.of(body)
                .fromX(panelWidth + navBarWidth)
                .toX(0f)
                .interpolator(Easing.OutExpo)
                .delay(50)
                .play(400);

        Animation.of(loading)
                .fromAlpha(0)
                .toAlpha(1)
                .fromScale(0.8f)
                .toScale(1)
                .runOnEnd(loadSettings::execute)
                .delay(300)
                .play(200);

        for (Tabs tab : Tabs.values()) {
            bindTouchListener(find(tab.name()), () -> navigateTo(tab));
        }
    }

    private void navigateTo(Tabs tab) {
        if (currentTab == tab) {
            return;
        }
        currentTab = tab;

        float y = find(tab.name()).getY();

        AnimationTable.moveY(tabIndicator, y)
                .play(200);

        Animation.of(container)
                .runOnEnd(() -> {
                    Log.i("SettingsMenu", "Switched to tab " + tab.name());
                    fragment.navigate(tab);

                    Animation.of(container)
                            .fromX(80)
                            .toX(0)
                            .toAlpha(1)
                            .play(100);
                })
                .toX(-80f)
                .toAlpha(0)
                .play(100);
    }

    //--------------------------------------------------------------------------------------------//

    private void applySettings() {
        Config.loadConfig(Game.activity);
        OnlineHelper.update();
        OnlineScoring.getInstance().login();

        if (reloadBackground && Game.engine.currentScreen == Screens.Main) {
            UI.background.reload();
            reloadBackground = false;
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded()) {
            return;
        }
        currentTab = null;

        Game.platform.animate(true, true)
                .toX(0)
                .play(400);

        AnimationTable.fadeOut(container)
                .runOnEnd(() -> {
                    FragmentTransaction transaction = Game.platform.manager.beginTransaction();

                    transaction.remove(fragment);
                    transaction.commit();
                })
                .play(300);

        AnimationTable.fadeOut(rootBackground)
                .play();

        Animation.of(body)
                .toX(panelWidth + navBarWidth)
                .interpolator(Easing.OutExpo)
                .play(350);

        Animation.of(layer)
                .toX(panelWidth + navBarWidth)
                .interpolator(Easing.OutExpo)
                .delay(50)
                .play(400);

        Animation.of(navigationBar)
                .toX(navBarWidth)
                .interpolator(Easing.OutExpo)
                .delay(400)
                .runOnEnd(() -> {
                    super.close();
                    applySettings();
                })
                .play(200);
    }

    //--------------------------------------------------------------------------------------------//

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            setDivider(new ColorDrawable(Color.TRANSPARENT));
            setDividerHeight(0);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings_general, rootKey);
        }

        public void navigate(Tabs tab) {
            switch (tab) {
                case general:
                    setPreferencesFromResource(R.xml.settings_general, null);
                    break;
                case appearance:
                    setPreferencesFromResource(R.xml.settings_appearance, null);
                    break;
                case gameplay:
                    setPreferencesFromResource(R.xml.settings_gameplay, null);
                    break;
                case graphics:
                    setPreferencesFromResource(R.xml.settings_graphics, null);
                    break;
                case sounds:
                    setPreferencesFromResource(R.xml.settings_sounds, null);
                    break;
                case library:
                    setPreferencesFromResource(R.xml.settings_library, null);
                    break;
                case advanced:
                    setPreferencesFromResource(R.xml.settings_advanced, null);
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
                        if (Game.globalManager.getSkinNow().equals(newValue.toString()))
                            return true;

                        Game.globalManager.setSkinNow(Config.getSkinPath());
                        Game.skinManager.clearSkin();
                        Game.resourcesManager.loadSkin(newValue.toString());
                        Game.engine.getTextureManager().reloadTextures();
                        Game.activity.startActivity(new Intent(Game.activity, MainActivity.class));

                        Snackbar.make(Game.activity.findViewById(android.R.id.content),
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
                        Game.globalManager.getSongService().setVolume(Float.parseFloat(val.toString()) / 100f);
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
                    Game.libraryManager.clearCache();
                    return true;
                });

                clearCache.setOnPreferenceClickListener(p -> {
                    Game.propertiesLibrary.clear(Game.activity);
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
                        Config.loadConfig(Game.activity);
                        return false;
                    }

                    File file = new File(newValue.toString());
                    if (!file.exists() && !file.mkdirs()) {
                        ToastLogger.showText(StringTable.get(R.string.message_error_dir_not_found), true);
                        return false;
                    }

                    skinTopPath.setText(newValue.toString());
                    Config.loadConfig(Game.activity);
                    return false;
                });
            }
        }
    }
}
