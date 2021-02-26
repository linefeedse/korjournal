package se.linefeed.korjournal.models;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import se.linefeed.korjournal.api.JsonAPIResponseInterface;
import se.linefeed.korjournal.api.TeslaAPI;

public class TeslaVehicle {
    private String name;
    private String id;
    private double state_odometer;

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public TeslaVehicle(String name, String id) {
        this.name = name;
        this.id = id;
        this.state_odometer = 0.0;
    }

    public long getOdometerKm() { return Math.round(Math.ceil(state_odometer * 1.609)); }

    public void loadAnyFromAPI(final TeslaAPI api, final JsonAPIResponseInterface alldone) {
        api.get_vehicles(
                new JsonAPIResponseInterface() {
                    @Override
                    public void done(JSONObject response) {
                        // Load vehicle, then vehicle data
                        try {
                            JSONArray all = response.getJSONArray("response");
                            loadStateFromIdInJson(api, all.getJSONObject(0), alldone);
                        } catch (JSONException e) {
                            alldone.error("JSONfel");
                        }
                    }
                    @Override
                    public void error(String e) {
                        alldone.error(e);
                    }
                }
        );
    }

    public void loadStateFromIdInJson(TeslaAPI api, JSONObject v, final JsonAPIResponseInterface statedone) {
        try {
            String id = v.getString("id");
            api.get_vehicle_data(id, new JsonAPIResponseInterface() {
                @Override
                public void done(JSONObject response) {
                    try {
                        JSONObject vehicle_state = response.getJSONObject("vehicle_state");
                        state_odometer = vehicle_state.getDouble("odometer");
                        statedone.done(new JSONObject().put("odometer",state_odometer));
                    } catch (JSONException e) {
                        if (!e.getMessage().matches(".*vehicle_state.*")) {
                            statedone.error("JSON-fel! 003");
                        }
                    }
                }

                @Override
                public void error(String error) {
                    statedone.error(error);
                }
            });
        } catch (JSONException e) {
            statedone.error("JSONfel");
        }
    }
}
