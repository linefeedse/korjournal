package se.linefeed.korjournal;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/* All code talking to the API should be moved here so that only response processing is left
   in the calling classes
 */


public class KorjournalAPI {
    private RequestQueue mRequestQueue = null;
    private String username;
    private String password;
    private Context mContext;
    private final String base_url = "http://korjournal.linefeed.se";

    public KorjournalAPI(Context context) {
        mContext = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        username = sharedPreferences.getString("username_text","");
        password = sharedPreferences.getString("code_text","");
    }

    /**
     * Ask for a list of vehicles i am permitted to see
     * @param responseListener
     * @param errorListener
     */

    public void get_vehicles(Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener) {
        final String api_url = base_url + "/api/vehicle/";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.GET, api_url, null,
                responseListener,
                errorListener
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
        mRequestQueue.add(req);
    }

    /**
     * Create a vehicle with the name specified in name
     * @param name
     * @param responseListener
     * @param errorListener
     */
    public void create_vehicle(String name, Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener) {
        final String api_url = base_url + "/api/vehicle/";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        String body = "{ \"name\": \"" + name + "\" }";
        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.POST, api_url, body,
                responseListener,
                errorListener
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
        mRequestQueue.add(req);
    }

    /**
     * Upload an image for a previously sent odometer reading
     * @param odoImageFile String full path to file to send
     * @param linkedOdo String as "http://host/api/vehicle/2/
     * @param responseListener a Response.Listener<NetworkResponse>
     * @param errorListener a Response.ErrorListener
     */

    public void send_odoimage(final String odoImageFile,final String linkedOdo, Response.Listener<NetworkResponse> responseListener, Response.ErrorListener errorListener) {
        final String url = base_url + "/api/odometerimage/";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        MyMultipartRequest request = new MyMultipartRequest(url, responseListener, errorListener) {
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

        mRequestQueue.add(request);
    }

    /**
     * Upload an odometer reading
     * @param vehicle
     * @param odometer
     * @param location
     * @param streetAddress
     * @param reason
     * @param isStart
     * @param isEnd
     * @param responseListener
     * @param errorListener
     */

    public void send_odometersnap(String vehicle, String odometer, Location location, String streetAddress, String reason, boolean isStart, boolean isEnd, Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener){
        final String url = base_url + "/api/odometersnap/";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        String body = "{ \"vehicle\": \"" + vehicle +
                "\", \"odometer\": \"" + odometer + "\"";
        if (location != null) {
            body = body.concat(String.format(Locale.US, ", \"poslat\": \"%f\", \"poslon\": \"%f\"",
                    location.getLatitude(),
                    location.getLongitude()));
        }
        if (streetAddress != null) {
            body = body.concat(String.format(Locale.getDefault(),
                    ", \"where\": \"%s\"",
                    streetAddress.replace("\"","''")));
        }
        if (reason != null) {
            body = body.concat(String.format(Locale.getDefault(),
                    ", \"why\": \"%s\"",
                    reason.replace("\"","''")));
        }
        if (isStart) {
            body = body.concat(String.format(Locale.getDefault(),
                    ", \"type\": \"%d\"",
                    1));
        }
        if (isEnd) {
            body = body.concat(String.format(Locale.getDefault(),
                    ", \"type\": \"%d\"",
                    2));
        }
        body = body.concat(" }");
        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.POST, url, body,
                responseListener,
                errorListener
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
        mRequestQueue.add(req);
    }
}
