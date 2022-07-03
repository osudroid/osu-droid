package com.reco1l.utils.interfaces;

import com.reco1l.ui.Inbox;
import com.reco1l.ui.MusicPlayer;
import com.reco1l.ui.TopBar;
import com.reco1l.ui.platform.UIManager;

// Created by Reco1l on 29/6/22 22:38

public interface UI {

    // Their instances get initialized once the engine get started.
    TopBar topBar = UIManager.getTopBar();
    Inbox inbox = UIManager.getInbox();
    MusicPlayer musicPlayer = UIManager.getMusicPlayer();
}
