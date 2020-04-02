package pt.tecnico.sauron.silo.domain;

import java.time.Instant;

public class Report {
    private Cam cam;
    private Observation observation;
    private Instant timestamp;

    public Report(Cam cam, Observation observation, Instant timestamp) {
        this.cam = cam;
        this.observation = observation;
        this.timestamp = timestamp;
    }

    public Cam getCam() {
        return cam;
    }

    public Observation getObservation() {
        return observation;
    }

    public Instant getTimestamp() { return timestamp; }
}
