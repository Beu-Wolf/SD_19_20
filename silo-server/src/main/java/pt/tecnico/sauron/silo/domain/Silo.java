package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.domain.exceptions.ObservationNotFoundException;
import pt.tecnico.sauron.silo.domain.exceptions.DuplicateCameraNameException;
import pt.tecnico.sauron.silo.domain.exceptions.NoCameraFoundException;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Silo {
    private LinkedList<Report> reports = new LinkedList<>();
    private Map<String, Cam> cams = new ConcurrentHashMap<>();

    public Silo() {}

    public synchronized void registerCam(Cam cam) throws DuplicateCameraNameException {
        String name = cam.getName();
        if(this.cams.containsKey(name)) {
            if(!cam.equals(this.cams.get(name))) {
                throw new DuplicateCameraNameException();
            }
        } else {
            cams.put(cam.getName(), cam);
        }
    }



    public synchronized void recordReport(Report report) {
        reports.addFirst(report);
    }


    public synchronized  void registerObservation(Cam cam, Observation observation) {
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

    public Report track(Observation observation) throws ObservationNotFoundException {
        for (Report report : reports) {
            if (report.getObservation().equals(observation))
                return report;
        }
        throw new ObservationNotFoundException();
    }

    public List<Report> getReportsByNew() { return this.reports; }

    public Cam getCam(String name) throws NoCameraFoundException {
        Cam cam = cams.get(name);
        if (cam == null) {
            throw new NoCameraFoundException();
        }
        return cam;
    }
}
