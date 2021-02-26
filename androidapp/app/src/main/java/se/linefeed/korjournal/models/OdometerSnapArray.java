package se.linefeed.korjournal.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import se.linefeed.korjournal.DatabaseOpenHelper;
import se.linefeed.korjournal.ReviewRequestHelper;
import se.linefeed.korjournal.api.KorjournalAPI;
import se.linefeed.korjournal.api.JsonAPIResponseInterface;
import se.linefeed.korjournal.api.RequestDoneInterface;

public class OdometerSnapArray {
    private ArrayList<OdometerSnap> odometerSnaps;

    public OdometerSnapArray(KorjournalAPI api, Context context, RequestDoneInterface requestDone) {
        reload(api, context, requestDone);
    }

    public OdometerSnapArray(KorjournalAPI api, Context context, int year, int month, RequestDoneInterface requestDone) {
        loadMonth(api, context, year, month, requestDone);
    }

    void reload(KorjournalAPI api, final Context context, final RequestDoneInterface requestDone) {
        if (odometerSnaps != null) {
            odometerSnaps.clear();
        } else {
            odometerSnaps = new ArrayList<>();
        }
        final int logDays = 30;
        final String whereClause = "occurred >= julianday('now') - ?";
        final String[] whereArgs = { Integer.toString(logDays) };
        api.get_odosnaps(odometerSnaps, 30,
                new JsonAPIResponseInterface() {
                        @Override
                        public void done(JSONObject response) {
                            DatabaseOpenHelper dboh = new DatabaseOpenHelper(context);
                            SQLiteDatabase db = dboh.getWritableDatabase();
                            db.delete(DatabaseOpenHelper.ODOSNAPS_TABLE_NAME, whereClause, whereArgs);
                            int entries = 0;
                            for (OdometerSnap o: odometerSnaps) {
                                o.insertDb(db, DatabaseOpenHelper.ODOSNAPS_TABLE_NAME);
                                entries++;
                            }
                            if (entries > 0) {
                                ReviewRequestHelper.setUsagesLastMonth(context, entries);
                            }
                            db.close();
                            requestDone.done(entries);
                        }
                        @Override
                        public void error(String error) {
                            requestDone.error(error);
                        }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.getClass() == AuthFailureError.class) {
                            requestDone.error("auth");
                        }
                    }
                });
    }

    void loadMonth(KorjournalAPI api, final Context context, int year, int month, final RequestDoneInterface requestDone) {
        if (odometerSnaps != null) {
            odometerSnaps.clear();
        } else {
            odometerSnaps = new ArrayList<>();
        }
        final String whereClause = "strftime('%Y', occurred) == ? AND strftime('%m', occurred) == ?";
        final String[] whereArgs = {
                Integer.toString(year),
                String.format(Locale.getDefault(), "%02d", month)
        };
        api.get_odosnaps(odometerSnaps, year, month,
                new JsonAPIResponseInterface() {
                    @Override
                    public void done(JSONObject response) {
                        DatabaseOpenHelper dboh = new DatabaseOpenHelper(context);
                        SQLiteDatabase db = dboh.getWritableDatabase();
                        db.delete(DatabaseOpenHelper.ODOSNAPS_TABLE_NAME, whereClause, whereArgs);
                        int results = 0;
                        for (OdometerSnap o: odometerSnaps) {
                            o.insertDb(db, DatabaseOpenHelper.ODOSNAPS_TABLE_NAME);
                            results++;
                        }
                        db.close();
                        requestDone.done(results);
                    }
                    @Override
                    public void error(String error) {
                        requestDone.error(error);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.getClass() == AuthFailureError.class) {
                            requestDone.error("auth");
                        }
                    }
                });
    }

    public ArrayList<OdometerSnap> asArrayList() {
        return odometerSnaps;
    }

}
