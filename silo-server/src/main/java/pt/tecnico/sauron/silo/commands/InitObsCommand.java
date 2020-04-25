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

    public InitObsCommand(Silo silo, Gossip.InitObservationsCommand command) {
        super(silo);
        LinkedList<Report> newReports = new LinkedList<>();
        for (Gossip.InitObservationItem item: command.getRequest().getItemList()) {

        }

    }

    @Override
    public void execute() {
        for (Report r: this.reports) {
            silo.recordReport(r);
        }
    }

    @Override
    public Gossip.Record commandToGRPC(Gossip.Record record) {
        LinkedList<Gossip.InitObservationItem> siloInitItem = new LinkedList<>();
        try {
            for (Report r: this.reports) {
                siloInitItem.add(Gossip.InitObservationItem.newBuilder()
                .setCam(camToGRPC(r.getCam()))
                .setObservation(observationToGRPC(r.getObservation()))
                .setTimestamp(timestampToGRPC(r.getTimestamp()))
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
