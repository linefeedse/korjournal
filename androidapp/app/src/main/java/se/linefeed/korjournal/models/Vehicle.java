package se.linefeed.korjournal.models;

public class Vehicle {
    private String name;
    private String url;

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public Vehicle(String name, String url) {
        this.name = name;
        this.url = url;
    }
}
