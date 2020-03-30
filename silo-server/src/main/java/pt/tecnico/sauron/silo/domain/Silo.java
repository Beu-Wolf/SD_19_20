package pt.tecnico.sauron.silo.domain;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Silo {
    private LinkedList<Report> reports = new LinkedList<>();
    private Map<String, Cam> cams = new ConcurrentHashMap<>();

    public Silo() {}

    public void registerCam(Cam cam) {
        cams.put(cam.getName(), cam);
    }

    public synchronized void registerObservation(Report report) {
        reports.addFirst(report);
    }

    public void clearCams() {
        cams.clear();
    }

    public void clearObservations() {
        reports.clear();
    }

}
