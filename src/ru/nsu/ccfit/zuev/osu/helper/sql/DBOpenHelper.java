package ru.nsu.ccfit.zuev.osu.helper.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper extends SQLiteOpenHelper {

    public static final String SCORES_TABLENAME = "scores";

    public static final String MAPS_TABLENAME = "ddlmaps";

    private static final String DBNAME = "osudroid_test";

    private static final int DBVERSION = 6;

    private static DBOpenHelper helper = null;

    private DBOpenHelper(Context context) {
        super(new DatabaseContext(context), DBNAME, null, DBVERSION);
    }

    public static DBOpenHelper getOrCreate(Context context) {
        if (helper == null) {
            helper = new DBOpenHelper(context);
        }
        return helper;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + SCORES_TABLENAME + " (" + "id INTEGER PRIMARY KEY," + "filename TEXT," + "playername TEXT," + "replayfile TEXT," + "mode TEXT," + "score INTEGER," + "combo INTEGER," + "mark TEXT," + "h300k INTEGER," + "h300 INTEGER," + "h100k INTEGER," + "h100 INTEGER," + "h50 INTEGER," + "misses INTEGER," + "accuracy FLOAT," + "time TIMESTAMP," + "perfect INTEGER);");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + MAPS_TABLENAME + " (" + "id INTEGER PRIMARY KEY," + "size INTEGER," + "inserttime INTEGER," + "link TEXT);");

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("DROP TABLE IF EXISTS " + TABLENAME);
        if (oldVersion <= 5 && newVersion == 6) {
            if (oldVersion != 5) {
                String sql = "alter table [" + SCORES_TABLENAME + "] add [time] TIMESTAMP";
                db.execSQL(sql);
            }
            String sql = "alter table [" + SCORES_TABLENAME + "] add [perfect] INTEGER";
            db.execSQL(sql);
        }
    }

}
