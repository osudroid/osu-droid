package com.reco1l.ui.platform;

import com.reco1l.ui.fragments.BeatmapList;
import com.reco1l.ui.fragments.BeatmapPanel;
import com.reco1l.ui.fragments.extras.NotificationCenter;
import com.reco1l.ui.fragments.LoadingScene;
import com.reco1l.ui.fragments.MainMenu;
import com.reco1l.ui.fragments.extras.MusicPlayer;
import com.reco1l.ui.fragments.extras.SettingsMenu;
import com.reco1l.ui.fragments.TopBar;
import com.reco1l.ui.fragments.extras.UserProfile;
import com.reco1l.ui.platform.UIManager;

// Created by Reco1l on 29/6/22 22:38

public interface UI {

    // Their instances get initialized once the engine get started.
    TopBar topBar = UIManager.getTopBar();
    NotificationCenter notificationCenter = UIManager.getInbox();
    MusicPlayer musicPlayer = UIManager.getMusicPlayer();
    LoadingScene loadingScene = UIManager.getLoadingScene();
    MainMenu mainMenu = UIManager.getMainMenu();
    UserProfile userProfile = UIManager.getUserProfile();
    SettingsMenu settingsPanel = UIManager.getSettingsPanel();
    BeatmapPanel beatmapPanel = UIManager.getBeatmapPanel();
    BeatmapList beatmapList = UIManager.getBeatmapList();
}
