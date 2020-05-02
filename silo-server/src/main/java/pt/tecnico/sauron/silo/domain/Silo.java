package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.exceptions.DuplicateCameraNameException;
import pt.tecnico.sauron.silo.exceptions.InvalidCameraCoordsException;
import pt.tecnico.sauron.silo.exceptions.NoCameraFoundException;
import pt.tecnico.sauron.silo.exceptions.ObservationNotFoundException;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Silo {
    private Deque<Report> reports = new ConcurrentLinkedDeque<Report>();
    private Map<String, Cam> cams = new ConcurrentHashMap<>();

    public Silo() {}

    public void registerCam(Cam cam) throws DuplicateCameraNameException, InvalidCameraCoordsException {
        String name = cam.getName();
        // check if cam name already exists
        if(this.cams.containsKey(name)) {
            // if it does, it must be the same camera
            if(!cam.equals(this.cams.get(name))) {
                throw new DuplicateCameraNameException();
            }

        // check coordinates
        } else if (!validCoords(cam.getCoords())) {
            throw new InvalidCameraCoordsException();

        // add camera
        } else {
            cams.put(cam.getName(), cam);
        }
    }



    // used by ctrl operations
    public void recordReport(Report report) {
        reports.addFirst(report);
    }


    public void registerObservation(Cam cam, Observation observation) {
        // let the server register the time
        Report report = new Report(cam, observation, Instant.now());
        recordReport(report);
    }

    public void registerGossipObservation(Cam cam, Observation observation, Instant instant) {
        Report report = new Report(cam, observation, instant);
        recordReport(report);
    }


    public void clearCams() {
        cams.clear();
    }

    public void clearObservations() {
        reports.clear();
    }

    public Report track(Observation observation) throws ObservationNotFoundException {
        for (Report report : reports) {
            if (report.getObservation().equals(observation))
                return report;
        }
        throw new ObservationNotFoundException();
    }

    public Deque<Report> getReportsByNew() { return this.reports; }

    public Cam getCam(String name) throws NoCameraFoundException {
        Cam cam = cams.get(name);
        if (cam == null) {
            throw new NoCameraFoundException();
        }
        return cam;
    }

    private boolean validCoords(Coords coords) {
        return coords.getLat() >= -180.0 && coords.getLat() <= 180.0 && coords.getLon() >= -90.0 && coords.getLon() <= 90.0;
    }
}
