package ru.nsu.ccfit.zuev.osu.scoring;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.helper.sql.DBOpenHelper;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.online.SendingPanel;
import ru.nsu.ccfit.zuev.osuplus.R;

public class ScoreLibrary {
    private static final Pattern newPathPattern = Pattern.compile("[^/]*/[^/]*\\z");
    private static ScoreLibrary lib = new ScoreLibrary();
    private SQLiteDatabase db = null;

    private ScoreLibrary() {
    }

    public static ScoreLibrary getInstance() {
        return lib;
    }

    public static String getTrackPath(final String track) {
        final Matcher newPathMather = newPathPattern.matcher(track);
        if (newPathMather.find()) {
            return newPathMather.group();
        }
        return track;
    }

    public static String getTrackDir(final String track) {
        String s = getTrackPath(track);
        if (s.endsWith(".osu")) {
            return s.substring(0, s.indexOf('/'));
        } else {
            return s.substring(s.indexOf('/') + 1, s.length());
        }
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    public void load(Context context) {
        DBOpenHelper helper = DBOpenHelper.getOrCreate(context);
        try {
            db = helper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            ToastLogger.showText(
                    StringTable.get(R.string.require_storage_permission),
                    true);
            throw new RuntimeException(e);
        }
        loadOld(context);
    }

    @SuppressWarnings("unchecked")
    private void loadOld(Context context) {
        final File folder = new File(Config.getCorePath() + "/Scores");
        if (folder.exists() == false) {
            return;
        }
        final File f = new File(folder, "scoreboard");
        if (f.exists() == false) {
            return;
        }
        Debug.i("Loading old scores...");
        try {
            final ObjectInputStream in = new ObjectInputStream(
                    new FileInputStream(f));

            Object obj = in.readObject();
            String versionStr = "";
            if (obj instanceof String) {
                versionStr = (String) obj;
                if (versionStr.equals("scores1") == false
                        && versionStr.equals("scores2") == false) {
                    in.close();
                    return;
                }
            } else {
                in.close();
                return;
            }
            obj = in.readObject();
            Map<String, ArrayList<StatisticV2>> scores = null;
            if (obj instanceof Map<?, ?>) {
                if (versionStr.equals("scores1")) {
                    final Map<String, ArrayList<Statistic>> oldStat = (Map<String, ArrayList<Statistic>>) obj;
                    scores = new HashMap<String, ArrayList<StatisticV2>>();
                    for (final String str : oldStat.keySet()) {
                        final ArrayList<StatisticV2> newStat = new ArrayList<StatisticV2>();
                        for (final Statistic s : oldStat.get(str)) {
                            newStat.add(new StatisticV2(s));
                        }
                        final Matcher newPathMather = newPathPattern
                                .matcher(str);
                        if (newPathMather.find()) {
                            scores.put(newPathMather.group(), newStat);
                        } else {
                            scores.put(str, newStat);
                        }
                    }
                } else if (versionStr.equals("scores2")) {
                    scores = (Map<String, ArrayList<StatisticV2>>) obj;
                }
            }

            if (scores != null) {
                for (String track : scores.keySet()) {
                    for (StatisticV2 stat : scores.get(track)) {
                        addScore(track, stat, null);
                    }
                }
            }

            in.close();
        } catch (final Exception e) {
            Debug.e("ScoreLibrary.loadOld: " + e.getMessage());
            return;
        }
        f.delete();
    }

    public void save() {

    }

    public void sendScoreOnline(final StatisticV2 stat, final String replay,
                                final SendingPanel panel) {
        Debug.i("Preparing for online!");
        if (stat.getModifiedTotalScore() <= 0) return;
        OnlineScoring.getInstance().sendRecord(stat, panel, replay);
    }

    public void addScore(final String trackPath, final StatisticV2 stat, final String replay) {
        if (stat.getModifiedTotalScore() == 0) {
            return;
        }
        final String track = getTrackPath(trackPath);

        if (db == null) return;
        ContentValues values = new ContentValues();
        values.put("filename", track);
        values.put("playername", stat.getPlayerName());
        values.put("replayfile", replay);
        values.put("mode", stat.getModString());
        values.put("score", stat.getModifiedTotalScore());
        values.put("combo", stat.getMaxCombo());
        values.put("mark", stat.getMark());
        values.put("h300k", stat.getHit300k());
        values.put("h300", stat.getHit300());
        values.put("h100k", stat.getHit100k());
        values.put("h100", stat.getHit100());
        values.put("h50", stat.getHit50());
        values.put("misses", stat.getMisses());
        values.put("accuracy", stat.getAccuracy());
        values.put("time", stat.getTime());
        values.put("perfect", stat.isPerfect() ? 1 : 0);

        long result = db.insert(DBOpenHelper.SCORES_TABLENAME, null, values);
        Debug.i("Inserting data, result = " + result);

//		String[] columns = {"id", "filename", "score", "replayfile"};
//		Cursor response =
//			db.query(DBOpenHelper.SCORES_TABLENAME, columns, "filename = \"" + track + "\"" ,
//												null, null, null, "score ASC");
        //if scores > 5, we need to remove some
//		if (response.getCount() > 5)
//		{
//			response.moveToFirst();
//			int rowsToDelete = response.getCount() - 5;
//			String filter = "";
//			for (int i = 0; i < rowsToDelete; i++)
//			{
//				filter += response.getString(0);
//				if (i < rowsToDelete - 1)
//					filter += ",";
//
//				//removing score replay
//				try{
//					new File(response.getString(3)).delete();
//				} catch (Exception e){}
//
//				response.moveToNext();
//
//			}
//			//removing all unnecessary scores
//			filter = "id IN (" + filter + ")";
//			result = db.delete(DBOpenHelper.SCORES_TABLENAME, filter, null);
//		}

//		response.close();
    }

    public Cursor getMapScores(String[] columns, String filename) {
        final String track = getTrackPath(filename);
        if (db == null) return null;
        return db.query(DBOpenHelper.SCORES_TABLENAME, columns, "filename = ?",
                new String[]{track}, null, null, "score DESC");
    }

    public String getBestMark(final String trackPath) {
        final String track = getTrackPath(trackPath);
        String[] columns = {"mark", "filename", "id", "score"};
        Cursor response =
                db.query(DBOpenHelper.SCORES_TABLENAME, columns, "filename = ?",
                        new String[]{track}, null, null, "score DESC");
        if (response.getCount() == 0) {
            response.close();
            return null;
        }
        response.moveToFirst();

        String mark = response.getString(0);
//		do {
//			final String s = response.getString(0);
//			if (s.equals("XH")) {
//				mark = s;
//			} else if (s.equals("X") && mark.equals("XH") == false) {
//				mark = s;
//			} else if (s.equals("SH") && mark.equals("XH") == false
//					&& mark.equals("X") == false) {
//				mark = s;
//			} else if (s.equals("S") && mark.equals("XH") == false
//					&& mark.equals("X") == false && mark.equals("SH") == false) {
//				mark = s;
//			} else if (s.equals("A") && mark.equals("XH") == false
//					&& mark.equals("X") == false && mark.equals("SH") == false
//					&& mark.equals("S") == false) {
//				mark = s;
//			} else if (s.equals("B") && mark.equals("XH") == false
//					&& mark.equals("X") == false && mark.equals("SH") == false
//					&& mark.equals("S") == false && mark.equals("A") == false) {
//				mark = s;
//			} else if (s.equals("C") && mark.equals("XH") == false
//					&& mark.equals("X") == false && mark.equals("SH") == false
//					&& mark.equals("S") == false && mark.equals("A") == false
//					&& mark.equals("B") == false) {
//				mark = s;
//			}
//		} while (response.moveToNext());

        response.close();

        return mark;
    }

    public StatisticV2 getScore(int id) {
        Cursor c = db.query(DBOpenHelper.SCORES_TABLENAME, null, "id = " + id,
                null, null, null, null);
        StatisticV2 stat = new StatisticV2();
        if (c.getCount() == 0) {
            c.close();
            return stat;
        }
        c.moveToFirst();

        stat.setPlayerName(c.getString(c.getColumnIndexOrThrow("playername")));
        stat.setReplayName(c.getString(c.getColumnIndexOrThrow("replayfile")));
        stat.setModFromString(c.getString(c.getColumnIndexOrThrow("mode")));
        stat.setForcedScore(c.getInt(c.getColumnIndexOrThrow("score")));
        stat.maxCombo = c.getInt(c.getColumnIndexOrThrow("combo"));
        stat.setMark(c.getString(c.getColumnIndexOrThrow("mark")));
        stat.hit300k = c.getInt(c.getColumnIndexOrThrow("h300k"));
        stat.hit300 = c.getInt(c.getColumnIndexOrThrow("h300"));
        stat.hit100k = c.getInt(c.getColumnIndexOrThrow("h100k"));
        stat.hit100 = c.getInt(c.getColumnIndexOrThrow("h100"));
        stat.hit50 = c.getInt(c.getColumnIndexOrThrow("h50"));
        stat.misses = c.getInt(c.getColumnIndexOrThrow("misses"));
        stat.accuracy = c.getFloat(c.getColumnIndexOrThrow("accuracy"));
        stat.time = c.getLong(c.getColumnIndexOrThrow("time"));
        stat.setPerfect(c.getInt(c.getColumnIndexOrThrow("perfect")) != 0);

        c.close();

        return stat;
    }

    public boolean deleteScore(int id) {
        return db.delete(DBOpenHelper.SCORES_TABLENAME, "id = " + id, null) != 0;
    }
}
