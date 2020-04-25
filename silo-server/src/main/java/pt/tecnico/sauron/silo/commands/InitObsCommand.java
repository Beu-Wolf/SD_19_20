package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Observation;
import pt.tecnico.sauron.silo.domain.Silo;

import java.time.Instant;
import java.util.LinkedList;

public class InitObsCommand extends Command {
    private Cam cam;
    private LinkedList<Observation> obs;
    private Instant observationInstant;

    public InitObsCommand(Silo silo, Cam cam, LinkedList<Observation> obs, Instant observationInstant) {
        super(silo);
        this.cam = cam;
        this.obs = obs;
        this.observationInstant = observationInstant;
    }

    public void addObs(Observation o) {
        obs.add(o);
    }

    @Override
    public void execute() {
        for (Observation o : this.obs) {
            silo.registerGossipObservation(this.cam, o, observationInstant);
        }
    }
}
