package pt.tecnico.sauron.silo.client.dto;

public class CamDto {
    private String name;
    private CoordsDto coords;

    public CamDto(String name, double lat, double lon) {
        this.name = name;
        this.coords = new CoordsDto(lat, lon);
    }

    public String getName() { return this.name; }
}
