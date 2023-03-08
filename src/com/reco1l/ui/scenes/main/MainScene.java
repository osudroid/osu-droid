package com.reco1l.ui.scenes.main;

// Created by Reco1l on 19/11/2022, 23:31

import android.widget.LinearLayout;

import com.reco1l.Game;
import com.reco1l.ui.UI;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.view.IconButtonView;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.data.DialogTable;

import main.osu.Config;
import com.rimu.R;

public class MainScene extends BaseScene {

    public static final MainScene instance = new MainScene();

    public final IconButtonView musicButton;

    //--------------------------------------------------------------------------------------------//

    public MainScene() {
        super();

        Config.loadOnlineConfig(Game.activity);

        Game.libraryManager.loadLibraryCache(Game.activity, false);
        Game.onlineManager.Init(Game.activity);

        musicButton = new IconButtonView(Game.activity);
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
}
