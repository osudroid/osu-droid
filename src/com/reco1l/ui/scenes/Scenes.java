package com.reco1l.ui.scenes;

// Created by Reco1l on 26/9/22 22:21

import com.reco1l.ui.scenes.listing.ListingScene;
import com.reco1l.ui.scenes.main.MainScene;
import com.reco1l.ui.scenes.player.PlayerScene;
import com.reco1l.ui.scenes.selector.SelectorScene;
import com.reco1l.ui.scenes.summary.SummaryScene;
import com.reco1l.framework.Logging;

public final class Scenes {

    static {
        Logging.loadOf(Scenes.class);
    }

    //--------------------------------------------------------------------------------------------//

    public static final MainScene main = MainScene.instance;
    public static final PlayerScene player = PlayerScene.instance;
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
                summary,
                listing,
                selector
        };
    }
}
