package pt.tecnico.sauron.silo.domain;

public class Coords {
    private double lat;
    private double lon;

    public Coords(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
