package se.linefeed.korjournal.models;

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
}
