package com.reco1l.ui.data;

// Created by Reco1l on 30/6/22 19:20

import com.reco1l.UI;

import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.R;

/**
 * This class contains easy access to show notifications from other classes without adding a lot of lines.
 */
public class NotificationTable {

    private static GameNotification online;

    //--------------------------------------------------------------------------------------------//

    public static void debug(String text) {
        GameNotification notification = new GameNotification("Debug");
        notification.message = text;
        notification.isSilent = true;
        UI.notificationCenter.add(notification);
    }

    //--------------------------------------------------------------------------------------------//

    public static void accountLogIn(String state, int i) {
        if (online == null)
            online = new GameNotification("Online Account");

        if (state == null) {
            online.message = "Logging in...";
            online.showProgressBar = true;
            online.isProgressBarIndeterminate = true;
            UI.notificationCenter.add(online);
            return;
        }

        switch (state) {
            case "try":
                online.message = "Logging in...\n(Try: " + (i + 1) + ")";
                online.update();
                break;
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
