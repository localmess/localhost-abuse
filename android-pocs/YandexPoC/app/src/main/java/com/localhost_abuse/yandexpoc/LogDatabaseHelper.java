package com.localhost_abuse.yandexpoc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class LogDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "logs.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "visit_logs";
    private static final String COL_ID = "id";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_DOMAIN = "domain";
    private static final String COL_BROWSER = "browser";
    private static LogDatabaseHelper instance;

    public static synchronized LogDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LogDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private LogDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COL_TIMESTAMP + " TEXT," + COL_DOMAIN + " TEXT," + COL_BROWSER + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertLog(String timestamp, String domain, String browser) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TIMESTAMP, timestamp);
        values.put(COL_DOMAIN, domain);
        values.put(COL_BROWSER, browser);
        db.insert(TABLE_NAME, null, values);
    }

    public List<String> getAllLogs() {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT MAX(" + COL_TIMESTAMP + ") AS " + COL_TIMESTAMP + ", "
                + COL_DOMAIN + ", " + COL_BROWSER
                + " FROM " + TABLE_NAME
                + " GROUP BY " + COL_DOMAIN + ", " + COL_BROWSER
                + " ORDER BY " + COL_TIMESTAMP + " DESC";
        Cursor c = db.rawQuery(sql, null);
        List<String> logs = new ArrayList<>();
        while (c.moveToNext()) {
            String ts = c.getString(c.getColumnIndexOrThrow(COL_TIMESTAMP));
            String domain = c.getString(c.getColumnIndexOrThrow(COL_DOMAIN));
            String browser = c.getString(c.getColumnIndexOrThrow(COL_BROWSER));
            logs.add(ts + ", " + domain + " (" + browser + ")");
        }
        c.close();
        return logs;
    }
}
