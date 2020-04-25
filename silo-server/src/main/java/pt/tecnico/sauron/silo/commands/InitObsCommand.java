package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Observation;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.SiloInvalidArgumentException;
import pt.tecnico.sauron.silo.grpc.Gossip;

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

    @Override
    public Gossip.Record commandToGRPC(Gossip.Record record) {
        LinkedList<Gossip.InitObservationItem> siloInitItem = new LinkedList<>();
        try {
            for (Observation o: this.obs) {
                siloInitItem.add(Gossip.InitObservationItem.newBuilder()
                .setCam(camToGRPC(this.cam))
                .setObservation(observationToGRPC(o))
                .setTimestamp(timestampToGRPC(this.observationInstant))
                .build());
            }

        } catch (SiloInvalidArgumentException e) {
            System.out.println(e.getMessage());
        }

        Gossip.InitObservationsRequest initObservationsRequest = Gossip.InitObservationsRequest.newBuilder()
                .addAllItem(siloInitItem)
                .build();

        Gossip.InitObservationsCommand reportCommand = Gossip.InitObservationsCommand.newBuilder()
                .setRequest(initObservationsRequest)
                .build();

        return Gossip.Record.newBuilder(record).setInitObservations(reportCommand).build();

    }
}
