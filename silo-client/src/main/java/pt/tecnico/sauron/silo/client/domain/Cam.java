package pt.tecnico.sauron.silo.client.domain;

public class Cam {
    private String name;
    private Coords coords;

    public Cam(String name, double lat, double lon) {
        this.name = name;
        this.coords = new Coords(lat, lon);
    }

    public String getName() { return this.name; }
    public Double getLat() { return this.coords.getLat(); }
    public Double getLon() { return this.coords.getLon(); }
    public Coords getCoords() { return coords; }

    @Override
    public String toString() {
        return this.name + ',' + this.getLat() + ',' + this.coords.getLon();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Cam) {
            Cam c = (Cam) o;
            return this.name.equals(c.getName()) && this.coords.equals(c.getCoords());
        }
        return false;
    }
}
