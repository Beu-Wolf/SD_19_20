package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.exceptions.EmptyCameraNameException;
import pt.tecnico.sauron.silo.exceptions.InvalidCameraNameException;

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
            // equals if same name and same location
            return c.getName().equals(this.name) && this.coords.equals(c.getCoords());
        }
        return false;
    }


}
