package pt.tecnico.sauron.silo.client.domain;

public class Observation implements Comparable<Observation>{
    public enum ObservationType { UNSPEC, CAR, PERSON }

    private ObservationType type;
    private String id;

    public Observation(ObservationType type, String id) {
        this.type = type;
        this.id = id;
    }

    public ObservationType getType() { return this.type; }
    public String getId() { return this.id; }

    @Override
    public String toString() {
        return this.type.toString() + " with id " + this.id + ";";
    }

    @Override
    public int compareTo(Observation obs) {
        return this.id.compareTo(obs.getId());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Observation && ((Observation) o).getId().equals(this.id);
    }

}
