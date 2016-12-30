package se.linefeed.korjournal.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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

    public OdometerSnap(Cursor dbCursor) {
        if (dbCursor.getColumnIndex("vehicle") > -1) {
            vehicle = dbCursor.getString(dbCursor.getColumnIndex("vehicle"));
        } else {
            vehicle = null;
        }
        if (dbCursor.getColumnIndex("odometer") > -1) {
            odometer = dbCursor.getInt(dbCursor.getColumnIndex("odometer"));
        } else {
            odometer = 0;
        }

        double poslat;
        double poslon;
        if (dbCursor.getColumnIndex("poslat") > -1) {
            poslat = dbCursor.getDouble(dbCursor.getColumnIndex("poslat"));
        } else {
            poslat = 0.0;
        }
        if (dbCursor.getColumnIndex("poslon") > -1) {
            poslon = dbCursor.getDouble(dbCursor.getColumnIndex("poslon"));
        } else {
            poslon = 0.0;
        }
        position = new Position(poslat,poslon);

        if (dbCursor.getColumnIndex("streetAddress") > -1) {
            streetAddress = dbCursor.getString(dbCursor.getColumnIndex("streetAddress"));
        } else {
            streetAddress = null;
        }
        if (dbCursor.getColumnIndex("why") > -1) {
            reason = dbCursor.getString(dbCursor.getColumnIndex("why"));
        } else {
            reason = null;
        }

        isStart = false;
        isEnd = false;
        if (dbCursor.getColumnIndex("start_end") > -1) {
            if (dbCursor.getInt(dbCursor.getColumnIndex("start_end")) == 1) {
                isStart = true;
                isEnd = false;
            }
            if (dbCursor.getInt(dbCursor.getColumnIndex("start_end")) == 2) {
                isStart = false;
                isEnd = true;
            }
        }
        if (dbCursor.getColumnIndex("occurred") > -1) {
            when = dbCursor.getString(dbCursor.getColumnIndex("occurred"));
        } else {
            when = null;
        }
    }

    /**
     *
     * @param jsonObject
     * @return self
     * @throws JSONException
     */
    public OdometerSnap loadFromJSON(JSONObject jsonObject) throws JSONException {
        vehicle = jsonObject.getString("vehicle");
        odometer = jsonObject.getInt("odometer");
        isStart = jsonObject.getString("type").equals("1");
        isEnd = jsonObject.getString("type").equals("2");
        when = jsonObject.getString("when");
        streetAddress = jsonObject.getString("where");
        double poslat = jsonObject.getDouble("poslat");
        double poslon = jsonObject.getDouble("poslon");
        position = new Position(poslat, poslon);
        reason = jsonObject.getString("why");
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

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getWhen() {
        return when;
    }

    public String getWhenLocal() throws ParseException {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date value = parser.parse(when);
        SimpleDateFormat print = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        print.setTimeZone(TimeZone.getDefault());
        return print.format(value);
    }

    public void setWhen(String when) {
        this.when = when;
    }

    public void insertDb(SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues(10);

        contentValues.put("vehicle", getVehicle());
        contentValues.put("odometer", getOdometer());
        contentValues.put("poslat", getPosition().getPoslat());
        contentValues.put("poslon", getPosition().getPoslong());
        contentValues.put("streetAddress", getStreetAddress());
        contentValues.put("why", getReason());
        contentValues.put("start_end", ( isStart ? 1 : 2 ));
        contentValues.put("occurred", getWhen());
        db.insert("OdoSnaps", null, contentValues);
    }

    public Boolean isStart() {
        return this.isStart;
    }

    public Boolean isEnd() {
        return this.isEnd;
    }
}
