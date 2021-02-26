package se.linefeed.korjournal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import se.linefeed.korjournal.R;
import se.linefeed.korjournal.models.TripListModel;
import se.linefeed.korjournal.models.TripListModelArray;

public class TripListActivity extends AppCompatActivity {

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
        int year = intent.getIntExtra("year",1970);
        int month = intent.getIntExtra("month", 1);

        setListData(vehicleUrl, year, month);

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
        setResult(RESULT_OK);
    }

    /****** Function to set data in ArrayList *************/
    public void setListData(String vehicleUrl, int year, int month)
    {
        if (vehicleUrl == null) {
            return;
        }
        DatabaseOpenHelper dboh = new DatabaseOpenHelper(getApplicationContext());
        TripListModelArray loaded = new TripListModelArray();
        loaded.loadFromDb(dboh, vehicleUrl, year, month);
        tripViewValuesArr.clear();
        tripViewValuesArr.addAll(loaded.asArrayList());

    }


    /*****************  This function used by adapter ****************/
    public void onItemClick(int mPosition)
    {
        TripListModel tempValues = ( TripListModel ) tripViewValuesArr.get(mPosition);

        Toast.makeText(tripListView,
                "Kan inte redigera resan här", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                break;
        }
        //return super.onOptionsItemSelected(item);
        return true;
    }
}
