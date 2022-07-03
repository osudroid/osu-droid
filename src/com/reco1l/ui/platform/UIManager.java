package com.reco1l.ui.platform;

import com.reco1l.ui.Inbox;
import com.reco1l.ui.MusicPlayer;
import com.reco1l.ui.TopBar;

// Created by Reco1l on 2/7/22 22:44

public class UIManager {

    public static boolean isUserInterfaceInit = false;

    public static TopBar topBar;
    public static Inbox inbox;
    public static MusicPlayer musicPlayer;

    public static final BaseLayout[] extras = {
            inbox,
            musicPlayer
    };

    //--------------------------------------------------------------------------------------------//

    public static void initialize() {
        topBar = new TopBar();
        inbox = new Inbox();
        musicPlayer = new MusicPlayer();
        isUserInterfaceInit = true;
    }

    public static void loadResources() {
        MusicPlayer.onResourcesLoad();
    }

    //--------------------------------------------------------------------------------------------//

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
}
