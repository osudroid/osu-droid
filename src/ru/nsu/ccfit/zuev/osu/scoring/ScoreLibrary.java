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
        try (var db = helper.getWritableDatabase()) {

            String[] columns = { "id", "playername", "score", "combo", "mark", "accuracy", "mode" };

            try (var row = db.query(DBOpenHelper.SCORES_TABLENAME, columns, null, null, null, null, null)) {

                while (row.moveToNext()) try {

                    var score = new Score(
                            row.getInt(row.getColumnIndexOrThrow("id")),
                            row.getString(row.getColumnIndexOrThrow("filename")),
                            row.getString(row.getColumnIndexOrThrow("playername")),
                            row.getString(row.getColumnIndexOrThrow("replayfile")),
                            row.getString(row.getColumnIndexOrThrow("mode")),
                            row.getInt(row.getColumnIndexOrThrow("score")),
                            row.getInt(row.getColumnIndexOrThrow("combo")),
                            row.getString(row.getColumnIndexOrThrow("mark")),
                            row.getInt(row.getColumnIndexOrThrow("h300k")),
                            row.getInt(row.getColumnIndexOrThrow("h300")),
                            row.getInt(row.getColumnIndexOrThrow("h100k")),
                            row.getInt(row.getColumnIndexOrThrow("h100")),
                            row.getInt(row.getColumnIndexOrThrow("h50")),
                            row.getInt(row.getColumnIndexOrThrow("misses")),
                            row.getFloat(row.getColumnIndexOrThrow("accuracy")),
                            row.getLong(row.getColumnIndexOrThrow("time")),
                            row.getInt(row.getColumnIndexOrThrow("perfect")) == 1
                    );

                    DatabaseManager.getScoreTable().insertScore(score);

                } catch (Exception e) {
                    Log.e("ScoreLibrary", "Failed to import score from old database.", e);
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
