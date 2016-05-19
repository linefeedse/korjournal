package se.linefeed.korjournal;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private TextView mTextView;
    private Button odometerSend;
    private Button cameraButton;
    private EditText odometerText;
    private RequestQueue requestQueue = null;
    private Spinner vehicleSpinner;
    private ArrayList<String> vehicleArr;
    private ArrayAdapter<String> vehicleSpinnerAdapter;
    HashMap<String,String> myVehicles;
    private GoogleApiClient mGoogleApiClient;
    private final String TAG = "MainActivity";
    private final String API_URL = "http://korjournal.linefeed.se/api";
    private Location mLocation = null;
    private String streetAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocationRequest mLocationRequest;
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.volleyTextView);
        odometerSend = (Button) findViewById(R.id.odometerSend);
        cameraButton = (Button) findViewById(R.id.camerabutton);
        odometerText = (EditText) findViewById(R.id.odometerText);
        odometerText.setSelectAllOnFocus(true);
        vehicleSpinner = (Spinner) findViewById(R.id.vehicleSpinner);
        vehicleArr = new ArrayList<String>();
        vehicleSpinnerAdapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_item,
                vehicleArr);
        myVehicles = new HashMap<String,String>();
        vehicleSpinner.setAdapter(vehicleSpinnerAdapter);
    }

    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this);
        }
        requestVehicles();
        odometerSend.setClickable(true);
        odometerSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View parent) {
                MainActivity.this.sendOdometersnap();
            }
        });
        cameraButton.setClickable(true);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View parent) {
                MainActivity.this.startCameraActivity();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private void startCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private void onRequestResponse(String msg) {
        mTextView.setText(msg);
        odometerText.setText("");
        odometerSend.setText("Skicka");
        odometerSend.setClickable(true);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        if (mLocation != null) {
            mTextView.setText(String.format(Locale.getDefault(),"%f %f", mLocation.getLatitude(), mLocation.getLongitude()));
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                updateStreetAddress();
            }
        };
        thread.start();
        mTextView.setText("");
    }

    private void updateStreetAddress() {
        List<Address> addresses = null;
        streetAddress = null;
        Geocoder geocoder = new Geocoder(this,Locale.getDefault());
        Log.i(TAG, "Requesting address for " + String.format(Locale.getDefault(),"%f %f", mLocation.getLatitude(), mLocation.getLongitude()));
        try {
            addresses = geocoder.getFromLocation(
                    mLocation.getLatitude(),
                    mLocation.getLongitude(),
                    // just a single address.
                    1);
        } catch (IOException ioException) {
            Log.e(TAG, "service not available", ioException);
        }
        if (addresses == null || addresses.size()  == 0) {
            Log.e(TAG, "No street address found");
        }
        else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, "Address found");
            streetAddress = TextUtils.join(",", addressFragments);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(streetAddress);//stuff that updates ui

                }
            });
        }
    }

    private void requestVehicles() {
        final String url = API_URL + "/vehicle/";
        vehicleArr.clear();
        myVehicles.clear();
        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONArray vehicles = response.getJSONArray("results");
                            for (int i=0; i < vehicles.length(); i++) {
                                JSONObject v = vehicles.getJSONObject(i);
                                vehicleArr.add(v.getString("name"));
                                myVehicles.put(v.getString("name"),v.getString("url"));
                                vehicleSpinnerAdapter.notifyDataSetChanged();
                            }
                        }
                        catch (JSONException e)
                        {
                            onRequestResponse("JSON-fel!");
                            vehicleArr.add("Fel: inga fordon!");
                            vehicleSpinnerAdapter.notifyDataSetChanged();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        vehicleArr.add("Fel: inga fordon!");
                        onRequestResponse("Fel: inga fordon!");
                        vehicleSpinnerAdapter.notifyDataSetChanged();
                    }
                }
        ) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                String creds = String.format("%s:%s","abc","123");
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                return headers;
            }

        };
        // Add the request to the RequestQueue.
        requestQueue.add(req);
        vehicleSpinnerAdapter.notifyDataSetChanged();
    }

    private void sendOdometersnap() {
        final String url = API_URL + "/odometersnap/";
        String vehicle = myVehicles.get(vehicleSpinner.getSelectedItem().toString());
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this);
        }

        odometerSend.setText("...");
        odometerSend.setClickable(false);
        String body = "{ \"vehicle\": \"" + vehicle +
                "\", \"odometer\": \"" + odometerText.getText().toString() + "\"";
        if (mLocation != null) {
            body = body.concat(String.format(Locale.US, ", \"poslat\": \"%f\", \"poslon\": \"%f\"",
                mLocation.getLatitude(),
                mLocation.getLongitude()));
        }
        if (streetAddress != null) {
            body = body.concat(String.format(Locale.getDefault(),
                    ", \"where\": \"%s\"",
                    streetAddress.replace("\"","''")));
        }
        body = body.concat(" }");

        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.POST, url, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String odoSent = response.get("odometer").toString();
                            onRequestResponse("Skickade km: " + odoSent);
                        }
                        catch (JSONException e)
                        {
                            onRequestResponse("JSON Error!");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onRequestResponse("Error!");
                    }
                }
        ) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                String creds = String.format("%s:%s","abc","123");
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                return headers;
            }

        };
        // Add the request to the RequestQueue.
        requestQueue.add(req);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLocation != null) {
            mTextView.setText(String.format(Locale.getDefault(),"%f %f", mLocation.getLatitude(), mLocation.getLongitude()));
            Thread thread = new Thread() {
                @Override
                public void run() {
                    updateStreetAddress();
                }
            };
            thread.start();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services disconnected.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Location services failed connecting.");
    }

}
