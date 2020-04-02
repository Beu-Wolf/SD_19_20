package pt.tecnico.sauron.silo.client.dto;

import java.time.Instant;

public class ReportDto implements Comparable<ReportDto> {
    private CamDto cam;
    private ObservationDto observation;
    private Instant timestamp;

    public ReportDto(ObservationDto observation, CamDto cam, Instant timestamp) {
        this.observation = observation;
        this.cam = cam;
        this.timestamp = timestamp;
    }

    public ObservationDto getObservation() {
        return observation;
    }

    public CamDto getCam() {
        return cam;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return this.observation.getType().toString() + ','
                + this.observation.getId() + ','
                + this.timestamp.toString() + ','
                + this.cam.toString();
    }

    @Override
    public int compareTo(ReportDto r) {
        return this.observation.compareTo(r.getObservation());
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof ReportDto) {
            ReportDto r = (ReportDto) o;
            return this.observation.equals(r.getObservation()) && this.cam.equals(r.getCam()) && this.timestamp.equals(r.getTimestamp());
        }
        return false;
    }
}
