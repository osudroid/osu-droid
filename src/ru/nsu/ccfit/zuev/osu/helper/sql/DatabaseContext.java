package ru.nsu.ccfit.zuev.osu.helper.sql;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import org.andengine.util.debug.Debug;
import org.andengine.util.FileUtils;

import java.io.File;
import java.io.IOException;

import ru.nsu.ccfit.zuev.osu.Config;

/**
 * Created by Fuuko on 2015/2/27.
 */
public class DatabaseContext extends ContextWrapper {

    private static final String DEBUG_CONTEXT = "DatabaseContext";
    private Context context;

    public DatabaseContext(Context base) {
        super(base);
        context = base;
    }

    @Override
    public File getDatabasePath(String name) {
        String dbfile = Config.getCorePath() + "databases/" + name;
        if (!dbfile.endsWith(".db")) {
            dbfile += ".db";
        }

        File result = new File(dbfile);

        if (!result.getParentFile().exists()) {
            result.getParentFile().mkdirs();
        }
        Debug.i("getDatabasePath(" + name + ") = " + result.getAbsolutePath());
        final File olddb = context.getDatabasePath(name);
        if (result.exists() == false && olddb.exists() == true) {
            try {
                FileUtils.copyFile(olddb, result);
            } catch (IOException e) {
                Debug.e(e);
            }
        }
        final String olddbfile = Config.getCorePath() + File.separator + "databases" + File.separator + name;
        final File olddb2 = new File(olddbfile);
        if (result.exists() == false && olddb2.exists() == true) {
            try {
                FileUtils.copyFile(olddb2, result);
            } catch (IOException e) {
                Debug.e(e);
            }
        }
        final String olddbfile2 = Config.getCorePath() + File.separator + "databases" + File.separator + "osudroid.db";
        final File olddb3 = new File(olddbfile2);
        if (result.exists() == false && olddb3.exists() == true) {
            try {
                FileUtils.copyFile(olddb3, result);
            } catch (IOException e) {
                Debug.e(e);
            }
        }
        return result;
    }

    /* this version is called for android devices >= api-11. thank to @damccull for fixing this. */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openOrCreateDatabase(name, mode, factory);
    }

    /* this version is called for android devices < api-11 */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        // SQLiteDatabase result = super.openOrCreateDatabase(name, mode, factory);
        Debug.i("openOrCreateDatabase(" + name + ") = " + result.getPath());
        return result;
    }
}