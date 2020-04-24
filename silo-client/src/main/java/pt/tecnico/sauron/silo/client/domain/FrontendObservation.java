package pt.tecnico.sauron.silo.client.domain;

public class FrontendObservation implements Comparable<FrontendObservation>{
    public enum ObservationType { UNSPEC, CAR, PERSON }

    private ObservationType type;
    private String id;

    public FrontendObservation(ObservationType type, String id) {
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
    public int compareTo(FrontendObservation obs) {
        return this.id.compareTo(obs.getId());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FrontendObservation && ((FrontendObservation) o).getId().equals(this.id);
    }

}
