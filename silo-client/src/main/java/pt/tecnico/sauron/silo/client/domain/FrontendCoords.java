package pt.tecnico.sauron.silo.client.domain;

public class FrontendCoords {
    double lat;
    double lon;

    public FrontendCoords(double lat, double lon) {
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
        if (o instanceof FrontendCoords) {
            FrontendCoords c = (FrontendCoords) o;
            // equals if latitude and longitude equals
            return getLat() == c.getLat() && getLon() == c.getLon();
        }
        return false;
    }
}
