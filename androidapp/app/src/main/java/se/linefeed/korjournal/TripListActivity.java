package se.linefeed.korjournal;

import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import se.linefeed.korjournal.models.OdometerSnap;
import se.linefeed.korjournal.models.TripListModel;

public class TripListActivity extends AppCompatActivity {
    private CameraView mCameraView = null;

    ListView list;
    TripListAdapter adapter;
    public TripListActivity tripListView = null;
    public ArrayList<TripListModel> tripViewValuesArr = new ArrayList<TripListModel>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tripview);

        tripListView = this;

        /******** Take some data in Arraylist ( CustomListViewValuesArr ) ***********/
        setListData();

        Resources res = getResources();
        list= ( ListView )findViewById( R.id.tripListView );

        /**************** Create Custom Adapter *********/
        adapter=new TripListAdapter( tripListView, tripViewValuesArr,res );
        list.setAdapter( adapter );

    }

    /****** Function to set data in ArrayList *************/
    public void setListData()
    {

        DatabaseOpenHelper dboh = new DatabaseOpenHelper(getApplicationContext());
        SQLiteDatabase db = dboh.getReadableDatabase();
        String cols[] = { "occurred", "streetAddress", "odometer", "why", "start_end" };
        String orderBy = "occurred DESC";
        String groupBy = null;
        String having = null;
        String selection = null;

        Cursor cursor = db.query("OdoSnaps", cols, selection, null, groupBy, having, orderBy);
        ArrayList<OdometerSnap> odoSnaps = new ArrayList<>();

        while (cursor.moveToNext()) {
            OdometerSnap os = new OdometerSnap(cursor);
            odoSnaps.add(os);
        }
        cursor.close();
        TripListModel tmpTrip = null;
        for (OdometerSnap snap: odoSnaps) {

            if (snap.isStart() && tmpTrip == null) {
                final TripListModel trip = new TripListModel(snap.getWhen(),
                        "(pågående)",
                        "",
                        "Från: " + snap.getStreetAddress(),
                        snap.getReason());
                tripViewValuesArr.add( trip );
                continue;
            }
            if (tmpTrip != null) {
                tmpTrip.setFromAddress("Från: " + snap.getStreetAddress());
                if (tmpTrip.getReason().equals("") && !snap.getReason().equals("")) {
                    tmpTrip.setReason(snap.getReason());
                }
                tmpTrip.setKmString(tmpTrip.getKmString() + "-");
                tmpTrip.setWhen(snap.getWhen() + "-" + tmpTrip.getWhen());
                final TripListModel trip = new TripListModel(tmpTrip);
                tripViewValuesArr.add(trip);
                tmpTrip = null;
            }
            if (snap.isEnd()) {
                tmpTrip = new TripListModel("" + snap.getWhen(),
                        "Till: " + snap.getStreetAddress(),
                        "" + snap.getOdometer(),
                        null,
                        snap.getReason());
            }
        }

    }


    /*****************  This function used by adapter ****************/
    public void onItemClick(int mPosition)
    {
        TripListModel tempValues = ( TripListModel ) tripViewValuesArr.get(mPosition);

        Toast.makeText(tripListView,
                "Kan inte redigera resan här", Toast.LENGTH_LONG).show();
    }
}
