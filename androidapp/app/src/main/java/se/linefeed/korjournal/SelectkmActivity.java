package se.linefeed.korjournal;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

import se.linefeed.korjournal.api.KorjournalAPI;
import se.linefeed.korjournal.api.JsonAPIResponseInterface;
import se.linefeed.korjournal.api.TeslaAPI;
import se.linefeed.korjournal.models.OdometerSnap;
import se.linefeed.korjournal.models.Position;
import se.linefeed.korjournal.models.TeslaVehicle;
import se.linefeed.korjournal.models.VehicleList;

public class SelectkmActivity extends AppCompatActivity {

    private String[] kmChoices;
    private int kilometers = 0;
    private String odometerPhotoFile;
    private Location mLocation;
    private String streetAddress;
    private VehicleList vehicleList;
    private KorjournalAPI mApi;
    private SharedPreferences preferences;
    private FusedLocationProviderClient mFusedLocationClient;
    private static final int REQUEST_PERMISSIONS_FINE_LOCATION = 2;
    private static final int REQUEST_PERMISSIONS_COARSE_LOCATION = 3;
    private boolean accessCoarseLocation = false;
    private boolean accessFineLocation = false;
    private Thread reverseGeocoderThread = null;
    private MyApplication application;
    private Handler showKbdHandler;
    private boolean registrationHasBeenChecked = false;
    private boolean hadTeslaCredentialsOnCreate = false;
    private boolean teslaWasAsleep = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectkm);
        application = (MyApplication) getApplicationContext();
        kmChoices = new String[] {"..."};
        showKbdHandler = new Handler();
        final NumberPicker kmPicker = findViewById(R.id.numberPicker);
        kmPicker.setDisplayedValues(kmChoices);
        kmPicker.setMaxValue(0);
        kmPicker.setMinValue(0);
        kmPicker.setWrapSelectorWheel(false);
        kmPicker.setValue(0);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        hadTeslaCredentialsOnCreate = hasTeslaCredentials();
        if (application.getPictureTaken()) {
            Thread ocrThread = new Thread() {
                @Override
                public void run () {
                    updatePickerFromOCR();
                }
            };
            ocrThread.start();
        } else if (hadTeslaCredentialsOnCreate) {
            Thread teslaThread = new Thread() {
                @Override
                public void run () {
                    updatePickerFromTeslaAPI();
                }
            };
            teslaThread.start();
        } else {
            inputKmManually(null);
        }

        Button startTripButton = findViewById(R.id.startTripButton);
        startTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTripAndFinish(true);
            }
        });

        Button endTripButton = findViewById(R.id.endTripButton);
        endTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyApplication app = (MyApplication) getApplicationContext();
                sendTripAndFinish(false);
            }
        });

        kmPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                try {
                    kilometers = Integer.parseInt(kmChoices[newVal]);
                }
                catch (NumberFormatException e) {
                    kilometers = 0;
                }
            }
        });

        Spinner vehicleSpinner = findViewById(R.id.vehicleSpinner);
        vehicleList = new VehicleList(this,
                R.layout.my_spinner_item,
                vehicleSpinner);
        mApi = application.getApi();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_COARSE_LOCATION);
        } else {
            accessCoarseLocation = true;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS_FINE_LOCATION);
            } else {
                accessFineLocation = true;
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (accessCoarseLocation && accessFineLocation) {
            getLastLocation();
        }
        mApi.reloadCredentials();
        checkRegistration();
        vehicleList.request(mApi,
                preferences,
                new JsonAPIResponseInterface() {
                    @Override
                    public void done(JSONObject response) {
                        // request more stuffs
                    }

                    @Override
                    public void error(String error) {

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
    @Override
    protected void onResume() {
        super.onResume();
        if (hasTeslaCredentials() && (!hadTeslaCredentialsOnCreate || teslaWasAsleep)) {
            final NumberPicker kmPicker = findViewById(R.id.numberPicker);
            final EditText kmManualEdit = findViewById(R.id.kmManual);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    kmManualEdit.setVisibility(View.INVISIBLE);
                    kmPicker.setVisibility(View.VISIBLE);
                }
            });
            Thread teslaThread = new Thread() {
                @Override
                public void run () {
                    updatePickerFromTeslaAPI();
                }
            };
            teslaThread.start();
        }
    }

    @Override
    protected void onStop() {
        if (reverseGeocoderThread != null && reverseGeocoderThread.isAlive())
            reverseGeocoderThread.interrupt();
        super.onStop();
    }

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        //Log.d("SelectkmActivity", "getLastLocation invoked");
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            updateLocation(task.getResult());
                        } else {
                            Log.w("SelectkmActivity", "getLastLocation:exception", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessCoarseLocation = true;
                    if (accessFineLocation) {
                        getLastLocation();
                    } else {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_FINE_LOCATION);
                        }
                    }
                }
                return;
            }
            case REQUEST_PERMISSIONS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessFineLocation = true;
                    if (accessCoarseLocation)
                        getLastLocation();
                }
                return;
            }
        }
    }

    private void updateLocation(Location location) {
        if (location == null)
            Log.d("updateLocation", "called with null location");
        mLocation = location;
        TextView locationTextView = findViewById(R.id.locationTextView);
        locationTextView.setText(String.format(Locale.getDefault(),"%f %f",
                location.getLatitude(), location.getLongitude()));
        MyApplication app = (MyApplication) getApplicationContext();
        app.setLocation(location.getLatitude(),location.getLongitude());
        final Location streetLocation = location;
        reverseGeocoderThread = new Thread() {
            @Override
            public void run() {
                updateStreetAddress();
            }
        };
        reverseGeocoderThread.start();
    }

    private void updateStreetAddress() {
        Position position = new Position(mLocation);
        streetAddress = position.getStreetAddress(this);
        if (streetAddress == null || streetAddress.equals("")) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView locationTextView = findViewById(R.id.locationTextView);
                locationTextView.setText(streetAddress);//stuff that updates ui

            }
        });
    }

    private OdometerSnap manufactureOdometerSnap(boolean isStart) {
        String vehicleUrl = "";
        if (vehicleList != null && vehicleList.getSelectedVehicle() != null) {
           vehicleUrl = vehicleList.getSelectedVehicle().getUrl();
        }
        return new OdometerSnap(vehicleUrl,
                kilometers,
                new Position(mLocation),
                streetAddress,
                null,
                isStart,
                !isStart,
                odometerPhotoFile);
    }

    private void sendTripAndFinish(boolean isStart) {
        if (!checkHasVehicles()) {
            return;
        }
        final int nextState = (isStart ? MyApplication.FSM_DONE : MyApplication.FSM_REASON);
        final SelectkmActivity activity = this;
        final OdometerSnap odometerSnap = manufactureOdometerSnap(isStart);
        application.setLastOdometerSnap(null);

        application.showToast("Skickar mätarställning...", 1);
        odometerSnap.sendApi(mApi,
                new JsonAPIResponseInterface() {
                    @Override
                    public void done(JSONObject response) {
                        try {
                            odometerSnap.setUrl(response.get("url").toString());
                            application.setLastOdometerSnap(odometerSnap);
                            if (odometerPhotoFile != null) {
                                application.showToast("Skickar mätarfoto...", 2);
                                odometerSnap.sendImage(mApi,
                                        new JsonAPIResponseInterface() {
                                            @Override
                                            public void done(JSONObject response) {
                                                if (odometerPhotoFile != null) {
                                                    odometerSnap.deletePicture();
                                                }
                                                application.setNextFsmState(nextState);
                                                activity.finish();
                                            }
                                            @Override
                                            public void error(String error) {
                                                // XXX Fixme queue for later sending
                                                application.showToast("Ett nätverksfel inträffade (försök igen)", 1);
                                            }
                                        });
                            } else {
                                application.setNextFsmState(nextState);
                                activity.finish();
                            }
                        }
                        catch (JSONException e)
                        {
                            application.showToast("Ett fel inträffade (svar från tjänsten)", 1);
                        }
                    }
                    @Override
                    public void error(String error) {
                        // XXX Fixme queue for later sending
                        application.showToast("Ett nätverksfel inträffade (försök igen)", 1);
                    }
                }
        );
    }

    public void goBackCamera(View v) {
        final MyApplication app = (MyApplication) getApplicationContext();
        if (odometerPhotoFile != null) {
            File file = new File(odometerPhotoFile);
            if (file.exists()) {
                file.delete();
            }
            odometerPhotoFile = null;
        }
        app.setNextFsmState(MyApplication.FSM_CAMERA);
        finish();
    }
    public void inputKmManually(View v) {
        final NumberPicker kmPicker = findViewById(R.id.numberPicker);
        final EditText kmManualEdit = findViewById(R.id.kmManual);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                kmPicker.setVisibility(View.INVISIBLE);
                kmManualEdit.setVisibility(View.VISIBLE);
            }
        });
        kmManualEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    kilometers = Integer.parseInt(kmManualEdit.getText().toString());
                } catch (NumberFormatException n) {
                    // Avoid crash due to empty number entered
                }
            }
        });
        // Show keyboard after a small delay

        Runnable showKbdRunnable = new Runnable() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (kmManualEdit.requestFocus()) {
                            InputMethodManager imm = (InputMethodManager)
                                    getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(kmManualEdit, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                });
            }
        };
        showKbdHandler.postDelayed(showKbdRunnable,500);

    }
    private String[] addSortedUniqPositive(String[] insertIn, String insert) {
        // throws exception if insert does not parse
        int insertValue = Integer.parseInt(insert);
        if (insertValue < 0) {
            throw new NumberFormatException("Negative number");
        }
        int numIntegersIn = 0;
        for (String test: insertIn) {
            if (test.equals(insert))
                return insertIn;
            try {
                Integer.parseInt(test);
                numIntegersIn++;
            }
            catch (NumberFormatException e) { /* noop */ }
        }

        String[] insertOut = new String[numIntegersIn + 1];
        int insertIndex = 0;
        boolean inserted = false;
        for (String copy: insertIn) {
            try {
                if (!inserted && insertValue < Integer.parseInt(copy)) {
                    insertOut[insertIndex++] = insert;
                    inserted = true;
                }
                insertOut[insertIndex++] = copy;
            }
            catch (NumberFormatException e) {
                continue;
            }
        }
        if (!inserted) {
            insertOut[insertIndex++] = insert;
        }
        return insertOut;
    }

    private void updatePickerFromOCR() {
        OCREngine ocrEngine = new OCREngine(this);
        if (!ocrEngine.loadBitmap()) {
            inputKmManually(null);
            return;
        }
        final Object synchronizes = new Object();
        odometerPhotoFile = ocrEngine.getPhotoFile();
        for (int i = -1; i < 7; i++) {
            int pickerNoItems;
            synchronized (synchronizes) {
                pickerNoItems = kmChoices.length;
                try {
                    kmChoices = addSortedUniqPositive(
                            kmChoices,
                            String.format(Locale.getDefault(), "%d", ocrEngine.runOCR(i)));
                } catch (NumberFormatException e) {
                    continue;
                }
                if (kmChoices.length == pickerNoItems)
                    // No new unique items were added
                    continue;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NumberPicker kmPicker = findViewById(R.id.numberPicker);
                    synchronized(synchronizes) {
                        // kmChoices may not be altered by OCR thread while we are updating the UI.
                        kmPicker.setDisplayedValues(kmChoices);
                        kmPicker.setMaxValue(kmChoices.length - 1);
                        kmPicker.setMinValue(0);
                        kmPicker.setWrapSelectorWheel(false);
                        kmPicker.setValue(kmChoices.length / 2);
                        try {
                            kilometers = Integer.parseInt(kmChoices[kmChoices.length / 2]);
                        }
                        catch (NumberFormatException e) {
                            kilometers = 0;
                        }
                    }
                    Log.d("OCRThread", "kilometers is now " + kilometers);
                }
            });
        }
    }

    private void updatePickerFromTeslaAPI() {
        if (!TeslaAPI.prefTokenIsStillValid(this)) {
            return;
        }
        final Object synchronizes = new Object();
        class Odometer {
            private long km;
            private Odometer() {
                km = 0L;
            }
        }
        final Odometer odometer = new Odometer();
        TeslaAPI teslaAPI = new TeslaAPI(this);
        final TeslaVehicle teslaVehicle = new TeslaVehicle("any","");
        teslaVehicle.loadAnyFromAPI(teslaAPI, new JsonAPIResponseInterface() {
            @Override
            public void done(JSONObject response) {
                odometer.km = teslaVehicle.getOdometerKm();
                synchronized (synchronizes) {
                    try {
                        kmChoices = addSortedUniqPositive(
                                kmChoices,
                                String.format(Locale.getDefault(), "%d", odometer.km));
                    } catch (NumberFormatException e) {
                        return;
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        NumberPicker kmPicker = findViewById(R.id.numberPicker);
                        synchronized(synchronizes) {
                            // kmChoices may not be altered by other thread while we are updating the UI.
                            kmPicker.setDisplayedValues(kmChoices);
                            kmPicker.setMaxValue(kmChoices.length - 1);
                            kmPicker.setMinValue(0);
                            kmPicker.setWrapSelectorWheel(false);
                            kmPicker.setValue(kmChoices.length / 2);
                            try {
                                kilometers = Integer.parseInt(kmChoices[kmChoices.length / 2]);
                            }
                            catch (NumberFormatException e) {
                                kilometers = 0;
                            }
                        }
                        Log.d("TeslaThread", "kilometers is now " + kilometers);
                    }
                });
            }

            @Override
            public void error(String error) {
                if (error==null)
                    return;

                Log.e("TeslaAPI", error);
                if (error.contains("vehicle unavailable")) {
                    teslaWasAsleep = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Teslan är inte vaken", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }
            }
        });
    }

    public void clickVehicleSpinner(View v) {
        Spinner vs = findViewById(R.id.vehicleSpinner);
        vs.performClick();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return OptionsMenu.onCreateOptionsMenu(this, menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return OptionsMenu.handleOptionsItemSelected(this,item) || super.onOptionsItemSelected(item);
    }

    private void checkRegistration() {
        if (registrationHasBeenChecked) {
            return;
        }
        if (preferences.getString("username_text","").equals("") || preferences.getString("code_text","").equals("")) {
            registrationHasBeenChecked = true;
            RegistrationRequiredDialogFragment registrationNeeded = new RegistrationRequiredDialogFragment();
            Bundle args = new Bundle();
            args.putString("message", "För att använda tjänsten behöver du en personlig kod. Gå till inställningar nu?");
            registrationNeeded.setArguments(args);
            registrationNeeded.show(getFragmentManager(),"not_registered");
        }
        registrationHasBeenChecked = true;
    }

    private boolean checkHasVehicles() {
        if (vehicleList.getSelectedVehicle() != null) {
            if (vehicleList.getSelectedVehicle().getUrl() != "") {
                return true;
            }
        }
        RegistrationRequiredDialogFragment vehiclesNeeded = new RegistrationRequiredDialogFragment();
        Bundle args = new Bundle();
        args.putString("message", "Du verkar inte ha skapat något fordon. Gå till inställningar nu?");
        vehiclesNeeded.setArguments(args);
        vehiclesNeeded.show(getFragmentManager(),"no_vehicles");
        return false;
    }

    private void authFailed() {
        if (preferences.getString("username_text","").equals("") || preferences.getString("code_text","").equals("")) {
            checkRegistration();
        } else {
            RegistrationRequiredDialogFragment registrationNeeded = new RegistrationRequiredDialogFragment();
            Bundle args = new Bundle();
            args.putString("message", "Den personliga koden verkar vara ogiltig. Gå till inställningar nu?");
            registrationNeeded.setArguments(args);
            registrationNeeded.show(getFragmentManager(),"login_failed");
        }
    }

    private boolean hasTeslaCredentials() {
        if (preferences == null) {
            return false;
        }
        if (preferences.getString(TeslaAPI.PREF_TESLA_API_USERNAME, "").length() < 1) {
            return false;
        }
        if (!TeslaAPI.prefTokenIsStillValid(this)) {
            RegistrationRequiredDialogFragment registrationNeeded = new RegistrationRequiredDialogFragment();
            Bundle args = new Bundle();
            args.putString("message", "Du behöver mata in ditt Tesla-lösenord på nytt. Gå till inställningar nu?");
            registrationNeeded.setArguments(args);
            registrationNeeded.show(getFragmentManager(),"token_expired");
            return false;
        }
        return true;
    }
}
