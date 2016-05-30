package se.linefeed.korjournal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private TextView statusText;
    private TextView locationText;
    private Button odometerSend;
    private ImageButton cameraButton;
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
    private ProgressBar sendProgress;
    private ImageView odoImage;
    private String odoImageFile = null;
    private ImageButton deletePicButton;
    private RadioButton radioIsStartButton, radioIsEndButton;
    private SharedPreferences sharedPreferences;

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

        statusText = (TextView) findViewById(R.id.volleyTextView);
        locationText = (TextView) findViewById(R.id.locationTextView);
        odometerSend = (Button) findViewById(R.id.odometerSend);
        cameraButton = (ImageButton) findViewById(R.id.camerabutton);
        odometerText = (EditText) findViewById(R.id.odometerText);
        odometerText.setSelectAllOnFocus(true);
        vehicleSpinner = (Spinner) findViewById(R.id.vehicleSpinner);
        vehicleArr = new ArrayList<String>();
        vehicleSpinnerAdapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_item,
                vehicleArr);
        myVehicles = new HashMap<String,String>();
        vehicleSpinner.setAdapter(vehicleSpinnerAdapter);
        sendProgress = (ProgressBar) findViewById(R.id.progressBar);
        odoImage = (ImageView) findViewById(R.id.odoImageView);
        deletePicButton = (ImageButton) findViewById(R.id.picturedeletebtn);
        radioIsStartButton = (RadioButton) findViewById(R.id.radio_isstart);
        radioIsEndButton = (RadioButton) findViewById(R.id.radio_isend);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
        Thread thread = new Thread() {
            @Override
            public void run() {
                loadLastOdoImage();
            }
        };
        thread.start();

        cameraButton.setClickable(true);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View parent) {
                MainActivity.this.startCameraActivity();
            }
        });
        odometerSend.setClickable(true);
        odometerSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View parent) {
                MainActivity.this.sendOdometersnap();
            }
        });

        sendProgress.setProgress(0);
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
        statusText.setText(msg);
        odometerText.setText("");
        odometerSend.setText("Skicka");
        odometerSend.setClickable(true);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        if (mLocation != null) {
            locationText.setText(String.format(Locale.getDefault(),"%f %f", mLocation.getLatitude(), mLocation.getLongitude()));
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                updateStreetAddress();
            }
        };
        thread.start();
        locationText.setText("");
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
                    locationText.setText(streetAddress);//stuff that updates ui

                }
            });
        }
    }

    private void requestVehicles() {
        final String url = API_URL + "/vehicle/";
        final String username = sharedPreferences.getString("username_text","");
        final String password = sharedPreferences.getString("code_text","");

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
                String creds = String.format("%s:%s",username,password);
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
        final String username = sharedPreferences.getString("username_text","");
        final String password = sharedPreferences.getString("code_text","");

        String vehicle = myVehicles.get(vehicleSpinner.getSelectedItem().toString());
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this);
        }

        odometerSend.setText("...");
        odometerSend.setClickable(false);
        String odometer = odometerText.getText().toString();
        try {
            if (Integer.valueOf(odometer) < 1) {
                odometer = "0";
            }
        } catch (NumberFormatException e) {
            odometer = "0";
        }
        String body = "{ \"vehicle\": \"" + vehicle +
                "\", \"odometer\": \"" + odometer + "\"";
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
        if (radioIsStartButton.isChecked()) {
            body = body.concat(String.format(Locale.getDefault(),
                    ", \"type\": \"%d\"",
                    1));
        }
        if (radioIsEndButton.isChecked()) {
            body = body.concat(String.format(Locale.getDefault(),
                    ", \"type\": \"%d\"",
                    2));
        }
        body = body.concat(" }");

        sendProgress.setProgress(30);
        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.POST, url, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String odolink = response.get("url").toString();
                            onRequestResponse("Sparat!");
                            sendProgress.setProgress(50);
                            sendImageForOdo(odolink);
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
                        sendProgress.setProgress(0);
                    }
                }
        ) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                String creds = String.format("%s:%s",username,password);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                return headers;
            }

        };
        // Add the request to the RequestQueue.
        requestQueue.add(req);
    }

    private void sendImageForOdo(final String linkedOdo) {
        final String username = sharedPreferences.getString("username_text","");
        final String password = sharedPreferences.getString("code_text","");
        final String url = API_URL + "/odometerimage/";
        MyMultipartRequest request = new MyMultipartRequest(url, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                String resultResponse = new String(response.data);
                try {
                    JSONObject result = new JSONObject(resultResponse);

                    String imagefile = result.getString("imagefile");
                    sendProgress.setProgress(99);
                    Log.i("INFO", "Successfully uploaded imagefile: " + imagefile);
                    File file = new File(odoImageFile);
                    if (file.exists()) {
                        file.delete();
                    }
                    loadLastOdoImage();
                    sendProgress.setProgress(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                    }
                } else {
                    String result = new String(networkResponse.data);
                    try {
                        JSONObject response = new JSONObject(result);
                        String message = null;
                        try {
                            message = response.getString("imagefile");
                        } catch (JSONException e) {
                            message = "no imagefile in response";
                        }
                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message+" Please login again";
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message+ " Check your inputs";
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message+" Internal Server Error";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("Error", errorMessage);
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("odometersnap", linkedOdo);
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    params.put("imagefile", new DataPart("odometerimage.jpg", odoImageFile, "image/jpeg"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                String creds = String.format("%s:%s",username,password);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                return headers;
            };
        };

        requestQueue.add(request);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLocation != null) {
            locationText.setText(String.format(Locale.getDefault(),"%f %f", mLocation.getLatitude(), mLocation.getLongitude()));
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void loadLastOdoImage() {
        File dir = new File(this.getExternalFilesDir(null),"korjournal");
        File[] pictures = dir.listFiles();
        if (pictures.length < 1) {
            odoImageFile = null;
            deletePicButton.setClickable(false);
            return;
        }
        if (pictures.length > 1) {
            Arrays.sort(pictures, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    return Long.valueOf(rhs.lastModified()).compareTo(lhs.lastModified());
                }
            });
        }
        odoImageFile = pictures[0].getAbsolutePath();
        Log.d("loadLastOdoImage", "Found file: " + odoImageFile);
        final Bitmap bitmap = BitmapFactory.decodeFile(odoImageFile);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                odoImage.setImageBitmap(bitmap);
            }
        });
        deletePicButton.setClickable(true);
        deletePicButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View parent) {
                        File file = new File(odoImageFile);
                        if (file.exists()) {
                            file.delete();
                        }
                        Log.d("loadLastOdoImage", "Deleted file: " + odoImageFile);
                        loadLastOdoImage();
                    }
                });
    }


}
