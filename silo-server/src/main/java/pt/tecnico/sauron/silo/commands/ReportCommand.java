package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Observation;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.grpc.Gossip;

import java.time.Instant;
import java.util.LinkedList;

public class ReportCommand extends Command {

    private Cam cam;
    private LinkedList<Observation> obs;
    private Instant observationInstant;

    public ReportCommand(Silo silo, Cam cam, LinkedList<Observation> obs, Instant observationInstant) {
        super(silo);
        this.cam = cam;
        this.obs = obs;
        this.observationInstant = observationInstant;
    }

    public ReportCommand( Silo silo, Gossip.ReportCommand reportCommand) {
        super(silo);
        try {
            Cam camObj = camFromGRPC(reportCommand.getRequest().getCam());
            LinkedList<Observation> obs = new LinkedList<>();
            for (pt.tecnico.sauron.silo.grpc.Silo.Observation o : reportCommand.getRequest().getObservationsList()) {
                obs.add(observationFromGRPC(o));
            }
            Instant instant = timestampFromGRPC(reportCommand.getObservationInstant());
            this.cam = camObj;
            this.obs = obs;
            this.observationInstant = instant;
        } catch (InvalidCameraNameException | EmptyCameraNameException | InvalidCarIdException | InvalidPersonIdException | TypeNotSupportedException e) {
            System.out.println(e.getMessage());
        }
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
        LinkedList<pt.tecnico.sauron.silo.grpc.Silo.Observation> siloObs = new LinkedList<>();
        try {
            for (Observation o: this.obs) {
                siloObs.add(observationToGRPC(o));
            }
        } catch (SiloInvalidArgumentException e) {
            System.out.println(e.getMessage());
        }

        Gossip.ReportRequest reportRequest = Gossip.ReportRequest.newBuilder().
                setCam(camToGRPC(this.cam))
                .addAllObservations(siloObs)
                .build();

        Gossip.ReportCommand reportCommand = Gossip.ReportCommand.newBuilder()
                .setObservationInstant(timestampToGRPC(this.observationInstant))
                .setRequest(reportRequest)
                .build();

        return Gossip.Record.newBuilder(record).setReport(reportCommand).build();

    }
}
