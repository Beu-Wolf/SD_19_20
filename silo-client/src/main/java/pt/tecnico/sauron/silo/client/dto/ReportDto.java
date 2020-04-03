package pt.tecnico.sauron.silo.client.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
public class ReportDto implements Comparable<ReportDto> {
    private CamDto cam;
    private ObservationDto observation;
    private Instant timestamp;

    public ReportDto(ObservationDto observation, CamDto cam, Instant timestamp) {
        this.observation = observation;
        this.cam = cam;
        this.timestamp = timestamp;
    }


    public ObservationDto getObservation() { return this.observation; }

    public CamDto getCam() {return this.cam; }
    public Instant getTimestamp() { return this.timestamp; }

    @Override
    public String toString() {
        return this.observation.getType().toString().toLowerCase() + ','
                + this.observation.getId() + ','
                + LocalDateTime.ofInstant(this.timestamp, ZoneOffset.UTC)  + ','
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

    public String getId() { return this.observation.getId(); }
}
