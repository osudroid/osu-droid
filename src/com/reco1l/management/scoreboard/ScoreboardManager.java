package com.reco1l.management.scoreboard;

import android.database.Cursor;
import android.util.Log;

import com.reco1l.Game;
import com.reco1l.framework.execution.AsyncTask;

import java.io.File;
import java.util.ArrayList;

import main.osu.TrackInfo;
import main.osu.helper.FileUtils;
import main.osu.online.OnlineManager.OnlineManagerException;

// Created by Reco1l on 18/9/22 12:40

public final class ScoreboardManager {

    public static final ScoreboardManager instance = new ScoreboardManager();

    private final ArrayList<ScoreInfo> mScores;
    private final ArrayList<Observer> mObservers;

    private Cursor mCursor;
    private String mErrorMessage;
    private AsyncTask mLoadingTask;

    private long mLastScore;
    private boolean mIsOnlineBoard;

    //--------------------------------------------------------------------------------------------//

    public ScoreboardManager() {
        mScores = new ArrayList<>();
        mObservers = new ArrayList<>();
    }

    //--------------------------------------------------------------------------------------------//

    public void bindBoardObserver(Observer observer) {
        mObservers.add(observer);
    }

    public void unbindBoardObserver(Observer pListener) {
        mObservers.remove(pListener);
    }

    //--------------------------------------------------------------------------------------------//

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public ArrayList<ScoreInfo> getList() {
        return mScores;
    }

    public ScoreInfo[] getListAsArray() {
        return mScores.toArray(new ScoreInfo[0]);
    }

    //--------------------------------------------------------------------------------------------//

    public boolean isEmpty() {
        return mScores.isEmpty();
    }

    public boolean isOnlineBoard() {
        return mIsOnlineBoard;
    }

    //--------------------------------------------------------------------------------------------//

    public synchronized void setOnline(boolean bool) {
        mIsOnlineBoard = bool;
        load(Game.musicManager.getTrack());
    }

    public synchronized void load(TrackInfo pTrack) {
        clear();

        if (pTrack == null) {
            mErrorMessage = "No track selected!";
            notifyChange();
            return;
        }

        if (mIsOnlineBoard) {
            loadOnlineBoard(pTrack);
        } else {
            loadLocalBoard(pTrack);
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void notifyChange() {
        mObservers.forEach(l -> l.onBoardChange(mScores));
    }

    private void clear() {
        mErrorMessage = "No scores found!";

        if (mLoadingTask != null && !mLoadingTask.isShutdown()) {
            mLoadingTask.cancel(true);
        }
        mLoadingTask = null;

        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = null;
        mScores.clear();
    }

    //----------------------------------------Offline---------------------------------------------//

    private boolean isValidCursor() {
        if (mCursor == null) {
            mErrorMessage = "Failed to get data from local database!";
            return false;
        }
        return true;
    }

    private int getColumn(String pName) throws Exception {
        int index = mCursor.getColumnIndex(pName);
        if (index == -1) {
            throw new Exception("Column " + pName + " doesn't exist!");
        }
        return index;
    }

    private ScoreInfo parseScore(int i) {
        mCursor.moveToPosition(i);

        try {
            long score = mCursor.getLong(getColumn("score"));
            long difference = score - mLastScore;
            mLastScore = score;

            return new ScoreInfo()
                    .setRank(i + 1)
                    .setDifference(difference)
                    .setId(mCursor.getInt(getColumn("id")))
                    .setCombo(mCursor.getInt(getColumn("combo")))
                    .setScore(mCursor.getLong(getColumn("score")))
                    .setMods(mCursor.getString(getColumn("mode")))
                    .setMark(mCursor.getString(getColumn("mark")))
                    .setName(mCursor.getString(getColumn("playername")))
                    .setAccuracy(mCursor.getFloat(getColumn("accuracy")));

        } catch (Exception e) {
            Log.e("Scoreboard", "Failed to load score at position " + i + "\n" + e.getMessage());
            return null;
        }
    }

    private void loadLocalBoard(TrackInfo pTrack) {
        mLoadingTask = new AsyncTask() {
            public void run() {

                String[] columns = {"id", "playername", "score", "combo", "mark", "accuracy", "mode"};
                mCursor = Game.scoreLibrary.getMapScores(columns, pTrack.getFilename());

                if (!isValidCursor()) {
                    return;
                }
                mCursor.moveToFirst();
                mLastScore = 0;
                int count = mCursor.getCount();

                for (int i = count - 1; i >= 0; i--) {
                    if (mCursor == null || mCursor.isClosed()) {
                        break;
                    }

                    ScoreInfo score = parseScore(i);
                    if (score != null) {
                        mScores.add(score);
                    }
                }
            }

            public void onComplete() {
                if (mCursor != null) {
                    mCursor.close();
                    mCursor = null;
                }
                notifyChange();
            }

        };
        mLoadingTask.execute();
    }

    //-----------------------------------------Online---------------------------------------------//

    private void loadOnlineBoard(TrackInfo pTrack) {

        if (!Game.onlineManager.isStayOnline()) {
            mErrorMessage = "Cannot connect to server when offline mode is enabled!";
            notifyChange();
            return;
        }

        mLoadingTask = new AsyncTask() {

            public void run() {
                ArrayList<String> scores;

                try {
                    File file = new File(pTrack.getFilename());
                    scores = Game.onlineManager.getTop(file, FileUtils.getMD5Checksum(file));
                } catch (OnlineManagerException exception) {
                    mErrorMessage = "Error loading scores!" + "\n(" + exception.getMessage() + ")";
                    notifyChange();
                    return;
                }

                if (scores.size() == 0) {
                    notifyChange();
                    return;
                }
                mLastScore = 0;

                for (int i = scores.size() - 1; i >= 0; i--) {
                    ScoreInfo item = new ScoreInfo();

                    String[] data = scores.get(i).split("\\s+");
                    if (data.length < 8) {
                        Log.e("Scoreboard", "Failed to load score at position " + i);
                        continue;
                    }

                    long score = Long.parseLong(data[2]);
                    long difference = score - mLastScore;
                    mLastScore = score;

                    ScoreInfo info = new ScoreInfo()
                            .setId(Integer.parseInt(data[0]))
                            .setMark(data[4])
                            .setDifference(difference)
                            .setAccuracy(Float.parseFloat(data[6]))
                            .setCombo(Integer.parseInt(data[3]))
                            .setScore(Long.parseLong(data[2]))
                            .setMods(data[5]);

                    if (data.length == 10) {
                        info.setName(data[8]);
                        info.setAvatar(data[9]);
                        info.setRank(Integer.parseInt(data[7]));
                    } else {
                        info.setRank(i + 1);
                        info.setName(data[1]);
                        info.setAvatar(data[7]);
                    }

                    mScores.add(item);
                }
            }

            @Override
            public void onComplete() {
                notifyChange();
            }
        };
        mLoadingTask.execute();
    }
    //----------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface Observer {
        void onBoardChange(ArrayList<ScoreInfo> pList);
    }
}
