package com.reco1l.utils;

import com.reco1l.EngineBridge;
import com.reco1l.ui.platform.FragmentPlatform;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;

// Created by Reco1l on 25/6/22 00:39

public interface IMainClasses {

    // Be sure of call this variables once their classes get initialized.
    FragmentPlatform platform = FragmentPlatform.getInstance();
    GlobalManager global = GlobalManager.getInstance();
    MainActivity mActivity = global.getMainActivity();
    LibraryManager library = LibraryManager.getInstance();
    ResourceManager resources = ResourceManager.getInstance();
    OnlineManager online = OnlineManager.getInstance();
    EngineBridge engine = (EngineBridge) mActivity.getEngine();
}
