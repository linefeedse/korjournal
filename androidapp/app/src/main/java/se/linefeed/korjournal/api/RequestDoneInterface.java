package se.linefeed.korjournal.api;

public interface RequestDoneInterface {
    void done(int results);
    void error(String error);
}
