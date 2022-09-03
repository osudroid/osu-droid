package com.reco1l.utils.interfaces;

import com.reco1l.BitmapManager;
import com.reco1l.EngineMirror;
import com.reco1l.ui.data.helpers.OnlineHelper;
import com.reco1l.ui.platform.FragmentPlatform;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.skins.SkinManager;

// Created by Reco1l on 25/6/22 00:39

public interface IMainClasses {

    // Most of them got initialized once they are called or when MainActivity is created.
    GlobalManager global = GlobalManager.getInstance();
    MainActivity mActivity = global.getMainActivity();
    EngineMirror engine = (EngineMirror) mActivity.getEngine();

    ModMenu modMenu = ModMenu.getInstance();
    OnlineManager online = OnlineManager.getInstance();
    SkinManager skinManager = SkinManager.getInstance();
    LibraryManager library = LibraryManager.getInstance();
    ScoreLibrary scoreLibrary = ScoreLibrary.getInstance();
    ResourceManager resources = ResourceManager.getInstance();
    FragmentPlatform platform = FragmentPlatform.getInstance();
    PropertiesLibrary properties = PropertiesLibrary.getInstance();

    BitmapManager bitmapManager = BitmapManager.getInstance();
    OnlineHelper onlineHelper = OnlineHelper.getInstance();
}
