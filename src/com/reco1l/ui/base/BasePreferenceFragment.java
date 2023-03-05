package com.reco1l.ui.base;
// Created by Reco1l on 21/12/2022, 06:24

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.reco1l.Game;

import static androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {

    private FragmentManager mManager;

    //--------------------------------------------------------------------------------------------//

    public BasePreferenceFragment() {}

    //--------------------------------------------------------------------------------------------//

    protected @XmlRes int getPreferenceXML() {
        return -1;
    }

    protected void onLoad() {}

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (getPreferenceXML() != -1) {
            setPreferencesFromResource(getPreferenceXML(), rootKey);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDivider(new ColorDrawable(Color.TRANSPARENT));
        setDividerHeight(0);
    }

    @Override
    protected void onBindPreferences() {
        onLoad();
    }

    //--------------------------------------------------------------------------------------------//

    public <T extends Preference> T find(String key) {
        return findPreference(key);
    }

    //--------------------------------------------------------------------------------------------//

    private void remove() {
        FragmentManager manager = mManager;

        if (manager == null) {
            manager = Game.platform.getManager();
        }

        manager.popBackStackImmediate(null, POP_BACK_STACK_INCLUSIVE);

        manager.beginTransaction()
                .remove(this)
                .commitNowAllowingStateLoss();

        manager.executePendingTransactions();
    }

    //--------------------------------------------------------------------------------------------//

    public void add(View container) {
        add(container, Game.platform.getManager());
    }

    public void add(View container, FragmentManager manager) {
        mManager = manager;

        if (isAdded()) {
            remove();
        }

        manager.beginTransaction()
                .add(container.getId(), this)
                .commit();
    }

    //--------------------------------------------------------------------------------------------//

    public void replace(View container) {
        replace(container, Game.platform.getManager());
    }

    public void replace(View container, FragmentManager manager) {
        mManager = manager;

        if (isAdded()) {
            remove();
        }

        manager.beginTransaction()
                .replace(container.getId(), this)
                .commit();
    }
}
