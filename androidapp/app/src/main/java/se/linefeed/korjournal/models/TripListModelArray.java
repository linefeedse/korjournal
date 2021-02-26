package se.linefeed.korjournal.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

import se.linefeed.korjournal.DatabaseOpenHelper;

public class TripListModelArray {
    private ArrayList<TripListModel> array;

    public TripListModelArray() {
        array = new ArrayList<>();
    }

    public TripListModelArray loadFromDb(DatabaseOpenHelper dboh, String vehicleUrl, int year, int month) {
        SQLiteDatabase db = dboh.getReadableDatabase();
        String cols[] = { "occurred", "streetAddress", "odometer", "why", "start_end" };
        String orderBy = "occurred DESC";
        String groupBy = null;
        String having = null;
        String selection = "vehicle = '" + vehicleUrl + "'";
        selection = selection.concat(" AND strftime('%Y', occurred) == ?");
        selection = selection.concat(" AND strftime('%m', occurred) == ?");
        String[] selectionArgs = {
                Integer.toString(year),
                String.format(Locale.getDefault(), "%02d", month)
        };

        Cursor cursor = db.query(DatabaseOpenHelper.ODOSNAPS_TABLE_NAME,
                cols, selection, selectionArgs, groupBy, having, orderBy);
        ArrayList<OdometerSnap> odoSnaps = new ArrayList<>();

        array.clear();

        while (cursor.moveToNext()) {
            OdometerSnap os = new OdometerSnap(cursor);
            odoSnaps.add(os);
        }
        cursor.close();
        db.close();
        TripListModel tmpTrip = null;
        for (OdometerSnap snap: odoSnaps) {

            if (snap.isStart() && tmpTrip == null) {
                String occurred = "";
                try {
                    occurred = snap.getWhenLocal();
                } catch (ParseException e) {

                }
                final TripListModel trip = new TripListModel(occurred,
                        "(ej avslutad)",
                        snap.getOdometer() + "",
                        "",
                        snap.getStreetAddress(),
                        snap.getReason());
                array.add( trip );
                continue;
            }
            if (tmpTrip != null) {
                tmpTrip.setFromAddress(snap.getStreetAddress());
                if (tmpTrip.getReason().equals("") && !snap.getReason().equals("")) {
                    tmpTrip.setReason(snap.getReason());
                }
                tmpTrip.setEndKmString(tmpTrip.getStartKmString());
                tmpTrip.setStartKmString(snap.getOdometer() + "");
                String occurred = "";
                try {
                    occurred = snap.getWhenLocal();
                } catch (ParseException e) {

                }
                tmpTrip.setWhen(occurred + " - " + tmpTrip.getWhen());
                final TripListModel trip = new TripListModel(tmpTrip);
                array.add(trip);
                tmpTrip = null;
            }
            if (snap.isEnd()) {
                String occurred = "";
                try {
                    occurred = snap.getWhenLocal();
                } catch (ParseException e) {

                }
                tmpTrip = new TripListModel(occurred,
                        snap.getStreetAddress(),
                        "" + snap.getOdometer(),
                        "",
                        null,
                        snap.getReason());
            }
        }
        return this;
    }

    public ArrayList<TripListModel> asArrayList() {
        return array;
    }
}
