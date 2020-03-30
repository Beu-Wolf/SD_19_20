package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.domain.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.domain.exceptions.NoCameraFoundException;

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

    public Cam getCam(String name) throws NoCameraFoundException {
        Cam cam = cams.get(name);
        if (cam == null) {
            throw new NoCameraFoundException();
        }
        return cam;
    }
}
