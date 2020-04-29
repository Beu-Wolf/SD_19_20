package pt.tecnico.sauron.silo;

import pt.sauron.silo.contract.domain.Cam;
import pt.sauron.silo.contract.domain.Coords;
import pt.sauron.silo.contract.domain.Observation;
import pt.sauron.silo.contract.domain.Report;
import pt.tecnico.sauron.silo.exceptions.DuplicateCameraNameServerException;
import pt.sauron.silo.contract.domain.exceptions.InvalidCameraCoordsException;
import pt.tecnico.sauron.silo.exceptions.NoCameraFoundServerException;
import pt.tecnico.sauron.silo.exceptions.ObservationNotFoundServerException;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Silo {
    private Deque<Report> reports = new ConcurrentLinkedDeque<Report>();
    private Map<String, Cam> cams = new ConcurrentHashMap<>();

    public Silo() {}

    public void registerCam(Cam cam) throws DuplicateCameraNameServerException, InvalidCameraCoordsException {
        String name = cam.getName();
        if(this.cams.containsKey(name)) {
            if(!cam.equals(this.cams.get(name))) {
                throw new DuplicateCameraNameServerException();
            }
        } else if (!validCoords(cam.getCoords())) {
            throw new InvalidCameraCoordsException();
        } else {
            cams.put(cam.getName(), cam);
        }
    }



    public void recordReport(Report report) {
        reports.addFirst(report);
    }


    public void registerObservation(Cam cam, Observation observation) {
        // let the server register the time
        Report report = new Report(cam, observation, Instant.now());
        recordReport(report);
    }


    public void clearCams() {
        cams.clear();
    }

    public void clearObservations() {
        reports.clear();
    }

    public Report track(Observation observation) throws ObservationNotFoundServerException {
        for (Report report : reports) {
            if (report.getObservation().equals(observation))
                return report;
        }
        throw new ObservationNotFoundServerException();
    }

    public Deque<Report> getReportsByNew() { return this.reports; }

    public Cam getCam(String name) throws NoCameraFoundServerException {
        Cam cam = cams.get(name);
        if (cam == null) {
            throw new NoCameraFoundServerException();
        }
        return cam;
    }

    private boolean validCoords(Coords coords) {
        return coords.getLat() >= -180.0 && coords.getLat() <= 180.0 && coords.getLon() >= -90.0 && coords.getLon() <= 90.0;
    }
}