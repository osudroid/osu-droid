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
    private static final Drawable defaultAvatar = mActivity.getDrawable(R.drawable.default_avatar);

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

        String fileName = MD5Calcuator.getStringMD5(online.getAvatarURL() + online.getUsername());
        File file = new File(Config.getCachePath(), fileName);

        if(!file.exists()) {
            OnlineFileOperator.downloadFile(online.getAvatarURL(), file.getAbsolutePath());
        }
        else if ((file.exists() && file.length() < 1) & file.delete()) {
            OnlineFileOperator.downloadFile(online.getAvatarURL(), file.getAbsolutePath());
        }

        return Drawable.createFromPath(file.getPath());
    }

    public Drawable getAvatarFromURL(String url, String username) {
        if (url == null || url.length() == 0)
            return defaultAvatar;

        String fileName = MD5Calcuator.getStringMD5(url + username);
        File file = new File(Config.getCachePath(), fileName);

        if(!file.exists())
            OnlineFileOperator.downloadFile(url, file.getAbsolutePath());

        else if ((file.exists() && file.length() < 1) & file.delete())
            OnlineFileOperator.downloadFile(url, file.getAbsolutePath());

        return Drawable.createFromPath(file.getPath());
    }

    //--------------------------------------------------------------------------------------------//

    public void clear() {
        topBar.userBox.update(true);
    }

    public void update() {
        topBar.userBox.update(false);
    }

}
