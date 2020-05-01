package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Report;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.grpc.Gossip;

import java.time.Instant;
import java.util.LinkedList;

public class InitObsCommand extends Command {
    private LinkedList<Report> reports;

    public InitObsCommand(Silo silo, LinkedList<Report> reports) {
        super(silo);
        this.reports = reports;
    }

    public InitObsCommand(Silo silo, Gossip.InitObservationsCommand command) throws SiloException{
        super(silo);
        LinkedList<Report> newReports = new LinkedList<>();
        for (pt.tecnico.sauron.silo.grpc.Silo.InitObservationsItem item: command.getRequest().getObservationsList()) {
            newReports.add(reportFromGRPC(item));
        }
        this.reports = newReports;
    }

    @Override
    public void execute() {
        for (Report r: this.reports) {
            silo.recordReport(r);
        }
    }

    @Override
    public Gossip.Record commandToGRPC(Gossip.Record record) {
        LinkedList<pt.tecnico.sauron.silo.grpc.Silo.InitObservationsItem> siloInitItem = new LinkedList<>();
        try {
            for (Report r: this.reports) {
                siloInitItem.add(pt.tecnico.sauron.silo.grpc.Silo.InitObservationsItem.newBuilder()
                .setCam(camToGRPC(r.getCam()))
                .setObservation(observationToGRPC(r.getObservation()))
                .setTimestamp(timestampToGRPC(r.getTimestamp()))
                .build());
            }

        } catch (SiloInvalidArgumentException e) {
            System.out.println(e.getMessage());
        }

        pt.tecnico.sauron.silo.grpc.Silo.InitObservationsRequest initObservationsRequest = pt.tecnico.sauron.silo.grpc.Silo.InitObservationsRequest.newBuilder()
                .addAllObservations(siloInitItem)
                .build();

        Gossip.InitObservationsCommand reportCommand = Gossip.InitObservationsCommand.newBuilder()
                .setRequest(initObservationsRequest)
                .build();

        return Gossip.Record.newBuilder(record).setInitObservations(reportCommand).build();

    }

    @Override
    public String toString() {
        return "InitObsCommand{" +
                "reports=" + reports +
                '}';
    }
}
