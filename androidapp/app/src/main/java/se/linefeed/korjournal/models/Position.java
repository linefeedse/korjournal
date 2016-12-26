package se.linefeed.korjournal.models;

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

}
