package pt.tecnico.sauron.silo.client.domain;

import pt.sauron.silo.contract.domain.Cam;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
public class Report implements Comparable<Report> {
    private Cam cam;
    private Observation observation;
    private Instant timestamp;

    public Report(Observation observation, Cam cam, Instant timestamp) {
        this.observation = observation;
        this.cam = cam;
        this.timestamp = timestamp;
    }


    public Observation getObservation() { return this.observation; }

    public Cam getCam() {return this.cam; }
    public Instant getTimestamp() { return this.timestamp; }

    @Override
    public String toString() {
        return this.observation.getType().toString().toLowerCase() + ','
                + this.observation.getId() + ','
                + LocalDateTime.ofInstant(this.timestamp, ZoneOffset.UTC)  + ','
                + this.cam.toString();
    }

    @Override
    public int compareTo(Report r) {
        return this.observation.compareTo(r.getObservation());
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Report) {
            Report r = (Report) o;
            return this.observation.equals(r.getObservation()) && this.cam.equals(r.getCam()) && this.timestamp.equals(r.getTimestamp());
        }
        return false;
    }

    public String getId() { return this.observation.getId(); }
}
