package com.reco1l;

import main.osu.GlobalManager;
import main.osu.LibraryManager;
import main.osu.PropertiesLibrary;
import main.osu.ResourceManager;
import main.osu.online.OnlineManager;
import main.osu.scoring.ScoreLibrary;
import main.skins.SkinManager;

// Created by Reco1l on 25/6/22 00:39

public interface References {

    SkinManager skinManager = SkinManager.getInstance();
    GlobalManager globalManager = GlobalManager.getInstance();
    LibraryManager libraryManager = LibraryManager.getInstance();
    ResourceManager resourcesManager = ResourceManager.getInstance();

    ScoreLibrary scoreLibrary = ScoreLibrary.getInstance();
    PropertiesLibrary propertiesLibrary = PropertiesLibrary.getInstance();

    OnlineManager onlineManager = OnlineManager.getInstance();
}
