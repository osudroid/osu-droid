package com.reco1l.interfaces;

import com.reco1l.game.TimingWrapper;
import com.reco1l.management.BitmapManager;
import com.reco1l.GameEngine;
import com.reco1l.management.MusicManager;
import com.reco1l.utils.helpers.OnlineHelper;
import com.reco1l.ui.platform.FragmentPlatform;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.skins.SkinManager;

// Created by Reco1l on 25/6/22 00:39

public interface IReferences {

    // TODO Remove this, GlobalManager is useless now.
    GlobalManager global = GlobalManager.getInstance();

    MainActivity activity = MainActivity.getInstance();

    ModMenu modMenu = ModMenu.getInstance();
    GameEngine engine = GameEngine.getInstance();
    OnlineManager online = OnlineManager.getInstance();
    SkinManager skinManager = SkinManager.getInstance();
    LibraryManager library = LibraryManager.getInstance();
    ScoreLibrary scoreLibrary = ScoreLibrary.getInstance();
    ResourceManager resources = ResourceManager.getInstance();
    OnlineScoring onlineScoring = OnlineScoring.getInstance();
    FragmentPlatform platform = FragmentPlatform.getInstance();
    PropertiesLibrary properties = PropertiesLibrary.getInstance();

    BitmapManager bitmapManager = BitmapManager.getInstance();
    OnlineHelper onlineHelper = OnlineHelper.getInstance();

    MusicManager musicManager = MusicManager.getInstance();
    TimingWrapper timingWrapper = TimingWrapper.getInstance();
}
