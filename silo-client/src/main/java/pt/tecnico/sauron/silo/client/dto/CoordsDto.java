package pt.tecnico.sauron.silo.client.dto;

public class CoordsDto {
    double lat;
    double lon;

    public CoordsDto(double lat, double lon) {
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
        if (o instanceof  CoordsDto) {
            CoordsDto c = (CoordsDto) o;
            return getLat() == c.getLat() && getLon() == c.getLon();
        }
        return false;
    }
}
