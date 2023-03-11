package org.grove.result;

import com.google.gson.Gson;

public class ProfileStatistic {
    private long id;
    private String username;
    private String region;
    private boolean found;
    private long overallPlaycount;
    private boolean active;
    private boolean supporter;
    private long globalRanking;
    private long countryRanking;
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
    private long overallPerfect;
    private long overall300;
    private long overall100;
    private long overall50;
    private long overallGeki;
    private long overallKatu;
    private long overallMiss;
    private String registTime;
    private String lastLoginTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public long getOverallPlaycount() {
        return overallPlaycount;
    }

    public void setOverallPlaycount(long overallPlaycount) {
        this.overallPlaycount = overallPlaycount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSupporter() {
        return supporter;
    }

    public void setSupporter(boolean supporter) {
        this.supporter = supporter;
    }

    public long getGlobalRanking() {
        return globalRanking;
    }

    public void setGlobalRanking(long globalRanking) {
        this.globalRanking = globalRanking;
    }

    public long getCountryRanking() {
        return countryRanking;
    }

    public void setCountryRanking(long countryRanking) {
        this.countryRanking = countryRanking;
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

    public long getOverallPerfect() {
        return overallPerfect;
    }

    public void setOverallPerfect(long overallPerfect) {
        this.overallPerfect = overallPerfect;
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

    public String getRegistTime() {
        return registTime;
    }

    public void setRegistTime(String registTime) {
        this.registTime = registTime;
    }

    public String getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public static ProfileStatistic FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), ProfileStatistic.class);

//        ProfileStatistic res = new ProfileStatistic();
//        res.setId(((Double)linkedTreeMap.get("id")).longValue());
//        res.setUsername(((String)linkedTreeMap.get("username")));
//        res.setRegion(((String)linkedTreeMap.get("region")));
//        res.setFound(((Boolean)linkedTreeMap.get("found")));
//        res.setOverallPlaycount(((Double)linkedTreeMap.get("overallPlaycount")).longValue());
//        res.setActive(((Boolean)linkedTreeMap.get("active")));
//        res.setSupporter(((Boolean)linkedTreeMap.get("supporter")));
//        res.setGlobalRanking(((Double)linkedTreeMap.get("globalRanking")).longValue());
//        res.setCountryRanking(((Double)linkedTreeMap.get("countryRanking")).longValue());
//        res.setOverallScore(((Double)linkedTreeMap.get("overallScore")).longValue());
//        res.setOverallAccuracy(((Double)linkedTreeMap.get("overallAccuracy")).longValue());
//        res.setOverallCombo(((Double)linkedTreeMap.get("overallCombo")).longValue());
//        res.setOverallXss(((Double)linkedTreeMap.get("overallXss")).longValue());
//        res.setOverallSs(((Double)linkedTreeMap.get("overallSs")).longValue());
//        res.setOverallXs(((Double)linkedTreeMap.get("overallXs")).longValue());
//        res.setOverallS(((Double)linkedTreeMap.get("overallS")).longValue());
//        res.setOverallA(((Double)linkedTreeMap.get("overallA")).longValue());
//        res.setOverallB(((Double)linkedTreeMap.get("overallB")).longValue());
//        res.setOverallC(((Double)linkedTreeMap.get("overallC")).longValue());
//        res.setOverallD(((Double)linkedTreeMap.get("overallD")).longValue());
//        res.setOverallHits(((Double)linkedTreeMap.get("overallHits")).longValue());
//        res.setOverallPerfect(((Double)linkedTreeMap.get("overallPerfect")).longValue());
//        res.setOverall300(((Double)linkedTreeMap.get("overall300")).longValue());
//        res.setOverall100(((Double)linkedTreeMap.get("overall100")).longValue());
//        res.setOverall50(((Double)linkedTreeMap.get("overall50")).longValue());
//        res.setOverallGeki(((Double)linkedTreeMap.get("overallGeki")).longValue());
//        res.setOverallKatu(((Double)linkedTreeMap.get("overallKatu")).longValue());
//        res.setOverallMiss(((Double)linkedTreeMap.get("overallMiss")).longValue());
//        res.setRegistTime(((String)linkedTreeMap.get("registTime")));
//        res.setLastLoginTime(((String)linkedTreeMap.get("lastLoginTime")));
//
//        return res;
    }
}
