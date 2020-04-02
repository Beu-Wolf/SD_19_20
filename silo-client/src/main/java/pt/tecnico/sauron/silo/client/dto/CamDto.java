package pt.tecnico.sauron.silo.client.dto;

public class CamDto {
    private String name;
    private CoordsDto coords;

    public CamDto(String name, double lat, double lon) {
        this.name = name;
        this.coords = new CoordsDto(lat, lon);
    }

    public String getName() { return this.name; }
    public Double getLat() { return this.coords.getLat(); }
    public Double getLon() { return this.coords.getLon(); }

    @Override
    public String toString() {
        return this.name + ',' + this.getLat() + ',' + this.coords.getLon();
    }

}
