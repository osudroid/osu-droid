package ru.nsu.ccfit.zuev.osu.scoring;

import android.content.Context;
import android.util.Log;

import com.reco1l.osu.data.DatabaseManager;
import com.reco1l.osu.data.Score;
import com.rian.osu.ui.SendingPanel;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.sql.DBOpenHelper;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;

public class ScoreLibrary {


    private static final Pattern filenamePattern = Pattern.compile("[^/]*/[^/]*\\z");


    private ScoreLibrary() {
    }


    public static void load(Context context) {

        // Checking if the old database exists so we proceed with a migration.
        var oldDatabaseFile = new File(Config.getCorePath() + "databases/" + DBOpenHelper.DBNAME + ".db");

        if (!oldDatabaseFile.exists()) {
            return;
        }

        var helper = DBOpenHelper.getOrCreate(context);

        // Importing scores from the old database to the new database.
        GlobalManager.getInstance().setInfo("Importing scores from old database...");

        try (var db = helper.getWritableDatabase()) {

            String[] columns = { "id", "playername", "score", "combo", "mark", "accuracy", "mode" };

            try (var cursor = db.query(DBOpenHelper.SCORES_TABLENAME, columns, null, null, null, null, null)) {

                var pendingScores = cursor.getCount();

                while (cursor.moveToNext()) try {

                    var score = new Score(
                            cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                            cursor.getString(cursor.getColumnIndexOrThrow("filename")),
                            cursor.getString(cursor.getColumnIndexOrThrow("playername")),
                            cursor.getString(cursor.getColumnIndexOrThrow("replayfile")),
                            cursor.getString(cursor.getColumnIndexOrThrow("mode")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("score")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("combo")),
                            cursor.getString(cursor.getColumnIndexOrThrow("mark")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("h300k")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("h300")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("h100k")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("h100")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("h50")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("misses")),
                            cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy")),
                            cursor.getLong(cursor.getColumnIndexOrThrow("time")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("perfect")) == 1
                    );

                    DatabaseManager.getScoreTable().insertScore(score);

                    // Removing the score from the old database so if the process fails we can try again later.
                    db.delete(DBOpenHelper.SCORES_TABLENAME, "id = ?", new String[] { String.valueOf(score.getId()) });
                    pendingScores--;

                } catch (Exception e) {
                    Log.e("ScoreLibrary", "Failed to import score from old database.", e);
                }

                if (pendingScores <= 0) {
                    oldDatabaseFile.delete();
                }
            }

        } catch (Exception e) {
            Log.e("ScoreLibrary", "Exception caught during database migration", e);
        }
    }


    public static String getBeatmapFilename(String path) {

        var matcher = filenamePattern.matcher(path);

        if (matcher.find()) {
            return matcher.group();
        }
        return path;
    }

    public static String getBeatmapSetDirectory(String path) {

        var filename = getBeatmapFilename(path);

        if (filename.endsWith(".osu")) {
            return filename.substring(0, filename.indexOf('/'));
        } else {
            return filename.substring(filename.indexOf('/') + 1);
        }
    }


    public static void sendScoreOnline(StatisticV2 statistic, String replayPath, SendingPanel panel) {

        if (statistic.getTotalScoreWithMultiplier() <= 0) {
            return;
        }
        OnlineScoring.getInstance().sendRecord(statistic, panel, replayPath);
    }

    public static void addScore(String beatmapPath, StatisticV2 stat, String replayPath) {

        if (stat.getTotalScoreWithMultiplier() == 0 || stat.getMod().contains(GameMod.MOD_AUTO)) {
            return;
        }

        var score = Score.fromStatisticV2(stat, getBeatmapFilename(beatmapPath), replayPath);
        DatabaseManager.getScoreTable().insertScore(score);

        if (GlobalManager.getInstance().getSongMenu() != null) {
            GlobalManager.getInstance().getSongMenu().onScoreTableChange();
        }
    }

    public static List<Score> getBeatmapScores(String beatmapPath) {
        return DatabaseManager.getScoreTable().getBeatmapScores(getBeatmapFilename(beatmapPath));
    }

    @Nullable
    public static String getBestMark(String beatmapPath) {
        return DatabaseManager.getScoreTable().getBestMark(getBeatmapFilename(beatmapPath));
    }

    public static StatisticV2 getScore(int id) {

        var score = DatabaseManager.getScoreTable().getScore(id);
        var statistic = new StatisticV2();

        if (score == null) {
            return statistic;
        }

        statistic.setPlayerName(score.getPlayerName());
        statistic.setReplayName(score.getReplayPath());
        statistic.setModFromString(score.getMods());
        statistic.setForcedScore(score.getScore());
        statistic.maxCombo = score.getMaxCombo();
        statistic.setMark(score.getMark());
        statistic.hit300k = score.getHit300k();
        statistic.hit300 = score.getHit300();
        statistic.hit100k = score.getHit100k();
        statistic.hit100 = score.getHit100();
        statistic.hit50 = score.getHit50();
        statistic.misses = score.getMisses();
        statistic.accuracy = score.getAccuracy();
        statistic.time = score.getTime();
        statistic.setPerfect(score.isPerfect());

        return statistic;
    }

    public static boolean deleteScore(int id) {

        if (DatabaseManager.getScoreTable().deleteScore(id) > 0) {

            if (GlobalManager.getInstance().getSongMenu() != null) {
                GlobalManager.getInstance().getSongMenu().onScoreTableChange();
            }
            return true;
        }
        return false;
    }
}
