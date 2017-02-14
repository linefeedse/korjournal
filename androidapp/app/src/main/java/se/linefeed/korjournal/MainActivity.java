package se.linefeed.korjournal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import se.linefeed.korjournal.api.KorjournalAPI;
import se.linefeed.korjournal.api.KorjournalAPIInterface;
import se.linefeed.korjournal.models.OdometerSnap;
import se.linefeed.korjournal.models.Position;
import se.linefeed.korjournal.models.Vehicle;
import se.linefeed.korjournal.models.VehicleList;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private TextView statusText;
    private TextView locationText;
    private AutoCompleteTextView reasonText;
    private Button odometerSend;
    private ImageButton cameraButton;
    private EditText odometerText;
    private RequestQueue requestQueue = null;
    private VehicleList vehicleList = null;
    private Spinner vehicleSpinner;
    private ArrayList<OdometerSnap> odoSnapArr;
    private GoogleApiClient mGoogleApiClient;
    private final String TAG = "MainActivity";
    private Location mLocation = null;
    private String streetAddress;
    private ProgressBar sendProgress;
    private ImageView odoImage;
    private String odoImageFile = null;
    private ImageButton deletePicButton;
    private RadioButton radioIsStartButton, radioIsEndButton;
    private SharedPreferences sharedPreferences;
    private KorjournalAPI mApi = null;
    private ArrayList<String> reasons;
    private ArrayAdapter<String> reasonSuggestionAdapter;

    private final int flash_color = 0xFFEE3030;
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
        reasonText = (AutoCompleteTextView) findViewById(R.id.reasonText);
        odometerSend = (Button) findViewById(R.id.odometerSend);
        cameraButton = (ImageButton) findViewById(R.id.camerabutton);
        odometerText = (EditText) findViewById(R.id.odometerText);
        odometerText.setSelectAllOnFocus(true);
        vehicleSpinner = (Spinner) findViewById(R.id.vehicleSpinner);
        vehicleList = new VehicleList(getApplicationContext(),
                R.layout.my_spinner_item,
                vehicleSpinner);
        odoSnapArr = new ArrayList<OdometerSnap>();
        reasons = new ArrayList<String>();
        reasonSuggestionAdapter = new ArrayAdapter<String>(getApplicationContext(),
                R.layout.my_dropdown_item_1line,
                reasons);
        reasonText.setAdapter(reasonSuggestionAdapter);
        sendProgress = (ProgressBar) findViewById(R.id.progressBar);
        odoImage = (ImageView) findViewById(R.id.odoImageView);
        deletePicButton = (ImageButton) findViewById(R.id.picturedeletebtn);
        radioIsStartButton = (RadioButton) findViewById(R.id.radio_isstart);
        radioIsEndButton = (RadioButton) findViewById(R.id.radio_isend);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mApi = new KorjournalAPI(this);
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

        checkRegistration();
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this);
        }
        vehicleList.request(mApi, statusText, sharedPreferences);
        requestOdosnaps();
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
                if (MainActivity.this.checkSelections()) {
                    MainActivity.this.sendOdometersnap();
                }
            }
        });

        sendProgress.setProgress(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        vehicleList.resetSelected(sharedPreferences);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        vehicleList.saveSelected();
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

    private void setStatusText(String msg) {
        statusText.setText(msg);
    }

    private void onRequestResponse(String msg) {
        setStatusText(msg);
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
        Thread addressThread = new Thread() {
            @Override
            public void run() {
                updateStreetAddress();
            }
        };
        addressThread.start();
        locationText.setText("");
        Thread reasonThread = new Thread() {
            @Override
            public void run() {
                updateReasons();
            }
        };
        reasonThread.start();
    }

    private void checkRegistration() {
        if (sharedPreferences.getString("username_text","").equals("") || sharedPreferences.getString("code_text","").equals("")) {
            RegistrationRequiredDialogFragment registrationNeeded = new RegistrationRequiredDialogFragment();
            Bundle args = new Bundle();
            args.putString("message", "För att använda tjänsten behöver du en personlig kod. Gå till inställningar nu?");
            registrationNeeded.setArguments(args);
            registrationNeeded.show(getFragmentManager(),"not_registered");
        }
    }

    private void authFailed() {
        if (sharedPreferences.getString("username_text","").equals("") || sharedPreferences.getString("code_text","").equals("")) {
            checkRegistration();
        } else {
            RegistrationRequiredDialogFragment registrationNeeded = new RegistrationRequiredDialogFragment();
            Bundle args = new Bundle();
            args.putString("message", "Den personliga koden verkar vara ogiltig. Gå till inställningar nu?");
            registrationNeeded.setArguments(args);
            registrationNeeded.show(getFragmentManager(),"login_failed");
        }
    }

    private boolean checkSelections() {
        final int transparent = 0x00000000;
        if (!radioIsStartButton.isChecked() && !radioIsEndButton.isChecked()) {
           final RadioGroup startEndRadioGroup = (RadioGroup) findViewById(R.id.startEndRadioGroup);
            startEndRadioGroup.setBackgroundColor(flash_color);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startEndRadioGroup.setBackgroundColor(transparent);
                }
            }, 300);
            return false;
        }
        String odometer = odometerText.getText().toString();
        try {
            if (Integer.valueOf(odometer) < 1) {
                odometer = "0";
            }
        } catch (NumberFormatException e) {
            odometer = "0";
        }
        if (odometer.equals("0") && odoImageFile == null) {
            odoImage.setBackgroundColor(flash_color);
            odometerText.setBackgroundColor(flash_color);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    odoImage.setBackgroundColor(transparent);
                    odometerText.setBackgroundColor(transparent);
                }
            }, 300);
            return false;
        }
        if (vehicleList.getSelectedVehicle() == null) {
            vehicleList.flashSpinner(flash_color);
            return false;
        }
        return true;
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

    private void updateReasons() {
        if (mLocation == null) {
            return;
        }
        Position currentPos = new Position(mLocation.getLatitude(), mLocation.getLongitude());
        reasons.clear();
        for (OdometerSnap o: odoSnapArr) {
            if (o.getReason() == null || o.getReason().equals("")) {
                continue;
            }
            if (reasons.contains(o.getReason())) {
                continue;
            }
            if (currentPos.distanceFrom(o.getPosition()) > 0.5) {
                Log.i(TAG,"Distance: " + currentPos.distanceFrom(o.getPosition()));
                continue;
            }
            reasons.add(o.getReason());
            Log.i(TAG,"Added reason " + o.getReason());
        }
        reasonSuggestionAdapter.notifyDataSetChanged();
    }

    private void requestOdosnaps() {

        odoSnapArr.clear();
        setStatusText("Hämtar tidigare resor");
        mApi.get_odosnaps(odoSnapArr,
                new KorjournalAPIInterface() {
                    @Override
                    public void done() {
                        DatabaseOpenHelper dboh = new DatabaseOpenHelper(getApplicationContext());
                        SQLiteDatabase db = dboh.getWritableDatabase();
                        db.delete("OdoSnaps", null, null);
                        for (OdometerSnap o: odoSnapArr) {
                            o.insertDb(db);
                        }
                        db.close();
                        updateReasons();
                    }
                    @Override
                    public void error(String error) {
                        onRequestResponse(error);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.getClass() == AuthFailureError.class) {
                            authFailed();
                        }
                    }
                }
        );
    }

    private void sendOdometersnap() {

        if (vehicleList.getSelectedVehicle() == null) {
            vehicleList.flashSpinner(flash_color);
            return;
        }
        String vehicleUrl = vehicleList.getSelectedVehicle().getUrl();
        String odometer = odometerText.getText().toString();
        try {
            if (Integer.valueOf(odometer) < 1) {
                odometer = "0";
            }
        } catch (NumberFormatException e) {
            odometer = "0";
        }
        String reason = reasonText.getText().toString();

        odometerSend.setText("...");
        odometerSend.setClickable(false);

        sendProgress.setProgress(30);
        setStatusText("Skickar mätarställning...");
        mApi.send_odometersnap(vehicleUrl,
                odometer,
                mLocation,
                streetAddress,
                reason,
                radioIsStartButton.isChecked(),
                radioIsEndButton.isChecked(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String odolink = response.get("url").toString();
                            onRequestResponse("Skickat!");
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
            );
    }

    private void sendImageForOdo(final String linkedOdo) {
        setStatusText("Skickar bild...");
        mApi.send_odoimage(odoImageFile,linkedOdo, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                String resultResponse = new String(response.data);
                try {
                    JSONObject result = new JSONObject(resultResponse);

                    String imagefile = result.getString("imagefile");
                    sendProgress.setProgress(99);
                    Log.i("INFO", "Successfully uploaded imagefile: " + imagefile);
                    File file = new File(odoImageFile);
                    setStatusText("Raderar bild...");
                    if (file.exists()) {
                        file.delete();
                    }
                    loadLastOdoImage();
                    setStatusText("Mätarställning har skickats");
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
                        try {
                            response.getString("imagefile");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("Error", errorMessage);
                error.printStackTrace();
            }
        });
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int progress = sendProgress.getProgress();
                if (progress > 49 && progress < 79)
                    sendProgress.setProgress(progress + 20);
            }
        }, 1500);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int progress = sendProgress.getProgress();
                if (progress > 69 && progress < 79)
                    sendProgress.setProgress(progress + 20);
            }
        }, 4000);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLocation != null) {
            locationText.setText(String.format(Locale.getDefault(),"%f %f", mLocation.getLatitude(), mLocation.getLongitude()));
            Thread addressThread = new Thread() {
                @Override
                public void run() {
                    updateStreetAddress();
                }
            };
            addressThread.start();
            Thread reasonThread = new Thread() {
                @Override
                public void run() {
                    updateReasons();
                }
            };
            reasonThread.start();
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

    public void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.action_settings:
                showSettings();
                return true;
            case R.id.action_tripview:
                if (vehicleList.getSelectedVehicle() == null) {
                    vehicleList.flashSpinner(flash_color);
                    return false;
                }
                intent = new Intent(this, TripListActivity.class);
                String vehicleUrl = vehicleList.getSelectedVehicle().getUrl();
                intent.putExtra("vehicleUrl", vehicleUrl);
                startActivity(intent);
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void loadLastOdoImage() {
        File dir = new File(this.getExternalFilesDir(null),"korjournal");
        File[] pictures = dir.listFiles();
        if (pictures == null || pictures.length < 1) {
            odoImageFile = null;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    odoImage.setImageBitmap(null);
                }
            });

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
