package se.linefeed.korjournal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "korjournal";
    public static final String ODOSNAPS_TABLE_NAME = "OdoSnaps";
    public static final String QUEUED_ODOSNAPS_TABLE_NAME = "QueuedOdoSnaps";
    public static final String VEHICLES_TABLE_NAME = "Vehicles";

    private static final String ODOSNAP_FIELDS =
            " (id INTEGER PRIMARY KEY, url TEXT, vehicle TEXT, odometer INT, driver TEXT, poslat DOUBLE, poslon DOUBLE, streetAddress TEXT, occurred DATETIME, start_end INT, why TEXT, picturePath TEXT);";
    public static final String[] ODOSNAP_COLS = {
            "id", "url", "vehicle", "odometer", "driver", "poslat", "poslon", "streetAddress",
            "occurred", "start_end", "why", "picturePath" };

    private static final String QUEUED_ODOSNAPS_TABLE_CREATE =
            "CREATE TABLE "+ QUEUED_ODOSNAPS_TABLE_NAME + ODOSNAP_FIELDS;
    private static final String ODOSNAPS_TABLE_CREATE =
            "CREATE TABLE "+ ODOSNAPS_TABLE_NAME + ODOSNAP_FIELDS;
    private static final String VEHICLES_TABLE_CREATE =
            "CREATE TABLE "+ VEHICLES_TABLE_NAME +
                    " (url TEXT, name TEXT);";

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ODOSNAPS_TABLE_CREATE);
        db.execSQL(VEHICLES_TABLE_CREATE);
        db.execSQL(QUEUED_ODOSNAPS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versionFrom, int versionTo) {
        if (versionFrom == 1 && versionTo == 2) {
            db.execSQL(VEHICLES_TABLE_CREATE);
        }
        if (versionFrom < 3 && versionTo == 3) {
            db.execSQL("DROP TABLE OdoSnaps;");
            db.execSQL(ODOSNAPS_TABLE_CREATE);
            db.execSQL(QUEUED_ODOSNAPS_TABLE_CREATE);
        }
    }
}


