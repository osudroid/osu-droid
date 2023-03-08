package org.grove.result;

import com.google.gson.Gson;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class LeaderBoardUser {
    private long rank;
    private long id;
    private String username;
    private String region;
    private long overallScore;
    private long overallPlaycount;
    private long overallSs;
    private long overallS;
    private long overallA;
    private long overallAccuracy;

    public long getRank() {
        return rank;
    }

    public void setRank(long rank) {
        this.rank = rank;
    }

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

    public long getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(long overallScore) {
        this.overallScore = overallScore;
    }

    public long getOverallPlaycount() {
        return overallPlaycount;
    }

    public void setOverallPlaycount(long overallPlaycount) {
        this.overallPlaycount = overallPlaycount;
    }

    public long getOverallSs() {
        return overallSs;
    }

    public void setOverallSs(long overallSs) {
        this.overallSs = overallSs;
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

    public long getOverallAccuracy() {
        return overallAccuracy;
    }

    public void setOverallAccuracy(long overallAccuracy) {
        this.overallAccuracy = overallAccuracy;
    }

    public static LeaderBoardUser FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), LeaderBoardUser.class);
    }
}
