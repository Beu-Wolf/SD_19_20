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

    @Override
    public boolean equals(Object o) {
        if(o instanceof Cam) {
            Cam c = (Cam)o;
            return c.getName() == this.name && this.coords.equals(c.getCoords());
        }
        return false;
    }


}
