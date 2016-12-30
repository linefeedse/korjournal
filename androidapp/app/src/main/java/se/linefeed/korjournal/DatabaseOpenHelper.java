package se.linefeed.korjournal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "korjournal";
    private static final String ODOSNAPS_TABLE_NAME = "OdoSnaps";
    private static final String ODOSNAPS_TABLE_CREATE =
            "CREATE TABLE "+ ODOSNAPS_TABLE_NAME +
                    " (url TEXT, vehicle TEXT, odometer INT, driver TEXT, poslat DOUBLE, poslon DOUBLE, streetAddress TEXT, occurred DATETIME, start_end INT, why TEXT);";

    DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ODOSNAPS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versionFrom, int versionTo) {

    }
}


