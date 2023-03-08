package org.grove.result.entities;

import com.google.gson.Gson;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class BblUserStats {
    private long uid;
    private long overallPlaycount;
    private long overallScore;
    private long overallAccuracy;
    private long overallCombo;
    private long overallXss;
    private long overallSs;
    private long overallXs;
    private long overallS;
    private long overallA;
    private long overallB;
    private long overallC;
    private long overallD;
    private long overallHits;
    private long overall300;
    private long overall100;
    private long overall50;
    private long overallGeki;
    private long overallKatu;
    private long overallMiss;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long getOverallPlaycount() {
        return overallPlaycount;
    }

    public void setOverallPlaycount(long overallPlaycount) {
        this.overallPlaycount = overallPlaycount;
    }

    public long getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(long overallScore) {
        this.overallScore = overallScore;
    }

    public long getOverallAccuracy() {
        return overallAccuracy;
    }

    public void setOverallAccuracy(long overallAccuracy) {
        this.overallAccuracy = overallAccuracy;
    }

    public long getOverallCombo() {
        return overallCombo;
    }

    public void setOverallCombo(long overallCombo) {
        this.overallCombo = overallCombo;
    }

    public long getOverallXss() {
        return overallXss;
    }

    public void setOverallXss(long overallXss) {
        this.overallXss = overallXss;
    }

    public long getOverallSs() {
        return overallSs;
    }

    public void setOverallSs(long overallSs) {
        this.overallSs = overallSs;
    }

    public long getOverallXs() {
        return overallXs;
    }

    public void setOverallXs(long overallXs) {
        this.overallXs = overallXs;
    }

    public long getOverallS() {
        return overallS;
    }

    public void setOverallS(long overallS) {
        this.overallS = overallS;
    }

    public long getOverallA() {
        return overallA;
    }

    public void setOverallA(long overallA) {
        this.overallA = overallA;
    }

    public long getOverallB() {
        return overallB;
    }

    public void setOverallB(long overallB) {
        this.overallB = overallB;
    }

    public long getOverallC() {
        return overallC;
    }

    public void setOverallC(long overallC) {
        this.overallC = overallC;
    }

    public long getOverallD() {
        return overallD;
    }

    public void setOverallD(long overallD) {
        this.overallD = overallD;
    }

    public long getOverallHits() {
        return overallHits;
    }

    public void setOverallHits(long overallHits) {
        this.overallHits = overallHits;
    }

    public long getOverall300() {
        return overall300;
    }

    public void setOverall300(long overall300) {
        this.overall300 = overall300;
    }

    public long getOverall100() {
        return overall100;
    }

    public void setOverall100(long overall100) {
        this.overall100 = overall100;
    }

    public long getOverall50() {
        return overall50;
    }

    public void setOverall50(long overall50) {
        this.overall50 = overall50;
    }

    public long getOverallGeki() {
        return overallGeki;
    }

    public void setOverallGeki(long overallGeki) {
        this.overallGeki = overallGeki;
    }

    public long getOverallKatu() {
        return overallKatu;
    }

    public void setOverallKatu(long overallKatu) {
        this.overallKatu = overallKatu;
    }

    public long getOverallMiss() {
        return overallMiss;
    }

    public void setOverallMiss(long overallMiss) {
        this.overallMiss = overallMiss;
    }

    public static BblUserStats FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), BblUserStats.class);
    }
}
