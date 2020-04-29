package pt.tecnico.sauron.silo.client.domain;

public class Coords {
    double lat;
    double lon;

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

    @Override
    public boolean equals(Object o) {
        if (o instanceof Coords) {
            Coords c = (Coords) o;
            return getLat() == c.getLat() && getLon() == c.getLon();
        }
        return false;
    }
}
