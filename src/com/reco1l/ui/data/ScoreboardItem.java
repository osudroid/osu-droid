package com.reco1l.ui.data;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

// Created by Reco1l on 9/8/22 00:06

public class ScoreboardItem {

    public int id = -1;
    public String rank, avatar, mark, name;
    public Runnable onClick, onLongClick;

    private List<GameMod> mods;
    private String score, difference, combo, accuracy;

    public long lScore;

    //--------------------------------------------------------------------------------------------//

    public void setScore(long score) {
        this.score = NumberFormat.getNumberInstance(Locale.US).format(score);
        lScore = score;
    }

    public String getScore() {
        return score;
    }
    //--------------------------------------------------------------------------------------------//

    public void setDifference(long difference) {
        this.difference = NumberFormat.getNumberInstance(Locale.US).format(difference);
    }

    public String getDifference() {
        return difference;
    }
    //--------------------------------------------------------------------------------------------//

    public void setCombo(int combo) {
        this.combo = NumberFormat.getNumberInstance(Locale.US).format(combo);
    }

    public String getCombo() {
        return combo;
    }
    //--------------------------------------------------------------------------------------------//

    public void setAccuracy(float accuracy) {
        this.accuracy = String.format("%.2f", GameHelper.Round(accuracy * 100, 2));
    }

    public String getAccuracy() {
        return accuracy;
    }
    //--------------------------------------------------------------------------------------------//

    public void setMods(String mods) {
        this.mods = ScoringHelper.parseMods(mods);
    }

    public List<GameMod> getMods() {
        return mods;
    }

    //--------------------------------------------------------------------------------------------//
    // Workaround for DuringGameScoreBoard (old UI)

    public String get() {
        return name + "\n"
                + NumberFormat.getNumberInstance(Locale.US).format(score) + "\n"
                + NumberFormat.getNumberInstance(Locale.US).format(combo) + "x";
    }
}



