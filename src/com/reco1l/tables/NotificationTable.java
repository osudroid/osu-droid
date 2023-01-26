package com.reco1l.tables;

// Created by Reco1l on 30/6/22 19:20

import android.content.Intent;
import android.net.Uri;

import com.reco1l.Game;
import com.reco1l.data.GameNotification;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;

import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.R;

public class NotificationTable {

    //--------------------------------------------------------------------------------------------//

    public static void debug(String text) {
        GameNotification.of("Debug")
                .setMessage(text)
                .commit();
    }


    public static void exception(Exception e) {
        GameNotification.of(e.getClass().getSimpleName())
                .setMessage(e.getMessage())
                .runOnClick(() -> {
                    DialogBuilder builder = DialogTable.stacktrace(e);
                    new Dialog(builder).show();
                })
                .commit();
    }

    //--------------------------------------------------------------------------------------------//

    public static void accountLogIn(String state, int i) {
        GameNotification n = GameNotification.of("Online");

        if (state == null) {
            n.setMessage("Logging in...");
            n.showProgress(true);
            n.setProgress(-1);
            n.commit();
            return;
        }

        switch (state) {
            case "try":
                n.setMessage("Logging in...\n(Try: " + (i + 1) + ")");
                break;
            case "fail":
                n.setMessage("Login failed, retrying in 5 seconds...");
                break;
            case "success":
                n.setMessage("Logged in successfully");
                n.showProgress(false);
                break;
            case "error":
                n.setMessage("Cannot log in\n" + OnlineManager.getInstance().getFailMessage());
                n.showProgress(false);
                break;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static void update(String url) {
        GameNotification.of("Update")
                .setMessage(Res.str(R.string.update_dialog_message) + "\nClick to update!")
                .runOnClick(() -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .setPackage("com.edlplan.ui");

                    Game.activity.startActivity(intent);
                })
                .commit();
    }
}
