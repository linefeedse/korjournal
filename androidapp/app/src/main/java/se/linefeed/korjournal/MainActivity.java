package se.linefeed.korjournal;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView mTextView;
    private Button odometerSend;
    private EditText odometerText;
    private RequestQueue requestQueue = null;
    private Spinner vehicleSpinner;
    private ArrayList<String> vehicleArr;
    private ArrayAdapter<String> vehicleSpinnerAdapter;
    HashMap<String,String> myVehicles;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.volleyTextView);
        odometerSend = (Button) findViewById(R.id.odometerSend);
        odometerText = (EditText) findViewById(R.id.odometerText);
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
    }

    private void onRequestResponse(String msg) {
        mTextView.setText(msg);
        odometerText.setText("");
        odometerSend.setText("Skicka");
        odometerSend.setClickable(true);
    }

    private void requestVehicles() {
        final String url = "http://37.139.5.125/api/vehicle/";
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
        final String url = "http://37.139.5.125/api/odometersnap/";
        final String vehicle = "http://37.139.5.125/api/vehicle/1/";
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this);
        }

        odometerSend.setText("...");
        odometerSend.setClickable(false);
        String body = "{ \"vehicle\": \""
                + vehicle
                + "\", \"odometer\": \""
                + odometerText.getText().toString()
                + "\" }";

        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.POST, url, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
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
}
