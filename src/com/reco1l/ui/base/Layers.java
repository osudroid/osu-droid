package com.reco1l.ui.base;

public enum Layers {
    Screen(Identifiers.Platform_Screen),
    Overlay(Identifiers.Platform_Overlay),
    Background(Identifiers.Platform_Background);

    private final int mContainerID;

    Layers(int container) {
        mContainerID = container;
    }

    int getContainerID() {
        return mContainerID;
    }
}
