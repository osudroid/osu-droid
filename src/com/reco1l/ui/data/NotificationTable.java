package com.reco1l.ui.data;

// Created by Reco1l on 30/6/22 19:20

import com.reco1l.EngineMirror;
import com.reco1l.utils.interfaces.IMainClasses;
import com.reco1l.utils.interfaces.UI;

import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.R;

/**
 * This class contains easy access to show notifications from other classes without adding a lot of lines.
 */
public class NotificationTable implements IMainClasses, UI {

    // Single instance notifications
    private static GameNotification
            online,
            importing;

    // Notification for debugging
    //--------------------------------------------------------------------------------------------//

    public static void debug(String text) {
        GameNotification notification = new GameNotification("Debug");
        notification.message = text;
        notification.isSilent = true;
        inbox.add(notification);
    }

    // Notification for beatmap importing (Currently not used)
    //--------------------------------------------------------------------------------------------//
    private static int beatmapsFounded = 0;

    public static void beatmap(String state, int i, String s) {
        if (engine.currentScene != EngineMirror.Scenes.LOADING_SCREEN)
            return;

        if (importing == null)
            importing = new GameNotification("Beatmap Importing");

        switch (state) {
            case "importing":
                importing.message = StringTable.format(
                        R.string.message_lib_importing_several, i);
                importing.showProgressBar = true;
                importing.isProgressBarIndeterminate = false;
                importing.progressMax = i;
                beatmapsFounded = i;
                inbox.add(importing);
                break;
            case "imported":
                GameNotification imported = new GameNotification("Beatmap Imported");
                imported.message = StringTable.format(R.string.message_lib_imported, s);
                inbox.add(imported);
                importing.updateProgress(i);
                if (i == beatmapsFounded) {
                    inbox.remove(importing);
                    importing = null;
                    beatmapsFounded = 0;
                }
                break;
        }

    }

    // Notification for account log in.
    //--------------------------------------------------------------------------------------------//

    public static void accountLogIn(String state, int i) {
        if (online == null)
            online = new GameNotification("Online Account");

        if (state == null) {
            online.message = "Logging in...";
            online.showProgressBar = true;
            online.isProgressBarIndeterminate = true;
            inbox.add(online);
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
