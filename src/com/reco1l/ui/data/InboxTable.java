package com.reco1l.ui.data;

// Created by Reco1l on 30/6/22 19:20

import com.reco1l.utils.IMainClasses;
import com.reco1l.utils.UI;

import ru.nsu.ccfit.zuev.osu.online.OnlineManager;

public class InboxTable implements IMainClasses, UI {

    private static GameNotification online;

    //--------------------------------------------------------------------------------------------//

    public static void online(String state) {
        if (online == null)
            online = new GameNotification("Online Account");

        if (state == null) {
            online.message = "Logging in...";
            online.showProgressBar = true;
            online.isProgressBarIndeterminate = true;
            inbox.add(online);
            return;
        }

        if (state.startsWith("try")) {
            online.message = "Logging in...\n(Try: " + state.charAt(3) + ")";
            online.update();
            return;
        }

        switch (state) {
            case "fail":
                online.message = "Login failed, retrying in 5 seconds...";
                break;
            case "success":
                online.message = "Logged in successfully";
                online.showProgressBar = false;
                break;
            case "error":
                online.message = "Cannot log in\n" + OnlineManager.getInstance().getFailMessage();
                online.showProgressBar = false;
                break;
        }
        online.update();
    }
}
