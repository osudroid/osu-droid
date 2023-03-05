package com.reco1l.utils.helpers;


import com.reco1l.management.modding.ModAcronyms;

import java.util.EnumSet;

import main.osu.game.mods.GameMod;

// Created by Reco1l on 8/8/22 21:35

public class ScoringHelper implements ModAcronyms {

    public static String getCustomMods(String data) {
        String[] mods = data.split("\\|", 2);

        if (mods.length > 1) {
            StringBuilder builder = new StringBuilder();

            for (String mod : mods[1].split("\\|")) {
                if (mod.startsWith("x") && mod.length() == 5) {
                    builder.append(mod.substring(1)).append("x,");
                } else if (mod.startsWith("AR")) {
                    builder.append(mod).append(",");
                }
            }
            return builder.toString();
        }
        return null;
    }

    public static EnumSet<GameMod> parseMods(String pData) {
        String[] mods = pData.split("\\|", 2);
        EnumSet<GameMod> list = EnumSet.noneOf(GameMod.class);

        for (int i = 0; i < mods[0].length(); i++) {
            switch (mods[0].charAt(i)) {
                case 'a':
                    list.add(AU);
                    break;
                case 'x':
                    list.add(RX);
                    break;
                case 'p':
                    list.add(AP);
                    break;
                case 'e':
                    list.add(EZ);
                    break;
                case 'n':
                    list.add(NF);
                    break;
                case 'r':
                    list.add(HR);
                    break;
                case 'h':
                    list.add(HD);
                    break;
                case 'i':
                    list.add(FL);
                    break;
                case 'd':
                    list.add(DT);
                    break;
                case 'c':
                    list.add(NC);
                    break;
                case 't':
                    list.add(HT);
                    break;
                case 's':
                    list.add(PR);
                    break;
                case 'l':
                    list.add(REZ);
                    break;
                case 'm':
                    list.add(SC);
                    break;
                case 'u':
                    list.add(SD);
                    break;
                case 'f':
                    list.add(PF);
                    break;
                case 'v':
                    list.add(SV2);
                    break;
            }
        }
        return list;
    }

    public static String handleLegacyTextures(String texture) {
        if (texture.endsWith("0g") || texture.endsWith("0k")) {
            return texture.substring(0, texture.length() - 1);
        }
        return texture;
    }

    public static String parseAcronym(GameMod entry) {
        String s = "-";

        switch (entry) {
            case MOD_EASY:
                s = "ez";
                break;
            case MOD_NOFAIL:
                s = "nf";
                break;
            case MOD_AUTO:
                s = "au";
                break;
            case MOD_HARDROCK:
                s = "hr";
                break;
            case MOD_HIDDEN:
                s = "hd";
                break;
            case MOD_RELAX:
                s = "rx";
                break;
            case MOD_AUTOPILOT:
                s = "ap";
                break;
            case MOD_DOUBLETIME:
                s = "dt";
                break;
            case MOD_NIGHTCORE:
                s = "nc";
                break;
            case MOD_HALFTIME:
                s = "ht";
                break;
            case MOD_SUDDENDEATH:
                s = "sd";
                break;
            case MOD_PERFECT:
                s = "pf";
                break;
            case MOD_FLASHLIGHT:
                s = "fl";
                break;
            case MOD_PRECISE:
                s = "pr";
                break;
            case MOD_SMALLCIRCLE:
                s = "sc";
                break;
            case MOD_REALLYEASY:
                s = "rez";
                break;
            case MOD_SCOREV2:
                s = "sv2";
                break;
        }
        return s;
    }
}
