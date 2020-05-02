package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.grpc.Gossip;

public class CamJoinCommand extends Command {

    private Cam cam;

    public CamJoinCommand(Silo silo, Cam cam) {
        super(silo);
        this.cam = cam;
    }

    public CamJoinCommand(Silo silo, Gossip.CamJoinCommand command) throws SiloException {
        super(silo);
        this.cam = camFromGRPC(command.getRequest().getCam());

    }

    @Override
    public void execute() throws SiloException{
        this.silo.registerCam(this.cam);
    }

    @Override
    public Gossip.Record commandToGRPC(Gossip.Record record) {
        pt.tecnico.sauron.silo.grpc.Silo.JoinRequest joinRequest = pt.tecnico.sauron.silo.grpc.Silo.JoinRequest.newBuilder().setCam(camToGRPC(this.cam)).build();
        Gossip.CamJoinCommand camJoinCommand = Gossip.CamJoinCommand.newBuilder().setRequest(joinRequest).build();
        return Gossip.Record.newBuilder(record).setCamJoin(camJoinCommand).build();
    }

    @Override
    public String toString() {
        return "CamJoinCommand{" +
                "cam=" + cam +
                '}';
    }
}
