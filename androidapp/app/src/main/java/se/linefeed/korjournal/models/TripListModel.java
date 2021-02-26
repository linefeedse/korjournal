package se.linefeed.korjournal.models;

/**
 * Created by torkel on 2016-12-29.
 */

public class TripListModel {

    private String when;
    private String toAddress;
    private String startKmString;
    private String endKmString;
    private String fromAddress;
    private String reason;

    public TripListModel(String when, String toAddress, String startKmString, String endKmString, String fromAddress, String reason) {
        this.when = when;
        this.toAddress = toAddress;
        this.startKmString = startKmString;
        this.endKmString= endKmString;
        this.fromAddress = fromAddress;
        this.reason = reason;
    }

    public TripListModel(TripListModel copyThis) {
        this.when = copyThis.getWhen();
        this.toAddress = copyThis.getToAddress();
        this.startKmString = copyThis.getStartKmString();
        this.endKmString = copyThis.getEndKmString();
        this.fromAddress = copyThis.getFromAddress();
        this.reason = copyThis.getReason();
    }

    public String getWhen() {
        return when;
    }

    public String getWhenDay() { return when.substring(0,10); }

    public void setWhen(String when) {
        this.when = when;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getKmString() {
        return startKmString + " - " + endKmString;
    }

    public String getStartKmString() {
        return startKmString;
    }
    public String getEndKmString() {
        return endKmString;
    }

    public void setStartKmString(String kmString) {
        this.startKmString = kmString;
    }

    public void setEndKmString(String kmString) {
        this.endKmString = kmString;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
