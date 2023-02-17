package com.reco1l.global;

// Created by Reco1l on 26/9/22 22:21

import com.reco1l.scenes.BaseScene;
import com.reco1l.scenes.ListingScene;
import com.reco1l.scenes.LoaderScene;
import com.reco1l.scenes.MainScene;
import com.reco1l.scenes.PlayerScene;
import com.reco1l.scenes.SelectorScene;
import com.reco1l.scenes.SummaryScene;
import com.reco1l.utils.Logging;

import org.anddev.andengine.entity.scene.Scene;

public final class Scenes {

    static {
        Logging.loadOf(Scenes.class);
    }

    //--------------------------------------------------------------------------------------------//

    public static final MainScene main = MainScene.instance;
    public static final PlayerScene player = PlayerScene.instance;
    public static final LoaderScene loader = LoaderScene.instance;
    public static final SummaryScene summary = SummaryScene.instance;
    public static final ListingScene listing = ListingScene.instance;
    public static final SelectorScene selector = SelectorScene.instance;

    //--------------------------------------------------------------------------------------------//

    public static void initialize() {}

    //--------------------------------------------------------------------------------------------//

    public static BaseScene[] all() {
        return new BaseScene[] {
                main,
                player,
                loader,
                summary,
                listing,
                selector
        };
    }
}
