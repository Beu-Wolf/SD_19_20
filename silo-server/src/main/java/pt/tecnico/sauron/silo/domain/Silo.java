package pt.tecnico.sauron.silo.domain;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Silo {
    private static LinkedList<Report> reports = new LinkedList<>();
    private static Map<String, Cam> cams = new ConcurrentHashMap<>();

    public Silo() {}

    public void registerCam(Cam cam) {
        cams.put(cam.getName(), cam);
    }

    public static synchronized void registerObservation(Report report) {
        reports.addFirst(report);
    }

    public static Cam getCam(String name) {
        return cams.get(name);
    }
}
