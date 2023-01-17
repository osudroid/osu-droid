package com.reco1l.management;

import android.database.Cursor;
import android.util.Log;

import com.reco1l.Game;
import com.reco1l.data.ScoreInfo;
import com.reco1l.utils.execution.AsyncTask;

import java.io.File;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;

// Created by Reco1l on 18/9/22 12:40

public final class Scoreboard {

    private static final ArrayList<ScoreInfo> mScores;
    private static final ArrayList<Observer<ScoreInfo>> mListeners;

    static {
        mScores = new ArrayList<>();
        mListeners = new ArrayList<>();
    }

    private static Cursor mCursor;
    private static String mErrorMessage;
    private static AsyncTask mLoadingTask;

    private static long mLastScore;

    //--------------------------------------------------------------------------------------------//

    private Scoreboard() {}

    //--------------------------------------------------------------------------------------------//

    public static void bindListener(Observer<ScoreInfo> pListener) {
        mListeners.add(pListener);
    }

    public static void unbindListener(Observer<ScoreInfo> pListener) {
        mListeners.remove(pListener);
    }

    //--------------------------------------------------------------------------------------------//

    public static String getErrorMessage() {
        return mErrorMessage;
    }

    public static ArrayList<ScoreInfo> getList() {
        return mScores;
    }

    public static ScoreInfo[] getListAsArray() {
        return mScores.toArray(new ScoreInfo[0]);
    }

    public static boolean isEmpty() {
        return mScores.isEmpty();
    }

    //--------------------------------------------------------------------------------------------//

    public synchronized static void load(TrackInfo pTrack, boolean pOnline) {
        clear();

        if (pTrack == null) {
            mErrorMessage = "No track selected!";
            notifyChange();
            return;
        }

        if (pOnline) {
            loadOnlineBoard(pTrack);
        } else {
            loadLocalBoard(pTrack);
        }
    }

    //--------------------------------------------------------------------------------------------//

    private static void notifyChange() {
        mListeners.forEach(l -> l.onScoreboardChange(mScores));
    }

    private static void clear() {
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

    private static boolean isValidCursor() {
        if (mCursor == null) {
            mErrorMessage = "Failed to get data from local database!";
            return false;
        } else if (mCursor.getCount() == 0) {
            mCursor.close();
            mCursor = null;
            return false;
        }
        return true;
    }

    private static int getColumn(String pName) throws Exception {
        int index = mCursor.getColumnIndex(pName);
        if (index == -1) {
            throw new Exception("Column " + pName + " doesn't exist!");
        }
        return index;
    }

    private static ScoreInfo parseScore(int i) {
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

    private static void loadLocalBoard(TrackInfo pTrack) {
        mLoadingTask = new AsyncTask() {
            public void run() {

                String[] columns = {"id", "playername", "score", "combo", "mark", "accuracy", "mode"};
                mCursor = Game.scoreLibrary.getMapScores(columns, pTrack.getFilename());

                if (!isValidCursor()) {
                    return;
                }
                mCursor.moveToFirst();
                mLastScore = 0;

                for (int i = mCursor.getCount() - 1; i >= 0; i--) {
                    if (mCursor.isClosed()) {
                        break;
                    }

                    ScoreInfo score = parseScore(i);
                    if (score != null) {
                        mScores.add(score);
                    }
                }
            }

            public void onComplete() {
                mCursor.close();
                mCursor = null;
                notifyChange();
            }

        };
        mLoadingTask.execute();
    }

    //-----------------------------------------Online---------------------------------------------//

    private static void loadOnlineBoard(TrackInfo pTrack) {

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
    public interface Observer<T> {
        void onScoreboardChange(ArrayList<T> pList);
    }
}
