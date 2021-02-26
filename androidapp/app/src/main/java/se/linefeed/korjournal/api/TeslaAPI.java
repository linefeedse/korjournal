package se.linefeed.korjournal.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.ClientError;
import com.android.volley.Header;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import se.linefeed.korjournal.MyJsonStringRequest;

public class TeslaAPI {
    private RequestQueue mRequestQueue = null;
    private Context mContext;
    private static final String TESLA_AUTH_URL = "https://auth.tesla.com/oauth2/v3";
    private static final String TESLA_REDIRECT_URI = "https://auth.tesla.com/void/callback";
    private static final String TESLA_OWNERS_API_AUTH_URL = "https://owner-api.teslamotors.com/oauth/token";
    private static final String TESLA_CLIENT_ID = "81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384";
    private static final String TESLA_CLIENT_SECRET = "c7257eb71a564034f9419ee651c7d0e5f7aa6bfbd18bafb5c5c033b093bb2fa3";
    private String accessToken;
    public static final String PREF_TESLA_API_USERNAME = "tesla_username_text";
    public static final String PREF_TESLA_API_PASSWORD = "tesla_password_text";
    public static final String PREF_TESLA_API_TOKEN = "tesla_api_token";
    public static final String PREF_TESLA_API_TOKEN_EXPIRES = "tesla_api_token_expires";

