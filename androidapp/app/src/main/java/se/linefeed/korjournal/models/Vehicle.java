package se.linefeed.korjournal.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Vehicle {
    private String name;
    private String url;

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public Vehicle(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public Vehicle(Cursor dbCursor) {
        if (dbCursor.getColumnIndex("name") > -1) {
            name = dbCursor.getString(dbCursor.getColumnIndex("name"));
        } else {
            name = null;
        }
        if (dbCursor.getColumnIndex("url") > -1) {
            url = dbCursor.getString(dbCursor.getColumnIndex("url"));
        } else {
            url = null;
        }
    }

    public void insertDb(SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues(2);

        contentValues.put("name", getName());
        contentValues.put("url", getUrl());
        db.insert("Vehicles", null, contentValues);
    }
}
