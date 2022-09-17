package com.reco1l.interfaces;

// Created by Reco1l on 14/9/22 23:19

public interface ITextures {

    // Place used textures here to avoid loading unnecessary textures.
    // TODO: [ResourceManager] Use this list to load textures instead of loading every file in the
    //  skin folder, of course before that you must have to add all used file names to this list.

    String[] fileNames = {
            // Ranking marks
            /*"ranking-A",
            "ranking-B",
            "ranking-C",
            "ranking-D",
            "ranking-S",
            "ranking-SH",
            "ranking-X",
            "ranking-XH",*/

            // Small ranking marks
            "ranking-A-small",
            "ranking-B-small",
            "ranking-C-small",
            "ranking-D-small",
            "ranking-S-small",
            "ranking-SH-small",
            "ranking-X-small",
            "ranking-XH-small",

            // Selection mods
            "selection-mod-autoplay",
            "selection-mod-doubletime",
            "selection-mod-easy",
            "selection-mod-flashlight",
            "selection-mod-halftime",
            "selection-mod-hardrock",
            "selection-mod-hidden",
            "selection-mod-nightcore",
            "selection-mod-nofail",
            "selection-mod-perfect",
            "selection-mod-precise",
            "selection-mod-reallyeasy",
            "selection-mod-relax",
            "selection-mod-relax2",
            "selection-mod-scorev2",
            "selection-mod-smallcircle",
            "selection-mod-speedup",
            "selection-mod-suddendeath"
    };

}
