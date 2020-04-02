package pt.tecnico.sauron.silo.client.dto;

public class ObservationDto implements Comparable{
    public enum ObservationType { UNSPEC, CAR, PERSON }

    private ObservationType type;
    private String id;

    public ObservationDto(ObservationType type, String id) {
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
    public int compareTo(Object obj) {
        ObservationDto obs = (ObservationDto) obj;
        return this.id.compareTo(obs.getId());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ObservationDto && ((ObservationDto) o).getId().equals(this.id);
    }

}
