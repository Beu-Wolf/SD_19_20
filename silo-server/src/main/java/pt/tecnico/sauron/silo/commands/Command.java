package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Silo;

public abstract class Command {
    protected Silo silo;

    public Command(Silo silo) { this.silo = silo; }

    public abstract void execute();


}
