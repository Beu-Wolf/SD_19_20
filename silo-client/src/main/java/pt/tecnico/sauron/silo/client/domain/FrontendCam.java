package pt.tecnico.sauron.silo.client.domain;

public class FrontendCam {
    private String name;
    private FrontendCoords coords;

    public FrontendCam(String name, double lat, double lon) {
        this.name = name;
        this.coords = new FrontendCoords(lat, lon);
    }

    public String getName() { return this.name; }
    public Double getLat() { return this.coords.getLat(); }
    public Double getLon() { return this.coords.getLon(); }
    public FrontendCoords getCoords() {
        return coords;
    }

    @Override
    public String toString() {
        return this.name + ',' + this.getLat() + ',' + this.coords.getLon();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FrontendCam) {
            FrontendCam c = (FrontendCam) o;
            // equals if same location and same name
            return this.name.equals(c.getName()) && this.coords.equals(c.getCoords());
        }
        return false;
    }
}
