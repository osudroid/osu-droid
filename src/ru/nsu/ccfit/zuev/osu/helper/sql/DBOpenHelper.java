package ru.nsu.ccfit.zuev.osu.helper.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

@Deprecated // TODO: Please consider to remove this in the future.
public class DBOpenHelper extends SQLiteOpenHelper {

    public static final String SCORES_TABLENAME = "scores";
    public static final String DBNAME = "osudroid_test";
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

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
