package org.grove.result;

import com.google.gson.Gson;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class MapTopPlays {
    private long playId;
    private long userId;
    private String mode;
    private long score;
    private long combo;
    private String mark;
    private String date;
    private long accuracy;
    private String username;
    private long playRank;

    public long getPlayId() {
        return playId;
    }

    public void setPlayId(long playId) {
        this.playId = playId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public long getCombo() {
        return combo;
    }

    public void setCombo(long combo) {
        this.combo = combo;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(long accuracy) {
        this.accuracy = accuracy;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getPlayRank() {
        return playRank;
    }

    public void setPlayRank(long playRank) {
        this.playRank = playRank;
    }

    public static MapTopPlays FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), MapTopPlays.class);
    }
}
