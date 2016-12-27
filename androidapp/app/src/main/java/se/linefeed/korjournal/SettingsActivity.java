package se.linefeed.korjournal;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.linefeed.korjournal.api.KorjournalAPI;
import se.linefeed.korjournal.api.KorjournalAPIInterface;
import se.linefeed.korjournal.models.Vehicle;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || UserPreferenceFragment.class.getName().equals(fragmentName)
                || VehiclePreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class UserPreferenceFragment extends PreferenceFragment {
        protected Preference usernamePreference;
        protected Preference generateCodeButton;
        protected Preference codeText;
        protected RequestQueue mRequestQueue = null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_username);
            setHasOptionsMenu(true);

            usernamePreference = findPreference("username_text");
            codeText = findPreference("code_text");
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(usernamePreference);
            bindPreferenceSummaryToValue(codeText);

            generateCodeButton = (Preference)findPreference("generate_code");
            generateCodeButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    generateCodeButton.setSummary("Personlig kod begärd");
                    generateCodeButton.setSelectable(false);
                    codeText.setSummary("Kod från SMS");
                    askForCode();
                    return true;
                }
            });
            codeText.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    codeText.setSummary((String) newValue);
                    tryRegisterCode((String) newValue);
                    return true;
                }
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        private void askForCode() {
            final String REG_URL = getString(R.string.url) + "/register/";
            final String phone = usernamePreference.getSummary().toString();

            if (mRequestQueue == null) {
                mRequestQueue = Volley.newRequestQueue(getActivity());
            }
            StringRequest stringRequest = new StringRequest(Request.Method.POST, REG_URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("Info", "Server responded ok: " + response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Error", "Server responded " + error.networkResponse.statusCode);
                }
            }) {
                @Override
                protected Map<String,String> getParams(){
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("phone",phone);
                    return params;
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("Content-Type","application/x-www-form-urlencoded");
                    return params;
                }
            };
            mRequestQueue.add(stringRequest);
        }
        private void tryRegisterCode(String newCode) {
            final String VER_URL = getString(R.string.url) + "/verify/";
            final String CHK_URL = getString(R.string.url) + "/api/vehicle/";
            final String phone = PreferenceManager
                    .getDefaultSharedPreferences(usernamePreference.getContext())
                    .getString(usernamePreference.getKey(),"");
            final String code = newCode;

            if (mRequestQueue == null) {
                mRequestQueue = Volley.newRequestQueue(getActivity());
            }

            // Before trying to verify, check if code is already ok
            StringRequest checkRequest = new StringRequest(Request.Method.GET, CHK_URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("Info", "Server responded ok: " + response);
                    codeText.setSummary("OK!");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
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
                            codeText.setSummary("Ogiltig kod!");
                        }
                    }) {
                        @Override
                        protected Map<String,String> getParams(){
                            Map<String,String> params = new HashMap<String, String>();
                            params.put("phone",phone);
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
                    String creds = String.format("%s:%s",phone,code);
                    String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                    headers.put("Authorization", auth);
                    return headers;
                }
            };
            mRequestQueue.add(checkRequest);
        }

    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class VehiclePreferenceFragment extends PreferenceFragment {
        protected Preference newVehicleName;
        protected KorjournalAPI mApi = null;
        protected ListPreference vehicleListPreference;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_vehicles);
            setHasOptionsMenu(true);

            vehicleListPreference = (ListPreference) findPreference("vehicle_list");
            bindPreferenceSummaryToValue(vehicleListPreference);
            updateVehicleList();
            if (mApi == null) {
                mApi = new KorjournalAPI(getActivity());
            }

            newVehicleName = (Preference)findPreference("new_vehicle_name");
            newVehicleName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, Object newValue) {
                    mApi.create_vehicle((String) newValue,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    preference.setSummary("Nytt fordon skapades");
                                    updateVehicleList();
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    preference.setSummary("Kunde inte skapa fordon");
                                }
                            }
                    );
                    return true;
                }
            });

        }

        @Override
        public void onStart() {
            updateVehicleList();
            super.onStart();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }

            return super.onOptionsItemSelected(item);
        }

        protected void updateVehicleList() {
            final ArrayList<String> vehiclenames = new ArrayList<String>();
            final ArrayList<String> vehicleids = new ArrayList<String>();
            final HashMap<String, Vehicle> vehicles = new HashMap<String, Vehicle>();
            if (mApi == null) {
                mApi = new KorjournalAPI(getActivity());
            }
            mApi.get_vehicles(vehicles,
                    new KorjournalAPIInterface() {
                        @Override
                        public void done() {
                            for (String vName: vehicles.keySet()) {
                                vehiclenames.add(vName);
                                vehicleids.add(vehicles.get(vName).getUrl());
                            }
                            vehicleListPreference.setEntries(vehiclenames.toArray(new CharSequence[vehiclenames.size()]));
                            vehicleListPreference.setEntryValues(vehicleids.toArray(new CharSequence[vehicleids.size()]));
                        }
                        @Override
                        public void error(String e) {
                            CharSequence[] err = { "Inga fordon" };
                            CharSequence[] errVal = { "0" };
                            vehicleListPreference.setEntries(err);
                            vehicleListPreference.setEntryValues(errVal);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            CharSequence[] err = { "Kunde inte hämta fordon" };
                            CharSequence[] errVal = { "0" };
                            vehicleListPreference.setEntries(err);
                            vehicleListPreference.setEntryValues(errVal);
                        }
                    }
            );
        }
    }
}
