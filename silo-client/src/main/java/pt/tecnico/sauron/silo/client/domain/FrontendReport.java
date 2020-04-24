package pt.tecnico.sauron.silo.client.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
public class FrontendReport implements Comparable<FrontendReport> {
    private FrontendCam cam;
    private FrontendObservation observation;
    private Instant timestamp;

    public FrontendReport(FrontendObservation observation, FrontendCam cam, Instant timestamp) {
        this.observation = observation;
        this.cam = cam;
        this.timestamp = timestamp;
    }


    public FrontendObservation getObservation() { return this.observation; }

    public FrontendCam getCam() {return this.cam; }
    public Instant getTimestamp() { return this.timestamp; }

    @Override
    public String toString() {
        return this.observation.getType().toString().toLowerCase() + ','
                + this.observation.getId() + ','
                + LocalDateTime.ofInstant(this.timestamp, ZoneOffset.UTC)  + ','
                + this.cam.toString();
    }

    @Override
    public int compareTo(FrontendReport r) {
        return this.observation.compareTo(r.getObservation());
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof FrontendReport) {
            FrontendReport r = (FrontendReport) o;
            return this.observation.equals(r.getObservation()) && this.cam.equals(r.getCam()) && this.timestamp.equals(r.getTimestamp());
        }
        return false;
    }

    public String getId() { return this.observation.getId(); }
}
