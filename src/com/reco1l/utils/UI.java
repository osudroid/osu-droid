package com.reco1l.utils;

import com.reco1l.ui.Inbox;
import com.reco1l.ui.TopBar;

// Created by Reco1l on 29/6/22 22:38

public interface UI {

    // Their instances get initialized once the engine get started.
    TopBar topBar = TopBar.instance;
    Inbox inbox = Inbox.instance;
}
