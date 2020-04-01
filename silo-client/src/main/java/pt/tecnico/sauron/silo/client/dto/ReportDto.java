package pt.tecnico.sauron.silo.client.dto;

import java.time.Instant;

public class ReportDto {
    private CamDto cam;
    private ObservationDto observation;
    private Instant timestamp;

    public ReportDto(ObservationDto observation, CamDto cam, Instant timestamp) {
        this.observation = observation;
        this.cam = cam;
        this.timestamp = timestamp;
    }
}
