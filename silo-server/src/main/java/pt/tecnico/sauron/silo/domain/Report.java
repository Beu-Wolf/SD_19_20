package pt.tecnico.sauron.silo.domain;

import java.time.LocalDateTime;

public class Report {
    private Cam cam;
    private Observation observation;
    private LocalDateTime timestamp;

    public Report(Cam cam, Observation observation, LocalDateTime timestamp) {
        this.cam = cam;
        this.observation = observation;
        this.timestamp = timestamp;
    }
}
