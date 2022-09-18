package com.reco1l.utils.helpers;

import com.reco1l.interfaces.IGameMods;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

// Created by Reco1l on 8/8/22 21:35

public class ScoringHelper implements IGameMods {

    public static String getCustomMods(String data) {
        String[] mods = data.split("\\|", 2);

        if (mods.length > 1) {
            StringBuilder builder = new StringBuilder();

            for (String mod : mods[1].split("\\|")) {
                if (mod.startsWith("x") && mod.length() == 5) {
                    builder.append(mod.substring(1)).append("x,");
                }
                else if (mod.startsWith("AR")) {
                    builder.append(mod).append(",");
                }
            }
            return builder.toString();
        }
        return null;
    }

    public static List<GameMod> parseMods(String data) {
        String[] mods = data.split("\\|", 2);
        List<GameMod> list = new ArrayList<>();

        for (int i = 0; i < mods[0].length(); i++) {
            switch (mods[0].charAt(i)) {
                case 'a': list.add(AU);
                    break;
                case 'x': list.add(RX);
                    break;
                case 'p': list.add(AP);
                    break;
                case 'e': list.add(EZ);
                    break;
                case 'n': list.add(NF);
                    break;
                case 'r': list.add(HR);
                    break;
                case 'h': list.add(HD);
                    break;
                case 'i': list.add(FL);
                    break;
                case 'd': list.add(DT);
                    break;
                case 'c': list.add(NC);
                    break;
                case 't': list.add(HT);
                    break;
                case 's': list.add(PR);
                    break;
                case 'l': list.add(REZ);
                    break;
                case 'm': list.add(SC);
                    break;
                case 'u': list.add(SD);
                    break;
                case 'f': list.add(PF);
                    break;
                case 'b': list.add(SU);
                    break;
                case 'v': list.add(SV2);
                    break;
            }
        }
        return list;
    }
}
