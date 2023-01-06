package com.reco1l.view;
// Created by Reco1l on 08/12/2022, 17:02

import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.reco1l.tables.Res;

public interface BaseView {

    //--------------------------------------------------------------------------------------------//

    void onCreate(AttributeSet attrs);

    default View getView() {
        return null;
    }

    default int[] getStyleable() {
        return null;
    }

    //--------------------------------------------------------------------------------------------//

    default void onManageAttributes(TypedArray a) {}

    default void onResize(int w, int h) {}

    //--------------------------------------------------------------------------------------------//

    default void handleAttributes(AttributeSet attrs) {
        if (attrs == null || getStyleable() == null || getView() == null) {
            return;
        }

        TypedArray a = getView().getContext().obtainStyledAttributes(attrs, getStyleable());
        onManageAttributes(a);
        a.recycle();
    }

    default int sdp(int dp) {
        if (getView() == null || getView().isInEditMode()) {
            return dp * 3;
        }
        return Res.sdp(dp);
    }
}
