package com.reco1l.tables;

// Created by Reco1l on 30/6/22 19:20

import android.content.Intent;
import android.net.Uri;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.data.GameNotification;

import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.R;

public class NotificationTable {

    private static GameNotification
            online;

    //--------------------------------------------------------------------------------------------//

    public static void debug(String text) {
        GameNotification notification = new GameNotification("Debug");
        notification.message = text;
        UI.notificationCenter.add(notification);
    }

    //--------------------------------------------------------------------------------------------//

    public static void accountLogIn(String state, int i) {
        if (online == null)
            online = new GameNotification("Online Account");

        if (state == null) {
            online.message = "Logging in...";
            online.showProgress = true;
            online.hasIndeterminateProgress = true;
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
                online.showProgress = false;
                break;
            case "error":
                online.message = "Cannot log in\n" + OnlineManager.getInstance().getFailMessage();
                online.showProgress = false;
                break;
        }
        online.update();
    }

    //--------------------------------------------------------------------------------------------//

    public static void update(String url) {
        GameNotification update = new GameNotification("Update");

        update.message = Res.str(R.string.update_dialog_message) + "\nClick to update!";
        update.runOnClick = () -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            Game.activity.startActivity(intent);
        };

        UI.notificationCenter.add(update);
    }
}
