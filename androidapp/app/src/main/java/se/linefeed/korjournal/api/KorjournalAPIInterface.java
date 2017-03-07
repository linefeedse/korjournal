package se.linefeed.korjournal.api;


import org.json.JSONObject;

public interface KorjournalAPIInterface {
    void done(JSONObject response);
    void error(String error);
}
