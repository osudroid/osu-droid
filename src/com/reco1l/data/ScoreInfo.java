package com.reco1l.data;

import com.reco1l.utils.helpers.ScoringHelper;

import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public class ScoreInfo {

    private int
            id,
            rank,
            combo;

    private long
            score,
            difference;

    private float accuracy;

    private String
            name,
            mark,
            avatar;

    private EnumSet<GameMod> mods;

    //--------------------------------------------------------------------------------------------//

    @Deprecated
    public String get() {
        return name + "\n"
                + NumberFormat.getNumberInstance(Locale.US).format(score) + "\n"
                + NumberFormat.getNumberInstance(Locale.US).format(combo) + "x";
    }

    //--------------------------------------------------------------------------------------------//

    public ScoreInfo setId(int id) {
        this.id = id;
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
        this.score = score;
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
        this.name = name;
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
        return id;
    }

    public int getRank() {
        return rank;
    }

    public int getCombo() {
        return combo;
    }

    public long getScore() {
        return score;
    }

    public long getDifference() {
        return difference;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public String getName() {
        return name;
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
