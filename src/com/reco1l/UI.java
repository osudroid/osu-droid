package com.reco1l;

import com.reco1l.ui.fragments.BeatmapCarrousel;
import com.reco1l.ui.fragments.BeatmapPanel;
import com.reco1l.ui.fragments.DebugOverlay;
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
    public static BeatmapCarrousel beatmapCarrousel = getBeatmapList();
    public static DebugOverlay debugOverlay = getDebugOverlay();

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
        if (MainMenu.instance == null) {
            MainMenu.instance = new MainMenu();
        }
        return MainMenu.instance;
    }

    private static UserProfile getUserProfile() {
        if (UserProfile.instance == null) {
            UserProfile.instance = new UserProfile();
        }
        return UserProfile.instance;
    }

    private static SettingsMenu getSettingsMenu() {
        if (SettingsMenu.instance == null) {
            SettingsMenu.instance = new SettingsMenu();
        }
        return SettingsMenu.instance;
    }

    private static BeatmapPanel getBeatmapPanel() {
        if (BeatmapPanel.instance == null) {
            BeatmapPanel.instance = new BeatmapPanel();
        }
        return BeatmapPanel.instance;
    }

    private static BeatmapCarrousel getBeatmapList() {
        if (BeatmapCarrousel.instance == null) {
            BeatmapCarrousel.instance = new BeatmapCarrousel();
        }
        return BeatmapCarrousel.instance;
    }

    private static DebugOverlay getDebugOverlay() {
        if (DebugOverlay.instance == null) {
            DebugOverlay.instance = new DebugOverlay();
        }
        return DebugOverlay.instance;
    }
}
