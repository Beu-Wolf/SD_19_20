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
    public String getId() { return this.observation.getId(); }
    public String getCamName() { return this.cam.getName(); }
    public Double getLat() { return this.cam.getLat(); }
    public Double getLon() { return this.cam.getLon(); }
    public long getEpochSeconds() { return this.timestamp.getEpochSecond(); }
}
