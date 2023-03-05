package com.reco1l.management.scoreboard;

import com.reco1l.utils.helpers.ScoringHelper;

import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.Locale;

import main.osu.game.mods.GameMod;

public class ScoreInfo {

    public int scoreId;
    public long playScore;

    public String userName;

    private int
            rank,
            combo;

    private long difference;
    private float accuracy;

    private String
            mark,
            avatar;

    private EnumSet<GameMod> mods;

    //--------------------------------------------------------------------------------------------//

    @Deprecated
    public String get() {
        return userName + "\n"
                + NumberFormat.getNumberInstance(Locale.US).format(playScore) + "\n"
                + NumberFormat.getNumberInstance(Locale.US).format(combo) + "x";
    }

    //--------------------------------------------------------------------------------------------//

    public ScoreInfo setId(int id) {
        this.scoreId = id;
        return this;
    }

    public ScoreInfo setRank(int rank) {
        this.rank = rank;
        return this;
    }

    public ScoreInfo setCombo(int combo) {
        this.combo = combo;
        return this;
    }

    public ScoreInfo setScore(long score) {
        this.playScore = score;
        return this;
    }

    public ScoreInfo setDifference(long difference) {
        this.difference = difference;
        return this;
    }

    public ScoreInfo setAccuracy(float accuracy) {
        this.accuracy = accuracy;
        return this;
    }

    public ScoreInfo setName(String name) {
        this.userName = name;
        return this;
    }

    public ScoreInfo setMark(String mark) {
        this.mark = mark;
        return this;
    }

    public ScoreInfo setAvatar(String avatar) {
        this.avatar = avatar;
        return this;
    }

    public ScoreInfo setMods(String mods) {
        this.mods = ScoringHelper.parseMods(mods);
        return this;
    }

    //--------------------------------------------------------------------------------------------//


    public int getId() {
        return scoreId;
    }

    public int getRank() {
        return rank;
    }

    public int getCombo() {
        return combo;
    }

    public long getScore() {
        return playScore;
    }

    public long getDifference() {
        return difference;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public String getName() {
        return userName;
    }

    public String getMark() {
        return mark;
    }

    public String getAvatar() {
        return avatar;
    }

    public EnumSet<GameMod> getMods() {
        return mods;
    }
}
