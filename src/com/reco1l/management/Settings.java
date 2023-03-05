package com.reco1l.management;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import androidx.annotation.XmlRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import com.reco1l.Game;
import com.reco1l.preference.CheckPreference;
import com.reco1l.preference.FieldPreference;
import com.reco1l.preference.MenuPreference;
import com.reco1l.ui.base.BasePreferenceFragment;
import com.reco1l.ui.scenes.loader.fragments.LoaderFragment;
import com.reco1l.utils.execution.Async;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class Settings {

    private final static SharedPreferences mPreferences;

    //--------------------------------------------------------------------------------------------//

    static {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(Game.activity);
    }

    //--------------------------------------------------------------------------------------------//

    public static <T> T get(String key) {
        return get(key, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, T defValue) {
        Map<String, ?> map = mPreferences.getAll();

        if (!map.containsKey(key) || map.get(key) == null) {
            return defValue;
        }
        return (T) map.get(key);
    }

    public static Editor edit() {
        return mPreferences.edit();
    }

    //--------------------------------------------------------------------------------------------//

    public static abstract class Wrapper {

        public abstract @XmlRes int getPreferenceXML();

        public void onLoad(BasePreferenceFragment parent) {
        }

    }

    //--------------------------------------------------------------------------------------------//

    public static class General extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_general;
        }

    }

    //--------------------------------------------------------------------------------------------//

    public static class Appearance extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_appearance;
        }

        @Override
        public void onLoad(BasePreferenceFragment f) {
            MenuPreference selector = f.find("skinPath");

            Map<String, String> skins = new HashMap<>();

            skins.put("Default", Config.getSkinTopPath());
            skins.putAll(Config.getSkins());

            selector.setEntries(skins);

            skins.forEach((k, e) -> {
                if (Objects.equals(Config.getSkinPath(), e)) {
                    selector.setText(e);
                }
            });

            selector.setOnPreferenceChangeListener((p, v) -> {

                Async.run(() -> {
                    LoaderFragment fragment = new LoaderFragment();
                    fragment.show(true);

                    Game.skinManager.clearSkin();
                    Game.resourcesManager.loadSkin((String) v);
                    Game.engine.getTextureManager().reloadTextures();

                    fragment.close();
                });
                return true;
            });
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Gameplay extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_gameplay;
        }

    }

    //--------------------------------------------------------------------------------------------//

    public static class Graphics extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_graphics;
        }

    }

    //--------------------------------------------------------------------------------------------//

    public static class Sounds extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_sounds;
        }

        @Override
        public void onLoad(BasePreferenceFragment f) {
            SeekBarPreference bgmVolume = f.find("bgmvolume");

            if (bgmVolume != null) {
                bgmVolume.setOnPreferenceChangeListener((p, val) -> {
                    Game.musicManager.setVolume((int) val / 100f);
                    return true;
                });
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Library extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_library;
        }

        @Override
        public void onLoad(BasePreferenceFragment f) {
            Preference clearProperties = f.find("clear_properties");
            Preference clearCache = f.find("clear");

            if (clearProperties != null) {
                clearProperties.setOnPreferenceClickListener(p -> {
                    Game.libraryManager.clearCache();
                    return true;
                });
            }

            if (clearCache != null) {
                clearCache.setOnPreferenceClickListener(p -> {
                    Game.propertiesLibrary.clear(Game.activity);
                    return true;
                });
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Advanced extends Wrapper {

        public int getPreferenceXML() {
            return R.xml.settings_advanced;
        }

        @Override
        public void onLoad(BasePreferenceFragment f) {
            CheckPreference external = f.find("external");
            FieldPreference path = f.find("corePath");

            if (external == null || path == null) {
                return;
            }

            File[] volumes = Game.activity.getExternalFilesDirs(null);
            boolean hasExternal = volumes.length > 1 && volumes[1] != null;

            external.setEnabled(hasExternal);
            if (!hasExternal) {
                external.setChecked(false);
            }

            external.setOnPreferenceChangeListener((p, value) -> {
                Config.loadPaths();

                path.setEnabled(!(boolean) value);
                path.setText(Config.getCorePath());
                return true;
            });

            path.setDefaultValue(Config.getDefaultCorePath());
            path.setEnabled(!external.isChecked());
            path.setText(Config.getCorePath());

            path.setOnFocusLostListener(() -> {
                Config.loadPaths();
                path.setValue(Config.getCorePath());
            });
        }
    }
}
