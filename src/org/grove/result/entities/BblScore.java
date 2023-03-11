package org.grove.result.entities;

import com.google.gson.Gson;
import org.grove.prop.SimpleTokenProp;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class BblScore {
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
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

    public long getGeki() {
        return geki;
    }

    public void setGeki(long geki) {
        this.geki = geki;
    }

    public long getPerfect() {
        return perfect;
    }

    public void setPerfect(long perfect) {
        this.perfect = perfect;
    }

    public long getKatu() {
        return katu;
    }

    public void setKatu(long katu) {
        this.katu = katu;
    }

    public long getGood() {
        return good;
    }

    public void setGood(long good) {
        this.good = good;
    }

    public long getBad() {
        return bad;
    }

    public void setBad(long bad) {
        this.bad = bad;
    }

    public long getMiss() {
        return miss;
    }

    public void setMiss(long miss) {
        this.miss = miss;
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

    public static BblScore FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), BblScore.class);
    }

    private long id;
    private long uid;
    private String filename;
    private String hash;
    private String mode;
    private long score;
    private long combo;
    private String mark;
    private long geki;
    private long perfect;
    private long katu;
    private long good;
    private long bad;
    private long miss;
    private String date;
    private long accuracy;
}
