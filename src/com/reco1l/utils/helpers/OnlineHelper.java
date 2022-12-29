package com.reco1l.utils.helpers;

import android.graphics.drawable.Drawable;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.interfaces.IReferences;

import java.io.File;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calcuator;
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 26/6/22 21:20

public class OnlineHelper {

    public static final Drawable defaultAvatar =
            Game.activity.getDrawable(R.drawable.default_avatar);

    //--------------------------------------------------------------------------------------------//

    public static Drawable getPlayerAvatar() {
        if (Game.onlineManager == null || Game.onlineManager.getAvatarURL() == null || Game.onlineManager.getAvatarURL().length() == 0)
            return defaultAvatar;

        String name = MD5Calcuator.getStringMD5(Game.onlineManager.getAvatarURL() + "_" + Game.onlineManager.getUsername());
        File file = new File(Config.getCachePath(), name);

        if(!file.exists() || file.length() < 1 & file.delete()) {
            if (OnlineFileOperator.downloadFile(Game.onlineManager.getAvatarURL(), file.getAbsolutePath())) {
                return Drawable.createFromPath(file.getPath());
            }
        }
        return defaultAvatar;
    }

    public static Drawable getAvatarFromURL(String url, String username) {
        if (url == null || url.length() == 0 || username == null)
            return defaultAvatar;

        String name = MD5Calcuator.getStringMD5(url + "_" + username);
        File file = new File(Config.getCachePath(), name);

        if (!file.exists() || file.length() < 1 & file.delete()) {
            if (OnlineFileOperator.downloadFile(url, file.getAbsolutePath())) {
                return Drawable.createFromPath(file.getPath());
            }
        }
        return defaultAvatar;
    }

    //--------------------------------------------------------------------------------------------//

    public static void clear() {
        UI.topBar.userBox.loadUserData(true);
    }

    public static void update() {
        UI.topBar.userBox.loadUserData(false);
    }

}