    private HashMap<String,String> getAuthorizationHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        String auth = "Bearer " + accessToken;
        headers.put("Authorization", auth);
        return headers;
    }

    public TeslaAPI(Context context) {
        mContext = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        accessToken = sharedPreferences.getString(PREF_TESLA_API_TOKEN,"");
    }

    public TeslaAPI(Context context, final String accessToken) {
        mContext = context;
        this.accessToken = accessToken;
    }

    /**
     * Ask for a list of teslas i am permitted to see
     */
    public void get_vehicles(final JsonAPIResponseInterface done) {
        final String api_url = "https://owner-api.teslamotors.com/api/1/vehicles";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                api_url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                            try {
                                JSONArray results = response.getJSONArray("response");
                                if (results.length() < 1) {
                                    done.error("No Tesla vehicles found");
                                }
                                done.done(response);
                            } catch (JSONException e) {
                                done.error("JSON-fel! 001");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // Fixme
                            done.error(error.getMessage());
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

    public void get_vehicle_data(String id, final JsonAPIResponseInterface done) {
        final String api_url = "https://owner-api.teslamotors.com/api/1/vehicles/"
                + id + "/vehicle_data";
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        MyJsonStringRequest req = new MyJsonStringRequest(Request.Method.GET, api_url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject results = response.getJSONObject("response");
                            done.done(results);
                        } catch (JSONException e) {
                            done.error("JSON-fel! 002");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof ClientError && error.networkResponse != null && error.networkResponse.data != null) {
                            String responseData = new String(error.networkResponse.data);
                            if (responseData.contains("vehicle unavailable")) {
                                done.error("vehicle unavailable");
                                return;
                            }
                        }
                        done.error(error.getMessage());
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

    public static boolean prefTokenIsStillValid(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences == null) return false;
        Long expires = sharedPreferences.getLong(PREF_TESLA_API_TOKEN_EXPIRES, 0L);
        long seconds_since_epoch = Calendar.getInstance().getTimeInMillis()/1000;
        if (expires > seconds_since_epoch - 86400) {
            return true;
        }
        return false;
    }

    static String randomHex(int numChars) {
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while(sb.length() < numChars){
            sb.append(String.format("%08x", r.nextInt()));
        }
        return sb.toString().substring(0, numChars);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    static String newVerifier() {
        final int numChars = 86;
        return randomHex(numChars);
    }

    static String newChallenge(String verifier) throws NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        final String digest = bytesToHex(digester.digest(verifier.getBytes()));
        byte[] encoded = Base64.encode(
                digest.getBytes(), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        return new String(encoded);
    }

    static String newState() {
        return randomHex(48);
    }

    public static boolean getNewAccessToken(Context context, String username, String password, final JsonAPIResponseInterface done) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        getAuthSidCookie(requestQueue, username, password, newVerifier(), newState(), done);
        return true;
    }

    public static boolean getNewAccessToken(Context context, String password, final JsonAPIResponseInterface done) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences == null) return false;
        String username = sharedPreferences.getString(PREF_TESLA_API_USERNAME, "");
        if (username == "") return false;
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        getAuthSidCookie(requestQueue, username, password, newVerifier(), newState(), done);
        return true;
    }

    public static void getAuthSidCookie(final RequestQueue rq, final String username, final String pw, final String verifier, final String state, final JsonAPIResponseInterface done) throws UnsupportedEncodingException {
        String challenge;
        try {
            challenge = newChallenge(verifier);
        } catch (NoSuchAlgorithmException e) {
            return;
        }
        StringRequest r = new StringRequest(
                Request.Method.GET,
                TESLA_AUTH_URL + "/authorize?"
                        + "client_id=ownerapi&"
                        + "code_challenge=" + challenge + "&"
                        + "code_challenge_method=S256&"
                        + "redirect_uri=" + URLEncoder.encode(TESLA_REDIRECT_URI,"utf-8") + "&"
                        + "response_type=code&"
                        + "scope=openid+email+offline_access&"
                        + "state=" + state,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.d("getAuthSidCookie", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("getAuthSidCookie", error.getMessage());
                        done.error(error.getMessage());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Host", "auth.tesla.com");
                params.put("User-Agent", "curl/7.71.1");
                params.put("accept", "*/*");
                return params;
            }
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse networkResponse) {
                //FIXME check for 200
                String allHeaders = "";
                String sessionCookie = "";
                for (Header h: networkResponse.allHeaders) {
                    allHeaders += h.getName() + ": " + h.getValue() + "\n";
                    if (h.getName().equals("Set-Cookie")) {
                        if (h.getValue().substring(0,14).equals("tesla-auth.sid")) {
                            Pattern p = Pattern.compile("^(tesla[^;]*);\\ ");
                            Matcher m = p.matcher(h.getValue());
                            if (m.find())
                                sessionCookie = m.group(1);
                            else
                                sessionCookie = "";
                        }
                    }
                }
                String body;
                try {
                    body = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                } catch (UnsupportedEncodingException e) {
                    body = new String(networkResponse.data);
                }
                Pattern p = Pattern.compile("type=\"hidden\" name=\"(.*?)\" value=\"(.*?)\"");
                Matcher m = p.matcher(body);
                HashMap<String,String> formParams = new HashMap<>();
                while (m.find()) {
                    formParams.put(m.group(1),m.group(2));
                }
                try {
                    newAuthToken(rq, username, pw, verifier, state, sessionCookie, formParams, done);
                } catch (UnsupportedEncodingException e) {
                    // ignore
                }
                com.android.volley.Response<String> result = com.android.volley.Response.success(allHeaders + body,
                        HttpHeaderParser.parseCacheHeaders(networkResponse));
                return result;
            }
        };
        rq.add(r);
    }

    static void newAuthToken(final RequestQueue rq, final String username, final String pw, final String verifier, final String state, final String sessionCookie, final HashMap<String, String> formParams, final JsonAPIResponseInterface done) throws UnsupportedEncodingException {
        HttpURLConnection.setFollowRedirects(false);
        String challenge;
        try {
            challenge = newChallenge(verifier);
        } catch (NoSuchAlgorithmException e) {
            return;
        }
        StringRequest r = new StringRequest(
                Request.Method.POST,
                TESLA_AUTH_URL + "/authorize?"
                        + "client_id=ownerapi&"
                        + "code_challenge=" + challenge + "&"
                        + "code_challenge_method=S256&"
                        + "redirect_uri=" + URLEncoder.encode(TESLA_REDIRECT_URI,"utf-8") + "&"
                        + "response_type=code&"
                        + "scope=openid+email+offline_access&"
                        + "state=" + state,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        done.error(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof AuthFailureError)
                            done.error("AuthFailureError");
                        if (error instanceof ServerError)
                            // Intercepted the 302 as ServerError
                            return;
                        else
                            done.error("UnknownError");
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Host", "auth.tesla.com");
                params.put("User-Agent", "curl/7.71.1");
                params.put("accept", "*/*");
                params.put("Cookie", sessionCookie);
                return params;
            }
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String body = "";
                for (Map.Entry<String, String> entry : formParams.entrySet()) {
                    body += entry.getKey() + "=" + entry.getValue() + "&";
                }
                try {
                    body += "identity=" + URLEncoder.encode(username, "utf-8") + "&";
                    body += "credential=" + URLEncoder.encode(pw, "utf-8");
                    //String curlCommand = "curl -v -H 'Cookie: " + sessionCookie + "' --data '" + body + "' '" + getUrl() + "'";
                    return body.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.e("newAuthToken", "Username cannot be urlencoded in utf-8");
                }
                return null;
            }

            @Override
            public void deliverError(VolleyError error) {
                if (error instanceof ServerError && error.networkResponse != null && null != error.networkResponse.headers) {
                    for (Map.Entry<String, String> entry : error.networkResponse.headers.entrySet()) {
                        if (entry.getKey().equals("Location")) {
                            Pattern p = Pattern.compile("callback\\?code=([^&]*)&state");
                            Matcher m = p.matcher(entry.getValue());
                            if (m.find()) {
                                final String authToken = m.group(1);
                                newBearerToken(rq, verifier, authToken, done);
                                return;
                            }
                        }
                    }
                }
                super.deliverError(error);
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse networkResponse) {
                // We shouldn't get here
                String allHeaders = "";
                for (Header h: networkResponse.allHeaders) {
                    allHeaders += h.getName() + ": " + h.getValue() + "\n";
                }
                String body;
                try {
                    body = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                } catch (UnsupportedEncodingException e) {
                    body = new String(networkResponse.data);
                }
                com.android.volley.Response<String> result = com.android.volley.Response.success(allHeaders + body,
                        HttpHeaderParser.parseCacheHeaders(networkResponse));
                return result;
            }
        };
        rq.add(r);
    }

    static void newBearerToken(final RequestQueue rq, final String challengeVerifier, final String authToken, final JsonAPIResponseInterface done) {
        JSONObject body = new JSONObject();

        try {
            body.put("grant_type", "authorization_code");
            body.put("client_id", "ownerapi");
            body.put("code", authToken);
            body.put("code_verifier", challengeVerifier);
            body.put("redirect_uri", TESLA_REDIRECT_URI);
        } catch (JSONException e) {
            done.error("JSONException in newBearerToken");
        }
        JsonObjectRequest r = new JsonObjectRequest(
            Request.Method.POST,
            TESLA_AUTH_URL + "/token",
            body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    String bearerToken = null;
                    try {
                        bearerToken = response.getString("access_token");
                    } catch (JSONException e) {
                        done.error("JSONException in newBearerToken.onResponse");
                        return;
                    }
                    newAccessToken(rq, bearerToken, done);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    done.error(error.getMessage());
                }
            }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Host", "auth.tesla.com");
                params.put("User-Agent", "curl/7.71.1");
                params.put("Content-Type", "application/json");
                params.put("accept", "application/json");
                return params;
            }
        };
        rq.add(r);
    }

    static void newAccessToken(final RequestQueue rq, final String bearerToken, final JsonAPIResponseInterface done) {
        JSONObject body = new JSONObject();

        try {
            body.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            body.put("client_id", TESLA_CLIENT_ID);
            body.put("client_secret", TESLA_CLIENT_SECRET);
        } catch (JSONException e) {
            done.error("JSONException in newAccessToken");
        }
        JsonObjectRequest r = new JsonObjectRequest(
                Request.Method.POST,
                TESLA_OWNERS_API_AUTH_URL,
                body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        done.done(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        done.error(error.getMessage());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Host", "owner-api.teslamotors.com");
                params.put("User-Agent", "curl/7.71.1");
                params.put("Content-Type", "application/json");
                params.put("accept", "application/json");
                params.put("Authorization", "Bearer " + bearerToken);
                return params;
            }

            @Override
            public void deliverError(VolleyError error) {
                if (error.networkResponse != null)
                    if (error.networkResponse.data != null) {
                        String dataError = new String(error.networkResponse.data);
                        done.error(dataError);
                    }
                super.deliverError(error);
            }
        };
        rq.add(r);
    }
}
