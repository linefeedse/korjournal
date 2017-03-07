package se.linefeed.korjournal.models;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;

import java.util.ArrayList;

import se.linefeed.korjournal.DatabaseOpenHelper;
import se.linefeed.korjournal.api.KorjournalAPI;
import se.linefeed.korjournal.api.KorjournalAPIInterface;

public class SendQueue {
    private ArrayList<OdometerSnap> odometerSnaps;
    private Context mContext = null;

    public SendQueue(Context context) {
        odometerSnaps = new ArrayList<>();
        mContext = context;
    }

    public void queue(OdometerSnap odometerSnap) {
        DatabaseOpenHelper dboh = new DatabaseOpenHelper(mContext);
        SQLiteDatabase db = dboh.getWritableDatabase();

        odometerSnap.insertDb(db, DatabaseOpenHelper.QUEUED_ODOSNAPS_TABLE_NAME);
        db.close();
    }

    public void sendAll(final Context context, final KorjournalAPI korjournalAPI) {
        final DatabaseOpenHelper dboh = new DatabaseOpenHelper(mContext);
        SQLiteDatabase db = dboh.getReadableDatabase();
        String orderBy = null;
        String groupBy = null;
        String having = null;
        String selection = null;

        Cursor cursor = db.query(DatabaseOpenHelper.QUEUED_ODOSNAPS_TABLE_NAME,
                DatabaseOpenHelper.ODOSNAP_COLS, selection, null, groupBy, having, orderBy);

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                final OdometerSnap odometerSnap = new OdometerSnap(cursor);
                final int rowId;
                if (cursor.getColumnIndex("id") > -1) {
                    rowId = cursor.getInt(cursor.getColumnIndex("id"));
                } else {
                    rowId = -1;
                }
                Thread addressThread = new Thread() {
                    @Override
                    public void run() {
                        odometerSnap.updateStreetAddress(context);
                        odometerSnap.sendApi(korjournalAPI,
                                new KorjournalAPIInterface() {
                                    @Override
                                    public void done(JSONObject ignored) {
                                        // XXX Fixme send picture if there is a picturepath...

                                        SQLiteDatabase db = dboh.getWritableDatabase();
                                        db.delete(DatabaseOpenHelper.ODOSNAPS_TABLE_NAME,
                                                "id="+ rowId, null);
                                        db.close();
                                    }
                                    @Override
                                    public void error(String error) {
                                        // noop
                                    }
                                });
                    }
                };
            }
        }
        cursor.close();
        db.close();
    }

}
