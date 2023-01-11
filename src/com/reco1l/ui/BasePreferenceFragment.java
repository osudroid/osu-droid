package com.reco1l.ui;
// Created by Reco1l on 21/12/2022, 06:24

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.reco1l.Game;
import org.jetbrains.annotations.NotNull;

import static androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {

    //--------------------------------------------------------------------------------------------//

    @XmlRes
    protected abstract int getPreferenceXML();

    protected abstract void onLoad();

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(getPreferenceXML(), rootKey);
    }

    //--------------------------------------------------------------------------------------------//

    protected <T extends Preference> T find(String key) {
        return findPreference(key);
    }

    //--------------------------------------------------------------------------------------------//

    private void remove() {
        Game.platform.getManager().popBackStackImmediate(null, POP_BACK_STACK_INCLUSIVE);

        Game.platform.transaction()
                .remove(this)
                .commit();

        Game.platform.getManager().executePendingTransactions();
    }

    public void add(View container) {
        remove();

        Game.platform.transaction()
                .add(container.getId(), this)
                .runOnCommit(this::onLoad)
                .commit();
    }

    public void replace(View container) {
        remove();

        Game.platform.transaction()
                .replace(container.getId(), this)
                .runOnCommit(this::onLoad)
                .commit();
    }
}
