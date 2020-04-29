package pt.sauron.silo.contract.domain;

import pt.sauron.silo.contract.domain.exceptions.*;

public class Cam {
    private String name;
    private Coords coords;

    public Cam(String name, Coords coords) throws EmptyCameraNameException, InvalidCameraNameException {
        if(name.isEmpty()) {
            throw new EmptyCameraNameException();
        }
        if(name.length() < 3 || name.length() > 15) {
            throw new InvalidCameraNameException();
        }
        this.name = name;
        this.coords = coords;
    }

    public String getName() { return name; }
    public Double getLat() { return this.coords.getLat(); }
    public Double getLon() { return this.coords.getLon(); }
    public Coords getCoords() { return coords; }

    @Override
    public String toString() {
        return this.name + ',' + this.getLat() + ',' + this.coords.getLon();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Cam) {
            Cam c = (Cam)o;
            return c.getName().equals(this.name) && this.coords.equals(c.getCoords());
        }
        return false;
    }


}