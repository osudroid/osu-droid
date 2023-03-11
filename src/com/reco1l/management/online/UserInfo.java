package com.reco1l.management.online;

import android.graphics.Bitmap;

import java.util.UUID;

public class UserInfo {

    private String
            mRegion,
            mUsername;

    private long
            mID,
            mRank,
            mScore,
            mACount,
            mSCount,
            mSSCount,
            mAccuracy,
            mPlayCount;

    private Bitmap mAvatar;

    //--------------------------------------------------------------------------------------------//

    public UserInfo() {
        super();
    }

    public UserInfo(String name) {
        mUsername = name;
    }

    //--------------------------------------------------------------------------------------------//

    public UserInfo setID(long id) {
        mID = id;
        return this;
    }

    public UserInfo setRegion(String region) {
        mRegion = region;
        return this;
    }

    public UserInfo setUsername(String username) {
        mUsername = username;
        return this;
    }

    public UserInfo setRank(long rank) {
        mRank = rank;
        return this;
    }

    public UserInfo setScore(long score) {
        mScore = score;
        return this;
    }

    public UserInfo setACount(long ACount) {
        mACount = ACount;
        return this;
    }

    public UserInfo setSCount(long SCount) {
        mSCount = SCount;
        return this;
    }

    public UserInfo setSSCount(long SSCount) {
        mSSCount = SSCount;
        return this;
    }

    public UserInfo setAccuracy(long accuracy) {
        mAccuracy = accuracy;
        return this;
    }

    public UserInfo setPlayCount(long playCount) {
        mPlayCount = playCount;
        return this;
    }

    public UserInfo setAvatar(Bitmap bitmap) {
        mAvatar = bitmap;
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    public String getUsername() {
        return mUsername;
    }

    public long getID() {
        return mID;
    }

    public long getRank() {
        return mRank;
    }

    public long getScore() {
        return mScore;
    }

    public long getACount() {
        return mACount;
    }

    public long getSCount() {
        return mSCount;
    }

    public long getSSCount() {
        return mSSCount;
    }

    public long getAccuracy() {
        return mAccuracy;
    }

    public long getPlayCount() {
        return mPlayCount;
    }

    public String getRegion() {
        return mRegion;
    }

    public Bitmap getAvatar() {
        return mAvatar;
    }

    //--------------------------------------------------------------------------------------------//

    public double getAccuracyFP() {
        if (mPlayCount <= 0) {
            return 0;
        }
        return (double) mAccuracy / mPlayCount / 1000;
    }
}
