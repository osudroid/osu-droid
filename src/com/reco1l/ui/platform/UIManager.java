package com.reco1l.ui.platform;

import com.reco1l.ui.Inbox;
import com.reco1l.ui.LoadingScene;
import com.reco1l.ui.MainMenu;
import com.reco1l.ui.MusicPlayer;
import com.reco1l.ui.SettingsMenu;
import com.reco1l.ui.TopBar;
import com.reco1l.ui.UserProfile;

// Created by Reco1l on 2/7/22 22:44

public class UIManager {

    public static boolean isUserInterfaceInit = false;

    public static TopBar topBar;
    public static Inbox inbox;
    public static MusicPlayer musicPlayer;
    public static LoadingScene loadingScene;
    public static MainMenu mainMenu;
    public static UserProfile userProfile;
    public static SettingsMenu settingsPanel;

    //--------------------------------------------------------------------------------------------//

    public static void initialize() {
        topBar = new TopBar();
        inbox = new Inbox();
        musicPlayer = new MusicPlayer();
        loadingScene = new LoadingScene();
        mainMenu = new MainMenu();
        userProfile = new UserProfile();
        settingsPanel = new SettingsMenu();
        isUserInterfaceInit = true;
    }

    public static void loadResources() {
        MusicPlayer.onResourcesLoad();
    }

    //--------------------------------------------------------------------------------------------//

    /**
     * @return layouts that are "extras", that means panels or dialogs that aren't necessary related to a Scene.
     */
    public static BaseLayout[] getExtras() {
        return new BaseLayout[] {
                getInbox(),
                getMusicPlayer(),
                getUserProfile(),
                getSettingsPanel()
        };
    }

    public static TopBar getTopBar() {
        if (topBar == null) {
            topBar = new TopBar();
        }
        return topBar;
    }

    public static Inbox getInbox() {
        if (inbox == null) {
            inbox = new Inbox();
        }
        return inbox;
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
}
