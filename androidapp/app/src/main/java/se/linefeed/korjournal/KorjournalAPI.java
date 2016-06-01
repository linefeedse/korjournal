package se.linefeed.korjournal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
}
