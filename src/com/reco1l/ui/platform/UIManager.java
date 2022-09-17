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

// Created by Reco1l on 2/7/22 22:44

public class UIManager {

    public static boolean isUserInterfaceInit = false;

    public static TopBar topBar;
    public static NotificationCenter notificationCenter;
    public static MusicPlayer musicPlayer;
    public static LoadingScene loadingScene;
    public static MainMenu mainMenu;
    public static UserProfile userProfile;
    public static SettingsMenu settingsPanel;
    public static BeatmapPanel beatmapPanel;
    public static BeatmapList beatmapList;

    //--------------------------------------------------------------------------------------------//

    public static void initialize() {
        topBar = new TopBar();
        notificationCenter = new NotificationCenter();
        musicPlayer = new MusicPlayer();
        loadingScene = new LoadingScene();
        mainMenu = new MainMenu();
        userProfile = new UserProfile();
        settingsPanel = new SettingsMenu();
        beatmapList = new BeatmapList();
        isUserInterfaceInit = true;
    }

    //--------------------------------------------------------------------------------------------//

    /**
     * @return layouts that are "extras", that means panels or dialogs that aren't necessary related to a Scene.
     */
    public static UIFragment[] getExtras() {
        return new UIFragment[] {
                getInbox(),
                getMusicPlayer(),
                getUserProfile(),
                getSettingsPanel()
        };
    }

    //--------------------------------------------------------------------------------------------//

    public static TopBar getTopBar() {
        if (topBar == null) {
            topBar = new TopBar();
        }
        return topBar;
    }

    public static NotificationCenter getInbox() {
        if (notificationCenter == null) {
            notificationCenter = new NotificationCenter();
        }
        return notificationCenter;
    }

    public static MusicPlayer getMusicPlayer() {
        if (musicPlayer == null) {
            musicPlayer = new MusicPlayer();
        }
        return musicPlayer;
    }

    public static LoadingScene getLoadingScene() {
        if (loadingScene == null) {
            loadingScene = new LoadingScene();
        }
        return loadingScene;
    }

    public static MainMenu getMainMenu() {
        if (mainMenu == null) {
            mainMenu = new MainMenu();
        }
        return mainMenu;
    }

    public static UserProfile getUserProfile() {
        if (userProfile == null) {
            userProfile = new UserProfile();
        }
        return userProfile;
    }

    public static SettingsMenu getSettingsPanel() {
        if (settingsPanel == null) {
            settingsPanel = new SettingsMenu();
        }
        return settingsPanel;
    }

    public static BeatmapPanel getBeatmapPanel() {
        if (beatmapPanel == null) {
            beatmapPanel = new BeatmapPanel();
        }
        return beatmapPanel;
    }

    public static BeatmapList getBeatmapList() {
        if (beatmapList == null) {
            beatmapList = new BeatmapList();
        }
        return beatmapList;
    }
}
