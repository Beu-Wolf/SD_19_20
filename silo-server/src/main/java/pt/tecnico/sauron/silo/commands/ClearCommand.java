package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.grpc.Gossip;

public class ClearCommand extends Command {

    public ClearCommand(Silo silo) {
        super(silo);
    }

    @Override
    public void execute() {
        this.silo.clearCams();
        this.silo.clearObservations();
    }

    @Override
    public Gossip.Record commandToGRPC(Gossip.Record record) {
        return Gossip.Record.newBuilder(record)
                .setClear(Gossip.ClearCommand.getDefaultInstance())
                .build();
    }
}
