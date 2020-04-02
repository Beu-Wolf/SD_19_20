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

    public ObservationDto getObservation() { return this.observation; }
    public CamDto getCam() { return this.cam; }
    public Instant getTimestamp() { return this.timestamp; }
}
