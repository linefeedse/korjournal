package se.linefeed.korjournal.models;

/**
 * Created by torkel on 2016-12-29.
 */

public class TripListModel {

    private String when;
    private String toAddress;
    private String kmString;
    private String fromAddress;
    private String reason;

    public TripListModel(String when, String toAddress, String kmString, String fromAddress, String reason) {
        this.when = when;
        this.toAddress = toAddress;
        this.kmString = kmString;
        this.fromAddress = fromAddress;
        this.reason = reason;
    }

    public TripListModel(TripListModel copyThis) {
        this.when = copyThis.getWhen();
        this.toAddress = copyThis.getToAddress();
        this.kmString = copyThis.getKmString();
        this.fromAddress = copyThis.getFromAddress();
        this.reason = copyThis.getReason();
    }

    public String getWhen() {
        return when;
    }

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
        return kmString;
    }

    public void setKmString(String kmString) {
        this.kmString = kmString;
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
