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
}
