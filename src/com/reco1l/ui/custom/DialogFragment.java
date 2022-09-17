package com.reco1l.ui.custom;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.reco1l.interfaces.IMainClasses;

// Created by Reco1l on 25/7/22 21:51

public class DialogFragment extends Fragment implements IMainClasses {

    public View root;

    private final int layoutId;
    private final OnFragmentLoad onFragmentLoad;

    //--------------------------------------------------------------------------------------------//

    public DialogFragment(@LayoutRes int layoutId, OnFragmentLoad onFragmentLoad) {
        this.layoutId = layoutId;
        this.onFragmentLoad = onFragmentLoad;
    }

    public interface OnFragmentLoad {
        void onLoad(View root);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        root = inflater.inflate(layoutId, container, false);
        onFragmentLoad.onLoad(root);
        return root;
    }
}
