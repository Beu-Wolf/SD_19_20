package pt.tecnico.sauron.silo.domain;

public abstract class Observation {
    private String id;

    public Observation(String id) { this.id = id; }

    public String getId() { return this.id; }

    public abstract boolean equals(Object o);

    public abstract boolean matches(ObservationVisitor o);
}
