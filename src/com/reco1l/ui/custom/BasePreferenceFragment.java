package com.reco1l.ui.custom;
// Created by Reco1l on 21/12/2022, 06:24

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.XmlRes;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.reco1l.Game;

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {


    //--------------------------------------------------------------------------------------------//

    @XmlRes
    protected abstract int getPreferenceXML();

    protected abstract void onLoad();

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(getPreferenceXML(), rootKey);
        onLoad();
    }

    protected <T extends Preference> T find(String key) {
        return findPreference(key);
    }

    public void show(FrameLayout container) {
        Game.activity.runOnUiThread(() -> {
            FragmentTransaction transaction = Game.platform.manager.beginTransaction();

            transaction.replace(container.getId(), this);
            transaction.commit();
        });
    }
}
