package com.reco1l.ui.data;

import android.graphics.drawable.Drawable;

import com.reco1l.utils.interfaces.UI;
import com.reco1l.utils.interfaces.IMainClasses;

import java.io.File;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calcuator;
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 26/6/22 21:20

public class OnlineHelper implements IMainClasses, UI {
    // This class translates contains tools to translate online data for the new UI.

    private static OnlineHelper instance;
    public static final Drawable defaultAvatar = mActivity.getDrawable(R.drawable.default_avatar);

    //--------------------------------------------------------------------------------------------//

    public static OnlineHelper getInstance() {
        if (instance == null)
            instance = new OnlineHelper();
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    public Drawable getPlayerAvatar() {
        if (online == null || online.getAvatarURL() == null || online.getAvatarURL().length() == 0)
            return defaultAvatar;

        String name = MD5Calcuator.getStringMD5(online.getAvatarURL() + "_" + online.getUsername());
        File file = new File(Config.getCachePath(), name);

        if(!file.exists() || file.length() < 1 & file.delete()) {
            if (OnlineFileOperator.downloadFile(online.getAvatarURL(), file.getAbsolutePath())) {
                return Drawable.createFromPath(file.getPath());
            }
        }
        return defaultAvatar;
    }

    public Drawable getAvatarFromURL(String url, String username) {
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

    public void clear() {
        topBar.userBox.update(true);
    }

    public void update() {
        topBar.userBox.update(false);
    }

}
