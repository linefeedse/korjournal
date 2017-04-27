package se.linefeed.korjournal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.text.ParseException;
import java.util.ArrayList;

import se.linefeed.korjournal.models.OdometerSnap;
import se.linefeed.korjournal.models.TripListModel;

public class TripListActivity extends AppCompatActivity {
    private CameraView mCameraView = null;

    ListView list;
    TripListAdapter adapter;
    public TripListActivity tripListView = null;
    public ArrayList<TripListModel> tripViewValuesArr = new ArrayList<TripListModel>();
    private Button linkButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tripview);

        tripListView = this;

        Intent intent = this.getIntent();
        String vehicleUrl = intent.getStringExtra("vehicleUrl");

        setListData(vehicleUrl);

        Resources res = getResources();
        list= ( ListView )findViewById( R.id.tripListView );

        /**************** Create Custom Adapter *********/
        adapter=new TripListAdapter( tripListView, tripViewValuesArr,res );
        list.setAdapter( adapter );
        linkButton = (Button)findViewById(R.id.linkButton);

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences =  PreferenceManager.getDefaultSharedPreferences(this);
        String phone = sharedPreferences.getString("username_text","");
        String prefillPhoneArgs = null;
        if (phone.equals("")) {
            prefillPhoneArgs = "";
        } else {
            prefillPhoneArgs = "?phone=" + phone;
        }
        final String clickUrl = "http://kilometerkoll.se/login/" + prefillPhoneArgs;
        linkButton.setClickable(true);
        linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View parent) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(clickUrl));
                startActivity(intent);
            }
        });

    }

    /****** Function to set data in ArrayList *************/
    public void setListData(String vehicleUrl)
    {
        if (vehicleUrl == null) {
            return;
        }
        DatabaseOpenHelper dboh = new DatabaseOpenHelper(getApplicationContext());
        SQLiteDatabase db = dboh.getReadableDatabase();
        String cols[] = { "occurred", "streetAddress", "odometer", "why", "start_end" };
        String orderBy = "occurred DESC";
        String groupBy = null;
        String having = null;
        String selection = "vehicle = '" + vehicleUrl + "'";

        Cursor cursor = db.query(DatabaseOpenHelper.ODOSNAPS_TABLE_NAME,
                cols, selection, null, groupBy, having, orderBy);
        ArrayList<OdometerSnap> odoSnaps = new ArrayList<>();

        while (cursor.moveToNext()) {
            OdometerSnap os = new OdometerSnap(cursor);
            odoSnaps.add(os);
        }
        cursor.close();
        db.close();
        TripListModel tmpTrip = null;
        for (OdometerSnap snap: odoSnaps) {

            if (snap.isStart() && tmpTrip == null) {
                String occured = "";
                try {
                    occured = snap.getWhenLocal();
                } catch (ParseException e) {

                }
                final TripListModel trip = new TripListModel(occured,
                        "(ej avslutad)",
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
                tmpTrip.setKmString(snap.getOdometer() + " - " + tmpTrip.getKmString());
                String occured = "";
                try {
                    occured = snap.getWhenLocal();
                } catch (ParseException e) {

                }
                tmpTrip.setWhen(occured + " - " + tmpTrip.getWhen());
                final TripListModel trip = new TripListModel(tmpTrip);
                tripViewValuesArr.add(trip);
                tmpTrip = null;
            }
            if (snap.isEnd()) {
                String occured = "";
                try {
                    occured = snap.getWhenLocal();
                } catch (ParseException e) {

                }
                tmpTrip = new TripListModel(occured,
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
