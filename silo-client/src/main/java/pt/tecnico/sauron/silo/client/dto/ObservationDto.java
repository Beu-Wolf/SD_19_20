package pt.tecnico.sauron.silo.client.dto;

import java.time.LocalDateTime;

public class ObservationDto {
    public enum ObservationType { UNSPEC, CAR, PERSON }

    private ObservationType type;
    private String id;

    public ObservationDto(ObservationType type, String id) {
        this.type = type;
        this.id = id;
    }
}
