package com.reco1l;

import com.reco1l.ui.fragments.BeatmapList;
import com.reco1l.ui.fragments.BeatmapPanel;
import com.reco1l.ui.fragments.extras.NotificationCenter;
import com.reco1l.ui.fragments.LoadingScene;
import com.reco1l.ui.fragments.MainMenu;
import com.reco1l.ui.fragments.extras.MusicPlayer;
import com.reco1l.ui.fragments.extras.SettingsMenu;
import com.reco1l.ui.fragments.TopBar;
import com.reco1l.ui.fragments.extras.UserProfile;
import com.reco1l.ui.platform.UIFragment;

// Created by Reco1l on 29/6/22 22:38

public final class UI {

    public static TopBar topBar = getTopBar();
    public static NotificationCenter notificationCenter = getNotificationCenter();
    public static MusicPlayer musicPlayer = getMusicPlayer();
    public static LoadingScene loadingScene = getLoadingScene();
    public static MainMenu mainMenu = getMainMenu();
    public static UserProfile userProfile = getUserProfile();
    public static SettingsMenu settingsPanel = getSettingsMenu();
    public static BeatmapPanel beatmapPanel = getBeatmapPanel();
    public static BeatmapList beatmapList = getBeatmapList();

    //--------------------------------------------------------------------------------------------//

    public static UIFragment[] getExtras() {
        return new UIFragment[] {
                getNotificationCenter(),
                getMusicPlayer(),
                getUserProfile(),
                getSettingsMenu()
        };
    }

    //--------------------------------------------------------------------------------------------//

    private static TopBar getTopBar() {
        if (TopBar.instance == null) {
            TopBar.instance = new TopBar();
        }
        return TopBar.instance;
    }

    private static NotificationCenter getNotificationCenter() {
        if (NotificationCenter.instance == null) {
            NotificationCenter.instance = new NotificationCenter();
        }
        return NotificationCenter.instance;
    }

    private static MusicPlayer getMusicPlayer() {
        if (MusicPlayer.instance == null) {
            MusicPlayer.instance = new MusicPlayer();
        }
        return MusicPlayer.instance;
    }

    private static LoadingScene getLoadingScene() {
        if (LoadingScene.instance == null) {
            LoadingScene.instance = new LoadingScene();
        }
        return LoadingScene.instance;
    }

    private static MainMenu getMainMenu() {
        if (mainMenuInstance == null) {
            mainMenuInstance = new MainMenu();
        }
        return mainMenuInstance;
    }

    private static UserProfile getUserProfile() {
        if (userProfileInstance == null) {
            userProfileInstance = new UserProfile();
        }
        return userProfileInstance;
    }

    private static SettingsMenu getSettingsMenu() {
        if (settingsMenuInstance == null) {
            settingsMenuInstance = new SettingsMenu();
        }
        return settingsMenuInstance;
    }

    private static BeatmapPanel getBeatmapPanel() {
        if (beatmapPanelInstance == null) {
            beatmapPanelInstance = new BeatmapPanel();
        }
        return beatmapPanelInstance;
    }

    private static BeatmapList getBeatmapList() {
        if (beatmapListInstance == null) {
            beatmapListInstance = new BeatmapList();
        }
        return beatmapListInstance;
    }

    //--------------------------------------------------------------------------------------------//

    private static TopBar topBarInstance;
    private static NotificationCenter notificationCenterInstance;
    private static MusicPlayer musicPlayerInstance;
    private static LoadingScene loadingSceneInstance;
    private static MainMenu mainMenuInstance;
    private static UserProfile userProfileInstance;
    private static SettingsMenu settingsMenuInstance;
    private static BeatmapPanel beatmapPanelInstance;
    private static BeatmapList beatmapListInstance;
}
