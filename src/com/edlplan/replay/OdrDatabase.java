package com.edlplan.replay;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OdrDatabase {

    private static OdrDatabase odrDatabase;

    private SQLiteDatabase database;

    private Runnable onDatabaseChangedListener;

    public OdrDatabase(File file) {
        database = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, 0);
    }

    public OdrDatabase(SQLiteDatabase database) {
        this.database = database;
    }

    public static OdrDatabase get() {
        if (odrDatabase == null) {
            odrDatabase = new OdrDatabase(OdrConfig.getMainDatabase());
        }
        return odrDatabase;
    }

    private static String getString(Cursor cursor, String c) {
        return cursor.getString(cursor.getColumnIndexOrThrow(c));
    }

    private static int getInt(Cursor cursor, String c) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(c));
    }

    private static long getLong(Cursor cursor, String c) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(c));
    }

    private static float getFloat(Cursor cursor, String c) {
        return cursor.getFloat(cursor.getColumnIndexOrThrow(c));
    }

    public boolean available() {
        return database != null;
    }

    public long write(OsuDroidReplay replay) {
        ContentValues values = new ContentValues();
        values.put("filename", SongsLibrary.get().toSetLocal(replay.getFileName()));
        values.put("playername", replay.getPlayerName());
        if (replay.isAbsoluteReplay()) {
            values.put("replayfile", replay.getReplayFile());
        } else {
            values.put("replayfile", new File(OdrConfig.getScoreDir(), replay.getReplayFile()).getAbsolutePath());
        }
        values.put("mode", replay.getMode());
        values.put("score", replay.getScore());
        values.put("combo", replay.getCombo());
        values.put("mark", replay.getMark());
        values.put("h300k", replay.getH300k());
        values.put("h300", replay.getH300());
        values.put("h100k", replay.getH100k());
        values.put("h100", replay.getH100());
        values.put("h50", replay.getH50());
        values.put("misses", replay.getMisses());
        values.put("accuracy", replay.getAccuracy());
        values.put("time", replay.getTime());
        values.put("perfect", replay.getPerfect());

        return database.insert("scores", null, values);
    }

    public List<OsuDroidReplay> getReplayById(int id) {
        ArrayList<OsuDroidReplay> replays = new ArrayList<>();
        if (!available()) {
            return replays;
        }

        Cursor cursor = database.rawQuery("SELECT * FROM scores WHERE id = ?", new String[]{String.valueOf(id)});
        while (cursor.moveToNext()) {
            OsuDroidReplay replay = new OsuDroidReplay();
            replay.setFileName(getString(cursor, "filename"));
            replay.setPlayerName(getString(cursor, "playername"));
            replay.setReplayFile(getString(cursor, "replayfile"));
            replay.setMode(getString(cursor, "mode"));
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
            replays.add(replay);
        }

        return replays;
    }

    public List<OsuDroidReplay> getReplays() {
        ArrayList<OsuDroidReplay> replays = new ArrayList<>();
        if (!available()) {
            return replays;
        }

        Cursor cursor = database.rawQuery("SELECT * FROM scores", new String[0]);
        while (cursor.moveToNext()) {
            OsuDroidReplay replay = new OsuDroidReplay();
            replay.setFileName(getString(cursor, "filename"));
            replay.setPlayerName(getString(cursor, "playername"));
            replay.setReplayFile(getString(cursor, "replayfile"));
            replay.setMode(getString(cursor, "mode"));
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
            replays.add(replay);
        }

        return replays;
    }

    public int deleteReplay(int id) {
        if (!available()) {
            return 0;
        }

        int result = database.delete("scores", "id = ?", new String[]{String.valueOf(id)});
        if (onDatabaseChangedListener != null) {
            onDatabaseChangedListener.run();
        }
        return result;
    }

    public void setOnDatabaseChangedListener(Runnable listener) {
        this.onDatabaseChangedListener = listener;
    }
}
