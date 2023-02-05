package com.reco1l.scenes;

// Created by Reco1l on 19/11/2022, 23:31

import android.widget.LinearLayout;

import com.reco1l.global.Game;
import com.reco1l.global.UI;
import com.reco1l.data.Notification;
import com.reco1l.view.IconButton;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.tables.DialogTable;
import com.reco1l.utils.helpers.BeatmapHelper;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Updater;
import ru.nsu.ccfit.zuev.osuplus.R;

public class MainScene extends BaseScene {

    public static final MainScene instance = new MainScene();

    public final IconButton musicButton;

    //--------------------------------------------------------------------------------------------//

    public MainScene() {
        super();

        Config.loadOnlineConfig(Game.activity);

        Game.libraryManager.loadLibraryCache(Game.activity, false);
        Game.onlineManager.Init(Game.activity);

        musicButton = new IconButton(Game.activity);
    }

    @Override
    public void onButtonContainerChange(LinearLayout layout) {
        musicButton.setIcon(R.drawable.v14_music);
        musicButton.setTouchListener(() ->
            musicButton.setSelected(UI.musicPlayer.alternate())
        );
        layout.addView(musicButton);
    }

    @Override
    protected void onFirstShow() {
        Game.onlineScoring.login();
        Updater.getInstance().checkForUpdates();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicEnd() {
        Game.musicManager.next();
    }

    @Override
    public boolean onBackPress() {
        new Dialog(DialogTable.exit()).show();
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    public void onExit() {
        Game.musicManager.stop();
        Game.resourcesManager.getSound("seeya").play();
        UI.mainMenu.onExit();
    }

    @Override
    public void onSceneChange(Scene oldScene, Scene newScene) {
        super.onSceneChange(oldScene, newScene);

        if (newScene == this) {
            UI.background.setBlur(false);
        }
    }
}
