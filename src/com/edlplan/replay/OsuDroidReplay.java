package com.edlplan.replay;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class OsuDroidReplay {

    private String fileName;

    private String playerName;

    private String replayFile;

    private String mode;

    private int score;

    private int combo;

    private String mark;

    private int h300k, h300, h100k, h100, h50, misses;

    private float accuracy;

    private long time;

    private int perfect;

    private static String getString(JSONObject cursor, String c) throws JSONException {
        return cursor.getString(c);
    }

    private static int getInt(JSONObject cursor, String c) throws JSONException {
        return cursor.getInt(c);
    }

    private static long getLong(JSONObject cursor, String c) throws JSONException {
        return cursor.getLong(c);
    }

    private static float getFloat(JSONObject cursor, String c) throws JSONException {
        return (float) cursor.getDouble(c);
    }

    public static OsuDroidReplay parseJSON(JSONObject cursor) throws JSONException {
        OsuDroidReplay replay = new OsuDroidReplay();
        replay.setFileName(getString(cursor, "filename"));
        replay.setPlayerName(getString(cursor, "playername"));
        replay.setReplayFile(getString(cursor, "replayfile"));
        replay.setMode(getString(cursor, "mod"));
        replay.setScore(getInt(cursor, "score"));
        replay.setCombo(getInt(cursor, "combo"));
        replay.setMark(getString(cursor, "mark"));
        replay.setH300k(getInt(cursor, "h300k"));
        replay.setH300(getInt(cursor, "h300"));
        replay.setH100k(getInt(cursor, "h100k"));
        replay.setH100(getInt(cursor, "h100"));
        replay.setH50(getInt(cursor, "h50"));
        replay.setMisses(getInt(cursor, "misses"));
        replay.setAccuracy(getFloat(cursor, "accuracy"));
        replay.setTime(getLong(cursor, "time"));
        replay.setPerfect(getInt(cursor, "perfect"));
        return replay;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getReplayFile() {
        return replayFile;
    }

    public void setReplayFile(String replayFile) {
        this.replayFile = replayFile;
    }

    public boolean isAbsoluteReplay() {
        return getReplayFile().contains("/");
    }

    public String getReplayFileName() {
        return isAbsoluteReplay() ? new File(replayFile).getName() : replayFile;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getCombo() {
        return combo;
    }

    public void setCombo(int combo) {
        this.combo = combo;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public int getH300k() {
        return h300k;
    }

    public void setH300k(int h300k) {
        this.h300k = h300k;
    }

    public int getH300() {
        return h300;
    }

    public void setH300(int h300) {
        this.h300 = h300;
    }

    public int getH100k() {
        return h100k;
    }

    public void setH100k(int h100k) {
        this.h100k = h100k;
    }

    public int getH100() {
        return h100;
    }

    public void setH100(int h100) {
        this.h100 = h100;
    }

    public int getH50() {
        return h50;
    }

    public void setH50(int h50) {
        this.h50 = h50;
    }

    public int getMisses() {
        return misses;
    }

    public void setMisses(int misses) {
        this.misses = misses;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getPerfect() {
        return perfect;
    }

    public void setPerfect(int perfect) {
        this.perfect = perfect;
    }

    public JSONObject toJSON() {
        OsuDroidReplay replay = this;
        JSONObject replayData = new JSONObject();
        try {
            replayData.put("filename", replay.getFileName());
            replayData.put("playername", replay.getPlayerName());
            replayData.put("replayfile", replay.getReplayFileName());
            replayData.put("mod", replay.getMode());
            replayData.put("score", replay.getScore());
            replayData.put("combo", replay.getCombo());
            replayData.put("mark", replay.getMark());
            replayData.put("h300k", replay.getH300k());
            replayData.put("h300", replay.getH300());
            replayData.put("h100k", replay.getH100k());
            replayData.put("h100", replay.getH100());
            replayData.put("h50", replay.getH50());
            replayData.put("misses", replay.getMisses());
            replayData.put("accuracy", replay.getAccuracy());
            replayData.put("time", replay.getTime());
            replayData.put("perfect", replay.getPerfect());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return replayData;
    }

}
