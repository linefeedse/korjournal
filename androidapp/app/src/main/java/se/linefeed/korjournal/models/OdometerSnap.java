package se.linefeed.korjournal.models;

import org.json.JSONException;
import org.json.JSONObject;

public class OdometerSnap {

    private String vehicle;
    private int odometer;
    private Position position;
    private String streetAddress;
    private String reason;
    private boolean isStart;
    private boolean isEnd;
    private String when;

    /**
     * Default Constructor
     */
    public OdometerSnap() {
        vehicle = null;
        odometer = 0;
        position = null;
        streetAddress = null;
        reason = null;
        isEnd = false;
        isStart = false;
        when = null;
    }

    /**
     *
     * @param vehicle
     * @param odometer
     * @param position
     * @param streetAddress
     * @param reason
     * @param isStart
     * @param isEnd
     */
    public OdometerSnap(String vehicle, int odometer, Position position, String streetAddress, String reason, boolean isStart, boolean isEnd) {
        this.vehicle = vehicle;
        this.odometer = odometer;
        this.position = position;
        this.streetAddress = streetAddress;
        this.reason = reason;
        this.isStart = isStart;
        this.isEnd = isEnd;
    }

    /**
     *
     * @param jsonObject
     * @return self
     * @throws JSONException
     */
    public OdometerSnap loadFromJSON(JSONObject jsonObject) throws JSONException {
       /* vehicle = jsonObject.getString("name");
        odometer = jsonObject.getInt("odometer"); */
        double poslat = jsonObject.getLong("poslat");
        double poslon = jsonObject.getLong("poslon");
        position = new Position(poslat, poslon);
//        streetAddress = jsonObject.getString("where");
        reason = jsonObject.getString("why");
  //      when = jsonObject.getString("when");
    //    isStart = jsonObject.getString("type").equals("1");
      //  isEnd = jsonObject.getString("type").equals("2");
        return this;
    }

    public String getVehicle() {
        return vehicle;
    }

    public OdometerSnap setVehicle(String vehicle) {
        this.vehicle = vehicle;
        return this;
    }
    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getOdometer() {
        return odometer;
    }
}
