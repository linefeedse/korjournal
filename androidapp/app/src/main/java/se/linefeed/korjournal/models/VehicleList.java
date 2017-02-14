package se.linefeed.korjournal.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.HashMap;

import se.linefeed.korjournal.api.KorjournalAPI;
import se.linefeed.korjournal.api.KorjournalAPIInterface;

public class VehicleList {
    private ArrayList<String> vehicleSelectorArrayList;
    private ArrayAdapter<String> vehicleSpinnerAdapter;
    private String selectedName = null;
    private Spinner spinner;
    private HashMap<String, Vehicle> vehicles;

    public VehicleList(Context context, int vehicle_spinner_item, Spinner vehicleSpinner) {

        spinner = vehicleSpinner;
        vehicleSelectorArrayList = new ArrayList<String>();
        vehicleSpinnerAdapter = new ArrayAdapter<String>(context,
                vehicle_spinner_item,
                vehicleSelectorArrayList);
        vehicleSpinner.setAdapter(vehicleSpinnerAdapter);
        vehicles = new HashMap<String, Vehicle>();
    }

    public void saveSelected() {
        if (spinner.getSelectedItem() == null) {
            selectedName = null;
            return;
        }
        selectedName = spinner.getSelectedItem().toString();
    }

    /**
     * Set the selected vehicle according to logic:
     * If we have a previous selection and pauses/rotates device, use that
     * If we have no previous selection, and there is config for preselect, use that
     * Else, we do nothing and it will just be the first item whatever that is.
     */
    public void resetSelected(SharedPreferences sharedPreferences) {
        if (selectedName != null) {
            boolean vehicleSelectedIsValid = false;
            for (int i=0;i<vehicleSelectorArrayList.size();i++) {
                if (selectedName.equals(vehicleSelectorArrayList.get(i))) {
                    spinner.setSelection(i);
                    vehicleSelectedIsValid = true;
                    break;
                }
            }
            if (!vehicleSelectedIsValid && vehicleSelectorArrayList.size() > 1) {
                selectedName = null;
            }
        } else {
            String prefVehicleUrl = sharedPreferences.getString("vehicle_list","");
            if (!prefVehicleUrl.equals("")) {
                for (String vName : vehicles.keySet()) {
                    if (vehicles.get(vName).getUrl().equals(prefVehicleUrl)) {
                        for (int i=0;i<vehicleSelectorArrayList.size();i++) {
                            if (vName.equals(vehicleSelectorArrayList.get(i))) {
                                spinner.setSelection(i);
                                selectedName = vName;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void request(KorjournalAPI mApi, final TextView statusText, final SharedPreferences sharedPreferences) {
        final HashMap<String, Vehicle> myVehicles = new HashMap<String, Vehicle>();
        statusText.setText("Hämtar fordon...");
        mApi.get_vehicles(myVehicles,
                new KorjournalAPIInterface() {
                    @Override
                    public void done() {
                        if (myVehicles.size() > 0) {
                            vehicleSelectorArrayList.clear();
                        }
                        for (String vName: myVehicles.keySet()) {
                            vehicleSelectorArrayList.add(vName);
                        }
                        vehicles = myVehicles;
                        vehicleSpinnerAdapter.notifyDataSetChanged();
                        resetSelected(sharedPreferences);
                        statusText.setText("Klar!");
                    }
                    @Override
                    public void error(String e) {
                        statusText.setText(e);
                        if (vehicleSelectorArrayList.size() == 0) {
                            vehicleSelectorArrayList.add("Fel: inga fordon!");
                            vehicleSpinnerAdapter.notifyDataSetChanged();
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    statusText.setText(error.getMessage());
                    if (vehicleSelectorArrayList.size() == 0) {
                        vehicleSelectorArrayList.add("Fel: inga fordon!");
                        vehicleSpinnerAdapter.notifyDataSetChanged();
                    }
                    }
                }
        );
        vehicleSpinnerAdapter.notifyDataSetChanged();
    }

    public Vehicle getSelectedVehicle() {
        if (spinner.getSelectedItem() == null) {
            return null;
        }
        return vehicles.get(spinner.getSelectedItem().toString());
    }

    public void flashSpinner(final int flash_color) {
        final int transparent = 0x00000000;
        spinner.setBackgroundColor(flash_color);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                spinner.setBackgroundColor(transparent);
            }
        }, 300);
    }
}