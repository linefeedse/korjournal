package se.linefeed.korjournal.models;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import se.linefeed.korjournal.DatabaseOpenHelper;
import se.linefeed.korjournal.api.JsonAPIResponseInterface;
import se.linefeed.korjournal.api.KorjournalAPI;

public class SendQueue {
    private Context mContext = null;

    public SendQueue(Context context) {
        mContext = context;
    }

    public void queue(OdometerSnap odometerSnap) {
        DatabaseOpenHelper dboh = new DatabaseOpenHelper(mContext);
        SQLiteDatabase db = dboh.getWritableDatabase();

        odometerSnap.insertDb(db, DatabaseOpenHelper.QUEUED_ODOSNAPS_TABLE_NAME);
        db.close();
    }

    public void sendAll(final KorjournalAPI kilometerkollAPI) {
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
                        odometerSnap.updateStreetAddress(mContext);
                        odometerSnap.sendApi(kilometerkollAPI,
                                new JsonAPIResponseInterface() {
                                    @Override
                                    public void done(JSONObject response) {
                                        // XXX Fixme send picture if there is a picturepath...

                                        SQLiteDatabase db = dboh.getWritableDatabase();
                                        db.delete(DatabaseOpenHelper.ODOSNAPS_TABLE_NAME,
                                                "id="+ rowId, null);
                                        db.close();
                                        if (odometerSnap.getPicturePath() == null || odometerSnap.getPicturePath().equals(""))
                                        {
                                            return;
                                        }
                                        try {
                                            odometerSnap.setUrl(response.get("url").toString());
                                            odometerSnap.sendImage(kilometerkollAPI,
                                                new JsonAPIResponseInterface() {
                                                    @Override
                                                    public void done(JSONObject response) {
                                                        try {
                                                            String imagefile = response.getString("imagefile");
                                                            Log.i("INFO", "Successfully uploaded imagefile: " + imagefile);
                                                            odometerSnap.deletePicture();
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                    @Override
                                                    public void error(String error) {
                                                        Log.i("Error", error);
                                                    }
                                                }
                                            );
                                        }
                                        catch (JSONException e)
                                        {
                                            Log.i("JSON Error!", e.getMessage());
                                        }
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
