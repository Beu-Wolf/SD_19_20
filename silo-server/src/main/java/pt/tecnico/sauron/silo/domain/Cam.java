package pt.tecnico.sauron.silo.domain;

public class Cam {
    private String name;
    private Coords coords;

    public Cam(String name, Coords coords) {
        this.name = name;
        this.coords = coords;
    }

    public String getName() {
        return name;
    }

    public Coords getCoords() {
        return coords;
    }

    public Double getLat() { return this.coords.getLat(); }
    public Double getLon() { return this.coords.getLon(); }
}
