package pt.tecnico.sauron.silo.client.dto;

public class ObservationDto {
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

}
