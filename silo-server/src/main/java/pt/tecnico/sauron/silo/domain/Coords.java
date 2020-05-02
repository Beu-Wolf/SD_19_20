package pt.tecnico.sauron.silo.domain;

public class Coords {
    private double lat;
    private double lon;

    public Coords(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() { return lat; }
    public double getLon() {
        return lon;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Coords) {
            Coords c = (Coords)o;
            // equals if latitude and longitude equals
            return this.lat == c.getLat() && this.lon == c.getLon();
        }
        return false;
    }
}
