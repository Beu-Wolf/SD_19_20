package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Silo;

public class ClearCommand extends Command {

    public ClearCommand(Silo silo) {
        super(silo);
    }

    @Override
    public void execute() {
        this.silo.clearCams();
        this.silo.clearObservations();
    }
}
