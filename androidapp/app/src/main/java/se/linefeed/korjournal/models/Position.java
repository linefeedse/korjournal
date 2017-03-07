package se.linefeed.korjournal.models;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class Position {

    private double poslong;
    private double poslat;

    /**
     *
     * @param poslat
     * @param poslong
     */
    public Position(double poslat, double poslong) {
        this.poslat = poslat;
        this.poslong = poslong;
    }

    /**
     *
     * @param location
     */
    public Position(Location location) {
        this.poslat = location.getLatitude();
        this.poslong = location.getLongitude();
    }

    public double getPoslong() {
        return poslong;
    }

    public void setPoslong(double poslong) {
        this.poslong = poslong;
    }

    public double getPoslat() {
        return poslat;
    }

    public void setPoslat(double poslat) {
        this.poslat = poslat;
    }

    /**
     * @param other Position
     * @return distanceKm double
     */
    public double distanceFrom(Position other) {
        double distanceLat = 111.2 * (other.getPoslat() - this.getPoslat());
        double distanceLon = 57.1 * (other.getPoslong() - this.getPoslong());
        return sqrt(pow(distanceLon,2)+pow(distanceLat,2));
    }

    public String getStreetAddress(Context context) {
        List<Address> addresses = null;
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(
                    this.getPoslat(),
                    this.getPoslong(),
                    1);
        } catch (IOException ioException) {
            return null;
        }
        if (addresses == null || addresses.size()  == 0) {
            return "";
        }
        else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            return TextUtils.join(",", addressFragments);
        }
    }
}
