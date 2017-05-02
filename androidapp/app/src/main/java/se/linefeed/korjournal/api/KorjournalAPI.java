package se.linefeed.korjournal.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import se.linefeed.korjournal.MyJsonStringRequest;
import se.linefeed.korjournal.MyMultipartRequest;
import se.linefeed.korjournal.models.OdometerSnap;
import se.linefeed.korjournal.models.Vehicle;


/* All code talking to the API should be moved here so that only response processing is left
   in the calling classes
 */

public class KorjournalAPI {
    private RequestQueue mRequestQueue = null;
    private String username;
    private String password;
    private Context mContext;
    private final String base_url = "http://kilometerkoll.se";

    private HashMap<String,String> getAuthorizationHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        String creds = String.format("%s:%s", username, password);
        String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
        headers.put("Authorization", auth);
        return headers;
    }

    public KorjournalAPI(Context context) {
        mContext = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        username = sharedPreferences.getString("username_text","");
        password = sharedPreferences.getString("code_text","");
    }

    /**
     * Ask for a list of vehicles i am permitted to see
     * @param myVehicles
     * @param done
     * @param errorListener
     */
    public void get_vehicles(final HashMap<String,Vehicle> myVehicles, final KorjournalAPIInterface done, Response.ErrorListener errorListener) {
        final String api_url = base_url + "/api/vehicle/";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.GET, api_url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject v = results.getJSONObject(i);
                                Vehicle vehicle = new Vehicle(v.getString("name"), v.getString("url"));
                                myVehicles.put(vehicle.getName(), vehicle);
                            }
                        } catch (JSONException e) {
                            done.error("JSON-fel!");
                        }
                        done.done(response);
                    }
                },
                errorListener
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthorizationHeaders();
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
                return getAuthorizationHeaders();
            }

        };
        // Add the request to the RequestQueue.
        mRequestQueue.add(req);
    }

    /**
     * Upload an image for a previously sent odometer reading
     * @param odoImageFile String full path to file to send
     * @param linkedOdo String as "http://host/api/vehicle/2/
     * @param done KorjournalAPIInterface
     */

    public void send_odoimage(final String odoImageFile,final String linkedOdo, final KorjournalAPIInterface done) {
        final String url = base_url + "/api/odometerimage/";
        if (odoImageFile == null || linkedOdo == null) {
            return;
        }
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        MyMultipartRequest request = new MyMultipartRequest(url,
            new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    String resultResponse = new String(response.data);
                    try {
                        done.done(new JSONObject(resultResponse));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        done.error(e.getLocalizedMessage());
                    }
                }
            },
            new Response.ErrorListener() {
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
                    done.error(errorMessage);
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
                return getAuthorizationHeaders();
            };
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                50000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        mRequestQueue.add(request);
    }

    public void send_odometersnap(OdometerSnap odometerSnap, final KorjournalAPIInterface done){
        final String url = base_url + "/api/odometersnap/";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }

        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.POST, url, odometerSnap.toJSON(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        done.done(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        done.error("" + error.networkResponse.statusCode);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthorizationHeaders();
            }

        };
        // Add the request to the RequestQueue.
        mRequestQueue.add(req);
    }

    /**
     * Ask for odometersnaps I am allowed to see
     * @param errorListener
     */
    public void get_odosnaps(final ArrayList<OdometerSnap> odoSnapArr, final KorjournalAPIInterface done, Response.ErrorListener errorListener) {
        final String api_url = base_url + "/api/odometersnap/?days=30";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.GET, api_url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray odoSnaps = response.getJSONArray("results");
                            for (int i=0; i < odoSnaps.length(); i++) {
                                JSONObject o = odoSnaps.getJSONObject(i);
                                OdometerSnap oSnap = new OdometerSnap();
                                odoSnapArr.add(oSnap.loadFromJSON(o));
                            }
                        }
                        catch (JSONException e)
                        {
                            done.error("get_odosnaps JSON-fel!" + e.getMessage());
                        }
                        done.done(null);
                    }
                },
                errorListener
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthorizationHeaders();
            }
        };
        // Add the request to the RequestQueue.
        mRequestQueue.add(req);
    }

    public void tryRegisterCode(final String phone, final String code, final KorjournalAPIInterface done) {
        final String VER_URL = base_url + "/verify/";
        final String CHK_URL = base_url + "/api/vehicle/";

        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }

        // Before trying to verify, check if code is already ok
        StringRequest checkRequest = new StringRequest(Request.Method.GET, CHK_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                done.done(null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse == null) {
                    return;
                }
                Log.e("Error", "Server responded " + error.networkResponse.statusCode);
                StringRequest verifyRequest = new StringRequest(Request.Method.POST, VER_URL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("Info", "Server responded ok: " + response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Error", "Server responded " + error.networkResponse.statusCode);
                        done.error("" + error.networkResponse.statusCode);
                    }
                }) {
                    @Override
                    protected Map<String,String> getParams(){
                        Map<String,String> params = new HashMap<String, String>();
                        params.put("phone", phone);
                        params.put("code", code);
                        return params;
                    }

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String,String> params = new HashMap<String, String>();
                        params.put("Content-Type","application/x-www-form-urlencoded");
                        return params;
                    }
                };
                mRequestQueue.add(verifyRequest);
            }
        }) {

            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                String creds = String.format("%s:%s", phone, code);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                return headers;
            }
        };
        mRequestQueue.add(checkRequest);
    }

    /**
     * Ask for invoices due
     * @param done KorjournalAPIInterface
     */
    public void get_invoices_due(final KorjournalAPIInterface done) {
        final String api_url = base_url + "/api/invoicesdue/";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.GET, api_url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        done.done(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            Log.e("Error", "Server responded " + error.networkResponse.statusCode);
                            done.error("" + error.networkResponse.statusCode);
                        }
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthorizationHeaders();
            }
        };
        // Add the request to the RequestQueue.
        mRequestQueue.add(req);
    }
}
